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
    // feature: board, roots-scales
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
    // feature: keyboard, chord-chips-rings
    func testChoosingTheKeyboardShowsAnOctave() throws {
        let guitarChoice = app.buttons["Guitar"]
        XCTAssertTrue(
            guitarChoice.waitForExistence(timeout: 3),
            "the instrument pills are in the toolbar"
        )
        guitarChoice.tap()
        app.openBoardSettings()
        let tuningRow = app.menuButton(labelled: "Tuning")
        XCTAssertTrue(
            tuningRow.waitForExistence(timeout: 3),
            "guitar options start in the settings"
        )
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

    /// The bass draws the scale on its own four-string neck, with its own tunings and no
    /// capo. The instrument is saved between launches, so the test puts the guitar back.
    @MainActor
    // feature: bass
    func testChoosingTheBassShowsItsNeckAndTunings() throws {
        let bassChoice = app.buttons["Bass"]
        XCTAssertTrue(bassChoice.waitForExistence(timeout: 3), "the bass is an instrument pill")
        bassChoice.tap()
        let bassBoard = app.descendants(matching: .any).matching(
            NSPredicate(format: "label BEGINSWITH %@", "Bass fretboard showing")
        ).firstMatch
        XCTAssertTrue(bassBoard.waitForExistence(timeout: 3), "the bass neck is on the board")
        attachScreenshot(named: "The scale on the bass")

        let firstChip = app.element(identified: "boardChords").buttons.firstMatch
        XCTAssertTrue(firstChip.waitForExistence(timeout: 3), "the chords sit under the bass")

        app.openBoardSettings()
        let tuningRow = app.menuButton(labelled: "Tuning")
        XCTAssertTrue(tuningRow.waitForExistence(timeout: 3), "the bass has a tuning")
        XCTAssertTrue(tuningRow.label.contains("E A D G"), "the bass starts in standard tuning")
        XCTAssertFalse(tuningRow.label.contains("B E)"), "its tunings are not the guitar's")
        XCTAssertFalse(app.element(identified: "capoPicker").exists, "the bass has no capo")
        tuningRow.tap()
        let fiveString = app.buttons["Five-string (B E A D G)"]
        XCTAssertTrue(fiveString.waitForExistence(timeout: 3), "the five-string bass is offered")
        fiveString.tap()
        app.dismissPopover()
        attachScreenshot(named: "The scale on a five-string bass")

        app.openBoardSettings()
        XCTAssertTrue(tuningRow.waitForExistence(timeout: 3))
        tuningRow.tap()
        let standard = app.buttons["Standard (E A D G)"]
        XCTAssertTrue(standard.waitForExistence(timeout: 3))
        standard.tap()
        app.dismissPopover()

        app.buttons["Guitar"].tap()
        app.openBoardSettings()
        XCTAssertTrue(
            app.element(identified: "capoPicker").waitForExistence(timeout: 3),
            "the guitar's capo returns"
        )
        app.dismissPopover()
    }

    @MainActor
    // feature: tv-board
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
    // feature: airplay-cast
    func testTVMenuOffersAirPlayAndStatus() throws {
        app.openTVMenu()
        let airPlayButton = app.buttons["AirPlay"]
        XCTAssertTrue(airPlayButton.waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["Not mirroring"].exists)
        attachScreenshot(named: "TV menu")
        app.dismissPopover()
    }

    @MainActor
    // feature: drum-trainer
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
    // feature: chord-tables
    func testChordTablesShowMajorAndMinorKeys() throws {
        app.openTab("Chords")
        XCTAssertTrue(app.staticTexts["C major"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["B diminished"].exists)
        app.buttons["Minor keys"].tap()
        XCTAssertTrue(app.staticTexts["A minor"].waitForExistence(timeout: 3))
        XCTAssertTrue(
            app.staticTexts["E minor"].firstMatch.exists,
            "minor keys list natural minor's chords"
        )
        XCTAssertFalse(app.staticTexts["or G♯ diminished"].exists, "no harmonic-minor stand-ins")
        attachScreenshot(named: "Minor key chords")
        app.buttons["Major keys"].tap()
        XCTAssertTrue(app.staticTexts["C major"].waitForExistence(timeout: 3))
        attachScreenshot(named: "Major key chords")
    }

    /// A blues scale shows the three seventh chords it is played over, with the chord note
    /// the scale lacks marked, and ringing one rings only the notes the scale has.
    // feature: chord-harmony
    @MainActor
    func testBluesScaleShowsTheTwelveBarChords() throws {
        app.menuButton(labelled: "Root").tap()
        app.buttons["A"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Blues"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["A Blues"].waitForExistence(timeout: 3))

        let tonicSeventh = app.buttons["I7, A dominant seventh: A, C♯ (not in the scale), E, G"]
        XCTAssertTrue(
            tonicSeventh.waitForExistence(timeout: 3),
            "the blues seventh chords are listed"
        )
        let subdominantSeventh = "IV7, D dominant seventh: D, F♯ (not in the scale), A, C"
        XCTAssertTrue(app.buttons[subdominantSeventh].exists)
        let dominantSeventh =
            "V7, E dominant seventh: E, G♯ (not in the scale), B (not in the scale), D"
        XCTAssertTrue(app.buttons[dominantSeventh].exists)
        XCTAssertTrue(
            app.element(identified: "harmonyNote").exists,
            "the cards say where the chords come from"
        )
        tonicSeventh.tap()
        XCTAssertTrue(tonicSeventh.isSelected, "the picked chord is marked")
        attachScreenshot(named: "A7 ringed over A blues")

        app.menuButton(labelled: "Scale").tap()
        app.buttons["Minor Pentatonic"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["A Minor Pentatonic"].waitForExistence(timeout: 3))
        let subdominant = app.buttons["iv, D minor: D, F (not in the scale), A"]
        XCTAssertTrue(subdominant.waitForExistence(timeout: 3))
        attachScreenshot(named: "Minor pentatonic chords")
    }

    /// A capo on the third fret turns B♭ major's chords into G major's open shapes: each
    /// card names the shape to finger and a line under the cards names the key. The capo is
    /// saved between launches, so the test takes it off again for the other tests.
    // feature: capo-shapes
    @MainActor
    func testCapoNamesTheShapeOnEachChordCard() throws {
        app.buttons["Guitar"].tap()
        app.menuButton(labelled: "Root").tap()
        app.buttons["B♭"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Major"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["B♭ Major"].waitForExistence(timeout: 3))

        app.openBoardSettings()
        let capoMenu = app.menuButton(labelled: "Capo")
        XCTAssertTrue(capoMenu.waitForExistence(timeout: 3), "the guitar options offer a capo")
        XCTAssertTrue(
            app.element(identified: "capoAdvice").exists,
            "the settings name the frets with the most open chords"
        )
        capoMenu.tap()
        let thirdFret = app.buttons["Fret 3 · as G Major · 5 of 7 open"]
        XCTAssertTrue(thirdFret.waitForExistence(timeout: 3), "each fret says what it gives")
        thirdFret.tap()
        // A tap made while the menu is still closing is swallowed, so wait for it to go.
        XCTAssertTrue(thirdFret.waitForNonExistence(timeout: 3), "the menu closes")
        app.dismissPopover()
        XCTAssertTrue(capoMenu.waitForNonExistence(timeout: 3), "the settings close")

        let tonic = app.buttons["I, B♭ major: B♭, D, F. Fingered as G major, an open chord."]
        XCTAssertTrue(tonic.waitForExistence(timeout: 3), "the card names its open shape")
        let mediant = "iii, D minor: D, F, A. Fingered as B minor, a barre chord."
        XCTAssertTrue(app.buttons[mediant].exists, "a shape that needs a barre says so")
        XCTAssertTrue(
            app.element(identified: "capoNote").exists,
            "a line under the cards names the key the shapes come from"
        )
        attachScreenshot(named: "B♭ major with a capo on fret 3")

        app.openBoardSettings()
        XCTAssertTrue(capoMenu.waitForExistence(timeout: 3))
        capoMenu.tap()
        let noCapo = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "No capo"))
        noCapo.firstMatch.tap()
        XCTAssertTrue(thirdFret.waitForNonExistence(timeout: 3), "the menu closes")
        app.dismissPopover()
        XCTAssertTrue(
            app.buttons["I, B♭ major: B♭, D, F"].waitForExistence(timeout: 3),
            "without the capo the cards are plain again"
        )
        XCTAssertTrue(capoMenu.waitForNonExistence(timeout: 3), "the settings close")
        app.menuButton(labelled: "Root").tap()
        app.buttons["G"].firstMatch.tap()
    }

    /// The Scales screen shows the chords as cards under the board and changes one note in
    /// place: the chords that used it change with it.
    @MainActor
    // feature: chord-chips-rings, note-edit-revert
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
    // feature: transform-lessons
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
    // feature: transform-lessons
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
        // My Scales comes after the built-in scales, below the menu's first screenful. The
        // menu only builds the rows on screen, and which rows those are depends on the scale
        // already chosen, so the list is found by either end of the built-in scales.
        let pickerChoice = app.buttons[name]
        let menuList = app.collectionViews.containing(
            NSPredicate(format: "label == %@ OR label == %@", "Major", "Whole Tone")
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

    /// The Notes, Degrees and Dots pills change what the circles show; VoiceOver hears it
    /// as the board's value, so the test reads the same thing a blind player would.
    @MainActor
    // feature: labels
    func testLabelPillsChangeWhatTheCirclesShow() throws {
        chooseGMajorOnTheGuitar()
        let board = boardElement(labelled: "Fretboard showing G Major")
        XCTAssertTrue(board.waitForExistence(timeout: 3))

        app.buttons["Degrees"].firstMatch.tap()
        XCTAssertEqual(board.value as? String, "Circles show scale degrees")
        attachScreenshot(named: "Degrees in the circles")
        app.buttons["Dots"].firstMatch.tap()
        XCTAssertEqual(board.value as? String, "Plain circles")
        app.buttons["Notes"].firstMatch.tap()
        XCTAssertEqual(board.value as? String, "Circles show note names")
    }

    /// The row above the board spells the scale, read as one sentence by VoiceOver.
    @MainActor
    // feature: notes-row
    func testNotesRowSpellsTheScale() throws {
        chooseGMajorOnTheGuitar()
        let notesRow = app.descendants(matching: .any)["Notes: G, A, B, C, D, E, F♯"]
        XCTAssertTrue(notesRow.waitForExistence(timeout: 3), "the notes row spells G major")
    }

    /// The board and the chord cards describe themselves in words: the board names the
    /// scale, and a card says its numeral, its name and its notes.
    @MainActor
    // feature: accessibility-labels
    func testBoardAndChordCardsHaveSpokenLabels() throws {
        chooseGMajorOnTheGuitar()
        let board = boardElement(labelled: "Fretboard showing G Major")
        XCTAssertTrue(board.waitForExistence(timeout: 3), "the board names its scale")
        let dominantChip = app.buttons["V, D major: D, F♯, A"]
        XCTAssertTrue(dominantChip.waitForExistence(timeout: 3), "a card reads out its chord")
    }

    /// The board by its spoken label, whatever kind of element carries it: the fretboard
    /// sits in a sideways scroll view, which takes the label, while the keyboard does not.
    @MainActor
    private func boardElement(labelled label: String) -> XCUIElement {
        let predicate = NSPredicate(format: "label == %@", label)
        return app.descendants(matching: .any).matching(predicate).firstMatch
    }

    /// Puts G major on the fretboard. Settings survive between launches, so each test that
    /// reads the board sets the scale and instrument it expects.
    @MainActor
    private func chooseGMajorOnTheGuitar() {
        let guitarChoice = app.buttons["Guitar"]
        XCTAssertTrue(guitarChoice.waitForExistence(timeout: 3), "the instrument pills are shown")
        guitarChoice.tap()
        app.menuButton(labelled: "Root").tap()
        app.buttons["G"].firstMatch.tap()
        app.menuButton(labelled: "Scale").tap()
        app.buttons["Major"].firstMatch.tap()
        XCTAssertTrue(app.navigationBars["G Major"].waitForExistence(timeout: 3))
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
