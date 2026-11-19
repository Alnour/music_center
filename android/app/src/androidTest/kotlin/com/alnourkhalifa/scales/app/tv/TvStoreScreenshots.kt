package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alnourkhalifa.scales.app.phone.saveStoreScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** How long a screen may take to appear after OK before the test gives up. */
private const val WAIT_MILLIS = 5_000L

/**
 * Walks the TV screen through the states shown on the Play TV listing and the Amazon Fire TV
 * page, with remote keys only, and keeps a 1920×1080 picture of each (Play asks for at least
 * one TV screenshot, Amazon for three to ten): the board, the options panel over it, the chord
 * tables and the drum trainer.
 *
 * Run alone on the TV emulator through `make screenshots-android`; it also runs with the other
 * TV interface tests, where it proves each state still opens by key presses. The pictures go
 * through the phone twin's [saveStoreScreenshot], so both land in the same folder.
 */
@RunWith(AndroidJUnit4::class)
class TvStoreScreenshots {
    @get:Rule
    val tv = TvTestRule()

    private val compose
        get() = tv.composeRule

    // feature: board, tv-board, chord-tables, drum-trainer
    @Test
    fun captureTvStoreScreenshots() {
        // 1. The board as it opens, G major across the neck, read from the sofa.
        compose.onNodeWithTag("tvBoard").assertIsFocused()
        saveStoreScreenshot("01-board")

        // 2. OK opens the options panel over the board.
        tv.openPanel()
        saveStoreScreenshot("02-options-panel")

        // 3. The chord tables, opened from their panel row.
        tv.focusByKeys(TvPanelRow.CHORD_TABLES.testTag)
        tv.pressOk()
        waitFor(hasContentDescription("C major"))
        saveStoreScreenshot("03-chord-tables")

        // 4. Back returns to the panel on the same row; the drum trainer sits further down.
        tv.pressBack()
        tv.focusByKeys(TvPanelRow.DRUMS.testTag)
        tv.pressOk()
        waitFor(hasTestTag("playPause"))
        saveStoreScreenshot("04-drums")
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(WAIT_MILLIS) {
            val nodes = compose.onAllNodes(matcher).fetchSemanticsNodes()
            nodes.isNotEmpty()
        }
    }
}
