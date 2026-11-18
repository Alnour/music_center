package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.ui.BOARD_TEST_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The options panel driven by the remote alone: every choice list applies through the
 * session and returns to its row, and every row can be reached with the D-pad.
 */
@RunWith(AndroidJUnit4::class)
class TvPanelTest {
    @get:Rule
    val tv = TvTestRule()

    // feature: roots-scales
    @Test
    fun choosingAScaleFromThePanel() {
        tv.openPanel()
        tv.pressOk()
        tv.composeRule
            .onNodeWithTag("choice-G")
            .assertIsFocused()
        tv.focusByKeys("choice-A")
        tv.pressOk()

        tv.composeRule
            .onNodeWithTag("row-root")
            .assertIsFocused()
        tv.press(Key.DirectionDown)
        tv.pressOk()
        tv.composeRule
            .onNodeWithTag("choice-builtIn:major")
            .assertIsFocused()
        tv.focusByKeys("choice-builtIn:minorPentatonic")
        tv.pressOk()

        tv.composeRule
            .onNodeWithText("A Minor Pentatonic")
            .assertIsDisplayed()
        tv.composeRule
            .onNodeWithTag("row-scale")
            .assertIsFocused()
        val settings = tv.app.sessionState.value.settings
        assertEquals(NoteName(Letter.A), settings.root)
        assertEquals(ScaleType.MINOR_PENTATONIC, settings.scaleType)
    }

    // feature: keyboard
    @Test
    fun keyboardFromThePanel() {
        tv.openPanel()
        tv.focusByKeys("row-instrument")
        tv.pressOk()
        tv.focusByKeys("choice-keyboard")
        tv.pressOk()

        tv.composeRule
            .onNodeWithTag("row-tuning")
            .assertDoesNotExist()
        tv.focusByKeys("row-octaves")
        tv.pressOk()
        tv.focusByKeys("choice-1")
        tv.pressOk()
        tv.composeRule
            .onNodeWithTag("row-octaves")
            .assertTextContains("1 octave")

        tv.pressBack()
        tv.composeRule
            .onNodeWithText("Keyboard")
            .assertIsDisplayed()
        val settings = tv.app.sessionState.value.settings
        assertEquals(Instrument.KEYBOARD, settings.selectedInstrument)
        assertEquals(1, settings.selectedKeyboardOctaves)
    }

    // feature: labels
    @Test
    fun labelsFromThePanel() {
        tv.openPanel()
        tv.focusByKeys("row-labels")
        tv.pressOk()
        tv.focusByKeys("choice-degrees")
        tv.pressOk()

        val degrees =
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "Circles show scale degrees",
            )
        tv.composeRule
            .onNodeWithTag(BOARD_TEST_TAG, useUnmergedTree = true)
            .assert(degrees)
        tv.composeRule
            .onNodeWithTag("row-labels")
            .assertTextContains("Degrees")
    }

    // feature: tv-board
    @Test
    fun darkBoardSwitch() {
        tv.openPanel()
        tv.focusByKeys("row-dark-board")
        tv.composeRule
            .onNodeWithTag("row-dark-board")
            .assertIsOn()

        tv.pressOk()

        tv.composeRule
            .onNodeWithTag("row-dark-board")
            .assertIsOff()
            .assertIsFocused()
        assertFalse(tv.app.sessionState.value.settings.tvUsesDarkTheme)
    }

    // feature: tv-board
    @Test
    fun everyRowIsReachableByDpad() {
        tv.openPanel()
        val guitarRows = mutableListOf<TvPanelRow>()
        for (row in TvPanelRow.entries) {
            if (row.isShown(Instrument.GUITAR)) {
                guitarRows.add(row)
            }
        }

        for (row in guitarRows) {
            tv.composeRule
                .onNodeWithTag(row.testTag)
                .assertIsFocused()
            // Left and right are trapped: focus never falls onto the board under the panel.
            tv.press(Key.DirectionLeft)
            tv.composeRule
                .onNodeWithTag(row.testTag)
                .assertIsFocused()
            tv.press(Key.DirectionDown)
        }

        val lastRow = guitarRows.last()
        tv.composeRule
            .onNodeWithTag(lastRow.testTag)
            .assertIsFocused()
    }
}
