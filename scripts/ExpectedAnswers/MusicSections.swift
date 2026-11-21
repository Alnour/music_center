// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

// The music-theory sections: what every root, scale, tuning and key spells and draws.
// These are the answers players see most, so they cover every root and scale type.

// MARK: - meta

struct MetaSection: Encodable {
    /// Bumped when a section changes shape, so Kotlin readers fail clearly.
    let formatVersion: Int
    /// Every Double in the file is rounded to this many decimals.
    let decimals: Int
    let regenerate: String
}

func metaSection() -> MetaSection {
    MetaSection(
        formatVersion: 1,
        decimals: 6,
        regenerate: "swift run --quiet ExpectedAnswers expected/answers.json"
    )
}

// MARK: - roots

func rootsSection() -> [NoteAnswer] {
    var roots: [NoteAnswer] = []
    for root in NoteName.rootChoices {
        roots.append(NoteAnswer(root))
    }
    return roots
}

// MARK: - scaleTypes and families

struct ScaleTypeAnswer: Encodable {
    let rawValue: String
    let displayName: String
    let formula: String
    let degreeLabels: [String]
    let semitonesAboveRoot: [Int]
}

func scaleTypesSection() -> [ScaleTypeAnswer] {
    var types: [ScaleTypeAnswer] = []
    for type in ScaleType.allCases {
        var labels: [String] = []
        var semitones: [Int] = []
        for degree in type.degrees {
            labels.append(degree.label)
            semitones.append(degree.semitonesAboveRoot)
        }
        let answer = ScaleTypeAnswer(
            rawValue: type.rawValue,
            displayName: type.displayName,
            formula: type.formula,
            degreeLabels: labels,
            semitonesAboveRoot: semitones
        )
        types.append(answer)
    }
    return types
}

struct FamilyAnswer: Encodable {
    let rawValue: String
    let title: String
    let members: [String]
}

func familiesSection() -> [FamilyAnswer] {
    var families: [FamilyAnswer] = []
    for family in ScaleFamily.allCases {
        var members: [String] = []
        for type in family.scales {
            members.append(type.rawValue)
        }
        families.append(
            FamilyAnswer(rawValue: family.rawValue, title: family.title, members: members)
        )
    }
    return families
}

// MARK: - tunings

struct TuningAnswer: Encodable {
    let rawValue: String
    let displayName: String
    let shortName: String
    /// Thickest string first.
    let openStrings: [String]
    let openPitchClasses: [Int]
}

func tuningsSection() -> [TuningAnswer] {
    var tunings: [TuningAnswer] = []
    for tuning in Tuning.allCases {
        var pitchClasses: [Int] = []
        for note in tuning.openStrings {
            pitchClasses.append(note.pitchClass)
        }
        let answer = TuningAnswer(
            rawValue: tuning.rawValue,
            displayName: tuning.displayName,
            shortName: tuning.shortName,
            openStrings: displayNames(tuning.openStrings),
            openPitchClasses: pitchClasses
        )
        tunings.append(answer)
    }
    return tunings
}

/// The bass guitar's tunings, in the same shape as the guitar's.
func bassTuningsSection() -> [TuningAnswer] {
    var tunings: [TuningAnswer] = []
    for tuning in BassTuning.allCases {
        var pitchClasses: [Int] = []
        for note in tuning.openStrings {
            pitchClasses.append(note.pitchClass)
        }
        let answer = TuningAnswer(
            rawValue: tuning.rawValue,
            displayName: tuning.displayName,
            shortName: tuning.shortName,
            openStrings: displayNames(tuning.openStrings),
            openPitchClasses: pitchClasses
        )
        tunings.append(answer)
    }
    return tunings
}

// MARK: - spellings

struct ToneAnswer: Encodable {
    let name: String
    let degree: String
    let pitchClass: Int
}

