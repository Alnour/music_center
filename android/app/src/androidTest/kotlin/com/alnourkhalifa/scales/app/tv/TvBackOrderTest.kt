package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Google's TV-DB: Back climbs one level at a time (a list or two, the panel, the board) and then
 * leaves the app, with no exit confirmation and no menu that reopens itself.
 */
@RunWith(AndroidJUnit4::class)
class TvBackOrderTest {
    @get:Rule
    val tv = TvTestRule()

    // feature: tv-board
    @Test
    fun backClimbsListPanelBoardThenExits() {
        val activity = tv.composeRule.activity
        tv.openPanel()
        tv.pressOk()
        tv.composeRule
            .onNodeWithTag("choiceList")
            .assertExists()

        tv.pressBack()
        tv.composeRule
            .onNodeWithTag("choiceList")
            .assertDoesNotExist()
        tv.composeRule
            .onNodeWithTag("row-root")
            .assertIsFocused()

        // The deepest level: a note's changes, two lists in the panel's place.
        tv.focusByKeys("row-change-a-note")
        tv.pressOk()
        tv.pressOk()
        tv.composeRule
            .onNodeWithTag("choice-lower:2")
            .assertIsFocused()
        tv.pressBack()
        tv.composeRule
            .onNodeWithTag("choice-2")
            .assertIsFocused()
        tv.pressBack()
        tv.composeRule
            .onNodeWithTag("choiceList")
            .assertDoesNotExist()
        tv.composeRule
            .onNodeWithTag("row-change-a-note")
            .assertIsFocused()

        tv.focusByKeys("row-chord-tables")
        tv.pressOk()
        tv.composeRule
            .onNodeWithTag("panel")
            .assertDoesNotExist()

        tv.pressBack()
        tv.composeRule
            .onNodeWithTag("row-chord-tables")
            .assertIsFocused()

        tv.pressBack()
        tv.composeRule
            .onNodeWithTag("panel")
            .assertDoesNotExist()
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()

        tv.pressBack()
        assertTrue(activity.isFinishing || activity.isDestroyed)
    }
}
