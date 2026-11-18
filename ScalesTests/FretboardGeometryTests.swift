// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics
import Testing

@testable import Scales

/// Where strings and frets land: the string order and left-handed mirroring, which the
/// player sets once and expects every board (phone, TV, full screen) to honour.
struct FretboardGeometryTests {
    /// The layout lists the thick low E string first. With the thin string on top (tab
    /// order, the default) row 0 shows the high E; turned off, row 0 shows the low E.
    // feature: left-handed-thin-top
    @Test func stringOrderFollowsTheThinStringOnTopSetting() {
        var thinOnTop: [Int] = []
        var thickOnTop: [Int] = []
        for row in 0..<6 {
            let thinIndex = FretboardGeometry.stringIndex(
                forRow: row,
                stringCount: 6,
                thinStringOnTop: true
            )
            let thickIndex = FretboardGeometry.stringIndex(
                forRow: row,
                stringCount: 6,
                thinStringOnTop: false
            )
            thinOnTop.append(thinIndex)
            thickOnTop.append(thickIndex)
        }
        #expect(thinOnTop == [5, 4, 3, 2, 1, 0])
        #expect(thickOnTop == [0, 1, 2, 3, 4, 5])
    }

    /// A left-handed board is the right-handed one seen in a mirror: every fret, the nut
    /// and the tuning labels sit at the same distance from the other edge, and nothing
    /// moves up or down.
    // feature: left-handed-thin-top
    @Test func leftHandedBoardMirrorsEveryColumn() {
        let size = CGSize(width: 1920, height: 1080)
        let right = FretboardGeometry(
            size: size,
            fretCount: 15,
            stringCount: 6,
            isLeftHanded: false
        )
        let left = FretboardGeometry(
            size: size,
            fretCount: 15,
            stringCount: 6,
            isLeftHanded: true
        )

        #expect(isClose(left.nutX, size.width - right.nutX))
        #expect(isClose(left.tuningLabelX, size.width - right.tuningLabelX))
        for fret in 0...15 {
            #expect(isClose(left.noteX(fret: fret), size.width - right.noteX(fret: fret)))
            #expect(isClose(left.fretWireX(fret: fret), size.width - right.fretWireX(fret: fret)))
        }
        for row in 0..<6 {
            #expect(left.stringY(row: row) == right.stringY(row: row))
        }
        #expect(right.noteX(fret: 0) < right.nutX, "open notes sit left of the nut")
        #expect(left.noteX(fret: 0) > left.nutX, "and right of it when left-handed")
    }

    /// The bass side is drawn thicker: the low E string is the widest line.
    // feature: board
    @Test func bassStringsAreDrawnThicker() {
        let geometry = FretboardGeometry(
            size: CGSize(width: 1920, height: 1080),
            fretCount: 15,
            stringCount: 6,
            isLeftHanded: false
        )
        let lowE = geometry.stringStrokeWidth(stringIndex: 0, stringCount: 6)
        let highE = geometry.stringStrokeWidth(stringIndex: 5, stringCount: 6)
        #expect(lowE > highE)
        #expect(isClose(lowE, highE * 2))
    }

    private func isClose(_ first: CGFloat, _ second: CGFloat) -> Bool {
        abs(first - second) < 0.000_001
    }
}
