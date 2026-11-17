// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// Which scale tone (if any) sits at every string and fret, ready to be drawn.
///
/// The layout is pure data so the phone screen and the TV screen draw exactly the same
/// board, and so tests can check note positions without rendering anything.
struct FretboardLayout: Sendable {
    struct GuitarString: Sendable {
        let openNote: NoteName
        /// Indexed by fret number; index 0 is the open string.
        let tonesByFret: [ScaleTone?]
    }

    let fretCount: Int
    /// Thickest string first, matching `Tuning.openStrings`.
    let strings: [GuitarString]

    init(scale: Scale, tuning: Tuning, fretCount: Int) {
        var strings: [GuitarString] = []
        for openNote in tuning.openStrings {
            var tonesByFret: [ScaleTone?] = []
            for fret in 0...fretCount {
                let pitchClass = PitchMath.wrap(openNote.pitchClass + fret)
                tonesByFret.append(scale.tone(forPitchClass: pitchClass))
            }
            strings.append(GuitarString(openNote: openNote, tonesByFret: tonesByFret))
        }
        self.fretCount = fretCount
        self.strings = strings
    }

    /// Fret numbers that carry a scale tone on the given string (0 is the open string).
    func frets(withToneOnString stringIndex: Int) -> [Int] {
        var frets: [Int] = []
        for (fret, tone) in strings[stringIndex].tonesByFret.enumerated() where tone != nil {
            frets.append(fret)
        }
        return frets
    }
}