struct ChordAnswer: Encodable {
    /// The degree the chord is built on, as a label ("♭3").
    let degree: String
    let numeral: String
    let formulaNumeral: String
    /// The Swift case name of the triad's quality ("majorFlatFive").
    let quality: String
    /// The chart name, with a 7 on a dominant seventh chord ("A7").
    let name: String
    let spokenName: String
    let spokenTitle: String
    let notes: [String]
    let isDominantSeventh: Bool

    init(_ chord: ScaleChord) {
        degree = chord.degree.label
        numeral = chord.numeral
        formulaNumeral = chord.formulaNumeral
        quality = String(describing: chord.triad.quality)
        name = chord.name
        spokenName = chord.spokenName
        spokenTitle = chord.spokenTitle
        notes = displayNames(chord.notes)
        isDominantSeventh = chord.isDominantSeventh
    }
}

func chordAnswers(_ chords: [ScaleChord]) -> [ChordAnswer] {
    var answers: [ChordAnswer] = []
    for chord in chords {
        answers.append(ChordAnswer(chord))
    }
    return answers
}

struct SpellingAnswer: Encodable {
    let root: String
    let scaleType: String
    let title: String
    let tones: [ToneAnswer]
    let keySignature: String
    let letterTriads: [ChordAnswer]
    /// The key-table chords: the same as `letterTriads` for seven-note scales, else empty.
    let diatonicTriadCount: Int
    /// The chords under the board: `letterTriads`, or the borrowed chords of a pentatonic
    /// or blues scale (`Scale.boardChords`).
    let boardChords: [ChordAnswer]
    /// The line under those chords saying where they come from; absent for a scale
    /// harmonised on its own notes.
    let harmonyNote: String?
}

func toneAnswers(_ scale: Scale) -> [ToneAnswer] {
    var tones: [ToneAnswer] = []
    for tone in scale.tones {
        let answer = ToneAnswer(
            name: tone.name.displayName,
            degree: tone.degree.label,
            pitchClass: tone.name.pitchClass
        )
        tones.append(answer)
    }
    return tones
}

/// 17 roots × 17 scale types, roots in picker order, types in declaration order.
func spellingsSection() -> [SpellingAnswer] {
    var spellings: [SpellingAnswer] = []
    for root in NoteName.rootChoices {
        for type in ScaleType.allCases {
            let scale = Scale(root: root, type: type)
            let answer = SpellingAnswer(
                root: root.displayName,
                scaleType: type.rawValue,
                title: scale.title,
                tones: toneAnswers(scale),
                keySignature: scale.keySignature,
                letterTriads: chordAnswers(scale.letterTriads()),
                diatonicTriadCount: scale.diatonicTriads().count,
                boardChords: chordAnswers(scale.boardChords()),
                harmonyNote: scale.harmonyNote
            )
            spellings.append(answer)
        }
    }
    return spellings
}

// MARK: - fretboards

struct GuitarStringAnswer: Encodable {
    let openNote: String
    /// One entry per fret from 0 (open) to the last fret: "F♯|7", or null off the scale.
    let tonesByFret: [String?]
}

struct FretboardAnswer: Encodable {
    let root: String
    let tuning: String
    let scaleType: String
    let fretCount: Int
    /// Thickest string first, as `FretboardLayout` lists them.
    let strings: [GuitarStringAnswer]
}

/// G at 24 frets, every tuning × every scale type: 102 boards.
func fretboardsSection() -> [FretboardAnswer] {
    let root = NoteName(.g)
    let fretCount = 24
    var boards: [FretboardAnswer] = []
    for tuning in Tuning.allCases {
        for type in ScaleType.allCases {
            let layout = FretboardLayout(
                scale: Scale(root: root, type: type),
                tuning: tuning,
                fretCount: fretCount
            )
            let board = FretboardAnswer(
                root: root.displayName,
                tuning: tuning.rawValue,
                scaleType: type.rawValue,
                fretCount: fretCount,
                strings: stringAnswers(layout)
            )
            boards.append(board)
        }
    }
    return boards
}

