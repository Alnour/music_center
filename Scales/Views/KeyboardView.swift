// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

/// Draws one piano octave with the scale's keys coloured and the root picked out.
/// Shared by the phone screen, the TV screen and the full-screen board, like `FretboardView`.
struct KeyboardView: View {
    let layout: KeyboardLayout
    let labelStyle: NoteLabelStyle
    let palette: FretboardPalette
    /// Notes to ring on the keys, the notes of the chord the player picked.
    var ringedNotes: Set<NoteName> = []
    /// Stretch the keys taller than a real piano's to fill the frame; only the full-screen
    /// board on a phone does, where keys of their true shape would be a strip.
    var stretchesToFill = false

    var body: some View {
        Canvas { context, size in
            let geometry = KeyboardGeometry(
                size: size,
                whiteKeyCount: layout.whiteKeys.count,
                whiteKeyHeightToWidth: keyHeightToWidth(for: size)
            )
            let keyRects = geometry.keyRects(for: layout.keys)
            drawWhiteKeys(context, geometry, keyRects)
            drawBlackKeys(context, keyRects)
        }
    }

    private func keyHeightToWidth(for size: CGSize) -> CGFloat {
        if stretchesToFill {
            return KeyboardGeometry.stretchedHeightToWidth(
                size: size,
                whiteKeyCount: layout.whiteKeys.count
            )
        }
        return KeyboardGeometry.whiteKeyHeightToWidth
    }

    private func drawWhiteKeys(
        _ context: GraphicsContext,
        _ geometry: KeyboardGeometry,
        _ keyRects: [CGRect]
    ) {
        for (index, key) in layout.keys.enumerated() where !key.isBlack {
            let rect = keyRects[index]
            context.fill(Path(rect), with: .color(palette.whiteKey))
            context.stroke(
                Path(rect),
                with: .color(palette.keyOutline),
                lineWidth: geometry.outlineWidth
            )
            drawTone(for: key, onKey: rect, context)
        }
    }

    /// Drawn after the white keys because the black keys sit on top of them.
    private func drawBlackKeys(_ context: GraphicsContext, _ keyRects: [CGRect]) {
        for (index, key) in layout.keys.enumerated() where key.isBlack {
            let rect = keyRects[index]
            context.fill(Path(rect), with: .color(palette.blackKey))
            drawTone(for: key, onKey: rect, context)
        }
    }

    /// A key in the scale gets the same circle as a fret on the guitar, sitting near the
    /// bottom of the key where a pianist's eye lands. The key itself keeps its natural
    /// colour, so a black key in the scale still reads as a black key.
    private func drawTone(
        for key: KeyboardLayout.Key,
        onKey keyRect: CGRect,
        _ context: GraphicsContext
    ) {
        guard let tone = key.tone else { return }
        let radius = KeyboardGeometry.circleRadius(keyWidth: keyRect.width, isBlack: key.isBlack)
        let center = KeyboardGeometry.circleCenter(onKey: keyRect, radius: radius)
        drawKeyName(tone, onKey: keyRect, above: center.y - radius, isBlack: key.isBlack, context)
        let circle = CGRect(
            x: center.x - radius,
            y: center.y - radius,
            width: radius * 2,
            height: radius * 2
        )
        let isRinged = ringedNotes.contains(tone.name)
        let colors = palette.toneColors(isRoot: tone.isRoot, isInChord: isRinged)
        context.fill(Path(ellipseIn: circle), with: .color(colors.fill))
        if isRinged {
            ChordRing.draw(around: circle, color: palette.chordRing, in: context)
        }

        guard let labelText = label(for: tone) else { return }
        let fontSize = FretboardGeometry.noteLabelFontSize(
            noteRadius: radius,
            labelLength: labelText.count
        )
        let textColor = colors.text
        let label = Text(labelText)
            .font(.system(size: fontSize, weight: .bold, design: .rounded))
            .foregroundStyle(textColor)
        context.draw(label, at: center, anchor: .center)
    }

    /// The note's name written on the key itself, above the circle, so the player can read
    /// the scale straight off the keys whatever the circles show. With note names already
    /// in the circles the key would say the same thing twice, so it stays blank then.
    private func drawKeyName(
        _ tone: ScaleTone,
        onKey keyRect: CGRect,
        above circleTop: CGFloat,
        isBlack: Bool,
        _ context: GraphicsContext
    ) {
        guard labelStyle != .noteNames else { return }
        let fontSize = KeyboardGeometry.keyNameFontSize(keyWidth: keyRect.width, isBlack: isBlack)
        let textColor = isBlack ? palette.whiteKey : palette.fretNumber
        let name = Text(tone.name.displayName)
            .font(.system(size: fontSize, weight: .semibold, design: .rounded))
            .foregroundStyle(textColor)
        let point = KeyboardGeometry.keyNameCenter(
            onKey: keyRect,
            circleTop: circleTop,
            fontSize: fontSize
        )
        context.draw(name, at: point, anchor: .center)
    }

    private func label(for tone: ScaleTone) -> String? {
        switch labelStyle {
        case .noteNames: tone.name.displayName
        case .degrees: tone.degree.label
        case .none: nil
        }
    }
}
