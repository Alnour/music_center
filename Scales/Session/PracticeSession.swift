// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Observation

/// The single source of truth shared by the phone scene and the TV scene.
///
/// Both scenes live in the same process, so when the player changes the scale on the
/// phone the TV redraws immediately; no networking is involved.
@MainActor
@Observable
final class PracticeSession {
    // The app's one session, `PracticeSession.shared`, is declared in
    // `App/SharedSession.swift`: it needs the AirPlay caster, which this file must not see.

    var settings: PracticeSettings {
        didSet { save() }
    }

    /// True while iOS has given the app its own screen on the TV through AirPlay.
    var isTVConnected = false

    /// True while the phone's screen is being mirrored or recorded. With AirPlay this
    /// turns on a moment before iOS offers the app the TV screen; if the TV screen never
    /// arrives, the TV is showing a plain copy of the phone.
    var isPhoneMirrored = false

    /// Sends the board to a TV when the player picks one in the app. The app passes
    /// `AirPlayCaster.shared`; the session only reads what the TV shows from it, so tests and
    /// the answers generator use `NoCaster` and never start AirPlay.
    let caster: any BoardCaster

    /// The last note change made from the Scales screen. Kept only for this run, so the
    /// chords under the board can show what the change did until the player picks another
    /// scale or key (see `currentBoardChange`).
    var boardChange: BoardChange?

    /// The chord whose notes are ringed on the board, picked from the chords under it.
    /// Kept for this run only; `ringedNotes` ignores it once the scale no longer holds
    /// all its notes, so the rings vanish with the chord instead of pointing at nothing.
    var ringedChord: ScaleChord?

    /// The notes to ring on the board: the picked chord's notes, while every one of them
    /// is in the scale as spelled now.
    var ringedNotes: Set<NoteName> {
        guard let ringedChord else { return [] }
        var scaleNotes = Set<NoteName>()
        for tone in scale.tones {
            scaleNotes.insert(tone.name)
        }
        let chordNotes = Set(ringedChord.notes)
        guard chordNotes.isSubset(of: scaleNotes) else { return [] }
        return chordNotes
    }

    /// Rings a chord's notes on the board, or clears the rings when the same chord is
    /// picked again.
    func toggleRing(for chord: ScaleChord) {
        if ringedChord == chord {
            ringedChord = nil
        } else {
            ringedChord = chord
        }
    }

    var tvStatus: TVStatus {
        if isTVConnected { return .showingBoard }
        if let error = caster.playbackError { return .castFailed(reason: error) }
        if caster.isPlayingOnTV { return .castingVideo(receiver: caster.receiverName) }
        if let receiver = caster.receiverName { return .castInterrupted(receiver: receiver) }
        if isPhoneMirrored { return .mirroringPhone }
        return .disconnected
    }

    @ObservationIgnored private let defaults: UserDefaults
    private static let settingsKey = "practiceSettings"

    init(defaults: UserDefaults, caster: any BoardCaster = NoCaster()) {
        self.defaults = defaults
        self.caster = caster
        self.settings = PracticeSession.loadSettings(from: defaults)
    }

    var scale: Scale {
        Scale(root: settings.root, definition: settings.scaleDefinition)
    }

    /// Puts a scale on the board, in a new key when one is given. A built-in scale clears
    /// any saved scale; a saved scale is copied in from the library, and an id the library
    /// doesn't know changes nothing. Settings are written once, so the TV redraws once.
    func choose(_ reference: ScaleReference, root: NoteName? = nil, from library: ScaleLibrary) {
        var updated = settings
        if let root {
            updated.root = root
        }
        switch reference {
        case .builtIn(let type):
            updated.scaleType = type
            updated.customScale = nil
        case .custom(let id):
            guard let customScale = library.scale(id: id) else { return }
            updated.customScale = customScale
        }
        settings = updated
    }

    /// The last board change while the board still shows its result; nil once the player
    /// has moved on to another scale or key, when there is nothing left to compare against.
    var currentBoardChange: BoardChange? {
        guard let boardChange else { return nil }
        guard boardChange.after == settings.scaleReference else { return nil }
        guard boardChange.transformation.root == settings.root else { return nil }
        return boardChange
    }

    /// Changes one note of the scale on the board and puts the result on the board.
    ///
    /// The change is kept as a lesson so the Transform screen can explain it later. A result
    /// the app knows takes that scale's name; a new one is saved to My Scales under its
    /// working name ("Major ♭7"), which the player can rename there. An edit the scale
    /// does not allow (the menus only offer legal ones) changes nothing.
    ///
    /// - Throws: `couldNotSave` when the library file can't be written.
    func transform(_ edit: ScaleEdit, from library: ScaleLibrary) throws(ScaleLibraryError) {
        guard
            let transformation = try? ScaleTransformation(
                root: settings.root,
                before: settings.scaleDefinition,
                edit: edit
            )
        else { return }
        let lesson = try library.saveLesson(
            transformation,
            newScaleName: transformation.after.name
        )
        let before = settings.scaleReference
        choose(lesson.result, from: library)
        boardChange = BoardChange(
            transformation: lesson.transformation,
            before: before,
            after: lesson.result
        )
    }

    /// Puts back the scale the last board change started from. The lesson it made stays.
    func revertBoardChange(from library: ScaleLibrary) {
        guard let change = currentBoardChange else { return }
        choose(change.before, from: library)
        boardChange = nil
    }

    /// Keeps the board's copy of a saved scale in step with the library: a renamed scale
    /// takes its new name, and a deleted one gives way to the built-in scale chosen before.
    func refreshCustomScale(from library: ScaleLibrary) {
        guard let current = settings.customScale else { return }
        guard let stored = library.scale(id: current.id) else {
            settings.customScale = nil
            return
        }
        if stored != current {
            settings.customScale = stored
        }
    }

    var layout: FretboardLayout {
        FretboardLayout(scale: scale, tuning: settings.tuning, fretCount: settings.fretCount)
    }

    var keyboardLayout: KeyboardLayout {
        KeyboardLayout(scale: scale, octaveCount: settings.selectedKeyboardOctaves)
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: Self.settingsKey)
    }

    /// Falls back to defaults when nothing is stored yet or the stored shape is outdated.
    private static func loadSettings(from defaults: UserDefaults) -> PracticeSettings {
        guard let data = defaults.data(forKey: settingsKey) else { return PracticeSettings() }
        let decoded = try? JSONDecoder().decode(PracticeSettings.self, from: data)
        return decoded ?? PracticeSettings()
    }
}

/// One note change made to the scale on the board, with where it started and ended.
struct BoardChange: Equatable, Sendable {
    let transformation: ScaleTransformation
    /// The scale on the board before the change, to go back to.
    let before: ScaleReference
    /// The scale the change produced, which the board shows now.
    let after: ScaleReference
}

/// What the TV is showing, as far as the app can tell.
enum TVStatus: Equatable, Sendable {
    /// No mirroring session is running.
    case disconnected
    /// The phone is being mirrored but iOS kept copying the phone screen to the TV.
    case mirroringPhone
    /// The app owns the TV screen and is drawing the large board there.
    case showingBoard
    /// The board clip is playing on a TV picked from the in-app AirPlay menu.
    case castingVideo(receiver: String?)
    /// The TV accepted the connection but could not play the clip.
    case castFailed(reason: String)
    /// A TV is still chosen in the AirPlay menu but it stopped playing the board.
    case castInterrupted(receiver: String)
}
