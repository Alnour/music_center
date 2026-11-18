package com.alnourkhalifa.scales.app.tv

import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/**
 * "Keep board on": holds the TV screen awake for a practice session the player chose.
 *
 * Without it the Android TV screensaver covers a board that never changes after ten minutes,
 * and half an hour later the TV drops to its home screen. Keeping the screen on forever would
 * burn a static picture into some panels and run the TV all night if the player walks away,
 * so the player picks a length, a coroutine lets the screen sleep again when it ends, and the
 * choice is never saved: each session is one the player started on purpose.
 */
@Stable
class TvPracticeTimer(
    private val window: Window?,
    private val scope: CoroutineScope,
) {
    /** The chosen length in minutes, or null for Off. */
    var minutes: Int? by mutableStateOf(null)
        private set

    private var countdown: Job? = null

    /** Starts a new session of [chosen] minutes, replacing any running one; null is Off. */
    fun choose(chosen: Int?) {
        countdown?.cancel()
        minutes = chosen
        if (chosen == null) {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            return
        }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        countdown =
            scope.launch {
                delay(chosen.minutes)
                choose(null)
            }
    }

    /** The lengths the panel offers, after Off. */
    companion object {
        val choices: List<Int> = listOf(30, 60, 90)
    }
}

/** The activity's timer; its countdown ends with the TV screen's composition. */
@Composable
fun rememberTvPracticeTimer(): TvPracticeTimer {
    val window = LocalActivity.current?.window
    val scope = rememberCoroutineScope()
    return remember(window, scope) { TvPracticeTimer(window, scope) }
}
