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
        let palette: FretboardPalette = settings.tvUsesDarkTheme ? .dark : .light
        GeometryReader { proxy in
            if fillsPhone {
                phoneLayout(size: proxy.size, settings: settings, palette: palette)
            } else {
                tvLayout(size: proxy.size, settings: settings, palette: palette)
            }
        }
        .background(palette.background.ignoresSafeArea())
    }

    /// Whether this layout fills an iPhone held sideways rather than a TV or an iPad.
    private var fillsPhone: Bool {
        onClose != nil && DeviceKind.current == .phone
    }

    /// The TV picture: title row, then the spelled notes, then the board, with margins
    /// clear of overscan.
    private func tvLayout(size: CGSize, settings: PracticeSettings, palette: FretboardPalette)
        -> some View
    {
        let scale = session.scale
        let metrics = TVLayoutMetrics(size: size)
        return VStack(alignment: .leading, spacing: metrics.spacing) {
            HStack(alignment: .firstTextBaseline) {
                title(scale, size: metrics.titleFontSize, palette: palette)
                Spacer()
                caption(for: settings, size: metrics.captionFontSize, palette: palette)
                if onClose != nil {
                    closeButton(size: metrics.closeIconSize, palette: palette)
                        .padding(.leading, metrics.closeLeadingPadding)
                }
            }
            ScaleNotesRow(
                scale: scale,
                palette: palette,
                noteSize: metrics.notesFontSize,
                highlightedNotes: session.ringedNotes
            )
            board(for: settings, palette: palette)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .padding(.horizontal, metrics.horizontalMargin)
        .padding(.vertical, metrics.verticalMargin)
    }

    /// The same picture squeezed onto a phone: one header row holding the title, the
    /// spelled notes, the tuning and the close button, so the board gets the rest.
    private func phoneLayout(size: CGSize, settings: PracticeSettings, palette: FretboardPalette)
        -> some View
    {
        let scale = session.scale
        let metrics = PhoneBoardMetrics(size: size)
        return VStack(alignment: .leading, spacing: metrics.spacing) {
            HStack(alignment: .center, spacing: metrics.headerSpacing) {
                title(scale, size: metrics.titleFontSize, palette: palette)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                ScaleNotesRow(
                    scale: scale,
                    palette: palette,
                    noteSize: metrics.notesFontSize,
                    highlightedNotes: session.ringedNotes
                )
                .fixedSize()
                Spacer(minLength: 0)
                caption(for: settings, size: metrics.captionFontSize, palette: palette)
                    .lineLimit(1)
                closeButton(size: metrics.closeIconSize, palette: palette)
            }
            board(for: settings, palette: palette)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .padding(.horizontal, metrics.horizontalMargin)
        .padding(.vertical, metrics.verticalMargin)
    }

    private func title(_ scale: Scale, size: CGFloat, palette: FretboardPalette) -> some View {
        Text(scale.title)
            .font(.system(size: size, weight: .bold, design: .rounded))
            .foregroundStyle(palette.title)
    }

    private func caption(for settings: PracticeSettings, size: CGFloat, palette: FretboardPalette)
        -> some View
    {
        Text(settings.boardCaption)
            .font(.system(size: size, weight: .medium, design: .rounded))
            .foregroundStyle(palette.secondaryText)
    }

    private func closeButton(size: CGFloat, palette: FretboardPalette) -> some View {
        Button("Close", systemImage: "xmark.circle.fill") { onClose?() }
            .labelStyle(.iconOnly)
            .font(.system(size: size))
            .foregroundStyle(palette.secondaryText)
    }

    /// The fretboard or the piano octave, whichever instrument the player chose.
    @ViewBuilder
    private func board(for settings: PracticeSettings, palette: FretboardPalette) -> some View {
        switch settings.selectedInstrument {
        case .guitar, .bass:
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
                ringedNotes: session.ringedNotes,
                stretchesToFill: fillsPhone
            )
        }
    }
}
