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

    @MainActor
    func testFullScreenBoardOpensInLandscapeAndCloses() throws {
        let fullScreenButton = app.buttons["Full-screen board"]
        app.swipeUp()
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
        XCTAssertTrue(fullScreenButton.waitForExistence(timeout: 3))
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
    func testTVSectionOffersAirPlayAndStatus() throws {
        app.swipeUp()
        app.swipeUp()
        let airPlayButton = app.buttons["AirPlay"]
        XCTAssertTrue(airPlayButton.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Not mirroring"].exists)
        attachScreenshot(named: "TV section")
    }

    @MainActor
    func testChordTablesShowMajorAndMinorKeys() throws {
        app.openTab("Chords")
        XCTAssertTrue(app.staticTexts["C major"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["B diminished"].exists)
        app.buttons["Minor keys"].tap()
        XCTAssertTrue(app.staticTexts["A minor"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["or G♯ diminished"].exists)
        attachScreenshot(named: "Minor key chords")
        app.buttons["Major keys"].tap()
        XCTAssertTrue(app.staticTexts["C major"].waitForExistence(timeout: 3))
        attachScreenshot(named: "Major key chords")
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

        let lessonRow = app.staticTexts["G Major → G Mixolydian"]
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
