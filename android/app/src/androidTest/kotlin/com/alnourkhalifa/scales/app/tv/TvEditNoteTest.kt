package com.alnourkhalifa.scales.app.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.session.ScaleLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * "Change a note" driven by the remote alone: the panel row opens the scale's notes, a note
 * opens its changes, and OK on a change puts the new scale on the board. Every test starts
 * from a fresh install, so the board shows G major (G A B C D E F♯).
 */
@RunWith(AndroidJUnit4::class)
class TvEditNoteTest {
    @get:Rule
    val tv = TvTestRule()

    // feature: note-edit-revert
    @Test
    fun changeANoteFromThePanel() {
        openNotesList()
        tv.composeRule
            .onNodeWithTag("choice-7")
            .assertTextContains("F♯ (7)")
        tv.focusByKeys("choice-7")
        tv.pressOk()

        tv.composeRule
            .onNodeWithText("F♯ (7)")
            .assertIsDisplayed()
        tv.composeRule
            .onNodeWithTag("choice-lower:7")
            .assertTextContains("Lower to F (♭7)")
            .assertIsFocused()
        tv.pressOk()

        tv.composeRule
            .onNodeWithTag("panel")
            .assertDoesNotExist()
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()
        tv.composeRule
            .onNodeWithText("G Mixolydian")
            .assertIsDisplayed()
        assertEquals(ScaleType.MIXOLYDIAN, tv.app.sessionState.value.settings.scaleType)

        tv.onMainThread {
            tv.app.session
                .revertBoardChange()
        }
        tv.composeRule.waitForIdle()
        tv.composeRule
            .onNodeWithText("G Major")
            .assertIsDisplayed()
    }

    // feature: note-edit-revert
    @Test
    fun addANoteListsEveryAddition() {
        openNotesList()
        tv.focusByKeys("choice-add")
        tv.pressOk()

        tv.composeRule
            .onNodeWithText("Add a note")
            .assertIsDisplayed()
        tv.composeRule
            .onNodeWithTag("choice-add:♭2")
            .assertTextContains("Add A♭ (♭2)")
            .assertIsFocused()
        tv.composeRule
            .onNodeWithTag("choice-add:♭3")
            .assertTextContains("Add B♭ (♭3)")
        tv.composeRule
            .onNodeWithTag("choice-add:♭7")
            .assertTextContains("Add F (♭7)")
    }

    // feature: tv-board
    @Test
    fun backClimbsEditListsOneLevel() {
        openNotesList()
        tv.pressOk()
        tv.composeRule
            .onNodeWithText("A (2)")
            .assertIsDisplayed()

        tv.pressBack()
        tv.composeRule
            .onNodeWithText("Change a note")
            .assertIsDisplayed()
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

        tv.pressBack()
        tv.composeRule
            .onNodeWithTag("panel")
            .assertDoesNotExist()
        tv.composeRule
            .onNodeWithTag("tvBoard")
            .assertIsFocused()
    }

    // feature: note-edit-revert
    @Test
    fun aFailedSaveIsToldAndLeavesTheBoardAsItWas() {
        // A folder with something in it where the library file belongs: the save's final
        // move can't replace it, which is the same failure a full disk gives.
        val libraryFile = ScaleLibrary.standardFile(tv.app.filesDir)
        libraryFile.mkdirs()
        File(libraryFile, "blocker").writeText("in the way")
        try {
            openNotesList()
            tv.focusByKeys("choice-7")
            tv.pressOk()
            tv.pressOk()

            tv.composeRule
                .onNodeWithTag("libraryError")
                .assertIsDisplayed()
            tv.composeRule
                .onNodeWithText("Couldn't Update Your Scales")
                .assertIsDisplayed()
            tv.composeRule
                .onNodeWithTag("libraryErrorOk")
                .assertIsFocused()
            tv.pressOk()

            tv.composeRule
                .onNodeWithTag("panel")
                .assertDoesNotExist()
            tv.composeRule
                .onNodeWithText("G Major")
                .assertIsDisplayed()
            assertNull(tv.app.sessionState.value.boardChange)
        } finally {
            libraryFile.deleteRecursively()
        }
    }

    /** Opens the panel and its "Change a note" row, landing on the first note's row. */
    private fun openNotesList() {
        tv.openPanel()
        tv.focusByKeys("row-change-a-note")
        tv.pressOk()
        tv.composeRule
            .onNodeWithText("Change a note")
            .assertIsDisplayed()
        tv.composeRule
            .onNodeWithTag("choice-2")
            .assertIsFocused()
    }
}
