// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

/// The TV layout filling the phone in landscape.
///
/// This is the fallback for TVs that only mirror the screen instead of giving the app its
/// own TV screen: mirrored, this view fills the TV much like the dedicated one.
///
/// The iPhone is turned to landscape for it. The iPad cannot be turned by an app, so while
/// an iPad is held upright the board keeps the TV's wide shape and asks the player to turn
/// the iPad sideways; stretched to an upright screen the strings would spread far apart,
/// and a TV mirroring an upright iPad shows only a narrow strip in the middle. A Mac window
/// taller than it is wide gets the same treatment, asking for a wider window.
struct FullScreenBoardView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(PracticeSession.self) private var session

    var body: some View {
        GeometryReader { proxy in
            let isUpright = proxy.size.height > proxy.size.width
            if isUpright && !OrientationLock.canRotateScreen {
                uprightBoard
            } else {
                board
            }
        }
        .statusBarHidden()
        .persistentSystemOverlays(.hidden)
        .onAppear { OrientationLock.lock(.landscape) }
        .onDisappear { OrientationLock.unlock() }
    }

    private var board: some View {
        TVDisplayView(onClose: { dismiss() })
    }

    /// The board at the TV's 16:9 shape, centred, with the hint to turn the iPad (or widen
    /// the Mac window) beneath.
    private var uprightBoard: some View {
        let palette: FretboardPalette = session.settings.tvUsesDarkTheme ? .dark : .light
        return VStack(spacing: 32) {
            Spacer()
            board
                .aspectRatio(16 / 9, contentMode: .fit)
            uprightHint
                .font(.title3.weight(.medium))
                .foregroundStyle(palette.secondaryText)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(palette.background.ignoresSafeArea())
    }

    @ViewBuilder
    private var uprightHint: some View {
        if DeviceKind.current == .mac {
            Label("Widen the window so the board fills the TV", systemImage: "arrow.left.and.right")
        } else {
            Label("Turn your iPad sideways so the board fills the TV", systemImage: "rotate.right")
        }
    }
}
