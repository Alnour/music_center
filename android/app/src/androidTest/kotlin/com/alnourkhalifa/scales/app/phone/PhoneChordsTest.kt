package com.alnourkhalifa.scales.app.phone

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** How long a table may take to appear after a tab or switch tap, as the iOS test waits. */
private const val APPEAR_TIMEOUT_MILLIS = 3_000L

/**
 * The Chords tab lists every major and minor key with its chords. Twin of
 * `testChordTablesShowMajorAndMinorKeys` in `ScalesUITests/ScalesUITests.swift`.
 *
 * The table reads keys and chords to TalkBack by their spoken names ("C major", "B
 * diminished"), so the finders match content descriptions, the Compose counterpart of the
 * accessibility labels the iOS test finds as static texts. A name such as "C major" names
 * both a key and a chord in several other keys, so each check asks for at least one match.
 */
@RunWith(AndroidJUnit4::class)
class PhoneChordsTest {
    @get:Rule
    val phone = PhoneTestRule()

    // feature: chord-tables
    @Test
    fun chordTablesShowMajorAndMinorKeys() {
        phone.openTab("chords")
        waitForSpokenName("C major")
        assertShows("B diminished")

        phone.composeRule
            .onNodeWithTag("minorKeys")
            .performClick()
        waitForSpokenName("A minor")
        assertShows("E minor")
        assertHidden("or G♯ diminished", "no harmonic-minor stand-ins")
        phone.composeRule
            .onNodeWithTag("minorKeys")
            .assertIsSelected()
        phone.composeRule
            .onNodeWithTag("majorKeys")
            .assertIsNotSelected()

        phone.composeRule
            .onNodeWithTag("majorKeys")
            .performClick()
        waitForSpokenName("C major")
        phone.composeRule
            .onNodeWithTag("majorKeys")
            .assertIsSelected()
    }

    /** Waits until some key or chord is read aloud as [name], failing after the timeout. */
    private fun waitForSpokenName(name: String) {
        val matcher = hasContentDescription(name)
        phone.composeRule.waitUntil(APPEAR_TIMEOUT_MILLIS) {
            val nodes =
                phone.composeRule
                    .onAllNodes(matcher)
                    .fetchSemanticsNodes()
            nodes.isNotEmpty()
        }
    }

    private fun assertShows(name: String) {
        val matcher = hasContentDescription(name)
        val nodes =
            phone.composeRule
                .onAllNodes(matcher)
                .fetchSemanticsNodes()
        assertTrue("nothing is read as $name", nodes.isNotEmpty())
    }

    /** Neither a label nor a spoken name may carry [text]. */
    private fun assertHidden(
        text: String,
        reason: String,
    ) {
        val asDescription = hasContentDescription(text, substring = true)
        val asText = hasText(text, substring = true)
        val matcher = asDescription.or(asText)
        val nodes =
            phone.composeRule
                .onAllNodes(matcher)
                .fetchSemanticsNodes()
        assertTrue(reason, nodes.isEmpty())
    }
}
