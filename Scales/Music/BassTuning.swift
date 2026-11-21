// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// Common bass guitar tunings.
///
/// The bass has its own list rather than sharing `Tuning`: a four-string bass is tuned
/// like the guitar's four thickest strings, so only the guitar tunings that keep those
/// strings a fourth apart (standard, half step down) or drop the lowest one (drop D) are
/// used on it, and the five-string bass adds a low B the guitar never has.
enum BassTuning: String, CaseIterable, Codable, Identifiable, Sendable {
    case standard
    case halfStepDown
    case dropD
    case fiveString

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .standard: "Standard (E A D G)"
        case .halfStepDown: "Half Step Down (E♭ A♭ D♭ G♭)"
        case .dropD: "Drop D (D A D G)"
        case .fiveString: "Five-string (B E A D G)"
        }
    }

    var shortName: String {
        switch self {
        case .standard: "Standard tuning"
        case .halfStepDown: "Half step down"
        case .dropD: "Drop D"
        case .fiveString: "Five-string"
        }
    }

    /// Open-string notes from the thickest string to the thinnest.
    var openStrings: [NoteName] {
        switch self {
        case .standard:
            [NoteName(.e), NoteName(.a), NoteName(.d), NoteName(.g)]
        case .halfStepDown:
            [NoteName(.e, -1), NoteName(.a, -1), NoteName(.d, -1), NoteName(.g, -1)]
        case .dropD:
            [NoteName(.d), NoteName(.a), NoteName(.d), NoteName(.g)]
        case .fiveString:
            [NoteName(.b), NoteName(.e), NoteName(.a), NoteName(.d), NoteName(.g)]
        }
    }
}
