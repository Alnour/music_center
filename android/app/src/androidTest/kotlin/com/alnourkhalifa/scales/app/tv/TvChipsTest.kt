package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alnourkhalifa.scales.core.music.ScaleDegree
import com.alnourkhalifa.scales.core.music.ScaleEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The first chip of G major, the one focus enters on when nothing is ringed. */
private const val TONIC_CHIP = "I, G major: G, B, D"

/** G major's fifth chord, four steps along the row. */
private const val DOMINANT_CHIP = "V, D major: D, F♯, A"

/** Steps Right from the first chip to the V chip. */
private const val STEPS_TO_DOMINANT = 4

/**
 * The chord chips under the TV board, driven by the remote: Down from the board enters them,
 * OK rings a chord's notes and OK again clears them, and after a note change the chips say
 * what it did while "Back to …" undoes it.
 */
@RunWith(AndroidJUnit4::class)
class TvChipsTest {
    @get:Rule
    val tv = TvTestRule()

    // feature: chord-chips-rings
    @Test
    fun downFromTheBoardFocusesTheFirstChip() {
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()

        tv.press(Key.DirectionDown)
        tv.composeRule
            .onNodeWithContentDescription(TONIC_CHIP)
            .assertIsFocused()

        // Left at the row's start stays put; Up goes back to the board.
        tv.press(Key.DirectionLeft)
        tv.composeRule
            .onNodeWithContentDescription(TONIC_CHIP)
            .assertIsFocused()
        tv.press(Key.DirectionUp)
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()
    }

    // feature: chord-chips-rings
    @Test
    fun okRingsAndUnringsAChip() {
        val settingsBefore = tv.app.sessionState.value.settings
        tv.press(Key.DirectionDown)
        repeat(STEPS_TO_DOMINANT) {
            tv.press(Key.DirectionRight)
        }
        val dominant = tv.composeRule.onNodeWithContentDescription(DOMINANT_CHIP)
        dominant.assertIsFocused()
        dominant.assertIsNotSelected()

        tv.pressOk()
        dominant.assertIsSelected()
        dominant.assertIsFocused()
        val ringed = tv.app.sessionState.value.ringedChord
        assertEquals("D", ringed?.triad?.name)
        // OK on a chip rings the chord; it doesn't open the panel behind it.
        tv.composeRule
            .onNodeWithTag("panel")
            .assertDoesNotExist()

        tv.pressOk()
        dominant.assertIsNotSelected()
        dominant.assertIsFocused()
        val state = tv.app.sessionState.value
        assertNull(state.ringedChord)
        assertEquals(settingsBefore, state.settings)
    }

    // feature: note-edit-revert
    @Test
    fun chipsShowWhatAChangeDid() {
        tv.onMainThread {
            tv.app.session
                .transform(ScaleEdit.Lower(ScaleDegree(7, 0)))
        }
        tv.composeRule.waitForIdle()

        val changedChip = hasContentDescription("D minor: D, F, A. Was D major", substring = true)
        val changedNodes =
            tv.composeRule
                .onAllNodes(changedChip)
                .fetchSemanticsNodes()
        assertTrue(changedNodes.isNotEmpty())
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()

        tv.press(Key.DirectionDown)
        tv.composeRule
            .onNodeWithTag("revertBoardChange")
            .assertIsFocused()

        // The chips sit under the button: Down reaches them and Up comes back.
        tv.press(Key.DirectionDown)
        tv.composeRule
            .onNodeWithTag("tvChip-0")
            .assertIsFocused()
        tv.press(Key.DirectionUp)
        tv.composeRule
            .onNodeWithTag("revertBoardChange")
            .assertIsFocused()

        tv.pressOk()
        tv.composeRule
            .onNodeWithText("G Major")
            .assertIsDisplayed()
        tv.composeRule
            .onNodeWithTag("revertBoardChange")
            .assertDoesNotExist()
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()
    }

    // feature: chord-chips-rings
    @Test
    fun backFromAChipReturnsToTheBoard() {
        val activity = tv.composeRule.activity
        tv.press(Key.DirectionDown)
        tv.composeRule
            .onNodeWithContentDescription(TONIC_CHIP)
            .assertIsFocused()

        tv.pressBack()
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()
        assertTrue(!activity.isFinishing)
    }
}
