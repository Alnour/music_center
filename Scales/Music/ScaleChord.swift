// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// A chord built from a scale's own notes on one of its tones, with the notes it uses.
struct ScaleChord: Hashable, Sendable {
    /// The degree the chord is built on.
    let degree: ScaleDegree
    let triad: Triad
    /// Root, middle note (the third, or the fourth or second of a suspended chord) and
    /// fifth, spelled as the scale spells them, then the seventh of a seventh chord.
    let notes: [NoteName]
    /// True for a dominant seventh chord (A7): the triad with a minor seventh on top, the
    /// chord of a twelve-bar blues. Triads are the rule everywhere else.
    let isDominantSeventh: Bool
    /// The numeral counted from the scale's own steps, as key tables print it: the chord
    /// on C in A minor is III.
    let numeral: String
    /// The numeral with the degree's flat or sharp, read against the major scale the way
    /// formulas are: the chord on C in A minor is ♭III, the chord on ♭2 in Phrygian is ♭II.
    let formulaNumeral: String

    private static let romanNumerals = ["I", "II", "III", "IV", "V", "VI", "VII"]

    init(degree: ScaleDegree, triad: Triad, notes: [NoteName], isDominantSeventh: Bool = false) {
        let base = Self.romanNumerals[degree.number - 1]
        let seventhSuffix = isDominantSeventh ? "7" : ""
        let numeral = triad.quality.numeral(base) + seventhSuffix
        let accidental: String
        switch degree.alteration {
        case ..<0: accidental = "♭"
        case 1...: accidental = "♯"
        default: accidental = ""
        }
        self.degree = degree
        self.triad = triad
        self.notes = notes
        self.isDominantSeventh = isDominantSeventh
        self.numeral = numeral
        self.formulaNumeral = accidental + numeral
    }

    /// The name as chord charts print it: Dm, Gsus4, A7.
    var name: String {
        triad.name + (isDominantSeventh ? "7" : "")
    }

    /// "D minor", "A dominant seventh", for sentences and VoiceOver. The seventh chord is
    /// named in full because "A major seventh" is a different chord (Amaj7).
    var spokenName: String {
        if isDominantSeventh {
            return "\(triad.root.displayName) dominant seventh"
        }
        return triad.spokenName
    }

    /// "V, G major", for the sentences of a lesson.
    var spokenTitle: String {
        "\(formulaNumeral), \(spokenName)"
    }
}

extension Scale {
    /// The chords this scale makes from its own notes, for a scale of any size.
    ///
    /// A chord is stacked by letter: on a tone, take the scale's tone two letters up (its
    /// third) and four letters up (its fifth). An altered third or fifth is still a chord
    /// with a chart name (C(♭5), Cm(♯5)), so an edit that moves one note renames the chord
    /// rather than losing it. When the scale has no tone on the third's letter, the fourth
    /// stands in for it, then the second (Csus4, Csus2), which is how players voice a chord
    /// in a scale that skips the third. When neither the fifth nor a middle note is there,
    /// or the three notes have no chart name, that tone has no chord.
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

    /// Letters above the root that can take the middle place of a chord, tried in order:
    /// the third first, then the fourth and the second for a suspended chord.
    private static let middleLetterSteps = [2, 3, 1]

    private func chord(on rootTone: ScaleTone) -> ScaleChord? {
        let fifthNumber = Self.letterNumber(rootTone.degree.number + 4)
        var fifths: [ScaleTone] = []
        var tonesByLetter: [Int: [ScaleTone]] = [:]
        for tone in tones {
            if tone.degree.number == fifthNumber {
                fifths.append(tone)
            }
            tonesByLetter[tone.degree.number, default: []].append(tone)
        }

        for step in Self.middleLetterSteps {
            let middleNumber = Self.letterNumber(rootTone.degree.number + step)
            guard let middles = tonesByLetter[middleNumber] else { continue }
            if let chord = bestChord(on: rootTone, middles: middles, fifths: fifths) {
                return chord
            }
        }
        return nil
    }

    /// The chord on a root from one set of candidate middle notes and fifths, favouring
    /// the pairing with the fewest flats and sharps.
    private func bestChord(on rootTone: ScaleTone, middles: [ScaleTone], fifths: [ScaleTone])
        -> ScaleChord?
    {
        let rootPitchClass = rootTone.name.pitchClass
        var best: ScaleChord?
        var bestAccidentalCount = Int.max
        for middle in middles {
            for fifth in fifths {
                let middleSemitones = PitchMath.wrap(middle.name.pitchClass - rootPitchClass)
                let fifthSemitones = PitchMath.wrap(fifth.name.pitchClass - rootPitchClass)
                guard
                    let quality = TriadQuality(
                        thirdSemitones: middleSemitones,
                        fifthSemitones: fifthSemitones
                    )
                else { continue }
                let accidentalCount = abs(middle.degree.alteration) + abs(fifth.degree.alteration)
                guard accidentalCount < bestAccidentalCount else { continue }
                bestAccidentalCount = accidentalCount
                best = ScaleChord(
                    degree: rootTone.degree,
                    triad: Triad(root: rootTone.name, quality: quality),
                    notes: [rootTone.name, middle.name, fifth.name]
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
