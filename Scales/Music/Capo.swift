// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// How the hand plays a chord shape.
enum ChordFingering: String, Sendable {
    /// One of the first chords every guitarist learns, with strings left ringing open
    /// (or, with a capo on, ringing at the capo).
    case open
    /// A major, minor, seventh or suspended chord with no open shape: the first finger
    /// lies across the strings.
    case barre
    /// A diminished, augmented or altered chord, played as a small movable shape.
    case movable
}

/// The chord a guitarist fingers, counted from the capo, to sound one of the scale's
/// chords: with a capo on fret 3, B♭ is fingered as G.
struct CapoChordShape: Hashable, Sendable {
    let triad: Triad
    let isDominantSeventh: Bool

    /// The shape's name as chord charts print it: G, Am, B7.
    var name: String {
        triad.name + (isDominantSeventh ? "7" : "")
    }

    /// "G major", "B dominant seventh", for VoiceOver.
    var spokenName: String {
        if isDominantSeventh {
            return "\(triad.root.displayName) dominant seventh"
        }
        return triad.spokenName
    }

    var fingering: ChordFingering {
        if Self.isOpenShape(triad: triad, isDominantSeventh: isDominantSeventh) {
            return .open
        }
        if isDominantSeventh || Self.barreQualities.contains(triad.quality) {
            return .barre
        }
        return .movable
    }

    /// The line under the chord's name on a card: "G shape" for an open chord, "Bm barre"
    /// when the shape needs a barre, so the player sees at a glance which chords are easy.
    var caption: String {
        switch fingering {
        case .open, .movable: "\(name) shape"
        case .barre: "\(name) barre"
        }
    }

    /// The same, as a sentence for VoiceOver: "Fingered as G major, an open chord."
    var spokenSentence: String {
        switch fingering {
        case .open: "Fingered as \(spokenName), an open chord."
        case .barre: "Fingered as \(spokenName), a barre chord."
        case .movable: "Fingered as \(spokenName)."
        }
    }

    /// Pitch classes of the roots that have an open shape, per kind of chord. Roots are
    /// compared by pitch rather than by spelling, so a shape spelled either way counts.
    private static let openMajorRoots: Set<Int> = [0, 2, 4, 7, 9]
    private static let openMinorRoots: Set<Int> = [2, 4, 9]
    private static let openSeventhRoots: Set<Int> = [0, 2, 4, 7, 9, 11]
    private static let openSuspendedFourthRoots: Set<Int> = [2, 4, 9]
    private static let openSuspendedSecondRoots: Set<Int> = [2, 9]

    /// The chord kinds played as barre chords when they have no open shape.
    private static let barreQualities: Set<TriadQuality> = [
        .major, .minor, .suspendedFourth, .suspendedSecond,
    ]

    /// The open chords of the beginner books: C A G E D, Am Em Dm, the open sevenths
    /// (A7 B7 C7 D7 E7 G7) and the open suspended chords. F is left out because its
    /// usual shape is a small barre.
    private static func isOpenShape(triad: Triad, isDominantSeventh: Bool) -> Bool {
        let rootPitchClass = triad.root.pitchClass
        if isDominantSeventh {
            return openSeventhRoots.contains(rootPitchClass)
        }
        switch triad.quality {
        case .major: return openMajorRoots.contains(rootPitchClass)
        case .minor: return openMinorRoots.contains(rootPitchClass)
        case .suspendedFourth: return openSuspendedFourthRoots.contains(rootPitchClass)
        case .suspendedSecond: return openSuspendedSecondRoots.contains(rootPitchClass)
        default: return false
        }
    }
}

/// Turns the chords a scale sounds into the shapes fingered with a capo on.
///
/// A capo on fret 3 raises every shape three semitones, so the shape for a chord is that
/// chord three semitones lower. The whole key moves together: the shapes are spelled as
/// the lower key spells them (B♭ major with a capo on 3 is fingered as G major: G, Am, Bm,
/// C, D, Em, F♯°), which is why one interval, a letter count and a semitone count, is
/// worked out from the scale's root and then applied to every chord.
struct CapoTransposition: Sendable {
    /// The fret the capo is on; 0 for no capo.
    let capoFret: Int
    /// How many semitones below its sound each shape is named. The capo's fret, less one
    /// when the guitar is tuned a half step down.
    let semitonesDown: Int
    /// The root of the key the shapes belong to: G for B♭ major with a capo on 3.
    let shapeRoot: NoteName
    /// The name of the scale on the board, to say which key's chords are fingered.
    private let scaleName: String
    /// Letters from a sounding note down to its shape: B♭ to G is two letters.
    private let letterStepsDown: Int

    /// Nil when there is nothing to rename: no capo, or a tuning whose chord shapes are
    /// not the standard ones (see `Tuning.standardShapeOffset`).
    init?(scale: Scale, capoFret: Int, tuning: Tuning) {
        guard capoFret > 0, let offset = tuning.standardShapeOffset else { return nil }
        self.init(scale: scale, capoFret: capoFret, semitonesDown: capoFret + offset)
    }

    init(scale: Scale, capoFret: Int, semitonesDown: Int) {
        let shapeRoot = Self.shapeRoot(for: scale, semitonesDown: semitonesDown)
        let letterCount = Letter.allCases.count
        let letterDistance = scale.root.letter.rawValue - shapeRoot.letter.rawValue
        self.capoFret = capoFret
        self.semitonesDown = semitonesDown
        self.shapeRoot = shapeRoot
        self.scaleName = scale.definition.name
        self.letterStepsDown = (letterDistance % letterCount + letterCount) % letterCount
    }

