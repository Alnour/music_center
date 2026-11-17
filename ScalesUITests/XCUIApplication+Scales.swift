// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import XCTest

/// Lookups shared by the interface tests and the App Store screenshot run, written so the
/// same steps work on iPhone and iPad.
extension XCUIApplication {
    /// Menu pickers expose their current value in the label ("Root, G"), so match the start.
    @MainActor
    func menuButton(labelled title: String) -> XCUIElement {
        let predicate = NSPredicate(format: "label BEGINSWITH %@", title + ",")
        return buttons.matching(predicate).firstMatch
    }

    /// Opens one of the app's tabs. The iPhone shows them in a bar along the bottom;
    /// the iPad floats them at the top of the window, where the test framework does not
    /// report a tab bar at all, so the tab's button is looked up wherever it is.
    @MainActor
    func openTab(_ title: String) {
        buttons[title].firstMatch.tap()
    }

    /// Puts the Scales screen's controls back when an earlier run left them hidden. The
    /// choice is saved between launches, and the other tests expect the Display and TV
    /// rows to be there to scroll to.
    @MainActor
    func showControlsIfHidden() {
        let toggle = buttons["toggleControls"]
        guard toggle.waitForExistence(timeout: 3) else { return }
        if toggle.label == "Show Controls" {
            toggle.tap()
        }
    }

    /// Any element with this accessibility identifier, whatever kind it is; SwiftUI menus
    /// do not always come through as plain buttons.
    @MainActor
    func element(identified identifier: String) -> XCUIElement {
        descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    /// Scrolls the screen up until the element can be tapped. Forms only build the rows on
    /// screen, so a row further down doesn't exist until it is scrolled into view.
    @MainActor
    @discardableResult
    func scrollUntilVisible(_ element: XCUIElement, maxSwipes: Int = 6) -> Bool {
        var swipes = 0
        while !(element.exists && element.isHittable) && swipes < maxSwipes {
            swipeUp()
            swipes += 1
        }
        return element.exists
    }

    /// True on iPad, where iPadOS never rotates an app on request, so the full-screen board
    /// stays upright until the device itself is turned. The test runner shares the
    /// simulator with the app, so its own device type is the app's.
    @MainActor
    var isPad: Bool {
        UIDevice.current.userInterfaceIdiom == .pad
    }
}
