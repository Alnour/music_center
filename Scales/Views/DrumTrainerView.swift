import SwiftUI

/// The Drums tab: a groove written on the drum staff with a ball that bounces from count
/// to count in time, so the player can read, count and play along.
struct DrumTrainerView: View {
    @State private var player = DrumPlayer()
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    staff
                    controls
                }
                .padding()
            }
            .navigationTitle("Drums")
        }
        .onDisappear(perform: player.stop)
    }

    private var staff: some View {
        TimelineView(.animation(paused: !player.isPlaying)) { timeline in
            DrumStaffView(
                groove: player.groove,
                ball: player.ballPosition(at: timeline.date),
                isPlaying: player.isPlaying
            )
        }
        // A phone is narrow, so the staff gets a taller shape there to keep the notes legible.
        .aspectRatio(horizontalSizeClass == .regular ? 4.5 : 3, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityLabel("\(player.groove.name) on the drum staff")
    }

    private var controls: some View {
        @Bindable var player = player
        return VStack(spacing: 16) {
            Button(action: player.togglePlayback) {
                Label(
                    player.isPlaying ? "Pause" : "Play",
                    systemImage: player.isPlaying ? "pause.fill" : "play.fill"
                )
                .font(.title2.weight(.semibold))
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)

            Picker("Groove", selection: $player.groove) {
                ForEach(DrumGroove.builtIn) { groove in
                    Text(groove.name).tag(groove)
                }
            }
            .pickerStyle(.menu)
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(alignment: .leading, spacing: 4) {
                Text("Tempo: \(Int(player.tempo)) BPM")
                    .font(.headline)
                    .accessibilityIdentifier("tempoLabel")
                Slider(value: $player.tempo, in: DrumPlayer.tempoRange, step: 1) {
                    Text("Tempo")
                }
            }

            Toggle("Drum sounds", isOn: $player.isSoundOn)
        }
    }
}

