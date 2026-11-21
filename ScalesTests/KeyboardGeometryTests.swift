// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics
import Testing

@testable import Scales

/// How the keys are shaped: true piano proportions everywhere but the full-screen board on
/// a phone, where they stretch taller so the keyboard is not a strip across the screen.
struct KeyboardGeometryTests {
    /// Two octaves on a phone turned sideways would be a strip at their true shape, so the
    /// keys grow to fill the frame, but never past twice their true shape.
    // feature: keyboard, tv-board
    @Test func keysStretchToFillAShortWideFrame() {
        let phoneFrame = CGSize(width: 735, height: 300)
        let stretched = KeyboardGeometry.stretchedHeightToWidth(size: phoneFrame, whiteKeyCount: 15)
        #expect(stretched > KeyboardGeometry.whiteKeyHeightToWidth)
        #expect(stretched <= KeyboardGeometry.maxStretchedHeightToWidth)

        let geometry = KeyboardGeometry(
            size: phoneFrame,
            whiteKeyCount: 15,
            whiteKeyHeightToWidth: stretched
        )
        #expect(isClose(geometry.keyHeight, 300))
        #expect(isClose(geometry.originY, 0))
    }

    /// A frame already taller than the keys' true shape (one octave on the same phone) keeps
    /// the true shape: stretching only ever makes keys taller, never squatter.
    // feature: keyboard, tv-board
    @Test func keysKeepTheirTrueShapeInATallFrame() {
        let frame = CGSize(width: 735, height: 300)
        let shape = KeyboardGeometry.stretchedHeightToWidth(size: frame, whiteKeyCount: 8)
        #expect(isClose(shape, KeyboardGeometry.whiteKeyHeightToWidth))
    }

    /// Without a shape of its own, the geometry is the one the TV and the answers file pin.
    // feature: keyboard
    @Test func defaultShapeIsTheTruePianoShape() {
        let frame = CGSize(width: 1920, height: 1080)
        let geometry = KeyboardGeometry(size: frame, whiteKeyCount: 15)
        let expectedHeight = 1920 / 15 * KeyboardGeometry.whiteKeyHeightToWidth
        #expect(isClose(geometry.keyHeight, expectedHeight))
    }

    private func isClose(_ first: CGFloat, _ second: CGFloat) -> Bool {
        abs(first - second) < 0.000_001
    }
}
