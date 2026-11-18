// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics
import Foundation

// Where things are drawn: the fretboard, the keyboard, the drum staff, the TV layout and
// the chord tables, at the sizes a TV and a phone give them. Android draws in dp from the
// same formulas, so any mismatch here is a visible difference between the platforms.

// MARK: - fretboard

struct FretboardGeometryAnswer: Encodable {
    let width: Double
    let height: Double
    let fretCount: Int
    let stringCount: Int
    let isLeftHanded: Bool
    let columnUnits: Double
    let cellWidth: Double
    let rowHeight: Double
    let noteRadius: Double
    let nutX: Double
    let tuningLabelX: Double
    let fretNumberY: Double
    let inlayY: Double
    /// Frets 0 (open, left of the nut) to `fretCount`.
    let noteX: [Double]
    /// Frets 0 (the nut's line) to `fretCount`.
    let fretWireX: [Double]
    /// Rows from the top.
    let stringY: [Double]
    /// The layout's string index drawn on each row, thin string on top and thick on top.
    let stringIndexThinOnTop: [Int]
    let stringIndexThickOnTop: [Int]
    /// By the layout's string index, thickest string first.
    let stringStrokeWidths: [Double]
    let inlayRadius: Double
    let doubleInlayOffset: Double
    let fretWireWidth: Double
    let nutWidth: Double
    let fretNumberFontSize: Double
    let openStringLabelFontSize: Double
    /// Note label sizes in a circle of `noteRadius` for one- and two-character labels.
    let noteLabelFontSizeShort: Double
    let noteLabelFontSizeLong: Double
    /// The chord ring around a circle of `noteRadius`.
    let chordRingLineWidth: Double
    let chordRingOutset: Double
}

struct FretboardConstantsAnswer: Encodable {
    let tuningColumnShare: Double
    let numberBandShare: Double
    let inlayBandShare: Double
    let singleInlayFrets: [Int]
    let doubleInlayFrets: [Int]
}

func fretboardGeometryAnswer(size: CGSize, fretCount: Int, isLeftHanded: Bool)
    -> FretboardGeometryAnswer
{
    let stringCount = 6
    let geometry = FretboardGeometry(
        size: size,
        fretCount: fretCount,
        stringCount: stringCount,
        isLeftHanded: isLeftHanded
    )
    var noteX: [CGFloat] = []
    var fretWireX: [CGFloat] = []
    for fret in 0...fretCount {
        noteX.append(geometry.noteX(fret: fret))
        fretWireX.append(geometry.fretWireX(fret: fret))
    }
    var stringY: [CGFloat] = []
    var thinOnTop: [Int] = []
    var thickOnTop: [Int] = []
    var strokeWidths: [CGFloat] = []
    for row in 0..<stringCount {
        stringY.append(geometry.stringY(row: row))
        let thinIndex = FretboardGeometry.stringIndex(
            forRow: row,
            stringCount: stringCount,
            thinStringOnTop: true
        )
        let thickIndex = FretboardGeometry.stringIndex(
            forRow: row,
            stringCount: stringCount,
            thinStringOnTop: false
        )
        thinOnTop.append(thinIndex)
        thickOnTop.append(thickIndex)
        strokeWidths.append(geometry.stringStrokeWidth(stringIndex: row, stringCount: stringCount))
    }
    let diameter = geometry.noteRadius * 2
    let shortLabel = FretboardGeometry.noteLabelFontSize(
        noteRadius: geometry.noteRadius,
        labelLength: 1
    )
    let longLabel = FretboardGeometry.noteLabelFontSize(
        noteRadius: geometry.noteRadius,
        labelLength: 2
    )
    return FretboardGeometryAnswer(
        width: rounded(size.width),
        height: rounded(size.height),
        fretCount: fretCount,
        stringCount: stringCount,
        isLeftHanded: isLeftHanded,
        columnUnits: rounded(FretboardGeometry.columnUnits(fretCount: fretCount)),
        cellWidth: rounded(geometry.cellWidth),
        rowHeight: rounded(geometry.rowHeight),
        noteRadius: rounded(geometry.noteRadius),
        nutX: rounded(geometry.nutX),
        tuningLabelX: rounded(geometry.tuningLabelX),
        fretNumberY: rounded(geometry.fretNumberY),
        inlayY: rounded(geometry.inlayY(stringCount: stringCount)),
        noteX: rounded(noteX),
        fretWireX: rounded(fretWireX),
        stringY: rounded(stringY),
        stringIndexThinOnTop: thinOnTop,
        stringIndexThickOnTop: thickOnTop,
        stringStrokeWidths: rounded(strokeWidths),
        inlayRadius: rounded(geometry.inlayRadius),
        doubleInlayOffset: rounded(geometry.doubleInlayOffset),
        fretWireWidth: rounded(geometry.fretWireWidth),
        nutWidth: rounded(geometry.nutWidth),
        fretNumberFontSize: rounded(geometry.fretNumberFontSize),
        openStringLabelFontSize: rounded(geometry.openStringLabelFontSize),
        noteLabelFontSizeShort: rounded(shortLabel),
        noteLabelFontSizeLong: rounded(longLabel),
        chordRingLineWidth: rounded(FretboardGeometry.chordRingLineWidth(circleDiameter: diameter)),
        chordRingOutset: rounded(FretboardGeometry.chordRingOutset(circleDiameter: diameter))
    )
}

