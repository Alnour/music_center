package com.alnourkhalifa.scales.app.drums

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.alnourkhalifa.scales.core.drums.DrumBarRenderer
import com.alnourkhalifa.scales.core.music.BallPosition
import com.alnourkhalifa.scales.core.music.DrumClock
import com.alnourkhalifa.scales.core.music.DrumGroove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Runs the drum trainer: the tempo, the groove, and the clock the ball follows. Twin of
 * `DrumPlayer` in `Scales/Drums/DrumPlayer.swift`.
 *
 * Sound and picture start together. The bar is rendered once at the chosen tempo and looped
 * by [sound], while the staff asks [ballPosition] where the ball is from the clock, so both
 * agree on where beat one falls. Any change while playing (tempo, groove, sound on or off)
 * restarts both from beat one, which keeps them in step without cross-clock bookkeeping.
 *
 * What differs from Swift:
 * - The bar is rendered off the main thread on [renderContext] (a slow bar at 40 BPM is a
 *   quarter of a million samples), then handed to [sound] on [scope]. The run's start time is
 *   set again once the sound is playing, so the ball restarts from beat one with the sound,
 *   as on iOS where both begin in the same call. A render still running when the player
 *   stops or restarts is cancelled, so a stale bar never plays.
 * - Times are [clock] nanoseconds rather than `Date`s, so tests can drive a fake clock.
 * - Setting a value equal to the current one changes nothing; Swift's `didSet` would restart.
 * - A tempo outside [tempoRange] is pulled into it, so a stray value can never divide by zero
 *   in the clock.
 * - Losing the audio output to another app ([DrumSound.onFocusLost]) stops the player.
 *
 * Observable state lives in Compose `mutableStateOf`, so the drums screens recompose when
 * the groove, tempo, sound switch or playing state changes.
 */
class DrumPlayer(
    private val sound: DrumSound,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::nanoTime,
    private val renderContext: CoroutineContext = Dispatchers.Default,
) {
    private var grooveState by mutableStateOf(DrumGroove.builtIn[0])
    private var tempoState by mutableDoubleStateOf(DEFAULT_TEMPO)
    private var soundOnState by mutableStateOf(true)

    /** When the current run began, in [clock] nanoseconds; null while stopped. */
    var startNanos: Long? by mutableStateOf(null)
        private set

    /** The bar being rendered for the current run, cancelled when the run ends. */
    private var renderJob: Job? = null

    init {
        sound.onFocusLost = { stop() }
    }

    var groove: DrumGroove
        get() = grooveState
        set(value) {
            if (value == grooveState) {
                return
            }
            grooveState = value
            restartIfPlaying()
        }

    var tempo: Double
        get() = tempoState
        set(value) {
            val clamped = value.coerceIn(tempoRange)
            if (clamped == tempoState) {
                return
            }
            tempoState = clamped
            restartIfPlaying()
        }

    var isSoundOn: Boolean
        get() = soundOnState
        set(value) {
            if (value == soundOnState) {
                return
            }
            soundOnState = value
            restartIfPlaying()
        }

    val isPlaying: Boolean
        get() = startNanos != null

    fun togglePlayback() {
        if (isPlaying) {
            stop()
        } else {
            start()
        }
    }

    /** The ball at this instant, or resting on beat one while stopped. */
    fun ballPosition(nowNanos: Long): BallPosition {
        val start = startNanos ?: return BallPosition(step = 0, travel = 0.0, bounce = 0.0)
        val elapsedSeconds = (nowNanos - start).toDouble() / NANOS_PER_SECOND
        val progress =
            DrumClock.barProgress(
                elapsed = elapsedSeconds,
                tempo = tempo,
                beatsPerBar = groove.beatsPerBar,
            )
        return DrumClock.ballPosition(progress = progress, stepsPerBar = groove.stepsPerBar)
    }

    /** Starts from beat one, with sound when it is on. */
    fun start() {
        stop()
        startNanos = clock()
        if (isSoundOn) {
            startSound()
        }
    }

    fun stop() {
        startNanos = null
        renderJob?.cancel()
        renderJob = null
        sound.stop()
    }

    /** Stops and frees the audio output; the drums screen calls this when it goes away. */
    fun release() {
        stop()
        sound.release()
    }

    private fun restartIfPlaying() {
        if (isPlaying) {
            start()
        }
    }

    /**
     * Renders the bar for this groove and tempo off the main thread, then loops it. The
     * groove and tempo are read now, so a change during the render starts a fresh one instead.
     */
    private fun startSound() {
        val barGroove = groove
        val barTempo = tempo
        val sampleRate = sound.sampleRate
        renderJob =
            scope.launch {
                val samples =
                    withContext(renderContext) {
                        val renderer = DrumBarRenderer(sampleRate.toDouble())
                        renderer.render(barGroove, barTempo)
                    }
                sound.play(samples, sampleRate)
                startNanos = clock()
            }
    }

    /** The tempo bounds and the starting tempo, shared with the drums screens. */
    companion object {
        val tempoRange: ClosedFloatingPointRange<Double> = 40.0..220.0
        const val DEFAULT_TEMPO = 90.0
        private const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
