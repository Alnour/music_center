package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The TV chord tables, worked with the remote alone: the major keys first, Right switches to
 * the minor keys, and all twelve keys and the footnote are on screen at once, because nothing
 * in the table can take focus to scroll it.
 *
 * Keys and chords are found by their spoken names ("C major", "B diminished"). A name such as
 * "G major" names a key and chords in several other keys, so a check for the last row asks
 * that every node with the name is displayed.
 */
@RunWith(AndroidJUnit4::class)
class TvChordsTest {
    @get:Rule
    val compose = createComposeRule()

    // feature: chord-tables
    @Test
    fun majorKeysFitOnOneScreen() {
        compose.setContent { TvChordsScreen() }

        compose.onNodeWithTag("majorKeys").assertIsFocused()
        assertEveryNodeDisplayed("C major")
        assertEveryNodeDisplayed("B diminished")
        // G is the last of the twelve major keys; its row on screen means none was cut off.
        assertEveryNodeDisplayed("G major")
        compose.onNodeWithText("Each chord stacks", substring = true).assertIsDisplayed()
    }

    // feature: chord-tables
    @Test
    fun rightSwitchesToMinorKeys() {
        compose.setContent { TvChordsScreen() }

        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }

        compose.onNodeWithTag("minorKeys").assertIsFocused()
        assertEveryNodeDisplayed("A minor")
        assertEveryNodeDisplayed("E minor")
        // G♯ minor is the last of the twelve minor keys.
        assertEveryNodeDisplayed("G♯ minor")
        compose
            .onAllNodesWithContentDescription("or G♯ diminished", substring = true)
            .assertCountEquals(0)
        compose.onNodeWithText("natural minor scale", substring = true).assertIsDisplayed()
    }

    /** At least one node reads [spokenName], and every one of them is on screen. */
    private fun assertEveryNodeDisplayed(spokenName: String) {
        val nodes = compose.onAllNodesWithContentDescription(spokenName)
        val count = nodes.fetchSemanticsNodes().size
        assertTrue("no node reads '$spokenName'", count > 0)
        for (index in 0 until count) {
            nodes[index].assertIsDisplayed()
        }
    }
}
