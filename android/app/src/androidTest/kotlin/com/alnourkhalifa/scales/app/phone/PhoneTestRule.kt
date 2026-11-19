package com.alnourkhalifa.scales.app.phone

import android.content.Context
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.alnourkhalifa.scales.app.ScalesApplication
import com.alnourkhalifa.scales.app.SharedPreferencesKeyValueStore
import com.alnourkhalifa.scales.core.session.ScaleLibrary
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * The shared starting point for phone UI tests: every test opens [PhoneActivity] on a fresh
 * install. The saved settings and the library file are wiped and the app's session rebuilt
 * from the empty storage before the activity launches, because the application object (and
 * its session) outlives each test in the instrumentation process; without the wipe one
 * test's chosen scale would leak into the next.
 *
 * Use it as the only rule: `@get:Rule val phone = PhoneTestRule()`, then `phone.composeRule`
 * for finders and `phone.openTab("chords")` to switch tabs.
 */
class PhoneTestRule : TestRule {
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<PhoneActivity>, PhoneActivity> =
        createAndroidComposeRule<PhoneActivity>()

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

    /** Taps a tab by the name in its RootView tag: "scales", "chords", "transform", "drums". */
    fun openTab(name: String) {
        composeRule.onNodeWithTag("tab-$name").performClick()
        composeRule.waitForIdle()
    }

    /** Runs an action on the main thread, where the session's changes belong. */
    fun onMainThread(action: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    }

    /**
     * What a relaunch does: the session is rebuilt from what is on disk (a new process) and
     * the activity is destroyed and created again.
     */
    fun relaunch() {
        onMainThread { app.reloadFromStorage() }
        composeRule.activityRule.scenario
            .recreate()
        composeRule.waitForIdle()
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
