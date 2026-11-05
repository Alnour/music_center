/// The four kinds of three-note chord, told apart by the size of their third and fifth.
enum TriadQuality: Hashable, Sendable {
    case major
    case minor
    case diminished
    case augmented

    /// Identifies a triad from the semitone distances of its third and fifth above the
    /// root: a major third is 4, a minor third 3; a perfect fifth is 7.
    init?(thirdSemitones: Int, fifthSemitones: Int) {
        switch (thirdSemitones, fifthSemitones) {
        case (4, 7): self = .major
        case (3, 7): self = .minor
        case (3, 6): self = .diminished
        case (4, 8): self = .augmented
        default: return nil
        }
    }

    /// Appended to the root in a chord name: C, Cm, C°, C+.
    var nameSuffix: String {
        switch self {
        case .major: ""
        case .minor: "m"
        case .diminished: "°"
        case .augmented: "+"
        }
    }

    /// The quality as it is said aloud: "C minor", "B diminished".
    var spokenName: String {
        switch self {
        case .major: "major"
        case .minor: "minor"
        case .diminished: "diminished"
        case .augmented: "augmented"
        }
    }

    /// Writes a Roman numeral the way the quality is shown: upper case for major and
    /// augmented chords, lower case for minor and diminished, with ° or + after it.
    func numeral(_ base: String) -> String {
        switch self {
        case .major: base
        case .minor: base.lowercased()
        case .diminished: base.lowercased() + "°"
        case .augmented: base + "+"
        }
    }
}

/// A three-note chord such as Dm or B°.
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
