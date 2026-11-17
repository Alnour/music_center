// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI
import UIKit

/// Reports whether the phone screen is being mirrored or recorded.
///
/// iOS exposes this as a trait on views rather than as a notification, so a zero-size
/// UIKit view sits in the hierarchy and listens for the trait to change. AirPlay screen
/// mirroring sets it, which is how the app knows the TV is at least showing a copy of
/// the phone even when iOS never offers a separate TV screen.
struct CaptureStateReader: UIViewRepresentable {
    let onChange: (Bool) -> Void

    func makeUIView(context: Context) -> CaptureStateView {
        let view = CaptureStateView()
        view.onChange = onChange
        return view
    }

    func updateUIView(_ uiView: CaptureStateView, context: Context) {
        uiView.onChange = onChange
    }

    final class CaptureStateView: UIView {
        var onChange: ((Bool) -> Void)?

        override init(frame: CGRect) {
            super.init(frame: frame)
            isUserInteractionEnabled = false
            registerForTraitChanges([UITraitSceneCaptureState.self]) {
                (view: CaptureStateView, _: UITraitCollection) in
                view.report()
            }
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError("CaptureStateView is created in code only")
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            report()
        }

        private func report() {
            let isCaptured = traitCollection.sceneCaptureState == .active
            onChange?(isCaptured)
        }
    }
}
