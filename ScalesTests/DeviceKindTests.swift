// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Testing

@testable import Scales

/// The app names the device in its sentences and changes a few behaviours on the Mac.
@MainActor
struct DeviceKindTests {
    @Test func theMacBuildKnowsItIsOnAMac() {
        #if targetEnvironment(macCatalyst)
            #expect(DeviceKind.current == .mac)
            #expect(!OrientationLock.canRotateScreen, "a Mac window never rotates")
        #else
            #expect(DeviceKind.current != .mac)
        #endif
    }

    @Test func devicesAreNamedTheWayPlayersSayThem() {
        #expect(DeviceKind.phone.name == "phone")
        #expect(DeviceKind.pad.name == "iPad")
        #expect(DeviceKind.mac.name == "Mac")
    }
}
