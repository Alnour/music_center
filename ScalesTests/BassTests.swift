// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Testing

@testable import Scales

/// The bass guitar: its strings, its own tuning, the neck it draws and the way it is saved.
struct BassTests {
    private let gMajor = Scale(root: NoteName(.g), type: .major)

    private func openNoteNames(of settings: PracticeSettings) -> [String] {
        var names: [String] = []
        for note in settings.boardOpenStrings {
            names.append(note.displayName)
        }
        return names
    }

    // feature: bass
    @Test func theBassHasTheGuitarsFourThickestStrings() {
        var settings = PracticeSettings()
        settings.selectedInstrument = .bass
        #expect(openNoteNames(of: settings) == ["E", "A", "D", "G"])

        let guitarStrings = Tuning.standard.openStrings
        let thickestFour = Array(guitarStrings.prefix(4))
        #expect(BassTuning.standard.openStrings == thickestFour)
    }

    // feature: bass
    @Test func everyBassTuningSpellsItsStrings() {
        var spelled: [BassTuning: [String]] = [:]
        for tuning in BassTuning.allCases {
            var names: [String] = []
            for note in tuning.openStrings {
                names.append(note.displayName)
            }
            spelled[tuning] = names
        }
        #expect(spelled[.standard] == ["E", "A", "D", "G"])
        #expect(spelled[.halfStepDown] == ["E♭", "A♭", "D♭", "G♭"])
        #expect(spelled[.dropD] == ["D", "A", "D", "G"])
        #expect(spelled[.fiveString] == ["B", "E", "A", "D", "G"])
    }

    /// G major on the standard bass: the same frets as the guitar's four thickest strings.
    // feature: bass
    @Test func theBassNeckCarriesTheScaleOnFourStrings() {
        let bass = FretboardLayout(
            scale: gMajor,
            openStrings: BassTuning.standard.openStrings,
            fretCount: 12
        )
        let guitar = FretboardLayout(scale: gMajor, tuning: .standard, fretCount: 12)
        #expect(bass.strings.count == 4)
        #expect(bass.frets(withToneOnString: 0) == [0, 2, 3, 5, 7, 8, 10, 12])
        for stringIndex in 0..<4 {
            let bassFrets = bass.frets(withToneOnString: stringIndex)
            let guitarFrets = guitar.frets(withToneOnString: stringIndex)
            #expect(bassFrets == guitarFrets)
        }
    }

    /// The bass keeps its own tuning, so the guitar's is untouched when the player
    /// switches back.
    // feature: bass
    @Test func theBassTuningLeavesTheGuitarTuningAlone() {
        var settings = PracticeSettings()
        settings.tuning = .dadgad
        settings.selectedInstrument = .bass
        settings.selectedBassTuning = .dropD
        #expect(openNoteNames(of: settings) == ["D", "A", "D", "G"])

        settings.selectedInstrument = .guitar
        #expect(openNoteNames(of: settings) == ["D", "A", "D", "G", "A", "D"])
    }

    // feature: bass
    @Test func theCaptionNamesTheBassAndItsTuning() {
        var settings = PracticeSettings()
        settings.selectedInstrument = .bass
        #expect(settings.boardCaption == "Bass · Standard tuning")
        settings.selectedBassTuning = .fiveString
        #expect(settings.boardCaption == "Bass · Five-string")
    }

    /// Bass players do not use a capo: a fret left over from the guitar is ignored.
    // feature: bass, capo-shapes
    @Test func theBassIgnoresTheCapo() {
        var settings = PracticeSettings()
        settings.selectedCapoFret = 3
        settings.selectedInstrument = .bass
        #expect(settings.boardCapoFret == 0)
        #expect(settings.capoTransposition(for: gMajor) == nil)
    }

    // feature: bass, settings-persist
    @Test func theBassTuningIsSavedOnlyWhenItIsNotStandard() throws {
        var settings = PracticeSettings()
        settings.selectedInstrument = .bass
        settings.selectedBassTuning = .dropD
        let data = try JSONEncoder().encode(settings)
        let reloaded = try JSONDecoder().decode(PracticeSettings.self, from: data)
        #expect(reloaded.selectedInstrument == .bass)
        #expect(reloaded.selectedBassTuning == .dropD)

        settings.selectedBassTuning = .standard
        settings.selectedInstrument = .guitar
        #expect(settings == PracticeSettings(), "the defaults store nothing")
        let plain = try JSONEncoder().encode(settings)
        let text = try #require(String(data: plain, encoding: .utf8))
        #expect(!text.contains("bassTuning"))
    }

    // feature: bass, airplay-cast
    @Test func theBassGetsItsOwnTVClip() {
        var bass = PracticeSettings()
        bass.selectedInstrument = .bass
        var fiveString = bass
        fiveString.selectedBassTuning = .fiveString

        #expect(ClipCache.key(for: bass) != ClipCache.key(for: PracticeSettings()))
        #expect(ClipCache.key(for: fiveString) != ClipCache.key(for: bass))
        #expect(ClipCache.bundledShortClip(for: bass) == nil)
    }
}
