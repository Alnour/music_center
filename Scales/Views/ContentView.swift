import AVFoundation
import SwiftUI

/// The phone or iPad screen: the board on top, the choices below. While the TV is
/// connected this screen acts as the remote and the TV shows the large board.
struct ContentView: View {
    @Environment(PracticeSession.self) private var session
    @Environment(ScaleLibrary.self) private var library
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var isShowingFullScreenBoard = false
    @State private var libraryError: ScaleLibraryError?
    /// Whether the Scale, Display and TV sections are on screen. Hidden, the screen is
    /// the board and its chords alone, which is all a player needs while practising.
    @AppStorage("showsControls") private var showsControls = true
    /// Delay before falling back to the full-screen board once mirroring starts, giving
    /// iOS time to offer the app its own TV screen first.
    private static let tvScreenGracePeriod: Duration = .seconds(2)

    var body: some View {
        @Bindable var session = session
        let palette = FretboardPalette.matching(colorScheme)
        let isWide = horizontalSizeClass == .regular
        NavigationStack {
            Form {
                // One screen: the notes, the board, the key and scale in a single row with
                // the note-change menu, and the chords. Rarer choices sit below, behind the
                // controls button, so the board and its chords never have to scroll.
                Section {
                    ScaleNotesRow(
                        scale: session.scale,
                        palette: palette,
                        noteSize: isWide ? 20 : 15,
                        highlightedNotes: session.ringedNotes
                    )
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 4)
                    phoneBoard(palette: palette, boardHeight: isWide ? 420 : 300)
                        .listRowInsets(EdgeInsets())
                        .accessibilityLabel(boardAccessibilityLabel)
                    ScaleChoiceRow(
                        root: $session.settings.root,
                        scale: scaleSelection,
                        customScales: library.scales,
                        changeNote: CompactScaleEditMenu(
                            scale: session.scale,
                            onPick: transformBoard
                        )
                    )
                    ScaleChordsRow(
                        scale: session.scale,
                        change: session.currentBoardChange,
                        ringedChord: session.ringedChord,
                        onTap: { chord in session.toggleRing(for: chord) }
                    )
                    if let change = session.currentBoardChange {
                        BoardChangeRow(change: change) {
                            session.revertBoardChange(from: library)
                        }
                    }
                }
                if showsControls {
                    DisplayOptionsSection(settings: $session.settings)
                    TVSection(
                        status: session.tvStatus,
                        player: session.caster.player,
                        resumeCasting: { session.caster.resume() },
                        takeOverTV: { session.caster.takeOverTVIfChosen() },
                        tvUsesDarkTheme: $session.settings.tvUsesDarkTheme,
                        showFullScreenBoard: { isShowingFullScreenBoard = true }
                    )
                }
            }
            .navigationTitle(session.scale.title)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(
                        showsControls ? "Hide Controls" : "Show Controls",
                        systemImage: showsControls
                            ? "slider.horizontal.below.square.filled.and.square"
                            : "slider.horizontal.3"
                    ) {
                        withAnimation {
                            showsControls.toggle()
                        }
                    }
                    .accessibilityIdentifier("toggleControls")
                }
            }
        }
        .libraryErrorAlert($libraryError)
        .fullScreenCover(isPresented: $isShowingFullScreenBoard) {
            FullScreenBoardView()
        }
        .background {
            // Watching for mirroring is for iPhones and iPads mirrored from Control Center.
            // A Mac mirrors to a TV as one of its displays, and what macOS reports to the app
            // then is untested, so the Mac leaves the full-screen board to the player's button.
            if DeviceKind.current != .mac {
                CaptureStateReader { isMirrored in
                    session.isPhoneMirrored = isMirrored
                }
                .frame(width: 0, height: 0)
            }
        }
        .task(id: session.tvStatus) {
            await fallBackToFullScreenIfNeeded()
        }
        .task(id: session.settings) {
            castCurrentBoard()
        }
        .onAppear {
            // Mirroring stops when the phone locks, so keep the screen on while practising.
            UIApplication.shared.isIdleTimerDisabled = true
        }
    }

    /// The fretboard or the piano octave, whichever instrument the player chose.
    @ViewBuilder
    private func phoneBoard(palette: FretboardPalette, boardHeight: CGFloat) -> some View {
        switch session.settings.selectedInstrument {
        case .guitar:
            PhoneFretboard(
                settings: session.settings,
                layout: session.layout,
                palette: palette,
                boardHeight: boardHeight,
                ringedNotes: session.ringedNotes
            )
        case .keyboard:
            PhoneKeyboard(
                settings: session.settings,
                layout: session.keyboardLayout,
                palette: palette,
                boardHeight: boardHeight,
                ringedNotes: session.ringedNotes
            )
        }
    }

    private var boardAccessibilityLabel: String {
        switch session.settings.selectedInstrument {
        case .guitar: "Fretboard showing \(session.scale.title)"
        case .keyboard: "Keyboard showing \(session.scale.title)"
        }
    }

    /// The scale picker's selection. Choosing goes through the session so a saved scale is
    /// copied in from the library.
    private var scaleSelection: Binding<ScaleReference> {
        Binding(
            get: { session.settings.scaleReference },
            set: { reference in session.choose(reference, from: library) }
        )
    }

    /// Changes one note of the scale on the board; the chords section shows what it did.
    private func transformBoard(_ edit: ScaleEdit) {
        do {
            try session.transform(edit, from: library)
        } catch {
            libraryError = error
        }
    }

    /// Hands the current settings to the caster, which draws the board only when it has
    /// no clip for them yet. The TV layout is drawn at full HD, the size AirPlay sends.
    private func castCurrentBoard() {
        session.caster.cast(session.settings) {
            let renderer = ImageRenderer(
                content: TVDisplayView()
                    .environment(session)
                    .frame(width: 1920, height: 1080)
            )
            renderer.scale = 1
            return renderer.cgImage
        }
    }

    /// Some receivers only ever get a copy of the phone screen. When mirroring is running
    /// and iOS hasn't handed over a TV screen shortly after, the phone itself switches to
    /// the TV layout so the copy on the TV is the large board.
    private func fallBackToFullScreenIfNeeded() async {
        guard session.tvStatus == .mirroringPhone else { return }
        try? await Task.sleep(for: Self.tvScreenGracePeriod)
        guard !Task.isCancelled, session.tvStatus == .mirroringPhone else { return }
        isShowingFullScreenBoard = true
    }
}

