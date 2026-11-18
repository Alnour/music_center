package com.alnourkhalifa.scales.app.phone

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alnourkhalifa.scales.core.music.ScaleReference
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * The Transform tab's two journeys, twins of `testTransformTabExplainsALoweredSeventh` and
 * `testNamingANewScaleAddsItToMyScales` in `ScalesUITests/ScalesUITests.swift`: a change the
 * app recognises is named and kept as a lesson, and a new scale is named, saved to My Scales
 * and offered on the board.
 */
@RunWith(AndroidJUnit4::class)
class PhoneTransformTest {
    @get:Rule
    val phone = PhoneTestRule()

    private val compose
        get() = phone.composeRule

    // feature: transform-lessons
    @Test
    fun transformTabExplainsALoweredSeventh() {
        phone.openTab("transform")
        chooseTransformStart(key = "G", scale = "Major")
        compose.onNodeWithTag("editNote-7").performClick()
        compose.onNodeWithText("Lower to F (♭7)").performClick()

        compose.waitUntil(3_000) { exists(hasText("This is G Mixolydian")) }
        val dominant = hasText("V, D major, becomes v, D minor", substring = true)
        compose.onNodeWithTag("lessonSheetList").performScrollToNode(dominant)
        compose.onNodeWithTag("saveLessonButton").performClick()

        val lessonRow = hasText("G Major → G Mixolydian")
        scrollTransformListTo(lessonRow)
        compose.onNode(lessonRow).performTouchInput { swipeLeft() }
        compose.waitUntil(3_000) { !exists(lessonRow) }
    }

    // feature: transform-lessons
    @Test
    fun namingANewScaleAddsItToMyScales() {
        val name = "Test Scale ${UUID.randomUUID().toString().take(4)}"
        phone.openTab("transform")
        chooseTransformStart(key = "G", scale = "Major")
        compose.onNodeWithTag("editNote-5").performClick()
        compose.onNodeWithText("Raise to D♯ (♯5)").performClick()

        compose.waitUntil(3_000) { exists(hasTestTag("scaleNameField")) }
        compose.onNodeWithTag("saveLessonButton").assertIsNotEnabled()
        compose.onNodeWithTag("scaleNameField").performTextInput(name)
        compose.onNodeWithTag("saveLessonButton").assertIsEnabled()
        compose.onNodeWithTag("saveLessonButton").performClick()

        val scaleRow = hasTestTag("myScale-$name")
        scrollTransformListTo(scaleRow)

        // The Scales screen's own Scale menu belongs to another screen's port; until it is
        // here, the scale is put on the board the way that menu does, through the session.
        val saved =
            phone.app.library.scales
                .first { scale -> scale.definition.name == name }
        phone.onMainThread {
            phone.app.session
                .choose(ScaleReference.Custom(saved.id))
        }
        phone.openTab("scales")
        val boardTitle = phone.app.sessionState.value.scale.title
        assertTrue("the fretboard shows it: $boardTitle", boardTitle.endsWith(name))

        phone.openTab("transform")
        scrollTransformListTo(scaleRow)
        compose.onNode(scaleRow).performTouchInput { swipeLeft() }
        compose.onNodeWithText("Delete Scale").performClick()
        compose.waitUntil(3_000) { !exists(scaleRow) }
    }

    /** Swift's `chooseTransformStart`: the Key menu, then the Starting scale menu. */
    private fun chooseTransformStart(
        key: String,
        scale: String,
    ) {
        compose.onNodeWithTag("transformKey").performClick()
        compose.onNodeWithTag("keyChoice-$key").performClick()
        compose.onNodeWithTag("transformScale").performClick()
        compose.onNodeWithTag("scaleChoice-$scale").performClick()
        compose.waitForIdle()
    }

    private fun scrollTransformListTo(matcher: SemanticsMatcher) {
        compose.waitForIdle()
        compose.onNodeWithTag("formPageList").performScrollToNode(matcher)
    }

    private fun exists(matcher: SemanticsMatcher): Boolean {
        val nodes = compose.onAllNodes(matcher).fetchSemanticsNodes()
        return nodes.isNotEmpty()
    }
}
