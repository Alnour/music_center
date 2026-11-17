// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// Arithmetic on pitch classes: the twelve semitone positions of the octave, numbered
/// 0 (C) through 11 (B). Octaves don't matter for scale membership, so every
/// calculation here wraps around twelve.
enum PitchMath {
    static let semitonesPerOctave = 12

    /// Folds any semitone count into the 0...11 range, including negative counts.
    static func wrap(_ semitones: Int) -> Int {
        let remainder = semitones % semitonesPerOctave
        return remainder < 0 ? remainder + semitonesPerOctave : remainder
    }

    /// The shortest signed step from one pitch class to another, in -5...6.
    /// Used to turn "this letter needs to land on that pitch" into a sharp or flat count.
    static func signedDistance(from start: Int, to end: Int) -> Int {
        let upward = wrap(end - start)
        return upward > semitonesPerOctave / 2 ? upward - semitonesPerOctave : upward
    }
}
