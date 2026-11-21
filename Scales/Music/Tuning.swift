// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// Common six-string tunings.
enum Tuning: String, CaseIterable, Codable, Identifiable, Sendable {
    case standard
    case halfStepDown
    case dropD
    case dadgad
    case openG
    case openD

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .standard: "Standard (E A D G B E)"
        case .halfStepDown: "Half Step Down (E♭ A♭ D♭ G♭ B♭ E♭)"
        case .dropD: "Drop D (D A D G B E)"
        case .dadgad: "DADGAD"
        case .openG: "Open G (D G D G B D)"
        case .openD: "Open D (D A D F♯ A D)"
        }
    }

    var shortName: String {
        switch self {
        case .standard: "Standard tuning"
        case .halfStepDown: "Half step down"
        case .dropD: "Drop D"
        case .dadgad: "DADGAD"
        case .openG: "Open G"
        case .openD: "Open D"
        }
    }

    /// How far the tuning sits from standard when its chord shapes are the standard ones:
    /// 0 for standard, -1 a half step down (an E shape then sounds E♭). Nil for the
    /// tunings that change the gaps between strings, where the open chord shapes of the
    /// books no longer apply, so a capo names no shapes there.
    var standardShapeOffset: Int? {
        switch self {
        case .standard: 0
        case .halfStepDown: -1
        case .dropD, .dadgad, .openG, .openD: nil
        }
    }

    /// Open-string notes from the thickest (6th) string to the thinnest (1st).
    var openStrings: [NoteName] {
        switch self {
        case .standard:
            [NoteName(.e), NoteName(.a), NoteName(.d), NoteName(.g), NoteName(.b), NoteName(.e)]
        case .halfStepDown:
            [
                NoteName(.e, -1), NoteName(.a, -1), NoteName(.d, -1),
                NoteName(.g, -1), NoteName(.b, -1), NoteName(.e, -1),
            ]
        case .dropD:
            [NoteName(.d), NoteName(.a), NoteName(.d), NoteName(.g), NoteName(.b), NoteName(.e)]
        case .dadgad:
            [NoteName(.d), NoteName(.a), NoteName(.d), NoteName(.g), NoteName(.a), NoteName(.d)]
        case .openG:
            [NoteName(.d), NoteName(.g), NoteName(.d), NoteName(.g), NoteName(.b), NoteName(.d)]
        case .openD:
            [NoteName(.d), NoteName(.a), NoteName(.d), NoteName(.f, 1), NoteName(.a), NoteName(.d)]
        }
    }
}