// MARK: - keyboard

struct KeyGeometryAnswer: Encodable {
    let pitchClass: Int
    let isBlack: Bool
    let rect: RectAnswer
    let circleRadius: Double
    let circleCenter: PointAnswer
    let keyNameFontSize: Double
    let keyNameCenter: PointAnswer
}

struct KeyboardGeometryAnswer: Encodable {
    let width: Double
    let height: Double
    let octaves: Int
    let whiteKeyCount: Int
    let widthToHeight: Double
    let whiteKeyWidth: Double
    let keyHeight: Double
    let originX: Double
    let originY: Double
    let outlineWidth: Double
    /// Every key, left to right, in `KeyboardLayout.keys` order.
    let keys: [KeyGeometryAnswer]
}

struct KeyboardConstantsAnswer: Encodable {
    let whiteKeyHeightToWidth: Double
    let blackKeyWidthRatio: Double
    let blackKeyHeightRatio: Double
    let whiteKeyCircleShare: Double
    let blackKeyCircleShare: Double
}

func keyboardGeometryAnswer(size: CGSize, octaves: Int) -> KeyboardGeometryAnswer {
    let layout = KeyboardLayout(
        scale: Scale(root: NoteName(.g), type: .major),
        octaveCount: octaves
    )
    let whiteKeyCount = layout.whiteKeys.count
    let geometry = KeyboardGeometry(size: size, whiteKeyCount: whiteKeyCount)
    let rects = geometry.keyRects(for: layout.keys)
    var keys: [KeyGeometryAnswer] = []
    for (index, key) in layout.keys.enumerated() {
        let rect = rects[index]
        let radius = KeyboardGeometry.circleRadius(keyWidth: rect.width, isBlack: key.isBlack)
        let center = KeyboardGeometry.circleCenter(onKey: rect, radius: radius)
        let fontSize = KeyboardGeometry.keyNameFontSize(keyWidth: rect.width, isBlack: key.isBlack)
        let nameCenter = KeyboardGeometry.keyNameCenter(
            onKey: rect,
            circleTop: center.y - radius,
            fontSize: fontSize
        )
        let answer = KeyGeometryAnswer(
            pitchClass: key.pitchClass,
            isBlack: key.isBlack,
            rect: RectAnswer(rect),
            circleRadius: rounded(radius),
            circleCenter: PointAnswer(center),
            keyNameFontSize: rounded(fontSize),
            keyNameCenter: PointAnswer(nameCenter)
        )
        keys.append(answer)
    }
    return KeyboardGeometryAnswer(
        width: rounded(size.width),
        height: rounded(size.height),
        octaves: octaves,
        whiteKeyCount: whiteKeyCount,
        widthToHeight: rounded(KeyboardGeometry.widthToHeight(whiteKeyCount: whiteKeyCount)),
        whiteKeyWidth: rounded(geometry.whiteKeyWidth),
        keyHeight: rounded(geometry.keyHeight),
        originX: rounded(geometry.originX),
        originY: rounded(geometry.originY),
        outlineWidth: rounded(geometry.outlineWidth),
        keys: keys
    )
}

// MARK: - drum staff