    /// The key the shapes belong to: "G Major".
    var shapeScaleTitle: String {
        "\(shapeRoot.displayName) \(scaleName)"
    }

    /// One line under the chord cards saying what their shape lines mean.
    var note: String {
        "Capo on fret \(capoFret): finger the chords as in \(shapeScaleTitle)."
    }

    /// A sounding note moved down to the note the hand plays, keeping the interval from
    /// the key's root so every shape is spelled within the same key.
    func shapeNote(for sounding: NoteName) -> NoteName {
        let letter = sounding.letter.advanced(by: -letterStepsDown)
        let targetPitchClass = PitchMath.wrap(sounding.pitchClass - semitonesDown)
        let accidental = PitchMath.signedDistance(
            from: letter.naturalPitchClass,
            to: targetPitchClass
        )
        return NoteName(letter, accidental)
    }

    func shape(for chord: ScaleChord) -> CapoChordShape {
        let shapeTriad = Triad(root: shapeNote(for: chord.triad.root), quality: chord.triad.quality)
        return CapoChordShape(triad: shapeTriad, isDominantSeventh: chord.isDominantSeventh)
    }

    /// The root the key takes once it is moved down. A black key has two spellings (C♯
    /// and D♭); the one whose scale needs fewer sharps and flats wins, as a musician
    /// would name it: D major one semitone down is fingered as D♭ major, not C♯ major.
    private static func shapeRoot(for scale: Scale, semitonesDown: Int) -> NoteName {
        let targetPitchClass = PitchMath.wrap(scale.root.pitchClass - semitonesDown)
        if targetPitchClass == scale.root.pitchClass {
            return scale.root
        }
        var best = scale.root
        var bestAccidentalCount = Int.max
        for candidate in NoteName.rootChoices where candidate.pitchClass == targetPitchClass {
            let candidateScale = Scale(root: candidate, definition: scale.definition)
            var accidentalCount = 0
            for tone in candidateScale.tones {
                accidentalCount += abs(tone.name.accidental)
            }
            if accidentalCount < bestAccidentalCount {
                bestAccidentalCount = accidentalCount
                best = candidate
            }
        }
        return best
    }
}

/// One row of the capo picker: a fret, the key its shapes come from, and how many of the
/// scale's chords become open chords there.
struct CapoSuggestion: Hashable, Sendable {
    /// 0 for no capo.
    let capoFret: Int
    /// The key the chords are fingered in: "G Major".
    let shapeScaleTitle: String
    let openChordCount: Int
    let chordCount: Int

    /// "No capo" or "Fret 3".
    var title: String {
        capoFret == 0 ? "No capo" : "Fret \(capoFret)"
    }

    /// "as G Major · 5 of 7 open". With no capo the key is the one on the board, so only
    /// the count is given.
    var detail: String {
        let openCount = "\(openChordCount) of \(chordCount) open"
        if capoFret == 0 {
            return openCount
        }
        return "as \(shapeScaleTitle) · \(openCount)"
    }

    /// The whole row as one line, for a menu item.
    var label: String {
        "\(title) · \(detail)"
    }
}

/// The capo: where it may sit and which fret suits the scale on the board.
enum Capo {
    /// Frets offered in the picker; 0 is no capo. Past the ninth fret the neck left to
    /// play on is too short to be useful.
    static let fretChoices = Array(0...9)

    /// What the capo picker shows when the tuning's chord shapes are not the standard ones.
    static let unsupportedTuningAdvice =
        "Chord shapes are named for standard tuning, so this tuning shows the capo only."

    /// One suggestion per fret choice, in fret order: the players' question is "where do
    /// I put the capo to play this key with easy chords?", and the open chord count per
    /// fret answers it. Empty for a tuning whose shapes are not the standard ones.
    static func suggestions(for scale: Scale, tuning: Tuning) -> [CapoSuggestion] {
        guard let offset = tuning.standardShapeOffset else { return [] }
        let chords = scale.boardChords()
        var suggestions: [CapoSuggestion] = []
        for fret in fretChoices {
            let transposition = CapoTransposition(
                scale: scale,
                capoFret: fret,
                semitonesDown: fret + offset
            )
            var openChordCount = 0
            for chord in chords where transposition.shape(for: chord).fingering == .open {
                openChordCount += 1
            }
            let suggestion = CapoSuggestion(
                capoFret: fret,
                shapeScaleTitle: transposition.shapeScaleTitle,
                openChordCount: openChordCount,
                chordCount: chords.count
            )
            suggestions.append(suggestion)
        }
        return suggestions
    }

    /// One sentence naming the frets that give the most open chords: "Most open chords:
    /// capo on fret 3 (as C Major) or fret 8 (as G Major)."
    static func advice(for scale: Scale, tuning: Tuning) -> String {
        let suggestions = suggestions(for: scale, tuning: tuning)
        guard let noCapo = suggestions.first else { return unsupportedTuningAdvice }
        var bestCount = 0
        for suggestion in suggestions where suggestion.capoFret > 0 {
            bestCount = max(bestCount, suggestion.openChordCount)
        }
        if bestCount <= noCapo.openChordCount {
            return "No capo already gives the most open chords."
        }
        var bestFrets: [String] = []
        for suggestion in suggestions {
            let isBest = suggestion.capoFret > 0 && suggestion.openChordCount == bestCount
            if isBest {
                bestFrets.append("fret \(suggestion.capoFret) (as \(suggestion.shapeScaleTitle))")
            }
        }
        return "Most open chords: capo on \(bestFrets.joined(separator: " or "))."
    }
}