/// The board sized for the screen in hand: cells never shrink below a comfortable reading
/// size, and the board scrolls sideways when all the frets don't fit. An iPad gets a taller
/// board than the phone so the note circles grow with the wider frets instead of sitting
/// small inside them.
private struct PhoneFretboard: View {
    let settings: PracticeSettings
    let layout: FretboardLayout
    let palette: FretboardPalette
    let boardHeight: CGFloat
    let ringedNotes: Set<NoteName>

    private static let minimumCellWidth: CGFloat = 46

    var body: some View {
        GeometryReader { proxy in
            let columnUnits = FretboardGeometry.columnUnits(fretCount: settings.fretCount)
            let fittedCellWidth = proxy.size.width / columnUnits
            let cellWidth = max(Self.minimumCellWidth, fittedCellWidth)
            ScrollView(.horizontal, showsIndicators: false) {
                FretboardView(
                    layout: layout,
                    labelStyle: settings.labelStyle,
                    isLeftHanded: settings.isLeftHanded,
                    thinStringOnTop: settings.thinStringOnTop,
                    palette: palette,
                    ringedNotes: ringedNotes
                )
                .frame(width: cellWidth * columnUnits, height: proxy.size.height)
            }
            .defaultScrollAnchor(settings.isLeftHanded ? .trailing : .leading)
        }
        .frame(height: boardHeight)
        .background(palette.background)
    }
}

/// One piano octave across the screen. The whole octave always fits the width, so there is
/// nothing to scroll, and the row is only as tall as the keys need, up to the fretboard's
/// height on a wide screen.
private struct PhoneKeyboard: View {
    let settings: PracticeSettings
    let layout: KeyboardLayout
    let palette: FretboardPalette
    let boardHeight: CGFloat
    let ringedNotes: Set<NoteName>

    var body: some View {
        KeyboardView(
            layout: layout,
            labelStyle: settings.labelStyle,
            palette: palette,
            ringedNotes: ringedNotes
        )
        .aspectRatio(
            KeyboardView.widthToHeight(whiteKeyCount: layout.whiteKeys.count),
            contentMode: .fit
        )
        .frame(maxWidth: .infinity, maxHeight: boardHeight)
        .padding(12)
        .background(palette.background)
    }
}

/// The key and the scale in one row, each a small menu, with the note-change menu at the
/// end, so choosing takes one line under the board and the chords stay on the same screen.
private struct ScaleChoiceRow: View {
    @Binding var root: NoteName
    @Binding var scale: ScaleReference
    let customScales: [CustomScale]
    let changeNote: CompactScaleEditMenu

    var body: some View {
        HStack(spacing: 16) {
            Picker("Root", selection: $root) {
                ForEach(NoteName.rootChoices, id: \.self) { note in
                    Text(note.displayName).tag(note)
                }
            }
            .pickerStyle(.menu)
            .fixedSize()
            Picker("Scale", selection: $scale) {
                ScalePickerOptions(customScales: customScales)
            }
            .pickerStyle(.menu)
            .layoutPriority(1)
            Spacer(minLength: 0)
            changeNote
        }
    }
}

