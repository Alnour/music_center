// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// Where a scale's chords come from.
///
/// A seven-note scale is harmonised the textbook way: a chord stacked on each of its
/// notes (`Scale.letterTriads`). Pentatonic and blues scales are melody scales, five or
/// six notes picked out of a key, and the theory books do not harmonise them on their own
/// notes (stacking gives only two real triads and a handful of suspended chords). They are
/// played over the chords of the key they come from: the major pentatonic over its major
/// key, the minor pentatonic over its minor key, and the blues scales over the three
/// dominant seventh chords of a twelve-bar blues (I7, IV7, V7), where the scale's ♭3 and
/// ♭5 rub against the chords' major thirds and fifths to make the blue notes.
enum HarmonicHome: Sendable {
    /// The major key on the same root, whose seven chords the scale is played over.
    case majorKey
    /// The natural minor key on the same root.
    case minorKey
    /// The twelve-bar blues on the same root: I7, IV7 and V7.
    case twelveBarBlues
}

extension ScaleType {
    /// The key or progression a scale borrows its chords from, or nil for a scale that
    /// is harmonised on its own notes.
    var harmonicHome: HarmonicHome? {
        switch self {
        case .majorPentatonic: .majorKey
        case .minorPentatonic: .minorKey
        case .blues, .majorBlues: .twelveBarBlues
        default: nil
        }
    }
}

extension Scale {
    /// The built-in scale this one is, when it is one of them unchanged. A saved scale or
    /// an edited one is harmonised on its own notes even when it happens to sound like a
    /// built-in scale, because its chords are what the edit lesson talks about.
    var builtInType: ScaleType? {
        ScaleType.allCases.first { $0.definition == definition }
    }

    /// The chords shown under the board: the scale's own stacked chords, or, for a
    /// pentatonic or blues scale, the chords of its harmonic home (see `HarmonicHome`).
    func boardChords() -> [ScaleChord] {
        switch builtInType?.harmonicHome {
        case nil:
            return letterTriads()
        case .majorKey:
            return Scale(root: root, type: .major).letterTriads()
        case .minorKey:
            return Scale(root: root, type: .naturalMinor).letterTriads()
        case .twelveBarBlues:
            return twelveBarBluesChords()
        }
    }

    /// Degrees of the dominant seventh chords of a twelve-bar blues, each read against
    /// the major scale of the key: I7 is 1 3 5 ♭7, IV7 is 4 6 1 ♭3, V7 is 5 7 2 4.
    private static let twelveBarBluesFormulas = [
        ["1", "3", "5", "♭7"],
        ["4", "6", "1", "♭3"],
        ["5", "7", "2", "4"],
    ]

    private func twelveBarBluesChords() -> [ScaleChord] {
        var chords: [ScaleChord] = []
        for formula in Self.twelveBarBluesFormulas {
            var notes: [NoteName] = []
            for symbol in formula {
                let degree = ScaleDegree(symbol: symbol)
                notes.append(Scale.spell(degree, above: root))
            }
            let chord = ScaleChord(
                degree: ScaleDegree(symbol: formula[0]),
                triad: Triad(root: notes[0], quality: .major),
                notes: notes,
                isDominantSeventh: true
            )
            chords.append(chord)
        }
        return chords
    }

    /// One line under the chords saying where they come from, for a scale that borrows
    /// them; nil for a scale harmonised on its own notes.
    var harmonyNote: String? {
        guard let type = builtInType, let home = type.harmonicHome else { return nil }
        let keyName = root.displayName
        switch home {
        case .majorKey:
            return "\(type.displayName) is \(keyName) major without its 4 and 7, "
                + "so it is played over \(keyName) major's chords."
        case .minorKey:
            return "\(type.displayName) is \(keyName) minor without its 2 and ♭6, "
                + "so it is played over \(keyName) minor's chords."
        case .twelveBarBlues:
            return "\(type.displayName) is played over the three seventh chords of a "
                + "twelve-bar blues in \(keyName); the scale's notes outside them are the blue notes."
        }
    }
}
