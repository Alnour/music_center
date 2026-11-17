// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import UIKit

/// The kind of device the app runs on, for wording ("this iPad") and for behaviour that
/// only makes sense on some of them.
enum DeviceKind: Equatable, Sendable {
    case phone
    case pad
    case mac

    /// The Mac version is this app compiled for macOS (Mac Catalyst), where UIKit still
    /// reports an iPad, so the Mac is recognised from the build itself.
    @MainActor
    static var current: DeviceKind {
        #if targetEnvironment(macCatalyst)
            return .mac
        #else
            return UIDevice.current.userInterfaceIdiom == .pad ? .pad : .phone
        #endif
    }

    /// The device's name in a sentence: "this phone", "this iPad", "this Mac".
    var name: String {
        switch self {
        case .phone: "phone"
        case .pad: "iPad"
        case .mac: "Mac"
        }
    }
}
