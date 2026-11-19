package com.alnourkhalifa.scales.app.phone

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alnourkhalifa.scales.ui.BOARD_TEST_TAG
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** How long a test waits for the screen to catch up with a tap or a rotation. */
private const val WAIT_MILLIS = 5_000L

/**
 * Drives the Scales screen as a player would: the menus, the pills, the chord cards, the
 * note change and the TV controls. Twin of the Scales-screen tests in
 * `ScalesUITests/ScalesUITests.swift`; every test starts from a fresh install
 * ([PhoneTestRule]), so none needs to put the guitar back as the iOS tests do.
 */
@RunWith(AndroidJUnit4::class)
class PhoneScalesTest {
    @get:Rule
    val phone = PhoneTestRule()

    private val compose
        get() = phone.composeRule

    // feature: board, roots-scales
    @Test
    fun choosingAScaleUpdatesTheTitle() {
        chooseRoot("A")
        chooseScale("Minor Pentatonic")
        waitForTitle("A Minor Pentatonic")
    }

    // The keyboard replaces the fretboard and hides the guitar-only options.
    // feature: keyboard, chord-chips-rings
    @Test
    fun choosingTheKeyboardShowsAnOctave() {
        compose.onNodeWithText("Guitar").assertIsSelected()
        openBoardSettings()
        compose.onNodeWithText("Tuning").assertExists("guitar options start in the settings")
        closeSheet()
        waitUntilGone("Tuning")

        compose.onNodeWithText("Keyboard").performClick()
        val keyboard = hasContentDescription("Keyboard showing", substring = true)
        compose.onNode(keyboard).assertExists("the keyboard replaces the fretboard")

        val firstChip = firstChordCard()
        firstChip.performClick()
        firstChip.assertIsSelected()
        firstChip.performClick()
        firstChip.assertIsNotSelected()

        openBoardSettings()
        compose.onNodeWithText("Tuning").assertDoesNotExist()
        compose.onNodeWithText("1 octave").performClick()
        compose.onNodeWithText("1 octave").assertIsSelected()
        compose.onNodeWithText("2 octaves").performClick()
        compose.onNodeWithText("2 octaves").assertIsSelected()
        closeSheet()
        waitUntilGone("2 octaves")

        compose.onNodeWithText("Guitar").performClick()
        openBoardSettings()
        compose.onNodeWithText("Tuning").assertExists("guitar options return")
        closeSheet()
    }

    // feature: tv-board
    @Test
    fun fullScreenBoardOpensInLandscapeAndCloses() {
        compose.onNodeWithTag("tvMenu").performClick()
        compose.onNodeWithText("Full-screen board").performClick()
        compose.waitUntil(WAIT_MILLIS) { exists(hasContentDescription("Close")) }

        if (isTablet()) {
            if (isUpright()) {
                compose.onNodeWithText(TURN_TABLET_HINT).assertExists("upright tablet")
            }
        } else {
            compose.waitUntil(WAIT_MILLIS) { !isUpright() }
        }

        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitUntil(WAIT_MILLIS) { !exists(hasContentDescription("Close")) }
        compose.onNodeWithTag("tvMenu").assertIsDisplayed()
    }

    // feature: tv-board
    @Test
    fun tvMenuOffersThemeAndFullScreen() {
        compose.onNodeWithTag("tvMenu").performClick()
        val darkTheme = compose.onNode(hasContentDescription("Dark TV theme") and isToggleable())
        darkTheme.assertIsOn()
        darkTheme.performClick()
        darkTheme.assertIsOff()
        assertFalse(phone.app.sessionState.value.settings.tvUsesDarkTheme)
        compose.onNodeWithText("Full-screen board").assertExists()
        closeSheet()
    }

    // A blues scale shows the three seventh chords it is played over, with the chord note the
    // scale lacks marked, and ringing one rings only the notes the scale has.
    // feature: chord-harmony
    @Test
    fun bluesScaleShowsTheTwelveBarChords() {
        chooseRoot("A")
        chooseScale("Blues")
        waitForTitle("A Blues")

        val tonicSeventh =
            compose.onNodeWithContentDescription(
                "I7, A dominant seventh: A, C♯ (not in the scale), E, G",
            )
        tonicSeventh.assertExists("the blues seventh chords are listed")
        compose
            .onNodeWithContentDescription("IV7, D dominant seventh: D, F♯ (not in the scale), A, C")
            .assertExists()
        compose
            .onNodeWithContentDescription(
                "V7, E dominant seventh: E, G♯ (not in the scale), B (not in the scale), D",
            ).assertExists()
        compose
            .onNodeWithTag("harmonyNote")
            .assertExists("the cards say where the chords come from")
        tonicSeventh.performClick()
        tonicSeventh.assertIsSelected()

        chooseScale("Minor Pentatonic")
        waitForTitle("A Minor Pentatonic")
        val subdominant = hasContentDescription("iv, D minor: D, F (not in the scale), A")
        compose.onNodeWithTag("boardChords").performScrollToNode(subdominant)
        compose.onNode(subdominant).assertExists()
    }

