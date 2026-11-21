// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Testing

@testable import Scales

/// The capo: the shapes it gives each chord, the frets it suggests, the neck behind it and
/// the way it is saved.
struct CapoTests {
    private let bFlatMajor = Scale(root: NoteName(.b, -1), type: .major)

    private func shapeCaptions(of scale: Scale, capoFret: Int, tuning: Tuning = .standard)
        -> [String]
    {
        guard let capo = CapoTransposition(scale: scale, capoFret: capoFret, tuning: tuning) else {
            return []
        }
        var captions: [String] = []
        for chord in scale.boardChords() {
            captions.append(capo.shape(for: chord).caption)
        }
        return captions
    }

    /// The textbook case: B♭ major with a capo on the third fret is played with G major's
    /// chords, of which only Bm needs a barre.
    // feature: capo-shapes
    @Test func bFlatMajorOnFretThreeIsFingeredAsGMajor() throws {
        let capo = try #require(
            CapoTransposition(scale: bFlatMajor, capoFret: 3, tuning: .standard)
        )
        #expect(capo.shapeScaleTitle == "G Major")
        #expect(capo.note == "Capo on fret 3: finger the chords as in G Major.")
        let captions = shapeCaptions(of: bFlatMajor, capoFret: 3)
        let expected = [
            "G shape", "Am shape", "Bm barre", "C shape", "D shape", "Em shape", "F♯° shape",
        ]
        #expect(captions == expected)
    }

    /// A black-key shape key takes the spelling with fewer sharps and flats, and every
    /// chord is spelled inside that key.
    // feature: capo-shapes
    @Test func shapesAreSpelledInTheSimplerKey() {
        let dMajor = Scale(root: NoteName(.d), type: .major)
        let captions = shapeCaptions(of: dMajor, capoFret: 1)
        let expected = [
            "D♭ barre", "E♭m barre", "Fm barre", "G♭ barre", "A♭ barre", "B♭m barre",
            "C° shape",
        ]
        #expect(captions == expected)
    }

    /// The seventh chords of a blues have open shapes too: B♭ blues on fret 1 is A7, D7, E7.
    // feature: capo-shapes
    @Test func bluesSeventhChordsFindTheirOpenShapes() throws {
        let bFlatBlues = Scale(root: NoteName(.b, -1), type: .blues)
        let capo = try #require(
            CapoTransposition(scale: bFlatBlues, capoFret: 1, tuning: .standard)
        )
        var names: [String] = []
        for chord in bFlatBlues.boardChords() {
            let shape = capo.shape(for: chord)
            #expect(shape.fingering == .open)
            names.append(shape.name)
        }
        #expect(names == ["A7", "D7", "E7"])
    }

    /// A guitar tuned a half step down already sounds one semitone below its shapes, so
    /// a capo on fret 1 brings the shapes back to the sounding names.
    // feature: capo-shapes
    @Test func halfStepDownCountsOneSemitoneLess() throws {
        let gMajor = Scale(root: NoteName(.g), type: .major)
        let capo = try #require(
            CapoTransposition(scale: gMajor, capoFret: 1, tuning: .halfStepDown)
        )
        #expect(capo.semitonesDown == 0)
        #expect(capo.shapeScaleTitle == "G Major")
    }

    /// No capo, or a tuning that changes the gaps between strings, names no shapes.
    // feature: capo-shapes
    @Test func noShapesWithoutACapoOrInAnOpenTuning() {
        #expect(CapoTransposition(scale: bFlatMajor, capoFret: 0, tuning: .standard) == nil)
        #expect(CapoTransposition(scale: bFlatMajor, capoFret: 3, tuning: .dropD) == nil)
        #expect(Capo.suggestions(for: bFlatMajor, tuning: .openG).isEmpty)
        #expect(Capo.advice(for: bFlatMajor, tuning: .openG) == Capo.unsupportedTuningAdvice)
    }

    // feature: capo-shapes
    @Test func suggestionsCountOpenChordsPerFret() {
        let suggestions = Capo.suggestions(for: bFlatMajor, tuning: .standard)
        #expect(suggestions.count == Capo.fretChoices.count)
        #expect(suggestions[0].label == "No capo · 1 of 7 open")
        #expect(suggestions[3].label == "Fret 3 · as G Major · 5 of 7 open")
        let advice = Capo.advice(for: bFlatMajor, tuning: .standard)
        #expect(advice == "Most open chords: capo on fret 3 (as G Major).")
    }

    // feature: capo-shapes
    @Test func keysThatAreAlreadyOpenNeedNoCapo() {
        let gMajor = Scale(root: NoteName(.g), type: .major)
        let advice = Capo.advice(for: gMajor, tuning: .standard)
        #expect(advice == "No capo already gives the most open chords.")
    }

    /// Frets behind the capo cannot be played, and the capo's own fret is the new open string.
    // feature: capo-shapes
    @Test func neckIsEmptyBehindTheCapo() {
        let layout = FretboardLayout(
            scale: Scale(root: NoteName(.g), type: .major),
            tuning: .standard,
            fretCount: 15,
            capoFret: 3
        )
        #expect(layout.capoFret == 3)
        #expect(layout.frets(withToneOnString: 0) == [3, 5, 7, 8, 10, 12, 14, 15])
    }

    // feature: capo-shapes
    @Test func chordCardsCarryTheShapeAndSayIt() throws {
        let capo = try #require(
            CapoTransposition(scale: bFlatMajor, capoFret: 3, tuning: .standard)
        )
        let chips = BoardChordChipContent.chips(for: bFlatMajor, change: nil, capo: capo)
        #expect(chips[0].capoShape?.caption == "G shape")
        let spoken = "I, B♭ major: B♭, D, F. Fingered as G major, an open chord."
        #expect(chips[0].accessibilityLabel == spoken)
        let plainChips = BoardChordChipContent.chips(for: bFlatMajor, change: nil)
        #expect(plainChips[0].capoShape == nil)
        #expect(plainChips[0].accessibilityLabel == "I, B♭ major: B♭, D, F")
    }

    /// No capo is stored as a missing key, so a player who never uses one keeps the saved
    /// settings and the prebuilt TV clip they had before the capo existed.
    // feature: capo-shapes, settings-persist
    @Test func capoIsSavedOnlyWhenOn() throws {
        var settings = PracticeSettings()
        settings.selectedCapoFret = 3
        #expect(settings.boardCaption == "Standard tuning · Capo 3")
        let saved = try JSONEncoder().encode(settings)
        let reloaded = try JSONDecoder().decode(PracticeSettings.self, from: saved)
        #expect(reloaded.selectedCapoFret == 3)

        settings.selectedCapoFret = 0
        #expect(settings == PracticeSettings())
        let plain = try JSONEncoder().encode(settings)
        let text = try #require(String(data: plain, encoding: .utf8))
        #expect(!text.contains("capoFret"))
    }

    /// The keyboard has no capo: a fret left over from the guitar is ignored there.
    // feature: capo-shapes
    @Test func keyboardIgnoresTheCapo() {
        var settings = PracticeSettings()
        settings.selectedCapoFret = 3
        settings.selectedInstrument = .keyboard
        #expect(settings.boardCapoFret == 0)
        #expect(settings.capoTransposition(for: bFlatMajor) == nil)
        #expect(settings.boardCaption == "Keyboard")
    }
}
