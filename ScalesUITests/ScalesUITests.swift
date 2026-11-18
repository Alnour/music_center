// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import XCTest

/// Drives the real interface: picking a scale and opening the full-screen board.
final class ScalesUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUp() async throws {
        continueAfterFailure = false
        app = await XCUIApplication()
        await app.launch()
    }

    @MainActor
    func testChoosingAScaleUpdatesTheTitle() throws {
        app.menuButton(labelled: "Root").tap()
        app.buttons["A"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Minor Pentatonic"].tap()

        XCTAssertTrue(app.navigationBars["A Minor Pentatonic"].waitForExistence(timeout: 3))
        attachScreenshot(named: "A minor pentatonic on the phone")
    }

    /// The keyboard replaces the fretboard and hides the guitar-only options. The choice is
    /// saved between launches, so the test puts the guitar back for the other tests.
    @MainActor
    func testChoosingTheKeyboardShowsAnOctave() throws {
        let guitarChoice = app.buttons["Guitar"]
        XCTAssertTrue(guitarChoice.waitForExistence(timeout: 3), "the instrument pills are in the toolbar")
        guitarChoice.tap()
        app.openBoardSettings()
        let tuningRow = app.menuButton(labelled: "Tuning")
        XCTAssertTrue(tuningRow.waitForExistence(timeout: 3), "guitar options start in the settings")
        app.dismissPopover()
        XCTAssertTrue(tuningRow.waitForNonExistence(timeout: 3), "the settings close")

        app.buttons["Keyboard"].tap()
        let keyboard = app.otherElements.matching(
            NSPredicate(format: "label BEGINSWITH %@", "Keyboard showing")
        ).firstMatch
        XCTAssertTrue(keyboard.waitForExistence(timeout: 3), "the octave replaces the fretboard")
        attachScreenshot(named: "The scale on two octaves")

        let firstChip = app.element(identified: "boardChords").buttons.firstMatch
        XCTAssertTrue(firstChip.waitForExistence(timeout: 3), "the chords sit under the keyboard")
        firstChip.tap()
        XCTAssertTrue(firstChip.isSelected, "the picked chord is marked on the keyboard too")
        attachScreenshot(named: "A chord ringed on the keyboard")
        firstChip.tap()
        XCTAssertFalse(firstChip.isSelected, "picking it again clears the keyboard rings")

        app.openBoardSettings()
        XCTAssertFalse(tuningRow.exists, "tuning means nothing on a piano")
        let oneOctave = app.buttons["1 octave"]
        XCTAssertTrue(oneOctave.waitForExistence(timeout: 3), "the keyboard offers one octave")
        oneOctave.tap()
        app.dismissPopover()
        attachScreenshot(named: "The scale on one octave")
        app.openBoardSettings()
        XCTAssertTrue(app.buttons["2 octaves"].waitForExistence(timeout: 3))
        app.buttons["2 octaves"].tap()
        app.dismissPopover()

        guitarChoice.tap()
        app.openBoardSettings()
        XCTAssertTrue(tuningRow.waitForExistence(timeout: 3), "guitar options return")
        app.dismissPopover()
    }

    @MainActor
    func testFullScreenBoardOpensInLandscapeAndCloses() throws {
        app.openTVMenu()
        let fullScreenButton = app.buttons["Full-screen board"]
        XCTAssertTrue(fullScreenButton.waitForExistence(timeout: 3))
        fullScreenButton.tap()

        let closeButton = app.buttons["Close"]
        XCTAssertTrue(closeButton.waitForExistence(timeout: 3))
        if app.isPad {
            assertUprightIPadAsksToBeTurned()
        } else {
            let window = app.windows.firstMatch
            let isLandscape = window.frame.width > window.frame.height
            XCTAssertTrue(isLandscape, "board should be landscape")
        }

        closeButton.tap()
        XCTAssertTrue(app.buttons["tvMenu"].waitForExistence(timeout: 3))
    }

    /// iPadOS never rotates an app on request, so an upright iPad shows the board at the
    /// TV's shape with a hint, and the hint goes away once the iPad is turned sideways.
    @MainActor
    private func assertUprightIPadAsksToBeTurned() {
        let hint = app.staticTexts["Turn your iPad sideways so the board fills the TV"]
        XCTAssertTrue(hint.exists, "upright iPad should be asked to turn")
        attachScreenshot(named: "Full-screen board on an upright iPad")

        XCUIDevice.shared.orientation = .landscapeLeft
        XCTAssertTrue(hint.waitForNonExistence(timeout: 3), "hint should go once turned")
        attachScreenshot(named: "Full-screen board on a sideways iPad")
        XCUIDevice.shared.orientation = .portrait
    }

    @MainActor
    func testTVMenuOffersAirPlayAndStatus() throws {
        app.openTVMenu()
        let airPlayButton = app.buttons["AirPlay"]
        XCTAssertTrue(airPlayButton.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Not mirroring"].exists)
        attachScreenshot(named: "TV menu")
        app.dismissPopover()
    }

    @MainActor
    func testDrumsTabPlaysAndPausesTheGroove() throws {
        app.openTab("Drums")
        let playButton = app.buttons["Play"]
        XCTAssertTrue(playButton.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["tempoLabel"].exists)
        playButton.tap()
        let pauseButton = app.buttons["Pause"]
        XCTAssertTrue(pauseButton.waitForExistence(timeout: 3))
        attachScreenshot(named: "Drum trainer playing")
        pauseButton.tap()
        XCTAssertTrue(playButton.waitForExistence(timeout: 3))
    }

    @MainActor
    func testChordTablesShowMajorAndMinorKeys() throws {
        app.openTab("Chords")
        XCTAssertTrue(app.staticTexts["C major"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["B diminished"].exists)
        app.buttons["Minor keys"].tap()
        XCTAssertTrue(app.staticTexts["A minor"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["E minor"].firstMatch.exists, "minor keys list natural minor's chords")
        XCTAssertFalse(app.staticTexts["or G♯ diminished"].exists, "no harmonic-minor stand-ins")
        attachScreenshot(named: "Minor key chords")
        app.buttons["Major keys"].tap()
        XCTAssertTrue(app.staticTexts["C major"].waitForExistence(timeout: 3))
        attachScreenshot(named: "Major key chords")
    }

    /// The Scales screen shows the chords as cards under the board and changes one note in
    /// place: the chords that used it change with it.
    @MainActor
    func testScalesScreenChangesANote() throws {
        let rootMenu = app.menuButton(labelled: "Root")
        rootMenu.tap()
        app.buttons["G"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Major"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["G Major"].waitForExistence(timeout: 3))

        attachScreenshot(named: "Board and chords")

        let dominantChip = app.buttons["V, D major: D, F♯, A"]
        XCTAssertTrue(dominantChip.waitForExistence(timeout: 3), "the chords are listed")
        dominantChip.tap()
        XCTAssertTrue(dominantChip.isSelected, "the picked chord is marked")
        attachScreenshot(named: "The V chord ringed on the board")
        dominantChip.tap()
        XCTAssertFalse(dominantChip.isSelected, "picking it again clears the rings")

        app.buttons["changeBoardNote"].tap()
        let seventh = app.descendants(matching: .any)["F♯ (7)"]
        XCTAssertTrue(seventh.waitForExistence(timeout: 3), "the menu lists the seventh")
        seventh.tap()
        let lowering = app.descendants(matching: .any)["Lower to F (♭7)"]
        XCTAssertTrue(lowering.waitForExistence(timeout: 3), "the seventh can be lowered")
        lowering.tap()
        XCTAssertTrue(app.navigationBars["G Mixolydian"].waitForExistence(timeout: 3))
        let dominant = app.descendants(matching: .any).matching(
            NSPredicate(format: "label CONTAINS %@", "D minor: D, F, A. Was D major")
        ).firstMatch
        XCTAssertTrue(dominant.waitForExistence(timeout: 3), "the V chord shows what it was")
        attachScreenshot(named: "Chords after lowering the seventh")

        app.buttons["revertBoardChange"].tap()
        XCTAssertTrue(app.navigationBars["G Major"].waitForExistence(timeout: 3))

        // The change was kept as a lesson; remove it so the library is as it was.
        app.openTab("Transform")
        let lessonRow = app.staticTexts["G Major → G Mixolydian"].firstMatch
        XCTAssertTrue(app.scrollUntilVisible(lessonRow), "the board change is a lesson")
        lessonRow.swipeLeft()
        app.buttons["Delete"].firstMatch.tap()
        XCTAssertTrue(lessonRow.waitForNonExistence(timeout: 3))
    }

    @MainActor
    func testTransformTabExplainsALoweredSeventh() throws {
        app.openTab("Transform")
        chooseTransformStart(key: "G", scale: "Major")
        app.element(identified: "editNote-7").tap()
        app.buttons["Lower to F (♭7)"].tap()

        XCTAssertTrue(app.staticTexts["This is G Mixolydian"].waitForExistence(timeout: 3))
        attachScreenshot(named: "Lesson for G major with a flat seventh")
        let dominant = app.staticTexts.matching(
            NSPredicate(format: "label BEGINSWITH %@", "V, D major, becomes v, D minor")
        ).firstMatch
        XCTAssertTrue(app.scrollUntilVisible(dominant), "the dominant chord's change is explained")
        attachScreenshot(named: "Chord changes for a flat seventh")
        app.buttons["saveLessonButton"].tap()

        let lessonRow = app.staticTexts["G Major → G Mixolydian"].firstMatch
        XCTAssertTrue(app.scrollUntilVisible(lessonRow), "the saved lesson is listed")
        attachScreenshot(named: "Saved lesson in the list")
        lessonRow.swipeLeft()
        app.buttons["Delete"].firstMatch.tap()
        XCTAssertTrue(lessonRow.waitForNonExistence(timeout: 3))
    }

    @MainActor
    func testNamingANewScaleAddsItToMyScales() throws {
        let name = "Test Scale \(UUID().uuidString.prefix(4))"
        app.openTab("Transform")
        chooseTransformStart(key: "G", scale: "Major")
        app.element(identified: "editNote-5").tap()
        app.buttons["Raise to D♯ (♯5)"].tap()

        let nameField = app.textFields["scaleNameField"]
        XCTAssertTrue(nameField.waitForExistence(timeout: 3))
        XCTAssertFalse(app.buttons["saveLessonButton"].isEnabled, "a new scale needs a name")
        nameField.tap()
        nameField.typeText(name)
        attachScreenshot(named: "Naming a new scale")
        app.buttons["saveLessonButton"].tap()

        // Found by identifier: the iPad keeps the Scales screen alive, and once the scale is
        // chosen there its name appears in that screen's title and picker too.
        let scaleRow = app.element(identified: "myScale-\(name)")
        XCTAssertTrue(app.scrollUntilVisible(scaleRow), "the new scale is in My Scales")
        attachScreenshot(named: "A new scale in My Scales")

        app.openTab("Scales")
        app.menuButton(labelled: "Scale").tap()
        // My Scales comes after the built-in scales, below the menu's first screenful.
        let pickerChoice = app.buttons[name]
        let menuList = app.collectionViews.containing(
            NSPredicate(format: "label == %@", "Whole Tone")
        ).firstMatch
        var menuSwipes = 0
        while !pickerChoice.waitForExistence(timeout: 1) && menuSwipes < 3 {
            menuList.swipeUp()
            menuSwipes += 1
        }
        XCTAssertTrue(pickerChoice.exists, "the Scales picker offers it")
        pickerChoice.tap()
        let title = app.navigationBars.matching(NSPredicate(format: "identifier ENDSWITH %@", name))
        XCTAssertTrue(title.firstMatch.waitForExistence(timeout: 3), "the fretboard shows it")
        attachScreenshot(named: "A new scale on the fretboard")

        app.openTab("Transform")
        XCTAssertTrue(app.scrollUntilVisible(scaleRow))
        scaleRow.swipeLeft()
        app.buttons["Delete"].firstMatch.tap()
        app.buttons["Delete Scale"].tap()
        XCTAssertTrue(scaleRow.waitForNonExistence(timeout: 3), "the scale is deleted")
    }

    /// Sets the Transform screen's starting key and scale from their pickers.
    @MainActor
    private func chooseTransformStart(key: String, scale: String) {
        app.menuButton(labelled: "Key").tap()
        app.buttons[key].firstMatch.tap()
        app.menuButton(labelled: "Starting scale").tap()
        app.buttons[scale].firstMatch.tap()
    }

    @MainActor
    private func attachScreenshot(named name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