/// The bass guitar's neck: G at 24 frets, every bass tuning × every scale type.
func bassFretboardsSection() -> [FretboardAnswer] {
    let root = NoteName(.g)
    let fretCount = 24
    var boards: [FretboardAnswer] = []
    for tuning in BassTuning.allCases {
        for type in ScaleType.allCases {
            let layout = FretboardLayout(
                scale: Scale(root: root, type: type),
                openStrings: tuning.openStrings,
                fretCount: fretCount
            )
            let board = FretboardAnswer(
                root: root.displayName,
                tuning: tuning.rawValue,
                scaleType: type.rawValue,
                fretCount: fretCount,
                strings: stringAnswers(layout)
            )
            boards.append(board)
        }
    }
    return boards
}

/// Every string of a neck, thickest first, with the tone on each fret.
func stringAnswers(_ layout: FretboardLayout) -> [GuitarStringAnswer] {
    var strings: [GuitarStringAnswer] = []
    for guitarString in layout.strings {
        var tones: [String?] = []
        for tone in guitarString.tonesByFret {
            tones.append(toneLabel(tone))
        }
        let answer = GuitarStringAnswer(
            openNote: guitarString.openNote.displayName,
            tonesByFret: tones
        )
        strings.append(answer)
    }
    return strings
}

// MARK: - keyboards

struct KeyAnswer: Encodable {
    let pitchClass: Int
    let isBlack: Bool
    /// "F♯|7", or null when the key is outside the scale.
    let tone: String?
}

struct KeyboardAnswer: Encodable {
    let octaves: Int
    let root: String
    let scaleType: String
    let keys: [KeyAnswer]
}

struct KeyboardsSection: Encodable {
    let blackPitchClasses: [Int]
    let defaultOctaves: Int
    let layouts: [KeyboardAnswer]
}

/// One and two octaves × roots G and F♯ × every scale type.
func keyboardsSection() -> KeyboardsSection {
    let roots = [NoteName(.g), NoteName(.f, 1)]
    var layouts: [KeyboardAnswer] = []
    for octaves in [1, 2] {
        for root in roots {
            for type in ScaleType.allCases {
                let layout = KeyboardLayout(
                    scale: Scale(root: root, type: type),
                    octaveCount: octaves
                )
                var keys: [KeyAnswer] = []
                for key in layout.keys {
                    let answer = KeyAnswer(
                        pitchClass: key.pitchClass,
                        isBlack: key.isBlack,
                        tone: toneLabel(key.tone)
                    )
                    keys.append(answer)
                }
                let board = KeyboardAnswer(
                    octaves: octaves,
                    root: root.displayName,
                    scaleType: type.rawValue,
                    keys: keys
                )
                layouts.append(board)
            }
        }
    }
    return KeyboardsSection(
        blackPitchClasses: KeyboardLayout.blackPitchClasses.sorted(),
        defaultOctaves: PracticeSettings.defaultKeyboardOctaves,
        layouts: layouts
    )
}

// MARK: - keyChordTable

struct TableChordAnswer: Encodable {
    let numeral: String
    let name: String
    let spokenName: String

    init(_ chord: DiatonicChord) {
        numeral = chord.numeral
        name = chord.triad.name
        spokenName = chord.triad.spokenName
    }
}

struct KeyRowAnswer: Encodable {
    let keyName: String
    let id: String
    let chords: [TableChordAnswer]
}

struct KeyChordTableSection: Encodable {
    let major: [KeyRowAnswer]
    let minor: [KeyRowAnswer]
}

func keyRowAnswers(_ rows: [KeyChordRow]) -> [KeyRowAnswer] {
    var answers: [KeyRowAnswer] = []
    for row in rows {
        var chords: [TableChordAnswer] = []
        for chord in row.chords {
            chords.append(TableChordAnswer(chord))
        }
        let answer = KeyRowAnswer(
            keyName: row.keyName,
            id: row.id,
            chords: chords
        )
        answers.append(answer)
    }
    return answers
}

func keyChordTableSection() -> KeyChordTableSection {
    KeyChordTableSection(
        major: keyRowAnswers(KeyChordTable.majorRows()),
        minor: keyRowAnswers(KeyChordTable.minorRows())
    )
}