/// The notation itself, drawn as one picture: counts along the top, beamed hi-hats above
/// the staff, snare and kick on their lines, the current count marked by a bar, and the
/// ball above it all.
struct DrumStaffView: View {
    let groove: DrumGroove
    let ball: BallPosition
    let isPlaying: Bool

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Canvas { context, size in
            let layout = DrumStaffLayout(groove: groove, size: size)
            let ink = Color.primary
            drawStaff(context, layout, ink)
            if isPlaying {
                drawCurrentCountMarker(context, layout)
            }
            drawCounts(context, layout, ink)
            drawHits(context, layout, ink)
            drawBall(context, layout)
        }
        .background(
            colorScheme == .dark ? Color(white: 0.13) : Color(red: 0.89, green: 0.94, blue: 0.97)
        )
    }

    private func drawStaff(_ context: GraphicsContext, _ layout: DrumStaffLayout, _ ink: Color) {
        var lines = Path()
        for index in 0..<5 {
            let y = layout.staffTop + CGFloat(index) * layout.lineSpacing
            lines.move(to: CGPoint(x: layout.staffLeft, y: y))
            lines.addLine(to: CGPoint(x: layout.staffRight, y: y))
        }
        context.stroke(lines, with: .color(ink), lineWidth: layout.lineSpacing * 0.08)

        var barLine = Path()
        barLine.move(to: CGPoint(x: layout.staffRight, y: layout.staffTop))
        barLine.addLine(to: CGPoint(x: layout.staffRight, y: layout.staffBottom))
        context.stroke(barLine, with: .color(ink), lineWidth: layout.lineSpacing * 0.2)
    }

    private func drawCurrentCountMarker(_ context: GraphicsContext, _ layout: DrumStaffLayout) {
        let x = layout.columnX(ball.step)
        let width = layout.lineSpacing * 0.9
        let rect = CGRect(
            x: x - width / 2,
            y: layout.beamY - layout.lineSpacing * 0.6,
            width: width,
            height: layout.staffBottom - layout.beamY + layout.lineSpacing * 1.2
        )
        let marker = Path(roundedRect: rect, cornerRadius: width / 4)
        context.fill(marker, with: .color(Color.accentColor.opacity(0.55)))
    }

    private func drawCounts(_ context: GraphicsContext, _ layout: DrumStaffLayout, _ ink: Color) {
        let font = Font.system(size: layout.lineSpacing * 1.7, weight: .heavy, design: .rounded)
        for step in 0..<groove.stepsPerBar {
            let label = Text(groove.countLabel(forStep: step)).font(font).foregroundStyle(ink)
            context.draw(label, at: CGPoint(x: layout.columnX(step), y: layout.countY))
        }
    }

    private func drawHits(_ context: GraphicsContext, _ layout: DrumStaffLayout, _ ink: Color) {
        let headRadius = layout.lineSpacing * 0.45
        var stems = Path()
        var beams = Path()
        for beat in 0..<groove.beatsPerBar {
            let firstStep = beat * groove.subdivision
            let lastStep = firstStep + groove.subdivision - 1
            var beam = Path()
            beam.move(to: CGPoint(x: layout.stemX(firstStep), y: layout.beamY))
            beam.addLine(to: CGPoint(x: layout.stemX(lastStep), y: layout.beamY))
            beams.addPath(beam)
            if groove.subdivision == 4 {
                var second = Path()
                let y = layout.beamY + layout.lineSpacing * 0.55
                second.move(to: CGPoint(x: layout.stemX(firstStep), y: y))
                second.addLine(to: CGPoint(x: layout.stemX(lastStep), y: y))
                beams.addPath(second)
            }
        }
        for step in 0..<groove.stepsPerBar {
            let voices = groove.voices(atStep: step)
            let x = layout.columnX(step)
            var lowestY = layout.beamY
            for voice in voices {
                let y = layout.headY(for: voice)
                lowestY = max(lowestY, y)
                drawHead(context, voice: voice, at: CGPoint(x: x, y: y), radius: headRadius, ink)
            }
            stems.move(to: CGPoint(x: layout.stemX(step), y: layout.beamY))
            stems.addLine(to: CGPoint(x: layout.stemX(step), y: lowestY))
        }
        context.stroke(stems, with: .color(ink), lineWidth: layout.lineSpacing * 0.1)
        context.stroke(beams, with: .color(ink), lineWidth: layout.lineSpacing * 0.35)
    }

    private func drawHead(
        _ context: GraphicsContext,
        voice: DrumVoice,
        at center: CGPoint,
        radius: CGFloat,
        _ ink: Color
    ) {
        if voice == .hiHat {
            var cross = Path()
            cross.move(to: CGPoint(x: center.x - radius, y: center.y - radius))
            cross.addLine(to: CGPoint(x: center.x + radius, y: center.y + radius))
            cross.move(to: CGPoint(x: center.x + radius, y: center.y - radius))
            cross.addLine(to: CGPoint(x: center.x - radius, y: center.y + radius))
            context.stroke(cross, with: .color(ink), lineWidth: radius * 0.3)
        } else {
            let rect = CGRect(
                x: center.x - radius * 1.2,
                y: center.y - radius * 0.85,
                width: radius * 2.4,
                height: radius * 1.7
            )
            context.fill(Path(ellipseIn: rect), with: .color(ink))
        }
    }

    private func drawBall(_ context: GraphicsContext, _ layout: DrumStaffLayout) {
        let fromX = layout.columnX(ball.step)
        let toX = fromX + layout.columnWidth
        let x = fromX + (toX - fromX) * ball.travel
        let y = layout.ballRestY - layout.bounceHeight * ball.bounce
        let radius = layout.lineSpacing * 0.75
        let rect = CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2)
        let shading = GraphicsContext.Shading.radialGradient(
            Gradient(colors: [Color.cyan, Color.blue]),
            center: CGPoint(x: x - radius * 0.35, y: y - radius * 0.35),
            startRadius: 0,
            endRadius: radius * 1.6
        )
        context.fill(Path(ellipseIn: rect), with: shading)
    }
}

/// Where everything sits on the staff for a view of this size.
struct DrumStaffLayout {
    let groove: DrumGroove
    let size: CGSize

    var lineSpacing: CGFloat { size.height * 0.075 }
    var staffTop: CGFloat { size.height * 0.6 }
    var staffBottom: CGFloat { staffTop + lineSpacing * 4 }
    var staffLeft: CGFloat { size.width * 0.02 }
    var staffRight: CGFloat { size.width * 0.98 }
    var beamY: CGFloat { staffTop - lineSpacing * 2.6 }
    var countY: CGFloat { staffTop - lineSpacing * 4.6 }
    var ballRestY: CGFloat { countY }
    var bounceHeight: CGFloat { size.height * 0.16 }

    var columnWidth: CGFloat {
        (staffRight - staffLeft) / CGFloat(groove.stepsPerBar + 1)
    }

    func columnX(_ step: Int) -> CGFloat {
        staffLeft + columnWidth * (CGFloat(step) + 0.6)
    }

    /// Stems hang off the right side of the head, as engraved music does.
    func stemX(_ step: Int) -> CGFloat {
        columnX(step) + lineSpacing * 0.5
    }

    func headY(for voice: DrumVoice) -> CGFloat {
        switch voice {
        case .hiHat: return staffTop - lineSpacing * 0.5
        case .snare: return staffTop + lineSpacing * 1.5
        case .kick: return staffTop + lineSpacing * 3.5
        }
    }
}

#Preview {
    DrumTrainerView()
}
