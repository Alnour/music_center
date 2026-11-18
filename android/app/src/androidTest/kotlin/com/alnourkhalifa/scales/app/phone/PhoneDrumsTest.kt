package com.alnourkhalifa.scales.app.phone

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test

/**
 * The phone's Drums tab: play and pause, and the tempo slider. Twin of
 * `testDrumsTabPlaysAndPausesTheGroove` in `ScalesUITests/ScalesUITests.swift`, plus a tempo
 * check iOS leaves to `DrumTrainerTests`, because on Android the slider hands its tempo to the
 * player only when the drag ends and that hand-off is the screen's own logic.
 */
class PhoneDrumsTest {
    @get:Rule
    val phone = PhoneTestRule()

    // feature: drum-trainer
    @Test
    fun drumsTabPlaysAndPausesTheGroove() {
        phone.openTab("drums")
        val compose = phone.composeRule
        compose.onNodeWithText("Play").assertIsDisplayed()
        compose.onNodeWithTag("tempoLabel").assertTextEquals("Tempo: 90 BPM")

        compose.onNodeWithText("Play").performClick()
        compose.onNodeWithText("Pause").assertIsDisplayed()

        compose.onNodeWithText("Pause").performClick()
        compose.onNodeWithText("Play").assertIsDisplayed()
    }

    // feature: drum-trainer
    @Test
    fun tempoSliderSetsTheTempo() {
        phone.openTab("drums")
        val compose = phone.composeRule
        val tempoSlider =
            compose.onNode(
                SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress),
            )
        tempoSlider.performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
            setProgress(120f)
        }
        compose.onNodeWithTag("tempoLabel").assertTextEquals("Tempo: 120 BPM")
    }
}
