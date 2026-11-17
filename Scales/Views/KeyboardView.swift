import SwiftUI

/// Draws one piano octave with the scale's keys coloured and the root picked out.
/// Shared by the phone screen, the TV screen and the full-screen board, like `FretboardView`.
struct KeyboardView: View {
    let layout: KeyboardLayout
    let labelStyle: NoteLabelStyle
    let palette: FretboardPalette
    /// Notes to ring on the keys, the notes of the chord the player picked.
    var ringedNotes: Set<NoteName> = []

    /// A white key is this many times taller than it is wide, so one octave of eight white
    /// keys is 7:3 and two octaves are proportionally wider. The keys keep this shape and
    /// are centred in whatever frame they are given, so the TV and the phone match.
    private static let whiteKeyHeightToWidth: CGFloat = 24.0 / 7.0

    static func widthToHeight(whiteKeyCount: Int) -> CGFloat {
        CGFloat(whiteKeyCount) / whiteKeyHeightToWidth
    }
    private static let blackKeyWidthRatio: CGFloat = 0.6
    private static let blackKeyHeightRatio: CGFloat = 0.6

    var body: some View {
        Canvas { context, size in
            let geometry = KeyboardGeometry(size: size, whiteKeyCount: layout.whiteKeys.count)
            drawWhiteKeys(context, geometry)
            drawBlackKeys(context, geometry)
        }
    }

    private func drawWhiteKeys(_ context: GraphicsContext, _ geometry: KeyboardGeometry) {
        let outlineWidth = max(1, geometry.whiteKeyWidth * 0.03)
        for (index, key) in layout.whiteKeys.enumerated() {
            let rect = geometry.whiteKeyRect(index: index)
            context.fill(Path(rect), with: .color(palette.whiteKey))
            context.stroke(Path(rect), with: .color(palette.keyOutline), lineWidth: outlineWidth)
            drawTone(for: key, onKey: rect, radius: geometry.whiteKeyWidth * 0.36, context)
        }
    }

    /// Each black key straddles the boundary after the white key before it, so the white
    /// keys are counted up while walking the octave in order.
    private func drawBlackKeys(_ context: GraphicsContext, _ geometry: KeyboardGeometry) {
        var whiteKeysPassed = 0
        for key in layout.keys {
            if !key.isBlack {
                whiteKeysPassed += 1
                continue
            }
            let rect = geometry.blackKeyRect(
                afterWhiteKey: whiteKeysPassed - 1,
                widthRatio: Self.blackKeyWidthRatio,
                heightRatio: Self.blackKeyHeightRatio
            )
            context.fill(Path(rect), with: .color(palette.blackKey))
            drawTone(for: key, onKey: rect, radius: rect.width * 0.4, context)
        }
    }

    /// A key in the scale gets the same circle as a fret on the guitar, sitting near the
    /// bottom of the key where a pianist's eye lands. The key itself keeps its natural
    /// colour, so a black key in the scale still reads as a black key.
    private func drawTone(
        for key: KeyboardLayout.Key,
        onKey keyRect: CGRect,
        radius: CGFloat,
        _ context: GraphicsContext
    ) {
        guard let tone = key.tone else { return }
        let center = CGPoint(x: keyRect.midX, y: keyRect.maxY - radius * 1.4)
        drawKeyName(tone, onKey: keyRect, above: center.y - radius, isBlack: key.isBlack, context)
        let circle = CGRect(
            x: center.x - radius,
            y: center.y - radius,
            width: radius * 2,
            height: radius * 2
        )
        let fill = tone.isRoot ? palette.rootFill : palette.toneFill
        context.fill(Path(ellipseIn: circle), with: .color(fill))
        if ringedNotes.contains(tone.name) {
            ChordRing.draw(around: circle, in: context)
        }

        guard let labelText = label(for: tone) else { return }
        let fontScale: CGFloat = labelText.count > 1 ? 0.78 : 0.95
        let textColor = tone.isRoot ? palette.rootText : palette.toneText
        let label = Text(labelText)
            .font(.system(size: radius * fontScale, weight: .bold, design: .rounded))
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
        let fontSize = keyRect.width * (isBlack ? 0.4 : 0.26)
        let textColor = isBlack ? palette.whiteKey : palette.fretNumber
        let name = Text(tone.name.displayName)
            .font(.system(size: fontSize, weight: .semibold, design: .rounded))
            .foregroundStyle(textColor)
        let point = CGPoint(x: keyRect.midX, y: circleTop - fontSize * 0.9)
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

/// Where each key lands inside the canvas: the octave keeps its shape and is centred.
private struct KeyboardGeometry {
    let whiteKeyWidth: CGFloat
    let keyHeight: CGFloat
    let originX: CGFloat
    let originY: CGFloat

    init(size: CGSize, whiteKeyCount: Int) {
        let widthToHeight = KeyboardView.widthToHeight(whiteKeyCount: whiteKeyCount)
        let fittedWidth = min(size.width, size.height * widthToHeight)
        let fittedHeight = fittedWidth / widthToHeight
        whiteKeyWidth = fittedWidth / CGFloat(whiteKeyCount)
        keyHeight = fittedHeight
        originX = (size.width - fittedWidth) / 2
        originY = (size.height - fittedHeight) / 2
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
    func blackKeyRect(afterWhiteKey index: Int, widthRatio: CGFloat, heightRatio: CGFloat) -> CGRect
    {
        let width = whiteKeyWidth * widthRatio
        let boundaryX = originX + CGFloat(index + 1) * whiteKeyWidth
        return CGRect(
            x: boundaryX - width / 2,
            y: originY,
            width: width,
            height: keyHeight * heightRatio
        )
    }
}
