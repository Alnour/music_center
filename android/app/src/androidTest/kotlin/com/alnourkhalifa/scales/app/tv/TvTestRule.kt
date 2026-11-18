package com.alnourkhalifa.scales.app.tv

import android.content.Context
import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.alnourkhalifa.scales.app.ScalesApplication
import com.alnourkhalifa.scales.app.SharedPreferencesKeyValueStore
import com.alnourkhalifa.scales.core.session.ScaleLibrary
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Key presses a focus search may take before a test gives up on reaching a node. */
private const val FOCUS_SEARCH_PRESSES = 24

/**
 * The shared starting point for TV UI tests: every test opens [TvActivity] on a fresh install
 * (saved settings and library wiped, session rebuilt) and drives it with remote keys only,
 * as Google's TV-DP review does.
 *
 * D-pad keys go in through Compose ([press]), which hands them to the focused node. Back goes
 * through the instrumentation instead ([pressBack]): Compose's injected keys skip the
 * activity's key dispatch, and Back is handled there, by `OnBackPressedDispatcher`.
 *
 * Use it as the only rule: `@get:Rule val tv = TvTestRule()`.
 */
class TvTestRule : TestRule {
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<TvActivity>, TvActivity> =
        createAndroidComposeRule<TvActivity>()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** The app object the activity reads its session from. */
    val app: ScalesApplication
        get() = ScalesApplication.from(context)

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement {
        val withActivity = composeRule.apply(base, description)
        return object : Statement() {
            override fun evaluate() {
                startFromFreshInstall()
                withActivity.evaluate()
            }
        }
    }

    /** Presses and releases one remote key on whatever has focus. */
    @OptIn(ExperimentalTestApi::class)
    fun press(key: Key) {
        composeRule.onRoot().performKeyInput { pressKey(key) }
        composeRule.waitForIdle()
    }

    /** The remote's OK button. */
    fun pressOk() {
        press(Key.DirectionCenter)
    }

    /** The remote's Back button, through the activity as a real remote's would go. */
    fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
    }

    /** OK on the board: opens the options panel. */
    fun openPanel() {
        composeRule.onNodeWithTag("tvBoard").assertIsFocused()
        pressOk()
        composeRule.onNodeWithTag("panel").assertExists()
    }

    /** True when the node tagged [tag] holds focus now. */
    fun isFocused(tag: String): Boolean {
        val matcher = hasTestTag(tag) and isFocused()
        val nodes = composeRule.onAllNodes(matcher).fetchSemanticsNodes()
        return nodes.isNotEmpty()
    }

    /**
     * Walks focus to the node tagged [tag] with the D-pad: down first, then up, as a player
     * scrolling a list would. Fails if the node can't be reached, which is the TV-DP dead end
     * the review looks for.
     */
    fun focusByKeys(tag: String) {
        repeat(FOCUS_SEARCH_PRESSES) {
            if (isFocused(tag)) {
                return
            }
            press(Key.DirectionDown)
        }
        repeat(FOCUS_SEARCH_PRESSES) {
            if (isFocused(tag)) {
                return
            }
            press(Key.DirectionUp)
        }
        composeRule.onNodeWithTag(tag).assertIsFocused()
    }

    /** Runs an action on the main thread, where the session's changes belong. */
    fun onMainThread(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    private fun startFromFreshInstall() {
        val preferences =
            context.getSharedPreferences(
                SharedPreferencesKeyValueStore.PREFERENCES_NAME,
                Context.MODE_PRIVATE,
            )
        preferences
            .edit()
            .clear()
            .commit()
        val libraryFile = ScaleLibrary.standardFile(context.filesDir)
        libraryFile.delete()
        ScaleLibrary.unreadableCopyFile(libraryFile).delete()
        onMainThread { app.reloadFromStorage() }
    }
}
