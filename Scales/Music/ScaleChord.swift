/// A chord built from a scale's own notes on one of its tones, with the notes it uses.
struct ScaleChord: Hashable, Sendable {
    /// The degree the chord is built on.
    let degree: ScaleDegree
    let triad: Triad
    /// Root, third and fifth, spelled as the scale spells them.
    let notes: [NoteName]
    /// The numeral counted from the scale's own steps, as key tables print it: the chord
    /// on C in A minor is III.
    let numeral: String
    /// The numeral with the degree's flat or sharp, read against the major scale the way
    /// formulas are: the chord on C in A minor is ♭III, the chord on ♭2 in Phrygian is ♭II.
    let formulaNumeral: String

    private static let romanNumerals = ["I", "II", "III", "IV", "V", "VI", "VII"]

    init(degree: ScaleDegree, triad: Triad, notes: [NoteName]) {
        let base = Self.romanNumerals[degree.number - 1]
        let numeral = triad.quality.numeral(base)
        let accidental: String
        switch degree.alteration {
        case ..<0: accidental = "♭"
        case 1...: accidental = "♯"
        default: accidental = ""
        }
        self.degree = degree
        self.triad = triad
        self.notes = notes
        self.numeral = numeral
        self.formulaNumeral = accidental + numeral
    }

    /// "V, G major", for the sentences of a lesson.
    var spokenTitle: String {
        "\(formulaNumeral), \(triad.spokenName)"
    }
}

extension Scale {
    /// The chords this scale makes from its own notes, for a scale of any size.
    ///
    /// A chord is stacked by letter: on a tone, take the scale's tone two letters up (its
    /// third) and four letters up (its fifth). When the scale has no tone on one of those
    /// letters, or the three notes are not one of the four triads, that tone has no chord.
    /// This is the textbook rule for seven-note scales, and it also shows what happens
    /// when a note is removed (chords that needed it disappear) or added (new ones form).
    ///
    /// A scale can hold two tones on one letter, as blues does with ♭5 and 5. Every pairing
    /// is tried and the one with the fewest flats and sharps against the major scale wins,
    /// so C blues gives C minor on C rather than C diminished.
    func letterTriads() -> [ScaleChord] {
        var chords: [ScaleChord] = []
        for rootTone in tones {
            if let chord = chord(on: rootTone) {
                chords.append(chord)
            }
        }
        return chords
    }

    private func chord(on rootTone: ScaleTone) -> ScaleChord? {
        let thirdNumber = Self.letterNumber(rootTone.degree.number + 2)
        let fifthNumber = Self.letterNumber(rootTone.degree.number + 4)
        var thirds: [ScaleTone] = []
        var fifths: [ScaleTone] = []
        for tone in tones {
            if tone.degree.number == thirdNumber {
                thirds.append(tone)
            } else if tone.degree.number == fifthNumber {
                fifths.append(tone)
            }
        }

        let rootPitchClass = rootTone.name.pitchClass
        var best: ScaleChord?
        var bestAccidentalCount = Int.max
        for third in thirds {
            for fifth in fifths {
                let thirdSemitones = PitchMath.wrap(third.name.pitchClass - rootPitchClass)
                let fifthSemitones = PitchMath.wrap(fifth.name.pitchClass - rootPitchClass)
                guard
                    let quality = TriadQuality(
                        thirdSemitones: thirdSemitones,
                        fifthSemitones: fifthSemitones
                    )
                else { continue }
                let accidentalCount = abs(third.degree.alteration) + abs(fifth.degree.alteration)
                guard accidentalCount < bestAccidentalCount else { continue }
                bestAccidentalCount = accidentalCount
                best = ScaleChord(
                    degree: rootTone.degree,
                    triad: Triad(root: rootTone.name, quality: quality),
                    notes: [rootTone.name, third.name, fifth.name]
                )
            }
        }
        return best
    }

    /// Wraps a degree number past 7 back round to 1, as letters wrap from B to C.
    private static func letterNumber(_ number: Int) -> Int {
        let letterCount = ScaleDegree.numbers.count
        return (number - 1) % letterCount + 1
    }
}