    // The chords sit under the board as cards, and a note changes in place: the chords that
    // used it change with it, and the way back restores the scale.
    // feature: chord-chips-rings, note-edit-revert
    @Test
    fun scalesScreenChangesANote() {
        chooseRoot("G")
        chooseScale("Major")
        waitForTitle("G Major")

        val dominantChip = compose.onNodeWithContentDescription("V, D major: D, F♯, A")
        dominantChip.performClick()
        dominantChip.assertIsSelected()
        dominantChip.performClick()
        dominantChip.assertIsNotSelected()

        compose.onNodeWithTag("changeBoardNote").performClick()
        compose.onNodeWithText("F♯ (7)").performClick()
        compose.onNodeWithText("Lower to F (♭7)").performClick()
        waitForTitle("G Mixolydian")
        val wasDominant = hasContentDescription("D minor: D, F, A. Was D major", substring = true)
        compose.onNodeWithTag("boardChords").performScrollToNode(wasDominant)
        compose.onNode(wasDominant).assertExists("the V chord shows what it was")

        compose.onNodeWithTag("revertBoardChange").performClick()
        waitForTitle("G Major")
    }

    // The pills change what the circles show; TalkBack hears it as the board's state.
    // feature: labels
    @Test
    fun labelPillsChangeTheBoardDescription() {
        val board = compose.onNodeWithTag(BOARD_TEST_TAG)
        board.assert(stateIs("Circles show note names"))
        compose.onNodeWithText("Degrees").performClick()
        board.assert(stateIs("Circles show scale degrees"))
        compose.onNodeWithText("Dots").performClick()
        board.assert(stateIs("Plain circles"))
        compose.onNodeWithText("Notes").performClick()
        board.assert(stateIs("Circles show note names"))
    }

    // The row above the board spells the scale, read as one sentence.
    // feature: notes-row
    @Test
    fun notesRowIsSpoken() {
        compose.onNodeWithContentDescription("Notes: G, A, B, C, D, E, F♯").assertExists()
    }

    // The board names its scale, and a card says its numeral, name and notes.
    // feature: accessibility-labels
    @Test
    fun boardAndChordCardsAreSpoken() {
        compose.onNodeWithContentDescription("Fretboard showing G Major").assertExists()
        compose.onNodeWithContentDescription("V, D major: D, F♯, A").assertExists()
    }

    private fun chooseRoot(name: String) {
        compose.onNodeWithTag("rootMenu").performClick()
        compose.onNodeWithTag("root-$name").performClick()
        compose.waitForIdle()
    }

    private fun chooseScale(name: String) {
        compose.onNodeWithTag("scaleMenu").performClick()
        val choice = compose.onNodeWithTag("scale-$name")
        choice.performScrollTo()
        choice.performClick()
        compose.waitForIdle()
    }

    private fun waitForTitle(title: String) {
        val titleNode = hasTestTag("scaleTitle") and hasText(title)
        compose.waitUntil(WAIT_MILLIS) { exists(titleNode) }
    }

    private fun openBoardSettings() {
        compose.onNodeWithTag("boardSettings").performClick()
        compose.waitForIdle()
    }

    /** Back closes a bottom sheet, as the system back gesture does. */
    private fun closeSheet() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun waitUntilGone(text: String) {
        compose.waitUntil(WAIT_MILLIS) {
            compose
                .onAllNodesWithText(text)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    private fun firstChordCard(): SemanticsNodeInteraction {
        val card = hasAnyAncestor(hasTestTag("boardChords")) and hasClickAction()
        return compose.onAllNodes(card).onFirst()
    }

    private fun exists(matcher: SemanticsMatcher): Boolean =
        compose
            .onAllNodes(matcher)
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun stateIs(description: String): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    private fun isTablet(): Boolean {
        val configuration = compose.activity.resources.configuration
        return configuration.smallestScreenWidthDp >= 600
    }

    private fun isUpright(): Boolean {
        val configuration = compose.activity.resources.configuration
        return configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    }
}
