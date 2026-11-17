// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// The kinds of three-note chord a scale can stack on one of its notes, told apart by
/// the size of the middle note and the fifth above the root.
///
/// Beyond the four textbook triads, a scale edit can leave a chord with an altered fifth
/// (C E G♭ is C(♭5), C E♭ G♯ is Cm(♯5)) or with no third at all, where the second or the
/// fourth stands in (Csus2, Csus4). Each is named the way chord charts print it.
enum TriadQuality: Hashable, CaseIterable, Sendable {
    case major
    case minor
    case diminished
    case augmented
    /// A major third over a diminished fifth: C E G♭.
    case majorFlatFive
    /// A minor third over an augmented fifth: C E♭ G♯.
    case minorSharpFive
    /// The fourth in place of the third: C F G.
    case suspendedFourth
    /// The second in place of the third: C D G.
    case suspendedSecond
    case suspendedFourthFlatFive
    case suspendedSecondFlatFive
    case suspendedFourthSharpFive
    case suspendedSecondSharpFive

    /// Semitones from the root to the middle note and to the fifth: a major third is 4, a
    /// minor third 3, a fourth 5, a second 2; a perfect fifth is 7, diminished 6, augmented 8.
    private var intervals: (middle: Int, fifth: Int) {
        switch self {
        case .major: (4, 7)
        case .minor: (3, 7)
        case .diminished: (3, 6)
        case .augmented: (4, 8)
        case .majorFlatFive: (4, 6)
        case .minorSharpFive: (3, 8)
        case .suspendedFourth: (5, 7)
        case .suspendedSecond: (2, 7)
        case .suspendedFourthFlatFive: (5, 6)
        case .suspendedSecondFlatFive: (2, 6)
        case .suspendedFourthSharpFive: (5, 8)
        case .suspendedSecondSharpFive: (2, 8)
        }
    }

    /// Identifies a chord from the semitone distances of its middle note and fifth above
    /// the root, or nil when no chart name fits (a diminished fifth under an augmented
    /// third, for instance).
    init?(thirdSemitones: Int, fifthSemitones: Int) {
        for quality in Self.allCases {
            let intervals = quality.intervals
            if intervals.middle == thirdSemitones && intervals.fifth == fifthSemitones {
                self = quality
                return
            }
        }
        return nil
    }

    /// True when the chord has a minor third, which chord charts show by a lower-case
    /// numeral and an "m" in the name.
    var hasMinorThird: Bool {
        switch self {
        case .minor, .diminished, .minorSharpFive: true
        default: false
        }
    }

    /// True when a second or fourth stands in for the third.
    var isSuspended: Bool {
        switch self {
        case .suspendedFourth, .suspendedSecond, .suspendedFourthFlatFive,
            .suspendedSecondFlatFive, .suspendedFourthSharpFive, .suspendedSecondSharpFive:
            true
        default:
            false
        }
    }

    /// Appended to the root in a chord name: C, Cm, C°, C+, C(♭5), Cm(♯5), Csus4, Csus2(♭5).
    var nameSuffix: String {
        switch self {
        case .major: ""
        case .minor: "m"
        case .diminished: "°"
        case .augmented: "+"
        case .majorFlatFive: "(♭5)"
        case .minorSharpFive: "m(♯5)"
        case .suspendedFourth: "sus4"
        case .suspendedSecond: "sus2"
        case .suspendedFourthFlatFive: "sus4(♭5)"
        case .suspendedSecondFlatFive: "sus2(♭5)"
        case .suspendedFourthSharpFive: "sus4(♯5)"
        case .suspendedSecondSharpFive: "sus2(♯5)"
        }
    }

    /// The quality as it is said aloud: "C minor", "B diminished", "D suspended fourth".
    var spokenName: String {
        switch self {
        case .major: "major"
        case .minor: "minor"
        case .diminished: "diminished"
        case .augmented: "augmented"
        case .majorFlatFive: "major flat five"
        case .minorSharpFive: "minor sharp five"
        case .suspendedFourth: "suspended fourth"
        case .suspendedSecond: "suspended second"
        case .suspendedFourthFlatFive: "suspended fourth flat five"
        case .suspendedSecondFlatFive: "suspended second flat five"
        case .suspendedFourthSharpFive: "suspended fourth sharp five"
        case .suspendedSecondSharpFive: "suspended second sharp five"
        }
    }

    /// Writes a Roman numeral the way the quality is shown: lower case for chords with a
    /// minor third, upper case otherwise, with the chart suffix after it (vii°, III+,
    /// I(♭5), i(♯5), IVsus2). The "m" of a minor chord is carried by the lower case alone.
    func numeral(_ base: String) -> String {
        let cased = hasMinorThird ? base.lowercased() : base
        if self == .minor {
            return cased
        }
        if self == .minorSharpFive {
            return cased + "(♯5)"
        }
        return cased + nameSuffix
    }
}

/// A three-note chord such as Dm, B° or Gsus4.
struct Triad: Hashable, Sendable {
    let root: NoteName
    let quality: TriadQuality

    var name: String {
        root.displayName + quality.nameSuffix
    }

    /// "D minor", for sentences and VoiceOver.
    var spokenName: String {
        "\(root.displayName) \(quality.spokenName)"
    }
}

/// A chord built on one step of a key, with its Roman numeral (IV, vi, vii°, …).
struct DiatonicChord: Hashable, Sendable {
    let numeral: String
    let triad: Triad
}

extension Scale {
    /// The seven chords of a key for the key tables, each numbered from the scale's own
    /// first note (so A minor starts at i and its chord on C is III).
    /// Only seven-note scales have a full table; other sizes give an empty list.
    /// Spelling comes from the scale, so F♯ major yields E♯° rather than F°.
    func diatonicTriads() -> [DiatonicChord] {
        guard tones.count == 7 else { return [] }
        var chords: [DiatonicChord] = []
        for chord in letterTriads() {
            chords.append(DiatonicChord(numeral: chord.numeral, triad: chord.triad))
        }
        return chords
    }

    /// The key signature as musicians label it: "—", "3♯" or "2♭".
    var keySignature: String {
        var sharps = 0
        var flats = 0
        for tone in tones {
            if tone.name.accidental > 0 {
                sharps += tone.name.accidental
            } else if tone.name.accidental < 0 {
                flats -= tone.name.accidental
            }
        }
        if sharps > 0 { return "\(sharps)♯" }
        if flats > 0 { return "\(flats)♭" }
        return "—"
    }
}
