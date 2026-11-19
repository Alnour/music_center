package com.alnourkhalifa.scales.app.phone

import android.app.UiAutomation
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** How long a step may take to show its result before the test gives up. */
private const val WAIT_MILLIS = 5_000L

/**
 * Time for animations (a sheet sliding in, a rotation, a ripple) to finish before a picture
 * is taken; the iOS twin waits one second for the same reason.
 */
private const val SETTLE_MILLIS = 1_000L

/** The folder under the app's external files where the pictures land. */
const val STORE_SCREENSHOTS_FOLDER = "store-screenshots"

/**
 * Takes a picture of the whole screen, status bar included, and keeps it as [name].png in
 * the app's external files folder, where `make screenshots-android` pulls it from.
 *
 * Why the whole screen through [UiAutomation] and not Compose's `captureToImage`: the store
 * pictures should look like the phone does, with the (demo) status bar, and `captureToImage`
 * sees only the activity's own window. The app's own external folder needs no storage
 * permission, so the app still requests none. The TV store pictures use this function too.
 */
fun saveStoreScreenshot(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    SystemClock.sleep(SETTLE_MILLIS)
    val picture = instrumentation.uiAutomation.takeScreenshot()
    assertNotNull("the screen could not be captured for $name", picture)

    val folder = instrumentation.targetContext.getExternalFilesDir(STORE_SCREENSHOTS_FOLDER)
    assertNotNull("no external files folder for $name", folder)
    val file = File(folder, "$name.png")
    file.outputStream().use { stream ->
        picture.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    picture.recycle()
}

/**
 * Walks the app through the states shown on the Play and Amazon pages and keeps a picture of
 * each. Twin of `ScalesUITests/StoreScreenshots.swift`: the same five states (01 to 05), plus
 * the Transform lesson (06) and the drum trainer (07), which the Android listing also shows.
 *
 * Run alone, on the phone emulator for the phone pictures and on the tablet emulator for the
 * tablet ones, with the demo status bar set first: `make screenshots-android`. It also runs
 * with the other phone interface tests, where it proves the five states still open.
 *
 * Every run starts from a fresh install ([PhoneTestRule]), so the app opens on G major; the
 * choices are still made explicitly, as the iOS twin does, so the pictures never depend on
 * the default.
 */
@RunWith(AndroidJUnit4::class)
class StoreScreenshots {
    @get:Rule
    val phone = PhoneTestRule()

    private val compose
        get() = phone.composeRule

    private val uiAutomation: UiAutomation
        get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    // feature: board, tv-board
    @Test
    fun captureStoreScreenshots() {
        // The device's own upright position: portrait on a phone, landscape on a tablet,
        // which is how each is usually held and how each store shows its pictures.
        uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_0)
        try {
            walkThroughTheStoreStates()
        } finally {
            // Hand rotation back to the system so later tests and the player are unaffected.
            uiAutomation.setRotation(UiAutomation.ROTATION_UNFREEZE)
        }
    }

    private fun walkThroughTheStoreStates() {
        // 1. G major across the neck, the state the app ships in.
        chooseRoot("G")
        chooseScale("Major")
        compose.onNodeWithText("Notes").performClick()
        waitForTitle("G Major")
        keep("01-g-major")

        // 2. A different key and scale, picked the way a player would.
        chooseRoot("A")
        chooseScale("Minor Pentatonic")
        waitForTitle("A Minor Pentatonic")
        keep("02-a-minor-pentatonic")

        // 3. Scale degrees instead of note names, on a blues scale.
        compose.onNodeWithText("Degrees").performClick()
        chooseRoot("E")
        chooseScale("Blues")
        waitForTitle("E Blues")
        keep("03-e-blues-degrees")

        // 4. The TV layout filling the screen, the picture the TV shows. The phone turns
        //    itself to landscape for it; a tablet already stands in landscape.
        compose.onNodeWithTag("tvMenu").performClick()
        compose.onNodeWithText("Full-screen board").performClick()
        waitFor(hasContentDescription("Close"))
        compose.waitUntil(WAIT_MILLIS) { isLandscape() }
        keep("04-tv-board-landscape")
        compose.onNode(hasContentDescription("Close")).performClick()
        compose.waitUntil(WAIT_MILLIS) { !exists(hasContentDescription("Close")) }
        compose.waitUntil(WAIT_MILLIS) { exists(hasTestTag("tvMenu")) }

        // 5. The chord tables for every key.
        phone.openTab("chords")
        waitFor(hasContentDescription("C major"))
        keep("05-chord-tables")

        // 6. A lesson: G major with its seventh lowered becomes G Mixolydian, and each chord
        //    says what it became.
        phone.openTab("transform")
        chooseTransformStart(key = "G", scale = "Major")
        // On a tablet held sideways the seventh's row starts below the fold, and a tap there
        // would land on the tab bar; scroll it into view first.
        val seventh = compose.onNodeWithTag("editNote-7")
        seventh.performScrollTo()
        seventh.performClick()
        waitFor(hasText("Lower to F (♭7)"))
        compose.onNodeWithText("Lower to F (♭7)").performClick()
        waitFor(hasText("This is G Mixolydian"))
        keep("06-transform-lesson")
        closeSheet()

        // 7. The drum trainer, ready to play.
        phone.openTab("drums")
        waitFor(hasText("Play"))
        keep("07-drums")
    }

    /**
     * Saves the picture as a player would see it. An instrumented test runs in keyboard input
     * mode, where a button keeps a focus highlight after its menu closes (the Root pill) and a
     * sheet highlights its first button (Cancel); a player touching the screen never sees
     * either, so the test switches to touch mode first, which drops that focus.
     */
    private fun keep(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.setInTouchMode(true)
        saveStoreScreenshot(name)
    }

    private fun chooseRoot(name: String) {
        compose.onNodeWithTag("rootMenu").performClick()
        compose.onNodeWithTag("root-$name").performClick()
        compose.waitForIdle()
    }

    private fun chooseScale(name: String) {
        compose.onNodeWithTag("scaleMenu").performClick()
        val choice = compose.onNodeWithTag("scale-$name")
        choice.performScrollTo()
        choice.performClick()
        compose.waitForIdle()
    }

    /** The Transform tab's Key menu, then its Starting scale menu. */
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

    private fun waitForTitle(title: String) {
        waitFor(hasTestTag("scaleTitle") and hasText(title))
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(WAIT_MILLIS) { exists(matcher) }
    }

    /** Back closes a bottom sheet, as the system back gesture does. */
    private fun closeSheet() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun exists(matcher: SemanticsMatcher): Boolean {
        val nodes = compose.onAllNodes(matcher).fetchSemanticsNodes()
        return nodes.isNotEmpty()
    }

    private fun isLandscape(): Boolean {
        val configuration = compose.activity.resources.configuration
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}
