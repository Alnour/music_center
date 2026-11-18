// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics
import Foundation

/// Where each key lands inside the canvas: the keyboard keeps its shape and is centred.
///
/// Kept apart from `KeyboardView` and free of SwiftUI so the numbers are plain arithmetic:
/// the Android keyboard mirrors this file and `expected/answers.json` pins every key rect.
struct KeyboardGeometry {
    /// A white key is this many times taller than it is wide, so one octave of eight white
    /// keys is 7:3 and two octaves are proportionally wider. The keys keep this shape and
    /// are centred in whatever frame they are given, so the TV and the phone match.
    static let whiteKeyHeightToWidth: CGFloat = 24.0 / 7.0
    /// A black key's width as a share of a white key's width.
    static let blackKeyWidthRatio: CGFloat = 0.6
    /// A black key's length as a share of a white key's length.
    static let blackKeyHeightRatio: CGFloat = 0.6
    /// Note circle radius as a share of the key's width. Black keys are narrower, so their
    /// circles take a larger share to stay readable.
    static let whiteKeyCircleShare: CGFloat = 0.36
    static let blackKeyCircleShare: CGFloat = 0.4

    let whiteKeyWidth: CGFloat
    let keyHeight: CGFloat
    let originX: CGFloat
    let originY: CGFloat

    /// Width over height of a keyboard with this many white keys; lets callers give the
    /// keyboard a frame of its own shape.
    static func widthToHeight(whiteKeyCount: Int) -> CGFloat {
        CGFloat(whiteKeyCount) / whiteKeyHeightToWidth
    }

    init(size: CGSize, whiteKeyCount: Int) {
        let widthToHeight = Self.widthToHeight(whiteKeyCount: whiteKeyCount)
        let fittedWidth = min(size.width, size.height * widthToHeight)
        let fittedHeight = fittedWidth / widthToHeight
        whiteKeyWidth = fittedWidth / CGFloat(whiteKeyCount)
        keyHeight = fittedHeight
        originX = (size.width - fittedWidth) / 2
        originY = (size.height - fittedHeight) / 2
    }

    /// The outline drawn around each white key.
    var outlineWidth: CGFloat {
        max(1, whiteKeyWidth * 0.03)
    }

    func whiteKeyRect(index: Int) -> CGRect {
        CGRect(
            x: originX + CGFloat(index) * whiteKeyWidth,
            y: originY,
            width: whiteKeyWidth,
            height: keyHeight
        )
    }

    /// Centred on the line between this white key and the next one.
    func blackKeyRect(afterWhiteKey index: Int) -> CGRect {
        let width = whiteKeyWidth * Self.blackKeyWidthRatio
        let boundaryX = originX + CGFloat(index + 1) * whiteKeyWidth
        return CGRect(
            x: boundaryX - width / 2,
            y: originY,
            width: width,
            height: keyHeight * Self.blackKeyHeightRatio
        )
    }

    /// One rect per key, in the layout's left-to-right key order. Each black key straddles
    /// the boundary after the white key before it, so the white keys are counted up while
    /// walking the keys in order.
    func keyRects(for keys: [KeyboardLayout.Key]) -> [CGRect] {
        var rects: [CGRect] = []
        var whiteKeysPassed = 0
        for key in keys {
            if key.isBlack {
                rects.append(blackKeyRect(afterWhiteKey: whiteKeysPassed - 1))
            } else {
                rects.append(whiteKeyRect(index: whiteKeysPassed))
                whiteKeysPassed += 1
            }
        }
        return rects
    }

    /// Radius of the note circle on a key of this width.
    static func circleRadius(keyWidth: CGFloat, isBlack: Bool) -> CGFloat {
        let share = isBlack ? blackKeyCircleShare : whiteKeyCircleShare
        return keyWidth * share
    }

    /// The circle sits near the bottom of the key, where a pianist's eye lands.
    static func circleCenter(onKey keyRect: CGRect, radius: CGFloat) -> CGPoint {
        CGPoint(x: keyRect.midX, y: keyRect.maxY - radius * 1.4)
    }

    /// Size of the note name written on the key above its circle.
    static func keyNameFontSize(keyWidth: CGFloat, isBlack: Bool) -> CGFloat {
        let share: CGFloat = isBlack ? 0.4 : 0.26
        return keyWidth * share
    }

    /// Centre of the note name on the key: just above the circle's top edge.
    static func keyNameCenter(onKey keyRect: CGRect, circleTop: CGFloat, fontSize: CGFloat)
        -> CGPoint
    {
        CGPoint(x: keyRect.midX, y: circleTop - fontSize * 0.9)
    }
}
