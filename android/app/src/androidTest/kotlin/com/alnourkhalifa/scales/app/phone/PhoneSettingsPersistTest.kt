package com.alnourkhalifa.scales.app.phone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alnourkhalifa.scales.app.SharedPreferencesKeyValueStore
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.session.PracticeSettings
import com.alnourkhalifa.scales.core.session.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one instrumented round trip of the saved settings through `SharedPreferences`: what the
 * player picks is still there after a relaunch, and a fresh install opens on G Major. A
 * relaunch here rebuilds the session from disk before recreating the activity, as a new
 * process would; recreating the activity alone would only reread the session in memory.
 */
@RunWith(AndroidJUnit4::class)
class PhoneSettingsPersistTest {
    @get:Rule
    val phone = PhoneTestRule()

    // feature: settings-persist
    @Test
    fun freshInstallOpensOnGMajor() {
        val settings = phone.app.sessionState.value.settings
        assertEquals(PracticeSettings(), settings)
        phone.composeRule
            .onNodeWithText("G Major")
            .assertIsDisplayed()
    }

    // feature: settings-persist
    @Test
    fun fretCountSurvivesARelaunch() {
        phone.onMainThread {
            phone.app.session
                .updateSettings { it.copy(fretCount = 12) }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val onDisk = SettingsStore(SharedPreferencesKeyValueStore(context)).load()
        assertEquals(12, onDisk.fretCount)

        phone.relaunch()

        val loaded = phone.app.sessionState.value.settings
        assertEquals(12, loaded.fretCount)
    }

    // feature: settings-persist
    @Test
    fun chosenScaleRedrawsTheBoardAndSurvivesARelaunch() {
        val aMinor = ScaleReference.BuiltIn(ScaleType.NATURAL_MINOR)
        phone.onMainThread {
            phone.app.session
                .choose(aMinor, NoteName(Letter.A))
        }
        phone.composeRule
            .onNodeWithText("A Natural Minor")
            .assertIsDisplayed()

        phone.relaunch()

        phone.composeRule
            .onNodeWithText("A Natural Minor")
            .assertIsDisplayed()
        val loaded = phone.app.sessionState.value.settings
        assertEquals(ScaleType.NATURAL_MINOR, loaded.scaleType)
    }
}
