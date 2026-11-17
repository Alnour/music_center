// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// The scale shapes the app can draw, each defined by its degrees.
enum ScaleType: String, CaseIterable, Codable, Identifiable, Sendable {
    case major
    case naturalMinor
    case harmonicMinor
    case melodicMinor
    case majorPentatonic
    case minorPentatonic
    case blues
    case majorBlues
    case ionian
    case dorian
    case phrygian
    case lydian
    case mixolydian
    case aeolian
    case locrian
    case phrygianDominant
    case wholeTone

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .major: "Major"
        case .naturalMinor: "Natural Minor"
        case .harmonicMinor: "Harmonic Minor"
        case .melodicMinor: "Melodic Minor"
        case .majorPentatonic: "Major Pentatonic"
        case .minorPentatonic: "Minor Pentatonic"
        case .blues: "Blues"
        case .majorBlues: "Major Blues"
        case .ionian: "Ionian"
        case .dorian: "Dorian"
        case .phrygian: "Phrygian"
        case .lydian: "Lydian"
        case .mixolydian: "Mixolydian"
        case .aeolian: "Aeolian"
        case .locrian: "Locrian"
        case .phrygianDominant: "Phrygian Dominant (Hijaz)"
        case .wholeTone: "Whole Tone"
        }
    }

    /// The formula as musicians write it, read against the major scale.
    var formula: String {
        switch self {
        case .major, .ionian: "1 2 3 4 5 6 7"
        case .naturalMinor, .aeolian: "1 2 ♭3 4 5 ♭6 ♭7"
        case .harmonicMinor: "1 2 ♭3 4 5 ♭6 7"
        case .melodicMinor: "1 2 ♭3 4 5 6 7"
        case .majorPentatonic: "1 2 3 5 6"
        case .minorPentatonic: "1 ♭3 4 5 ♭7"
        case .blues: "1 ♭3 4 ♭5 5 ♭7"
        case .majorBlues: "1 2 ♭3 3 5 6"
        case .dorian: "1 2 ♭3 4 5 6 ♭7"
        case .phrygian: "1 ♭2 ♭3 4 5 ♭6 ♭7"
        case .lydian: "1 2 3 ♯4 5 6 7"
        case .mixolydian: "1 2 3 4 5 6 ♭7"
        case .locrian: "1 ♭2 ♭3 4 ♭5 ♭6 ♭7"
        case .phrygianDominant: "1 ♭2 3 4 5 ♭6 ♭7"
        case .wholeTone: "1 2 3 ♯4 ♯5 ♭7"
        }
    }

    var degrees: [ScaleDegree] {
        var degrees: [ScaleDegree] = []
        for symbol in formula.split(separator: " ") {
            degrees.append(ScaleDegree(symbol: String(symbol)))
        }
        return degrees
    }
}

/// Groups for the scale picker, so the menu reads like a practice book's table of contents.
enum ScaleFamily: String, CaseIterable, Identifiable, Sendable {
    case everyday
    case pentatonicAndBlues
    case modes
    case colour

    var id: String { rawValue }

    var title: String {
        switch self {
        case .everyday: "Major & Minor"
        case .pentatonicAndBlues: "Pentatonic & Blues"
        case .modes: "Modes"
        case .colour: "Other Colours"
        }
    }

    var scales: [ScaleType] {
        switch self {
        case .everyday: [.major, .naturalMinor, .harmonicMinor, .melodicMinor]
        case .pentatonicAndBlues: [.majorPentatonic, .minorPentatonic, .blues, .majorBlues]
        case .modes: [.ionian, .dorian, .phrygian, .lydian, .mixolydian, .aeolian, .locrian]
        case .colour: [.phrygianDominant, .wholeTone]
        }
    }
}