struct DrumStaffAnswer: Encodable {
    let groove: String
    let width: Double
    let height: Double
    let lineSpacing: Double
    let staffTop: Double
    let staffBottom: Double
    let staffLeft: Double
    let staffRight: Double
    let beamY: Double
    let countY: Double
    let ballRestY: Double
    let bounceHeight: Double
    let staffLineWidth: Double
    let barLineWidth: Double
    let stemWidth: Double
    let beamWidth: Double
    let secondBeamOffset: Double
    let headRadius: Double
    let ballRadius: Double
    let countFontSize: Double
    let countMarkerWidth: Double
    let columnWidth: Double
    /// One entry per step.
    let columnX: [Double]
    let stemX: [Double]
    /// By `DrumVoice` case name.
    let headY: [String: Double]
}

func drumStaffAnswer(_ groove: DrumGroove, size: CGSize) -> DrumStaffAnswer {
    let layout = DrumStaffLayout(groove: groove, size: size)
    var columnX: [CGFloat] = []
    var stemX: [CGFloat] = []
    for step in 0..<groove.stepsPerBar {
        columnX.append(layout.columnX(step))
        stemX.append(layout.stemX(step))
    }
    // Encoded with sorted keys, so the dictionary's own order never reaches the file.
    var headY: [String: Double] = [:]
    for voice in DrumVoice.allCases {
        headY[voiceID(voice)] = rounded(layout.headY(for: voice))
    }
    return DrumStaffAnswer(
        groove: groove.name,
        width: rounded(size.width),
        height: rounded(size.height),
        lineSpacing: rounded(layout.lineSpacing),
        staffTop: rounded(layout.staffTop),
        staffBottom: rounded(layout.staffBottom),
        staffLeft: rounded(layout.staffLeft),
        staffRight: rounded(layout.staffRight),
        beamY: rounded(layout.beamY),
        countY: rounded(layout.countY),
        ballRestY: rounded(layout.ballRestY),
        bounceHeight: rounded(layout.bounceHeight),
        staffLineWidth: rounded(layout.staffLineWidth),
        barLineWidth: rounded(layout.barLineWidth),
        stemWidth: rounded(layout.stemWidth),
        beamWidth: rounded(layout.beamWidth),
        secondBeamOffset: rounded(layout.secondBeamOffset),
        headRadius: rounded(layout.headRadius),
        ballRadius: rounded(layout.ballRadius),
        countFontSize: rounded(layout.countFontSize),
        countMarkerWidth: rounded(layout.countMarkerWidth),
        columnWidth: rounded(layout.columnWidth),
        columnX: rounded(columnX),
        stemX: rounded(stemX),
        headY: headY
    )
}

// MARK: - TV layout

struct TVLayoutAnswer: Encodable {
    let width: Double
    let height: Double
    let unit: Double
    let spacing: Double
    let titleFontSize: Double
    let captionFontSize: Double
    let notesFontSize: Double
    let closeIconSize: Double
    let closeLeadingPadding: Double
    let horizontalMargin: Double
    let verticalMargin: Double
}

func tvLayoutAnswer(size: CGSize) -> TVLayoutAnswer {
    let metrics = TVLayoutMetrics(size: size)
    return TVLayoutAnswer(
        width: rounded(size.width),
        height: rounded(size.height),
        unit: rounded(metrics.unit),
        spacing: rounded(metrics.spacing),
        titleFontSize: rounded(metrics.titleFontSize),
        captionFontSize: rounded(metrics.captionFontSize),
        notesFontSize: rounded(metrics.notesFontSize),
        closeIconSize: rounded(metrics.closeIconSize),
        closeLeadingPadding: rounded(metrics.closeLeadingPadding),
        horizontalMargin: rounded(metrics.horizontalMargin),
        verticalMargin: rounded(metrics.verticalMargin)
    )
}

// MARK: - chord tables

struct ChordTableMetricsAnswer: Encodable {
    let name: String
    let keyColumnWidth: Double
    let chordColumnWidth: Double
    let cellHeight: Double
    let columnSpacing: Double
    /// `ChordTableTextStyle` raw values, named after the iOS text styles.
    let keyStyle: String
    let chordStyle: String
    let smallStyle: String
    let headerStyle: String
    let footnoteStyle: String
    /// Null on the iPhone, where the table already fills the screen.
    let contentMaxWidth: Double?
}

