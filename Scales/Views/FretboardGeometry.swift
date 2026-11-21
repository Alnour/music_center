// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics

/// Turns string and fret numbers into points on the canvas.
///
/// Frets are evenly spaced on purpose, as in the reference chart: real guitars narrow
/// toward the body, but equal cells give every note circle the same size, which keeps
/// the high frets readable from across the room.
///
/// Columns from left to right (right-handed): a narrow column naming each open string,
/// one column for open-string notes, then one column per fret. Rows from top to bottom:
/// a band of fret numbers, one row per string, then a band for the inlay dots.
struct FretboardGeometry {
    static let tuningColumnShare: CGFloat = 0.6
    static let numberBandShare: CGFloat = 0.8
    static let inlayBandShare: CGFloat = 0.6

    let size: CGSize
    let isLeftHanded: Bool
    let cellWidth: CGFloat
    let rowHeight: CGFloat

    init(size: CGSize, fretCount: Int, stringCount: Int, isLeftHanded: Bool) {
        let rowUnits = CGFloat(stringCount) + Self.numberBandShare + Self.inlayBandShare
        self.size = size
        self.isLeftHanded = isLeftHanded
        self.cellWidth = size.width / Self.columnUnits(fretCount: fretCount)
        self.rowHeight = size.height / rowUnits
    }

    /// How many cell widths the whole board needs; lets callers size a scrolling board.
    static func columnUnits(fretCount: Int) -> CGFloat {
        tuningColumnShare + 1 + CGFloat(fretCount)
    }

    var noteRadius: CGFloat {
        min(cellWidth, rowHeight) * 0.42
    }

    var tuningLabelX: CGFloat {
        placed(cellWidth * Self.tuningColumnShare / 2)
    }

    var nutX: CGFloat {
        placed(unplacedNutX)
    }

    /// Centre of the cell where a note on this fret is drawn; fret 0 sits left of the nut.
    func noteX(fret: Int) -> CGFloat {
        if fret == 0 {
            return placed(unplacedNutX - cellWidth / 2)
        }
        return placed(unplacedNutX + (CGFloat(fret) - 0.5) * cellWidth)
    }

    func fretWireX(fret: Int) -> CGFloat {
        placed(unplacedNutX + CGFloat(fret) * cellWidth)
    }

    /// Vertical centre of a string, counting rows from the top of the board.
    func stringY(row: Int) -> CGFloat {
        numberBandHeight + (CGFloat(row) + 0.5) * rowHeight
    }

    var fretNumberY: CGFloat {
        numberBandHeight / 2
    }

    func inlayY(stringCount: Int) -> CGFloat {
        numberBandHeight + CGFloat(stringCount) * rowHeight + inlayBandHeight / 2
    }

    private var numberBandHeight: CGFloat {
        rowHeight * Self.numberBandShare
    }

    private var inlayBandHeight: CGFloat {
        rowHeight * Self.inlayBandShare
    }

    private var unplacedNutX: CGFloat {
        cellWidth * (Self.tuningColumnShare + 1)
    }

    /// Mirrors a right-handed x position for left-handed players.
    private func placed(_ x: CGFloat) -> CGFloat {
        isLeftHanded ? size.width - x : x
    }
}

// MARK: - Drawing sizes

/// The sizes the board is drawn with, kept beside the positions rather than inside the view
/// so they are plain numbers: the Android board mirrors them and `expected/answers.json`
/// pins them, which only works if they can be computed without SwiftUI.
extension FretboardGeometry {
    /// Frets marked with one inlay dot below the strings, as on most guitars.
    static let singleInlayFrets: Set<Int> = [3, 5, 7, 9, 15, 17, 19, 21]
    /// Octave frets, marked with two dots side by side.
    static let doubleInlayFrets: Set<Int> = [12, 24]

    /// Which string sits on a given row. `FretboardLayout` lists the thickest string first,
    /// so the order flips when the thin string should be on top, as in tab.
    static func stringIndex(forRow row: Int, stringCount: Int, thinStringOnTop: Bool) -> Int {
        let lastIndex = stringCount - 1
        if thinStringOnTop {
            return lastIndex - row
        }
        return row
    }

    /// Line width of one string. Strings get slightly thicker toward the bass side, so the
    /// board reads like a real neck at a glance; `stringIndex` is the layout's index, where
    /// 0 is the thickest string.
    func stringStrokeWidth(stringIndex: Int, stringCount: Int) -> CGFloat {
        let baseWidth = max(1, rowHeight * 0.025)
        let bassRank = CGFloat(stringCount - 1 - stringIndex)
        return baseWidth * (1 + 0.2 * bassRank)
    }

    /// Radius of an inlay dot: a small fraction of a note circle, never vanishing on a
    /// small board.
    var inlayRadius: CGFloat {
        max(2, noteRadius * 0.16)
    }

    /// Gap between the two dots of a double inlay and the fret's centre.
    var doubleInlayOffset: CGFloat {
        inlayRadius * 1.8
    }

    var fretWireWidth: CGFloat {
        max(1, cellWidth * 0.02)
    }

    /// The nut is drawn thicker than a fret wire, as on a guitar.
    var nutWidth: CGFloat {
        max(3, cellWidth * 0.06)
    }

    /// The capo's bar: a rounded bar across every string, centred in the capo fret's cell
    /// and reaching a little past the outer strings, as a real capo does. It is narrower
    /// than a note circle, so the notes on the capo's fret (the new open strings) are
    /// drawn over it and stay readable.
    func capoBarRect(fret: Int, stringCount: Int) -> CGRect {
        let width = cellWidth * 0.3
        let overhang = rowHeight * 0.46
        let top = stringY(row: 0) - overhang
        let bottom = stringY(row: stringCount - 1) + overhang
        return CGRect(
            x: noteX(fret: fret) - width / 2,
            y: top,
            width: width,
            height: bottom - top
        )
    }

    /// The fret numbers above the board.
    var fretNumberFontSize: CGFloat {
        min(rowHeight, cellWidth) * 0.38
    }

    /// The open-string names left of the nut.
    var openStringLabelFontSize: CGFloat {
        min(rowHeight, cellWidth) * 0.32
    }

    /// The label inside a note circle. Two-character labels ("F♯", "♭3") get a smaller size
    /// than one-character ones so both fit inside the same circle; shared with the keyboard.
    static func noteLabelFontSize(noteRadius: CGFloat, labelLength: Int) -> CGFloat {
        let fontScale: CGFloat = labelLength > 1 ? 0.78 : 0.95
        return noteRadius * fontScale
    }

    /// Line width of the ring around a note of the picked chord, on the fretboard and on the
    /// keyboard alike: a tenth of the circle's diameter, never thinner than a hairline.
    static func chordRingLineWidth(circleDiameter: CGFloat) -> CGFloat {
        max(1.5, circleDiameter * 0.1)
    }

    /// How far outside the note's circle the ring is drawn, so the label stays clear.
    static func chordRingOutset(circleDiameter: CGFloat) -> CGFloat {
        chordRingLineWidth(circleDiameter: circleDiameter) * 0.9
    }
}
