package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What a screen reader says about the TV board: the same words as on the iPhone, so a player
 * who cannot see the screen hears the scale, its notes and each chord card.
 */
@RunWith(AndroidJUnit4::class)
class TvSpokenTest {
    @get:Rule
    val tv = TvTestRule()

    // feature: board
    @Test
    fun theDefaultBoardIsGMajorOnTheGuitar() {
        tv.composeRule
            .onNodeWithContentDescription("Fretboard showing G Major")
            .assertIsDisplayed()
        tv.composeRule
            .onNodeWithText("Standard tuning")
            .assertIsDisplayed()
    }

    // feature: notes-row
    @Test
    fun theNotesRowIsSpoken() {
        tv.composeRule
            .onNodeWithContentDescription("Notes: G, A, B, C, D, E, F♯")
            .assertIsDisplayed()
    }

    // feature: accessibility-labels
    @Test
    fun theBoardAndChordCardsAreSpoken() {
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsDisplayed()
        val chipDescriptions =
            tv.composeRule
                .onAllNodes(hasContentDescription("V, D major: D, F♯, A", substring = true))
                .fetchSemanticsNodes()
        assertTrue("the V chord card is spoken with its notes", chipDescriptions.isNotEmpty())
    }
}