func chordTableMetricsAnswer(name: String, _ metrics: ChordTableMetrics) -> ChordTableMetricsAnswer
{
    var contentMaxWidth: Double?
    if let width = metrics.contentMaxWidth {
        contentMaxWidth = rounded(width)
    }
    return ChordTableMetricsAnswer(
        name: name,
        keyColumnWidth: rounded(metrics.keyColumnWidth),
        chordColumnWidth: rounded(metrics.chordColumnWidth),
        cellHeight: rounded(metrics.cellHeight),
        columnSpacing: rounded(ChordTableMetrics.columnSpacing),
        keyStyle: metrics.keyStyle.rawValue,
        chordStyle: metrics.chordStyle.rawValue,
        smallStyle: metrics.smallStyle.rawValue,
        headerStyle: metrics.headerStyle.rawValue,
        footnoteStyle: metrics.footnoteStyle.rawValue,
        contentMaxWidth: contentMaxWidth
    )
}

// MARK: - the section

struct GeometrySection: Encodable {
    let fretboardConstants: FretboardConstantsAnswer
    let fretboards: [FretboardGeometryAnswer]
    let keyboardConstants: KeyboardConstantsAnswer
    let keyboards: [KeyboardGeometryAnswer]
    let drumStaffs: [DrumStaffAnswer]
    let tvLayouts: [TVLayoutAnswer]
    let chordTableMetrics: [ChordTableMetricsAnswer]
}

func geometrySection() throws -> GeometrySection {
    let fretboardConstants = FretboardConstantsAnswer(
        tuningColumnShare: rounded(FretboardGeometry.tuningColumnShare),
        numberBandShare: rounded(FretboardGeometry.numberBandShare),
        inlayBandShare: rounded(FretboardGeometry.inlayBandShare),
        singleInlayFrets: FretboardGeometry.singleInlayFrets.sorted(),
        doubleInlayFrets: FretboardGeometry.doubleInlayFrets.sorted()
    )
    var fretboards: [FretboardGeometryAnswer] = []
    let boardSizes = [CGSize(width: 1920, height: 1080), CGSize(width: 390, height: 300)]
    for size in boardSizes {
        for fretCount in [15, 24] {
            for isLeftHanded in [false, true] {
                let answer = fretboardGeometryAnswer(
                    size: size,
                    fretCount: fretCount,
                    isLeftHanded: isLeftHanded
                )
                fretboards.append(answer)
            }
        }
    }

    let keyboardConstants = KeyboardConstantsAnswer(
        whiteKeyHeightToWidth: rounded(KeyboardGeometry.whiteKeyHeightToWidth),
        blackKeyWidthRatio: rounded(KeyboardGeometry.blackKeyWidthRatio),
        blackKeyHeightRatio: rounded(KeyboardGeometry.blackKeyHeightRatio),
        whiteKeyCircleShare: rounded(KeyboardGeometry.whiteKeyCircleShare),
        blackKeyCircleShare: rounded(KeyboardGeometry.blackKeyCircleShare)
    )
    var keyboards: [KeyboardGeometryAnswer] = []
    for octaves in [1, 2] {
        keyboards.append(
            keyboardGeometryAnswer(size: CGSize(width: 1200, height: 400), octaves: octaves)
        )
    }

    var drumStaffs: [DrumStaffAnswer] = []
    for name in ["Basic Rock", "Sixteenth Hats"] {
        guard let groove = DrumGroove.builtIn.first(where: { $0.name == name }) else {
            throw AnswersError.fixtureFailed("\(name) is missing")
        }
        drumStaffs.append(drumStaffAnswer(groove, size: CGSize(width: 900, height: 300)))
    }

    var tvLayouts: [TVLayoutAnswer] = []
    for size in [CGSize(width: 1920, height: 1080), CGSize(width: 1280, height: 720)] {
        tvLayouts.append(tvLayoutAnswer(size: size))
    }

    let tables = [
        chordTableMetricsAnswer(name: "compact", ChordTableMetrics.compact),
        chordTableMetricsAnswer(name: "roomy", ChordTableMetrics.roomy),
    ]

    return GeometrySection(
        fretboardConstants: fretboardConstants,
        fretboards: fretboards,
        keyboardConstants: keyboardConstants,
        keyboards: keyboards,
        drumStaffs: drumStaffs,
        tvLayouts: tvLayouts,
        chordTableMetrics: tables
    )
}
