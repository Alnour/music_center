// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

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
    @State private var isShowingBoardSettings = false
    @State private var isShowingTVMenu = false
    /// Set by the TV menu's Full-screen button. The board opens once the menu has gone:
    /// presenting it while the popover is still closing cancels the presentation.
    @State private var opensFullScreenBoardAfterTVMenu = false
    @State private var libraryError: ScaleLibraryError?
    /// Delay before falling back to the full-screen board once mirroring starts, giving
    /// iOS time to offer the app its own TV screen first.
    private static let tvScreenGracePeriod: Duration = .seconds(2)
    /// The largest share of the screen the chord cards may take, so the board always keeps
    /// most of the height even when a changed scale lists every chord it had and has.
    private static let chordsHeightShare: CGFloat = 0.34
    /// With a capo on, every card is one line taller (its shape) and a line names the key,
    /// so two rows of cards need a larger share to show without scrolling.
    private static let chordsHeightShareWithCapo: CGFloat = 0.43

    var body: some View {
        @Bindable var session = session
        let palette = FretboardPalette.matching(colorScheme)
        let isWide = horizontalSizeClass == .regular
        NavigationStack {
            // The board and its chords get the whole screen. One slim row above the board
            // holds every choice a player makes while practising; one-time setup and the
            // TV controls sit behind the two toolbar buttons.
            GeometryReader { proxy in
                VStack(spacing: isWide ? 12 : 8) {
                    BoardToolbar(
                        root: $session.settings.root,
                        scale: scaleSelection,
                        instrument: $session.settings.selectedInstrument,
                        labelStyle: $session.settings.labelStyle,
                        customScales: library.scales,
                        changeNote: CompactScaleEditMenu(
                            scale: session.scale,
                            onPick: transformBoard
                        )
                    )
                    ScaleNotesRow(
                        scale: session.scale,
                        palette: palette,
                        noteSize: isWide ? 18 : 14,
                        highlightedNotes: session.ringedNotes
                    )
                    .frame(maxWidth: .infinity)
                    phoneBoard(palette: palette)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .frame(maxHeight: .infinity)
                        .accessibilityLabel(boardAccessibilityLabel)
                        .accessibilityValue(session.settings.labelStyle.accessibilityDescription)
                    if let change = session.currentBoardChange {
                        BoardChangeRow(change: change) {
                            session.revertBoardChange(from: library)
                        }
                    }
                    ScrollView(.vertical, showsIndicators: false) {
                        ScaleChordsRow(
                            scale: session.scale,
                            change: session.currentBoardChange,
                            capo: session.settings.capoTransposition(for: session.scale),
                            ringedChord: session.ringedChord,
                            onTap: { chord in session.toggleRing(for: chord) },
                            cardMinimumWidth: isWide ? 120 : 84
                        )
                    }
                    .frame(maxHeight: proxy.size.height * chordsHeightShare)
                    .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, isWide ? 20 : 12)
                .padding(.bottom, 8)
            }
            .navigationTitle(session.scale.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("TV", systemImage: tvMenuIcon) {
                        isShowingTVMenu.toggle()
                    }
                    .tint(tvMenuTint)
                    .accessibilityIdentifier("tvMenu")
                    .popover(isPresented: $isShowingTVMenu) {
                        TVMenu(
                            status: session.tvStatus,
                            player: AirPlayCaster.shared.player,
                            resumeCasting: { AirPlayCaster.shared.resume() },
                            takeOverTV: { AirPlayCaster.shared.takeOverTVIfChosen() },
                            tvUsesDarkTheme: $session.settings.tvUsesDarkTheme,
                            showFullScreenBoard: {
                                opensFullScreenBoardAfterTVMenu = true
                                isShowingTVMenu = false
                            }
                        )
                        .presentationCompactAdaptation(.popover)
                        .onDisappear(perform: openFullScreenBoardIfAsked)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Board settings", systemImage: "gearshape") {
                        isShowingBoardSettings.toggle()
                    }
                    .accessibilityIdentifier("boardSettings")
                    .popover(isPresented: $isShowingBoardSettings) {
                        BoardSettingsMenu(settings: $session.settings, scale: session.scale)
                            .presentationCompactAdaptation(.popover)
                    }
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

    /// The largest share of the screen's height the chord cards may take.
    private var chordsHeightShare: CGFloat {
        let hasCapoShapes = session.settings.capoTransposition(for: session.scale) != nil
        return hasCapoShapes ? Self.chordsHeightShareWithCapo : Self.chordsHeightShare
    }

    /// The fretboard or the piano octave, whichever instrument the player chose, filling
    /// whatever height the chords leave it.
    @ViewBuilder
    private func phoneBoard(palette: FretboardPalette) -> some View {
        switch session.settings.selectedInstrument {
        case .guitar, .bass:
            PhoneFretboard(
                settings: session.settings,
                layout: session.layout,
                palette: palette,
                ringedNotes: session.ringedNotes
            )
        case .keyboard:
            PhoneKeyboard(
                settings: session.settings,
                layout: session.keyboardLayout,
                palette: palette,
                ringedNotes: session.ringedNotes
            )
        }
    }

    private var boardAccessibilityLabel: String {
        switch session.settings.selectedInstrument {
        case .guitar: "Fretboard showing \(session.scale.title)"
        case .bass: "Bass fretboard showing \(session.scale.title)"
        case .keyboard: "Keyboard showing \(session.scale.title)"
        }
    }

    /// The TV button's symbol follows the TV: plain while nothing is connected, filled
    /// while the board is on the TV, a warning while casting has stopped or failed.
    private var tvMenuIcon: String {
        switch session.tvStatus {
        case .disconnected: "tv"
        case .mirroringPhone: "tv"
        case .showingBoard, .castingVideo: "tv.fill"
        case .castFailed: "exclamationmark.triangle"
        case .castInterrupted: "pause.circle"
        }
    }

    private var tvMenuTint: Color? {
        switch session.tvStatus {
        case .disconnected: nil
        case .mirroringPhone, .castInterrupted: .orange
        case .showingBoard, .castingVideo: .green
        case .castFailed: .red
        }
    }

    private func openFullScreenBoardIfAsked() {
        guard opensFullScreenBoardAfterTVMenu else { return }
        opensFullScreenBoardAfterTVMenu = false
        isShowingFullScreenBoard = true
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
        AirPlayCaster.shared.cast(session.settings) {
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

/// The board filling the height it is given. Cells never shrink below a comfortable reading
/// size and grow with the board's height, so a taller board means larger note circles, and
/// the board scrolls sideways when all the frets no longer fit the width.
private struct PhoneFretboard: View {
    let settings: PracticeSettings
    let layout: FretboardLayout
    let palette: FretboardPalette
    let ringedNotes: Set<NoteName>

    private static let minimumCellWidth: CGFloat = 46
    /// How wide a cell is made for each point of string row height. Note circles are sized
    /// by the smaller of the two, so cells narrower than this would waste the board's height.
    private static let cellWidthPerRowHeight: CGFloat = 0.9

    var body: some View {
        GeometryReader { proxy in
            let columnUnits = FretboardGeometry.columnUnits(fretCount: settings.fretCount)
            let rowUnits =
                CGFloat(layout.strings.count) + FretboardGeometry.numberBandShare
                + FretboardGeometry.inlayBandShare
            let rowHeight = proxy.size.height / rowUnits
            let fittedCellWidth = proxy.size.width / columnUnits
            let readableCellWidth = rowHeight * Self.cellWidthPerRowHeight
            let cellWidth = max(Self.minimumCellWidth, fittedCellWidth, readableCellWidth)
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
        .background(palette.background)
    }
}

/// The piano octaves across the screen. The whole keyboard always fits the width, so there
/// is nothing to scroll, and the keys are as tall as the width or the height allows.
private struct PhoneKeyboard: View {
    let settings: PracticeSettings
    let layout: KeyboardLayout
    let palette: FretboardPalette
    let ringedNotes: Set<NoteName>

    var body: some View {
        KeyboardView(
            layout: layout,
            labelStyle: settings.labelStyle,
            palette: palette,
            ringedNotes: ringedNotes
        )
        .aspectRatio(
            KeyboardGeometry.widthToHeight(whiteKeyCount: layout.whiteKeys.count),
            contentMode: .fit
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(12)
        .background(palette.background)
    }
}

/// One slim row above the board with everything a player changes while practising: the
/// key and scale as menus, the instrument and the labels as small segmented pills, and the
/// note-change menu. On a narrow phone the row folds into two lines, and when the pills are
/// still wider than the screen at their natural size (three instruments and three label
/// styles on an iPhone) they share the line's width, each segment as wide as its own word.
private struct BoardToolbar: View {
    @Binding var root: NoteName
    @Binding var scale: ScaleReference
    @Binding var instrument: Instrument
    @Binding var labelStyle: NoteLabelStyle
    let customScales: [CustomScale]
    let changeNote: CompactScaleEditMenu

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 12) {
                scaleMenus
                Spacer(minLength: 8)
                pills
                changeNoteButton
            }
            VStack(spacing: 6) {
                menusLine
                HStack(spacing: 8) {
                    pills
                    Spacer(minLength: 0)
                }
            }
            VStack(spacing: 6) {
                menusLine
                SharedPillRow {
                    WordWidthSegments(
                        label: "Instrument",
                        choices: Instrument.allCases,
                        titles: Instrument.allCases.map(\.title),
                        selection: $instrument
                    )
                    WordWidthSegments(
                        label: "Labels",
                        choices: NoteLabelStyle.allCases,
                        titles: NoteLabelStyle.allCases.map(\.title),
                        selection: $labelStyle
                    )
                }
            }
        }
        .controlSize(.small)
    }

    @ViewBuilder
    private var scaleMenus: some View {
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
        .fixedSize()
    }

    /// The Root and Scale menus with the note-change button, the first of two lines.
    private var menusLine: some View {
        HStack(spacing: 12) {
            scaleMenus
            Spacer(minLength: 0)
            changeNoteButton
        }
    }

    /// The instrument and label pills at their natural size, where each segment is as wide
    /// as the pill's longest word.
    @ViewBuilder
    private var pills: some View {
        Picker("Instrument", selection: $instrument) {
            ForEach(Instrument.allCases) { instrument in
                Text(instrument.title).tag(instrument)
            }
        }
        .pickerStyle(.segmented)
        .fixedSize()
        Picker("Labels", selection: $labelStyle) {
            ForEach(NoteLabelStyle.allCases) { style in
                Text(style.title).tag(style)
            }
        }
        .pickerStyle(.segmented)
        .fixedSize()
    }

    private var changeNoteButton: some View {
        changeNote
            .labelStyle(.iconOnly)
            .accessibilityLabel("Change a note")
    }
}

/// Lays the instrument pill and the label pill side by side across the whole width, each
/// taking the share of it that its natural width asks for, so the pill with the longer
/// words gets more room.
private struct SharedPillRow: Layout {
    private static let spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        var height: CGFloat = 0
        for subview in subviews {
            let subviewHeight = subview.sizeThatFits(.unspecified).height
            height = max(height, subviewHeight)
        }
        let width = proposal.width ?? 0
        return CGSize(width: width, height: height)
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        guard subviews.count == 2 else { return }
        let firstNaturalWidth = subviews[0].sizeThatFits(.unspecified).width
        let secondNaturalWidth = subviews[1].sizeThatFits(.unspecified).width
        let naturalWidth = firstNaturalWidth + secondNaturalWidth
        guard naturalWidth > 0 else { return }
        let sharedWidth = bounds.width - Self.spacing
        let firstWidth = sharedWidth * firstNaturalWidth / naturalWidth
        let secondWidth = sharedWidth - firstWidth
        let firstProposal = ProposedViewSize(width: firstWidth, height: bounds.height)
        let secondProposal = ProposedViewSize(width: secondWidth, height: bounds.height)
        subviews[0].place(at: bounds.origin, proposal: firstProposal)
        let secondOrigin = CGPoint(x: bounds.minX + firstWidth + Self.spacing, y: bounds.minY)
        subviews[1].place(at: secondOrigin, proposal: secondProposal)
    }
}

/// A segmented pill whose segments are as wide as their own words.
///
/// SwiftUI's segmented picker makes every segment as wide as the longest word, so
/// "Guitar | Bass | Keyboard" beside "Notes | Degrees | Dots" needs more width than an
/// iPhone has and "Keyboard" is cut short. UIKit's control can size each segment by its
/// content, which SwiftUI does not expose, so the narrow toolbar wraps the control itself.
private struct WordWidthSegments<Choice: Hashable>: UIViewRepresentable {
    /// What VoiceOver calls the whole pill.
    let label: String
    let choices: [Choice]
    /// One title per choice, in the same order.
    let titles: [String]
    @Binding var selection: Choice

    func makeCoordinator() -> Coordinator {
        Coordinator(owner: self)
    }

    func makeUIView(context: Context) -> UISegmentedControl {
        let control = UISegmentedControl(items: titles)
        control.apportionsSegmentWidthsByContent = true
        control.accessibilityLabel = label
        control.addTarget(
            context.coordinator,
            action: #selector(Coordinator.segmentChanged(_:)),
            for: .valueChanged
        )
        return control
    }

    func updateUIView(_ control: UISegmentedControl, context: Context) {
        context.coordinator.owner = self
        let selectedIndex = choices.firstIndex(of: selection) ?? UISegmentedControl.noSegment
        if control.selectedSegmentIndex != selectedIndex {
            control.selectedSegmentIndex = selectedIndex
        }
    }

    /// The control fills the width it is offered and keeps its own height.
    func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiView control: UISegmentedControl,
        context: Context
    ) -> CGSize? {
        let naturalSize = control.intrinsicContentSize
        let width = proposal.width ?? naturalSize.width
        return CGSize(width: width, height: naturalSize.height)
    }

    @MainActor
    final class Coordinator: NSObject {
        var owner: WordWidthSegments

        init(owner: WordWidthSegments) {
            self.owner = owner
        }

        @objc func segmentChanged(_ control: UISegmentedControl) {
            let index = control.selectedSegmentIndex
            guard owner.choices.indices.contains(index) else { return }
            owner.selection = owner.choices[index]
        }
    }
}

/// One-time setup behind the gear button: frets, tuning, capo and string order for the
/// guitar, the same without the capo for the bass, the octave count for the keyboard.
/// Each instrument shows only its own rows.
private struct BoardSettingsMenu: View {
    @Binding var settings: PracticeSettings
    /// The scale on the board, which decides how many open chords each capo fret gives.
    let scale: Scale

    var body: some View {
        Form {
            switch settings.selectedInstrument {
            case .guitar:
                guitarOptions
            case .bass:
                bassOptions
            case .keyboard:
                Picker("Octaves", selection: $settings.selectedKeyboardOctaves) {
                    ForEach(PracticeSettings.keyboardOctaveChoices, id: \.self) { count in
                        Text(count == 1 ? "1 octave" : "\(count) octaves").tag(count)
                    }
                }
                .pickerStyle(.segmented)
            }
        }
        .frame(minWidth: 320, minHeight: 380)
        .presentationBackground(.regularMaterial)
    }

    /// The capo's fret, each choice saying which key's shapes it gives and how many of
    /// them are open chords, with one line naming the best frets. A tuning without the
    /// standard shapes lists the bare frets.
    @ViewBuilder
    private var capoOptions: some View {
        let suggestions = Capo.suggestions(for: scale, tuning: settings.tuning)
        // The row shows only the chosen fret; the open menu spells out what each fret
        // gives, which is too long for the row on a phone.
        Picker(selection: $settings.selectedCapoFret) {
            if suggestions.isEmpty {
                ForEach(Capo.fretChoices, id: \.self) { fret in
                    Text(Self.capoTitle(fret: fret)).tag(fret)
                }
            } else {
                ForEach(suggestions, id: \.capoFret) { suggestion in
                    Text(suggestion.label).tag(suggestion.capoFret)
                }
            }
        } label: {
            Text("Capo")
        } currentValueLabel: {
            Text(Self.capoTitle(fret: settings.selectedCapoFret))
        }
        .accessibilityIdentifier("capoPicker")
        Text(Capo.advice(for: scale, tuning: settings.tuning))
            .font(.footnote)
            .foregroundStyle(.secondary)
            .accessibilityIdentifier("capoAdvice")
    }

    private static func capoTitle(fret: Int) -> String {
        fret == 0 ? "No capo" : "Fret \(fret)"
    }

    @ViewBuilder
    private var guitarOptions: some View {
        fretCountPicker
        Picker("Tuning", selection: $settings.tuning) {
            ForEach(Tuning.allCases) { tuning in
                Text(tuning.displayName).tag(tuning)
            }
        }
        capoOptions
        stringOrderToggles
    }

    /// The bass guitar's rows: the guitar's without the capo, and with the bass's own
    /// tunings, so the guitar's tuning is still there when the player switches back.
    @ViewBuilder
    private var bassOptions: some View {
        fretCountPicker
        Picker("Tuning", selection: $settings.selectedBassTuning) {
            ForEach(BassTuning.allCases) { tuning in
                Text(tuning.displayName).tag(tuning)
            }
        }
        .accessibilityIdentifier("bassTuningPicker")
        stringOrderToggles
    }

    private var fretCountPicker: some View {
        Picker("Frets", selection: $settings.fretCount) {
            ForEach(PracticeSettings.fretCountChoices, id: \.self) { count in
                Text("\(count) frets").tag(count)
            }
        }
    }

    @ViewBuilder
    private var stringOrderToggles: some View {
        Toggle("Thin string on top", isOn: $settings.thinStringOnTop)
        Toggle("Left-handed", isOn: $settings.isLeftHanded)
    }
}

/// The TV controls behind the TV button: the AirPlay picker with the TV's status, the way
/// back after an interruption, the dark theme and the full-screen board.
private struct TVMenu: View {
    let status: TVStatus
    let player: AVPlayer
    let resumeCasting: () -> Void
    let takeOverTV: @MainActor () -> Void
    @Binding var tvUsesDarkTheme: Bool
    let showFullScreenBoard: () -> Void

    var body: some View {
        Form {
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
            } footer: {
                Text(footerText)
            }
        }
        .frame(minWidth: 340, minHeight: 300)
        .presentationBackground(.regularMaterial)
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
