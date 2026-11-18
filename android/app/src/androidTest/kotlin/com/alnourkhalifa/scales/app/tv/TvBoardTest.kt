package com.alnourkhalifa.scales.app.tv

import android.view.WindowManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The TV's start screen: the board takes focus at launch so the remote's OK has something to
 * act on, OK opens the panel on Root, and Keep board on holds the screen awake.
 */
@RunWith(AndroidJUnit4::class)
class TvBoardTest {
    @get:Rule
    val tv = TvTestRule()

    // feature: tv-board
    @Test
    fun launchFocusesBoard() {
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()
        tv.composeRule
            .onNodeWithText("G Major")
            .assertIsDisplayed()
        tv.composeRule
            .onNodeWithText("Press OK for options")
            .assertExists()
    }

    // feature: tv-board
    @Test
    fun okOpensThePanelWithRootFocused() {
        tv.openPanel()

        tv.composeRule
            .onNodeWithTag("row-root")
            .assertIsFocused()
            .assertTextContains("G")
    }

    // feature: keep-awake
    @Test
    fun keepBoardOnRowOffersTimes() {
        tv.openPanel()
        tv.focusByKeys("row-keep-board-on")
        tv.pressOk()

        for (label in listOf("Off", "30 min", "60 min", "90 min")) {
            tv.composeRule
                .onNodeWithText(label)
                .assertExists()
        }
        tv.composeRule
            .onNodeWithTag("choice-off")
            .assertIsFocused()
        tv.press(Key.DirectionDown)
        tv.pressOk()

        tv.composeRule
            .onNodeWithTag("row-keep-board-on")
            .assertIsFocused()
            .assertTextContains("30 min")
        assertTrue(keepsScreenOn())

        tv.pressOk()
        tv.focusByKeys("choice-off")
        tv.pressOk()
        assertEquals(false, keepsScreenOn())
    }

    private fun keepsScreenOn(): Boolean {
        val flags =
            tv.composeRule.activity.window.attributes.flags
        return flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
    }
}