private struct DisplayOptionsSection: View {
    @Binding var settings: PracticeSettings

    var body: some View {
        Section("Display") {
            Picker("Instrument", selection: $settings.selectedInstrument) {
                ForEach(Instrument.allCases) { instrument in
                    Text(instrument.title).tag(instrument)
                }
            }
            .pickerStyle(.segmented)
            Picker("Labels", selection: $settings.labelStyle) {
                ForEach(NoteLabelStyle.allCases) { style in
                    Text(style.title).tag(style)
                }
            }
            .pickerStyle(.segmented)
            // Frets, tuning and string order mean nothing on a piano, and octaves nothing
            // on a guitar, so each instrument shows only its own rows.
            switch settings.selectedInstrument {
            case .guitar:
                guitarOptions
            case .keyboard:
                Picker("Octaves", selection: $settings.selectedKeyboardOctaves) {
                    ForEach(PracticeSettings.keyboardOctaveChoices, id: \.self) { count in
                        Text(count == 1 ? "1 octave" : "\(count) octaves").tag(count)
                    }
                }
                .pickerStyle(.segmented)
            }
        }
    }

    @ViewBuilder
    private var guitarOptions: some View {
        Picker("Frets", selection: $settings.fretCount) {
            ForEach(PracticeSettings.fretCountChoices, id: \.self) { count in
                Text("\(count) frets").tag(count)
            }
        }
        Picker("Tuning", selection: $settings.tuning) {
            ForEach(Tuning.allCases) { tuning in
                Text(tuning.displayName).tag(tuning)
            }
        }
        Toggle("Thin string on top", isOn: $settings.thinStringOnTop)
        Toggle("Left-handed", isOn: $settings.isLeftHanded)
    }
}

private struct TVSection: View {
    let status: TVStatus
    let player: AVPlayer
    let resumeCasting: () -> Void
    let takeOverTV: @MainActor () -> Void
    @Binding var tvUsesDarkTheme: Bool
    let showFullScreenBoard: () -> Void

    var body: some View {
        Section {
            HStack(spacing: 12) {
                ClipPreview(player: player)
                    .frame(width: 96, height: 54)
                    .clipShape(RoundedRectangle(cornerRadius: 6))
                statusRow
                Spacer()
                AirPlayPickerButton(onMenuClosed: takeOverTV)
                    .frame(width: 44, height: 44)
                    .accessibilityLabel("AirPlay")
            }
            if case .castInterrupted = status {
                Button("Resume on TV", systemImage: "play.fill", action: resumeCasting)
            }
            Toggle("Dark TV theme", isOn: $tvUsesDarkTheme)
            Button(
                "Full-screen board",
                systemImage: "arrow.up.left.and.arrow.down.right",
                action: showFullScreenBoard
            )
        } header: {
            Text("TV")
        } footer: {
            Text(footerText)
        }
    }

    @ViewBuilder
    private var statusRow: some View {
        switch status {
        case .disconnected:
            Label("Not mirroring", systemImage: "tv.slash")
                .foregroundStyle(.secondary)
        case .mirroringPhone:
            Label("TV is showing a copy of this \(deviceName)", systemImage: "tv")
                .foregroundStyle(.orange)
        case .showingBoard:
            Label("Showing the board on your TV", systemImage: "tv.fill")
                .foregroundStyle(.green)
        case .castingVideo(let receiver):
            Label("Playing on \(receiver ?? "your TV")", systemImage: "airplayvideo")
                .foregroundStyle(.green)
        case .castFailed(let reason):
            Label("Could not play: \(reason)", systemImage: "exclamationmark.triangle")
                .foregroundStyle(.red)
        case .castInterrupted(let receiver):
            Label("Stopped on \(receiver)", systemImage: "pause.circle")
                .foregroundStyle(.orange)
        }
    }

    /// What to call the device in hand, so iPad and Mac players aren't told about "this phone".
    private var deviceName: String {
        DeviceKind.current.name
    }

    private var footerText: String {
        switch status {
        case .disconnected:
            "Tap the AirPlay button and choose your Roku. The board plays on the TV and "
                + "this \(deviceName) stays your remote. Screen Mirroring from Control Center "
                + "works too."
        case .mirroringPhone:
            "Open Full-screen board so the copy on the TV is the large board. Change the "
                + "scale by closing it, picking a new one, and opening it again."
        case .showingBoard, .castingVideo:
            "Change the root or scale here and the TV follows."
        case .castFailed:
            "Make sure the Roku and this \(deviceName) are on the same Wi-Fi, then pick the TV "
                + "again from the AirPlay button."
        case .castInterrupted:
            "The board stopped on the TV, perhaps because another app streamed to it. Tap "
                + "Resume on TV, or pick the TV again from the AirPlay button."
        }
    }
}
