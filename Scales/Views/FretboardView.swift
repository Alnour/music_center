import SwiftUI

/// Draws the neck with every scale tone circled and the roots highlighted.
/// Shared by the phone screen, the TV screen and the full-screen board.
struct FretboardView: View {
    let layout: FretboardLayout
    let labelStyle: NoteLabelStyle
    let isLeftHanded: Bool
    let thinStringOnTop: Bool
    let palette: FretboardPalette

    private static let singleInlayFrets: Set<Int> = [3, 5, 7, 9, 15, 17, 19, 21]
    private static let doubleInlayFrets: Set<Int> = [12, 24]

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
            drawTones(context, geometry)
        }
    }

    /// Which string sits on a given row. The layout lists the thickest string first, so
    /// the order flips when the thin string should be on top, as in tab.
    private func stringIndex(forRow row: Int) -> Int {
        let lastIndex = layout.strings.count - 1
        return thinStringOnTop ? lastIndex - row : row
    }

    private func drawFretNumbers(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        let fontSize = min(geometry.rowHeight, geometry.cellWidth) * 0.38
        for fret in 1...layout.fretCount {
            let label = Text(String(fret))
                .font(.system(size: fontSize, weight: .medium, design: .rounded))
                .foregroundStyle(palette.fretNumber)
            let point = CGPoint(x: geometry.noteX(fret: fret), y: geometry.fretNumberY)
            context.draw(label, at: point, anchor: .center)
        }
    }

    private func drawInlays(_ context: GraphicsContext, _ geometry: FretboardGeometry) {
        let radius = max(2, geometry.noteRadius * 0.16)
        let y = geometry.inlayY(stringCount: layout.strings.count)
        for fret in 1...layout.fretCount {
            let centerX = geometry.noteX(fret: fret)
            if Self.singleInlayFrets.contains(fret) {
                fillDot(context, center: CGPoint(x: centerX, y: y), radius: radius)
            } else if Self.doubleInlayFrets.contains(fret) {
                let offset = radius * 1.8
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
        let wireWidth = max(1, geometry.cellWidth * 0.02)
        for fret in 1...layout.fretCount {
            let x = geometry.fretWireX(fret: fret)
            let wire = verticalLine(x: x, from: top, to: bottom)
            context.stroke(wire, with: .color(palette.fretWire), lineWidth: wireWidth)
        }
        let nut = verticalLine(x: geometry.nutX, from: top, to: bottom)
        let nutWidth = max(3, geometry.cellWidth * 0.06)
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
        let baseWidth = max(1, geometry.rowHeight * 0.025)
        let labelSize = min(geometry.rowHeight, geometry.cellWidth) * 0.32
        let stringCount = layout.strings.count
        for row in 0..<stringCount {
            let index = stringIndex(forRow: row)
            let y = geometry.stringY(row: row)
            let bassRank = CGFloat(stringCount - 1 - index)
            var line = Path()
            line.move(to: CGPoint(x: startX, y: y))
            line.addLine(to: CGPoint(x: endX, y: y))
            let lineWidth = baseWidth * (1 + 0.2 * bassRank)
            context.stroke(line, with: .color(palette.string), lineWidth: lineWidth)

            let openNoteName = layout.strings[index].openNote.displayName
            let label = Text(openNoteName)
                .font(.system(size: labelSize, weight: .semibold, design: .rounded))
                .foregroundStyle(palette.fretNumber)
            context.draw(label, at: CGPoint(x: geometry.tuningLabelX, y: y), anchor: .center)
        }
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
        let fill = tone.isRoot ? palette.rootFill : palette.toneFill
        context.fill(Path(ellipseIn: rect), with: .color(fill))

        guard let labelText = label(for: tone) else { return }
        let fontScale: CGFloat = labelText.count > 1 ? 0.78 : 0.95
        let textColor = tone.isRoot ? palette.rootText : palette.toneText
        let label = Text(labelText)
            .font(.system(size: radius * fontScale, weight: .bold, design: .rounded))
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
