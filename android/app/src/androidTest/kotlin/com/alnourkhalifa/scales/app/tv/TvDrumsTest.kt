package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The TV Drums screen, worked with the remote alone. The control row reads, left to right:
 * Play, Groove, "−", the tempo (not focusable), "+", Drum sounds. So from Play, two Rights
 * reach "−" and four reach the Drum sounds switch.
 */
@RunWith(AndroidJUnit4::class)
class TvDrumsTest {
    @get:Rule
    val compose = createComposeRule()

    // feature: drum-trainer
    @Test
    fun okAndThePlayPauseKeyStartAndStopTheDrums() {
        compose.setContent { TvDrumsScreen() }

        compose.onNodeWithTag("playPause").assertIsFocused()
        compose.onNodeWithText("Play").assertIsDisplayed()

        press(Key.DirectionCenter)
        compose.onNodeWithText("Pause").assertIsDisplayed()

        press(Key.MediaPlayPause)
        compose.onNodeWithText("Play").assertIsDisplayed()
    }

    // feature: drum-trainer
    @Test
    fun theMinusButtonLowersTheTempoByOne() {
        compose.setContent { TvDrumsScreen() }
        compose.onNodeWithTag("tempoLabel").assertTextEquals("Tempo: 90 BPM")

        // Right twice from Play passes Groove and lands on "−", so OK lowers 90 to 89.
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        compose.onNodeWithTag("tempoDown").assertIsFocused()
        press(Key.DirectionCenter)

        compose.onNodeWithTag("tempoLabel").assertTextEquals("Tempo: 89 BPM")
    }

    // feature: drum-trainer
    @Test
    fun okOnTheSwitchTurnsTheDrumSoundsOff() {
        compose.setContent { TvDrumsScreen() }
        compose.onNodeWithTag("drumSounds").assertIsOn()

        repeat(times = 4) {
            press(Key.DirectionRight)
        }
        compose.onNodeWithTag("drumSounds").assertIsFocused()
        press(Key.DirectionCenter)

        compose.onNodeWithTag("drumSounds").assertIsOff()
    }

    // feature: drum-trainer
    @Test
    fun theGrooveListPicksWithOkAndClosesWithBack() {
        compose.setContent { TvDrumsScreen() }

        press(Key.DirectionRight)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("grooveList").assertIsDisplayed()

        // Back closes the list and stays on the screen.
        press(Key.Back)
        compose.onNodeWithTag("grooveList").assertDoesNotExist()

        // Focus is back on the groove button; the list opens on Basic Rock, Down picks the next.
        press(Key.DirectionCenter)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("grooveList").assertDoesNotExist()
        compose.onNodeWithText("Groove: Four on the Floor").assertIsFocused()
    }

    /** One press and release of [key] on the remote, sent to whatever has focus. */
    private fun press(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
    }
}
