// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

// The capo section: the picker's rows and advice for every root and scale type, the shapes
// each fret gives a few keys, and the neck with the frets behind the capo emptied.

import CoreGraphics

struct CapoSuggestionAnswer: Encodable {
    let capoFret: Int
    let title: String
    let detail: String
    let label: String
    let openChordCount: Int
    let chordCount: Int
}

/// The capo picker for one scale in one tuning.
struct CapoPickerAnswer: Encodable {
    let root: String
    let scaleType: String
    let tuning: String
    /// One per fret choice; empty for a tuning without the standard shapes.
    let suggestions: [CapoSuggestionAnswer]
    let advice: String
}

struct CapoShapeAnswer: Encodable {
    /// The sounding chord: "B♭".
    let chord: String
    /// The shape fingered: "G".
    let name: String
    let spokenName: String
    let fingering: String
    let caption: String
    let spokenSentence: String
}

/// The shapes of one scale's board chords with the capo on one fret.
struct CapoShapesAnswer: Encodable {
    let root: String
    let scaleType: String
    let tuning: String
    let capoFret: Int
    let semitonesDown: Int
    let shapeRoot: String
    let shapeScaleTitle: String
    let note: String
    let shapes: [CapoShapeAnswer]
}

struct CapoBoardAnswer: Encodable {
    let requestedCapoFret: Int
    let fretCount: Int
    /// The fret the layout keeps, held below the last fret.
    let capoFret: Int
    /// G major in standard tuning, thickest string first.
    let strings: [GuitarStringAnswer]
}

/// The capo's bar on a 1500 × 500 board of 15 frets and 6 strings.
struct CapoBarAnswer: Encodable {
    let fret: Int
    let isLeftHanded: Bool
    let rect: RectAnswer
}

struct CapoSection: Encodable {
    let fretChoices: [Int]
    let unsupportedTuningAdvice: String
    /// Each tuning's `standardShapeOffset`, in `Tuning.allCases` order; null without one.
    let standardShapeOffsets: [Int?]
    /// Every root × every scale type in standard tuning, then G major in every tuning.
    let pickers: [CapoPickerAnswer]
    let shapes: [CapoShapesAnswer]
    let boards: [CapoBoardAnswer]
    let bars: [CapoBarAnswer]
}

func capoPickerAnswer(root: NoteName, type: ScaleType, tuning: Tuning) -> CapoPickerAnswer {
    let scale = Scale(root: root, type: type)
    var suggestions: [CapoSuggestionAnswer] = []
    for suggestion in Capo.suggestions(for: scale, tuning: tuning) {
        let answer = CapoSuggestionAnswer(
            capoFret: suggestion.capoFret,
            title: suggestion.title,
            detail: suggestion.detail,
            label: suggestion.label,
            openChordCount: suggestion.openChordCount,
            chordCount: suggestion.chordCount
        )
        suggestions.append(answer)
    }
    return CapoPickerAnswer(
        root: root.displayName,
        scaleType: type.rawValue,
        tuning: tuning.rawValue,
        suggestions: suggestions,
        advice: Capo.advice(for: scale, tuning: tuning)
    )
}

func capoShapesAnswer(root: NoteName, type: ScaleType, tuning: Tuning, capoFret: Int)
    -> CapoShapesAnswer?
{
    let scale = Scale(root: root, type: type)
    guard let capo = CapoTransposition(scale: scale, capoFret: capoFret, tuning: tuning) else {
        return nil
    }
    var shapes: [CapoShapeAnswer] = []
    for chord in scale.boardChords() {
        let shape = capo.shape(for: chord)
        let answer = CapoShapeAnswer(
            chord: chord.name,
            name: shape.name,
            spokenName: shape.spokenName,
            fingering: shape.fingering.rawValue,
            caption: shape.caption,
            spokenSentence: shape.spokenSentence
        )
        shapes.append(answer)
    }
    return CapoShapesAnswer(
        root: root.displayName,
        scaleType: type.rawValue,
        tuning: tuning.rawValue,
        capoFret: capoFret,
        semitonesDown: capo.semitonesDown,
        shapeRoot: capo.shapeRoot.displayName,
        shapeScaleTitle: capo.shapeScaleTitle,
        note: capo.note,
        shapes: shapes
    )
}

func capoBoardAnswer(requestedCapoFret: Int, fretCount: Int) -> CapoBoardAnswer {
    let layout = FretboardLayout(
        scale: Scale(root: NoteName(.g), type: .major),
        tuning: .standard,
        fretCount: fretCount,
        capoFret: requestedCapoFret
    )
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
    return CapoBoardAnswer(
        requestedCapoFret: requestedCapoFret,
        fretCount: fretCount,
        capoFret: layout.capoFret,
        strings: strings
    )
}

func capoSection() -> CapoSection {
    var offsets: [Int?] = []
    for tuning in Tuning.allCases {
        offsets.append(tuning.standardShapeOffset)
    }

    var pickers: [CapoPickerAnswer] = []
    var shapes: [CapoShapesAnswer] = []
    for root in NoteName.rootChoices {
        for type in ScaleType.allCases {
            pickers.append(capoPickerAnswer(root: root, type: type, tuning: .standard))
            // Every fret of every key would be thousands of rows; frets 1, 4 and 9 cover
            // both spellings of the black keys and the far end of the choices.
            for capoFret in [1, 4, 9] {
                let answer = capoShapesAnswer(
                    root: root,
                    type: type,
                    tuning: .standard,
                    capoFret: capoFret
                )
                if let answer {
                    shapes.append(answer)
                }
            }
        }
    }
    for tuning in Tuning.allCases where tuning != .standard {
        pickers.append(capoPickerAnswer(root: NoteName(.g), type: .major, tuning: tuning))
        let answer = capoShapesAnswer(
            root: NoteName(.g),
            type: .major,
            tuning: tuning,
            capoFret: 3
        )
        if let answer {
            shapes.append(answer)
        }
    }

    var boards: [CapoBoardAnswer] = []
    for requestedCapoFret in [0, 3, 9, 30] {
        boards.append(capoBoardAnswer(requestedCapoFret: requestedCapoFret, fretCount: 12))
    }

    var bars: [CapoBarAnswer] = []
    for isLeftHanded in [false, true] {
        let geometry = FretboardGeometry(
            size: CGSize(width: 1500, height: 500),
            fretCount: 15,
            stringCount: 6,
            isLeftHanded: isLeftHanded
        )
        for fret in [1, 3, 9] {
            let rect = geometry.capoBarRect(fret: fret, stringCount: 6)
            bars.append(
                CapoBarAnswer(fret: fret, isLeftHanded: isLeftHanded, rect: RectAnswer(rect))
            )
        }
    }

    return CapoSection(
        fretChoices: Capo.fretChoices,
        unsupportedTuningAdvice: Capo.unsupportedTuningAdvice,
        standardShapeOffsets: offsets,
        pickers: pickers,
        shapes: shapes,
        boards: boards,
        bars: bars
    )
}
