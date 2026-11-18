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

    /// Opens the Scales screen's board settings (frets, tuning, string order, octaves),
    /// which sit behind the gear button in the navigation bar.
    @MainActor
    func openBoardSettings() {
        buttons["boardSettings"].tap()
    }

    /// Opens the Scales screen's TV controls, behind the TV button in the navigation bar.
    @MainActor
    func openTVMenu() {
        buttons["tvMenu"].tap()
    }

    /// Closes an open popover by tapping the board under it, which does nothing on its own.
    @MainActor
    func dismissPopover() {
        let board = otherElements.matching(
            NSPredicate(format: "label BEGINSWITH %@ OR label BEGINSWITH %@",
                "Fretboard showing", "Keyboard showing")
        ).firstMatch
        if board.exists {
            board.tap()
        } else {
            coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.45)).tap()
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
