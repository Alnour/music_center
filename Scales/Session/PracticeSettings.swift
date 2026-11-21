// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// What goes inside each note circle.
enum NoteLabelStyle: String, CaseIterable, Codable, Identifiable, Sendable {
    case noteNames
    case degrees
    case none

    var id: String { rawValue }

    var title: String {
        switch self {
        case .noteNames: "Notes"
        case .degrees: "Degrees"
        case .none: "Dots"
        }
    }

    /// What the circles on the board show, read by VoiceOver (and TalkBack on Android) as
    /// the board's value, so a player who cannot see the board still hears what it holds.
    var accessibilityDescription: String {
        switch self {
        case .noteNames: "Circles show note names"
        case .degrees: "Circles show scale degrees"
        case .none: "Plain circles"
        }
    }
}

/// Everything the player chooses, saved between launches so the app reopens on the
/// scale they were last practising.
struct PracticeSettings: Codable, Equatable, Sendable {
    var root = NoteName(.g)
    /// The built-in scale, and the one the board falls back to if a saved scale is deleted.
    var scaleType: ScaleType = .major
    var tuning: Tuning = .standard
    var fretCount = 15
    var labelStyle: NoteLabelStyle = .noteNames
    var isLeftHanded = false
    /// Matches the reference chart and tab notation: the thin high E string at the top.
    var thinStringOnTop = true
    /// A dark board is easier on the eyes on a big TV in a dim room.
    var tvUsesDarkTheme = true
    /// One of the player's own scales, shown instead of `scaleType` while it is set.
    ///
    /// A full copy is kept rather than an id, for three reasons: settings saved before
    /// custom scales existed still load (the field is optional, so a missing key is fine);
    /// the TV clip cache, keyed by these settings, makes a new clip whenever the scale's
    /// notes or name change; and the TV screen can draw the board without the library.
    var customScale: CustomScale?
    /// Which board is drawn. Optional and nil for the guitar, so settings saved before the
    /// keyboard existed still load, a guitar player's saved JSON is unchanged, and the
    /// default settings still match the prebuilt G major clip (`ClipCache.bundledShortClip`).
    var instrument: Instrument?
    /// How many octaves the keyboard shows. Nil for the default of two, the same way
    /// `instrument` is nil for the guitar, so a default player's saved JSON and clip key
    /// never change.
    var keyboardOctaves: Int?
    /// The fret the capo is on. Nil for no capo, the same way `instrument` is nil for the
    /// guitar, so a player who never uses one keeps the same saved JSON and clip key.
    var capoFret: Int?
    /// How the bass guitar is tuned. The bass keeps its own tuning because the guitar's
    /// six-string tunings (DADGAD, open G) have no four-string meaning. Nil for standard,
    /// the same way `instrument` is nil for the guitar, so a player who never picks the
    /// bass keeps the same saved JSON and clip key.
    var bassTuning: BassTuning?

    /// Two octaves by default: a scale then shows its root twice and every chord shape
    /// fits without wrapping back to the start of the keyboard.
    static let defaultKeyboardOctaves = 2

    static let fretCountChoices = [12, 15, 17, 22, 24]
    static let keyboardOctaveChoices = [1, 2]

    /// The instrument as the pickers see it; the guitar is stored as nil (see `instrument`).
    var selectedInstrument: Instrument {
        get { instrument ?? .guitar }
        set { instrument = newValue == .guitar ? nil : newValue }
    }

    /// The octave count as the pickers see it; the default of two is stored as nil.
    var selectedKeyboardOctaves: Int {
        get { keyboardOctaves ?? Self.defaultKeyboardOctaves }
        set { keyboardOctaves = newValue == Self.defaultKeyboardOctaves ? nil : newValue }
    }

    /// The capo's fret as the pickers see it; no capo is 0 and is stored as nil.
    var selectedCapoFret: Int {
        get { capoFret ?? 0 }
        set { capoFret = newValue == 0 ? nil : newValue }
    }

    /// The bass guitar's tuning as the pickers see it; standard is stored as nil.
    var selectedBassTuning: BassTuning {
        get { bassTuning ?? .standard }
        set { bassTuning = newValue == .standard ? nil : newValue }
    }

    /// The capo as the guitar's board draws it. The keyboard has no capo and bass players
    /// do not use one, so a fret left set from the guitar is ignored on those boards.
    var boardCapoFret: Int {
        selectedInstrument == .guitar ? selectedCapoFret : 0
    }

    /// The open strings of the neck on the board, thickest first: the bass guitar's own
    /// tuning while the bass is chosen, the guitar's tuning otherwise.
    var boardOpenStrings: [NoteName] {
        switch selectedInstrument {
        case .bass: selectedBassTuning.openStrings
        case .guitar, .keyboard: tuning.openStrings
        }
    }

    /// How to rename the scale's chords as the shapes fingered with the capo on, or nil
    /// when there is nothing to rename: the keyboard, the bass, no capo, or a tuning whose chord
    /// shapes are not the standard ones.
    func capoTransposition(for scale: Scale) -> CapoTransposition? {
        CapoTransposition(scale: scale, capoFret: boardCapoFret, tuning: tuning)
    }

    /// The small caption beside the board's title on the TV: the tuning, and the capo's
    /// fret when one is on. The tuning matters on a guitar, not on a piano. The bass names
    /// itself first, because its neck could be mistaken for a guitar's from across a room.
    var boardCaption: String {
        switch selectedInstrument {
        case .keyboard:
            return Instrument.keyboard.title
        case .bass:
            return "\(Instrument.bass.title) · \(selectedBassTuning.shortName)"
        case .guitar:
            if boardCapoFret > 0 {
                return "\(tuning.shortName) · Capo \(boardCapoFret)"
            }
            return tuning.shortName
        }
    }

    /// The scale on the board, for pickers to select.
    var scaleReference: ScaleReference {
        if let customScale {
            return .custom(customScale.id)
        }
        return .builtIn(scaleType)
    }

    /// The name and formula of the scale on the board.
    var scaleDefinition: ScaleDefinition {
        customScale?.definition ?? scaleType.definition
    }
}
