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
