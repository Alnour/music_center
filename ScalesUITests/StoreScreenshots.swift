// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import XCTest

/// Walks the app through the states shown on the App Store page and keeps a screenshot
/// of each. Run alone on an iPhone 17 Pro simulator for the 6.3-inch size (1206x2622),
/// and on an iPad Pro 13-inch simulator for the 13-inch iPad size (2064x2752):
///   make screenshots
///   make screenshots-ipad
final class StoreScreenshots: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() async throws {
        continueAfterFailure = false
        app = await XCUIApplication()
        await app.launch()
        await app.showControlsIfHidden()
    }

    @MainActor
    func testCaptureStoreScreenshots() throws {
        // 1. G major across the neck, the state the app ships in. The settings survive
        //    between launches, so they are chosen explicitly rather than assumed fresh.
        XCUIDevice.shared.orientation = .portrait
        app.menuButton(labelled: "Root").tap()
        app.buttons["G"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Major"].firstMatch.tap()
        let notesChoice = app.buttons["Notes"]
        XCTAssertTrue(app.scrollUntilVisible(notesChoice), "the label choices are below the chords")
        notesChoice.tap()
        app.swipeDown()
        XCTAssertTrue(app.navigationBars["G Major"].waitForExistence(timeout: 5))
        keep("01-g-major")

        // 2. A different key and scale, picked the way a player would.
        app.menuButton(labelled: "Root").tap()
        app.buttons["A"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Minor Pentatonic"].tap()
        XCTAssertTrue(app.navigationBars["A Minor Pentatonic"].waitForExistence(timeout: 3))
        keep("02-a-minor-pentatonic")

        // 3. Scale degrees instead of note names, on a blues scale.
        let degreesChoice = app.buttons["Degrees"]
        XCTAssertTrue(app.scrollUntilVisible(degreesChoice))
        degreesChoice.tap()
        app.swipeDown()
        app.menuButton(labelled: "Root").tap()
        app.buttons["E"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Blues"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["E Blues"].waitForExistence(timeout: 3))
        keep("03-e-blues-degrees")

        // 4. The TV layout filling the screen, the picture the TV shows. The iPhone turns
        //    itself for it; iPadOS never rotates an app, so the iPad is turned by hand
        //    the way a player would.
        let fullScreenButton = app.buttons["Full-screen board"]
        XCTAssertTrue(app.scrollUntilVisible(fullScreenButton))
        if app.isPad {
            XCUIDevice.shared.orientation = .landscapeLeft
        }
        fullScreenButton.tap()
        let closeButton = app.buttons["Close"]
        XCTAssertTrue(closeButton.waitForExistence(timeout: 3))
        sleep(1)
        keep("04-tv-board-landscape")
        closeButton.tap()
        // The board locks the phone to landscape; turn it back before the next screen.
        XCUIDevice.shared.orientation = .portrait
        XCTAssertTrue(fullScreenButton.waitForExistence(timeout: 3))
        sleep(1)

        // 5. The chord tables for every key.
        app.openTab("Chords")
        XCTAssertTrue(app.staticTexts["C major"].waitForExistence(timeout: 3))
        keep("05-chord-tables")
    }

    @MainActor
    private func keep(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
