package com.alnourkhalifa.scales.app.drums

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * The drum player for one drums screen, phone or TV, with its audio output tied to the
 * screen's life. Swift keeps the player in `@State` and calls `player.stop` from
 * `onDisappear` (`Scales/Views/DrumTrainerView.swift`); Android needs three more rules:
 *
 * - Leaving the screen stops the drums and releases the `AudioTrack`, so a hidden screen never
 *   holds the audio output.
 * - The app going to the background (`ON_STOP`) stops the drums, as iOS does when the app
 *   leaves the screen. The player is not released then, so coming back needs only Play.
 * - The screen stays awake while the drums play: a player follows the ball from a music
 *   stand without touching the device, and a TV or phone dimming mid-groove would lose them.
 *
 * The bar is rendered with `DrumBarRenderer` on `Dispatchers.Default` by [DrumPlayer] before
 * it plays, so the main thread never waits for the synthesis.
 */
@Composable
fun rememberDrumPlayer(): DrumPlayer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player =
        remember {
            val sound = StreamingDrumSound(context)
            DrumPlayer(sound = sound, scope = scope)
        }
    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    player.stop()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val view = LocalView.current
    val isPlaying = player.isPlaying
    DisposableEffect(view, isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose {
            view.keepScreenOn = false
        }
    }
    return player
}
