// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// One row of a key table: the key and the chords that belong to it.
struct KeyChordRow: Identifiable, Sendable {
    let key: Scale
    /// The seven chords built from the key's own scale.
    let chords: [DiatonicChord]
    /// "major" or "minor", the word musicians put after the key's root.
    let familyName: String

    var id: String { key.title }

    /// The way musicians say the key: "C major", "A minor".
    var keyName: String {
        "\(key.root.displayName) \(familyName)"
    }
}

/// The major and minor key tables, one row per key, sorted by key name.
///
/// Rows run alphabetically from A to G so a player can find a key by name at a glance;
/// within a letter the flat comes first, then the natural, then the sharp (A♭, A, A♯),
/// which is also their order in pitch. The twelve keys are the ones with the usual
/// spelling (D♭ major rather than C♯ major, G♯ minor rather than A♭ minor).
enum KeyChordTable {
    static let majorKeyRoots: [NoteName] = [
        NoteName(.a, -1), NoteName(.a), NoteName(.b, -1), NoteName(.b), NoteName(.c),
        NoteName(.d, -1), NoteName(.d), NoteName(.e, -1), NoteName(.e), NoteName(.f),
        NoteName(.f, 1), NoteName(.g),
    ]

    static let minorKeyRoots: [NoteName] = [
        NoteName(.a), NoteName(.b, -1), NoteName(.b), NoteName(.c), NoteName(.c, 1),
        NoteName(.d), NoteName(.d, 1), NoteName(.e), NoteName(.f), NoteName(.f, 1),
        NoteName(.g), NoteName(.g, 1),
    ]

    static func majorRows() -> [KeyChordRow] {
        var rows: [KeyChordRow] = []
        for root in majorKeyRoots {
            let key = Scale(root: root, type: .major)
            let row = KeyChordRow(
                key: key,
                chords: key.diatonicTriads(),
                familyName: "major"
            )
            rows.append(row)
        }
        return rows
    }

    /// Minor keys use the natural minor scale for their seven chords, the standard listing
    /// (i ii° III iv v VI VII); harmonic-minor stand-ins are left out on purpose.
    static func minorRows() -> [KeyChordRow] {
        var rows: [KeyChordRow] = []
        for root in minorKeyRoots {
            let key = Scale(root: root, type: .naturalMinor)
            let row = KeyChordRow(
                key: key,
                chords: key.diatonicTriads(),
                familyName: "minor"
            )
            rows.append(row)
        }
        return rows
    }
}
