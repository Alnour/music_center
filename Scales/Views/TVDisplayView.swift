// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

/// The large layout shown on the TV: scale name, spelled notes and the full board.
///
/// Sizes are fractions of the screen height so the same layout fills a 720p, 1080p or 4K
/// picture, and generous margins keep everything clear of TV overscan, where some sets
/// crop the outer edge of the picture.
struct TVDisplayView: View {
    @Environment(PracticeSession.self) private var session
    /// Set only when this layout fills the phone; the TV itself has nothing to close.
    var onClose: (() -> Void)?

    var body: some View {
        let settings = session.settings
        let scale = session.scale
        let palette: FretboardPalette = settings.tvUsesDarkTheme ? .dark : .light
        GeometryReader { proxy in
            let unit = min(proxy.size.height / 100, proxy.size.width / 178)
            VStack(alignment: .leading, spacing: unit * 3) {
                HStack(alignment: .firstTextBaseline) {
                    Text(scale.title)
                        .font(.system(size: unit * 8, weight: .bold, design: .rounded))
                        .foregroundStyle(palette.title)
                    Spacer()
                    Text(boardCaption(for: settings))
                        .font(.system(size: unit * 3.5, weight: .medium, design: .rounded))
                        .foregroundStyle(palette.secondaryText)
                    if let onClose {
                        Button("Close", systemImage: "xmark.circle.fill", action: onClose)
                            .labelStyle(.iconOnly)
                            .font(.system(size: unit * 6))
                            .foregroundStyle(palette.secondaryText)
                            .padding(.leading, unit * 3)
                    }
                }
                ScaleNotesRow(
                    scale: scale,
                    palette: palette,
                    noteSize: unit * 3.6,
                    highlightedNotes: session.ringedNotes
                )
                board(for: settings, palette: palette)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .padding(.horizontal, proxy.size.width * 0.04)
            .padding(.vertical, proxy.size.height * 0.05)
        }
        .background(palette.background.ignoresSafeArea())
    }

    /// The fretboard or the piano octave, whichever instrument the player chose.
    @ViewBuilder
    private func board(for settings: PracticeSettings, palette: FretboardPalette) -> some View {
        switch settings.selectedInstrument {
        case .guitar:
            FretboardView(
                layout: session.layout,
                labelStyle: settings.labelStyle,
                isLeftHanded: settings.isLeftHanded,
                thinStringOnTop: settings.thinStringOnTop,
                palette: palette,
                ringedNotes: session.ringedNotes
            )
        case .keyboard:
            KeyboardView(
                layout: session.keyboardLayout,
                labelStyle: settings.labelStyle,
                palette: palette,
                ringedNotes: session.ringedNotes
            )
        }
    }

    /// The small caption beside the title: the tuning matters on a guitar, not on a piano.
    private func boardCaption(for settings: PracticeSettings) -> String {
        switch settings.selectedInstrument {
        case .guitar: settings.tuning.shortName
        case .keyboard: Instrument.keyboard.title
        }
    }
}
