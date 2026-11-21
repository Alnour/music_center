// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

/// Draws the neck with every scale tone circled and the roots highlighted.
/// Shared by the phone screen, the TV screen and the full-screen board.
struct FretboardView: View {
    let layout: FretboardLayout
    let labelStyle: NoteLabelStyle
    let isLeftHanded: Bool
    let thinStringOnTop: Bool
    let palette: FretboardPalette
    /// Notes to ring on the board, the notes of the chord the player picked.
    var ringedNotes: Set<NoteName> = []

    var body: some View {
        Canvas { context, size in
            let geometry = FretboardGeometry(
                size: size,
                fretCount: layout.fretCount,
                stringCount: layout.strings.count,
                isLeftHanded: isLeftHanded
            )
            drawFretNumbers(context, geometry)
            drawInlays(context, geometry)
            drawFretWires(context, geometry)
            drawStrings(context, geometry)
            drawCapo(context, geometry)
            drawTones(context, geometry)
        }
    }

    /// Which string sits on a given row; see `FretboardGeometry.stringIndex`.
    private func stringIndex(forRow row: Int) -> Int {
        FretboardGeometry.stringIndex(
            forRow: row,
            stringCount: layout.strings.count,
            thinStringOnTop: thinStringOnTop
        )
    }

    private func drawFretNumbers(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        let fontSize = geometry.fretNumberFontSize
        for fret in 1...layout.fretCount {
            let label = Text(String(fret))
                .font(.system(size: fontSize, weight: .medium, design: .rounded))
                .foregroundStyle(palette.fretNumber)
            let point = CGPoint(x: geometry.noteX(fret: fret), y: geometry.fretNumberY)
            context.draw(label, at: point, anchor: .center)
        }
    }

    private func drawInlays(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        let radius = geometry.inlayRadius
        let y = geometry.inlayY(stringCount: layout.strings.count)
        for fret in 1...layout.fretCount {
            let centerX = geometry.noteX(fret: fret)
            if FretboardGeometry.singleInlayFrets.contains(fret) {
                fillDot(context, center: CGPoint(x: centerX, y: y), radius: radius)
            } else if FretboardGeometry.doubleInlayFrets.contains(fret) {
                let offset = geometry.doubleInlayOffset
                fillDot(context, center: CGPoint(x: centerX - offset, y: y), radius: radius)
                fillDot(context, center: CGPoint(x: centerX + offset, y: y), radius: radius)
            }
        }
    }

    private func fillDot(_ context: GraphicsContext, center: CGPoint, radius: CGFloat) {
        let rect = CGRect(
            x: center.x - radius,
            y: center.y - radius,
            width: radius * 2,
            height: radius * 2
        )
        context.fill(Path(ellipseIn: rect), with: .color(palette.inlay))
    }

    private func drawFretWires(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        let lastRow = layout.strings.count - 1
        let top = geometry.stringY(row: 0)
        let bottom = geometry.stringY(row: lastRow)
        let wireWidth = geometry.fretWireWidth
        for fret in 1...layout.fretCount {
            let x = geometry.fretWireX(fret: fret)
            let wire = verticalLine(x: x, from: top, to: bottom)
            context.stroke(wire, with: .color(palette.fretWire), lineWidth: wireWidth)
        }
        let nut = verticalLine(x: geometry.nutX, from: top, to: bottom)
        let nutWidth = geometry.nutWidth
        context.stroke(nut, with: .color(palette.nut), lineWidth: nutWidth)
    }

    private func verticalLine(x: CGFloat, from top: CGFloat, to bottom: CGFloat) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: x, y: top))
        path.addLine(to: CGPoint(x: x, y: bottom))
        return path
    }

    /// Strings get slightly thicker toward the bass side, so the board reads like a real
    /// neck at a glance. Each string's open note is printed beside the nut.
    private func drawStrings(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        let startX = geometry.nutX
        let endX = geometry.fretWireX(fret: layout.fretCount)
        let labelSize = geometry.openStringLabelFontSize
        let stringCount = layout.strings.count
        for row in 0..<stringCount {
            let index = stringIndex(forRow: row)
            let y = geometry.stringY(row: row)
            var line = Path()
            line.move(to: CGPoint(x: startX, y: y))
            line.addLine(to: CGPoint(x: endX, y: y))
            let lineWidth = geometry.stringStrokeWidth(stringIndex: index, stringCount: stringCount)
            context.stroke(line, with: .color(palette.string), lineWidth: lineWidth)

            let openNoteName = layout.strings[index].openNote.displayName
            let label = Text(openNoteName)
                .font(.system(size: labelSize, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.fretNumber)
            context.draw(label, at: CGPoint(x: geometry.tuningLabelX, y: y), anchor: .center)
        }
    }

    /// The capo's bar across the strings, under the notes of its own fret.
    private func drawCapo(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        guard layout.capoFret > 0 else { return }
        let bar = geometry.capoBarRect(fret: layout.capoFret, stringCount: layout.strings.count)
        let cornerRadius = bar.width / 2
        let path = Path(roundedRect: bar, cornerRadius: cornerRadius)
        context.fill(path, with: .color(palette.capo))
    }

    private func drawTones(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        let radius = geometry.noteRadius
        for row in 0..<layout.strings.count {
            let guitarString = layout.strings[stringIndex(forRow: row)]
            let y = geometry.stringY(row: row)
            for (fret, tone) in guitarString.tonesByFret.enumerated() {
                guard let tone else { continue }
                let center = CGPoint(x: geometry.noteX(fret: fret), y: y)
                drawTone(tone, at: center, radius: radius, in: context)
            }
        }
    }

    private func drawTone(
        _ tone: ScaleTone,
        at center: CGPoint,
        radius: CGFloat,
        in context: GraphicsContext
    ) {
        let rect = CGRect(
            x: center.x - radius,
            y: center.y - radius,
            width: radius * 2,
            height: radius * 2
        )
        let isRinged = ringedNotes.contains(tone.name)
        let colors = palette.toneColors(isRoot: tone.isRoot, isInChord: isRinged)
        context.fill(Path(ellipseIn: rect), with: .color(colors.fill))
        if isRinged {
            ChordRing.draw(around: rect, color: palette.chordRing, in: context)
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

    private func label(for tone: ScaleTone) -> String? {
        switch labelStyle {
        case .noteNames: tone.name.displayName
        case .degrees: tone.degree.label
        case .none: nil
        }
    }
}

/// The ring drawn around a note that belongs to the chord the player picked, in the
/// palette's chord colour, just outside the note's circle so the label stays clear.
enum ChordRing {
    static func draw(around circle: CGRect, color: Color, in context: GraphicsContext) {
        let lineWidth = FretboardGeometry.chordRingLineWidth(circleDiameter: circle.width)
        let outset = FretboardGeometry.chordRingOutset(circleDiameter: circle.width)
        let ring = circle.insetBy(dx: -outset, dy: -outset)
        context.stroke(Path(ellipseIn: ring), with: .color(color), lineWidth: lineWidth)
    }
}

extension FretboardPalette {
    /// Fill and text for one note circle. A chord note takes the green chord colours; the
    /// scale's root keeps its own colour even inside a chord, because losing the root
    /// marker while a chord rings would confuse more than it helps. The ring says "chord".
    func toneColors(isRoot: Bool, isInChord: Bool) -> (fill: Color, text: Color) {
        if isRoot {
            return (rootFill, rootText)
        }
        if isInChord {
            return (chordFill, chordText)
        }
        return (toneFill, toneText)
    }
}
