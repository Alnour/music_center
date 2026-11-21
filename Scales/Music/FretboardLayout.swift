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
    /// The fret the capo is on, 0 for no capo. Always below `fretCount`.
    let capoFret: Int
    /// Thickest string first, matching `Tuning.openStrings`.
    let strings: [GuitarString]

    /// The guitar's neck in one of its six-string tunings.
    init(scale: Scale, tuning: Tuning, fretCount: Int, capoFret: Int = 0) {
        self.init(
            scale: scale,
            openStrings: tuning.openStrings,
            fretCount: fretCount,
            capoFret: capoFret
        )
    }

    /// A neck with any set of open strings, thickest first: the guitar's six, or the bass
    /// guitar's four or five (`BassTuning.openStrings`).
    ///
    /// A capo presses every string at its fret, so the frets behind it cannot be played:
    /// they carry no tones, and the tones on the capo's own fret are the new open strings.
    init(scale: Scale, openStrings: [NoteName], fretCount: Int, capoFret: Int = 0) {
        let playableCapoFret = max(0, min(capoFret, fretCount - 1))
        var strings: [GuitarString] = []
        for openNote in openStrings {
            var tonesByFret: [ScaleTone?] = []
            for fret in 0...fretCount {
                if fret < playableCapoFret {
                    tonesByFret.append(nil)
                    continue
                }
                let pitchClass = PitchMath.wrap(openNote.pitchClass + fret)
                tonesByFret.append(scale.tone(forPitchClass: pitchClass))
            }
            strings.append(GuitarString(openNote: openNote, tonesByFret: tonesByFret))
        }
        self.fretCount = fretCount
        self.capoFret = playableCapoFret
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
