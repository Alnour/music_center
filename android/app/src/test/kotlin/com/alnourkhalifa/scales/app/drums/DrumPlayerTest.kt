package com.alnourkhalifa.scales.app.drums

import com.alnourkhalifa.scales.core.music.BallPosition
import com.alnourkhalifa.scales.core.music.DrumGroove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The drum player's timing rules, run against a fake sound and a fake clock. The unconfined
 * dispatcher renders and plays the bar inside the call that asks for it, so every test reads
 * the outcome straight after the call.
 */
class DrumPlayerTest {
    /** Records what the player asked of the sound. */
    private class FakeSound : DrumSound {
        override val sampleRate: Int = 8_000
        override var onFocusLost: (() -> Unit)? = null
        val playedBars = mutableListOf<FloatArray>()
        var stopCount = 0
        var isReleased = false

        override fun play(
            samples: FloatArray,
            sampleRate: Int,
        ) {
            playedBars.add(samples)
        }

        override fun stop() {
            stopCount += 1
        }

        override fun release() {
            isReleased = true
        }
    }

    /** A clock the test moves by hand, in nanoseconds. */
    private class FakeClock {
        var now = 1_000_000_000L

        fun advanceSeconds(seconds: Double) {
            val nanos = seconds * 1_000_000_000.0
            now += nanos.toLong()
        }
    }

    private val sound = FakeSound()
    private val clock = FakeClock()
    private val player =
        DrumPlayer(
            sound = sound,
            scope = CoroutineScope(Dispatchers.Unconfined),
            clock = { clock.now },
            renderContext = Dispatchers.Unconfined,
        )

    // feature: drum-trainer
    @Test
    fun startPlaysOneBarAndMarksTheStart() {
        player.start()

        assertTrue(player.isPlaying)
        assertEquals(1, sound.playedBars.size)
        assertEquals(clock.now, player.startNanos)
    }

    // feature: drum-trainer
    @Test
    fun theBarLastsOneBarAtTheTempo() {
        player.tempo = 120.0
        player.start()

        // Four beats at 120 BPM take two seconds.
        val expectedFrames = 2 * sound.sampleRate
        assertEquals(
            expectedFrames,
            sound.playedBars
                .single()
                .size,
        )
    }

    // feature: drum-trainer
    @Test
    fun tempoChangeWhileStoppedPlaysNothing() {
        player.tempo = 120.0

        assertFalse(player.isPlaying)
        assertTrue(sound.playedBars.isEmpty())
    }

    // feature: drum-trainer
    @Test
    fun tempoChangeWhilePlayingRestartsFromBeatOne() {
        player.start()
        clock.advanceSeconds(0.7)

        player.tempo = 100.0

        assertEquals(2, sound.playedBars.size)
        assertEquals(clock.now, player.startNanos)
        assertEquals(BallPosition(0, 0.0, 0.0), player.ballPosition(clock.now))
    }

    // feature: drum-trainer
    @Test
    fun grooveChangeRestartsOnlyWhilePlaying() {
        val halfTime = DrumGroove.builtIn[3]
        player.groove = halfTime
        assertTrue(sound.playedBars.isEmpty())

        player.start()
        player.groove = DrumGroove.builtIn[4]

        assertEquals(2, sound.playedBars.size)
    }

    // feature: drum-trainer
    @Test
    fun settingTheSameValueDoesNotRestart() {
        player.start()

        player.tempo = player.tempo
        player.groove = player.groove
        player.isSoundOn = true

        assertEquals(1, sound.playedBars.size)
    }

    // feature: drum-trainer
    @Test
    fun soundOffPlaysNothingWhileTheBallMoves() {
        player.isSoundOn = false
        player.start()
        clock.advanceSeconds(0.5)

        assertTrue(player.isPlaying)
        assertTrue(sound.playedBars.isEmpty())
        val ball = player.ballPosition(clock.now)
        assertNotEquals(BallPosition(0, 0.0, 0.0), ball)
    }

    // feature: drum-trainer
    @Test
    fun turningSoundOffWhilePlayingRestartsSilently() {
        player.start()

        player.isSoundOn = false

        assertTrue(player.isPlaying)
        assertEquals(1, sound.playedBars.size)
        assertTrue(sound.stopCount >= 2)
    }

    // feature: drum-trainer
    @Test
    fun stopSilencesTheSoundAndRestsTheBall() {
        player.start()
        clock.advanceSeconds(1.3)

        player.stop()

        assertFalse(player.isPlaying)
        assertNull(player.startNanos)
        assertTrue(sound.stopCount >= 1)
        assertEquals(BallPosition(0, 0.0, 0.0), player.ballPosition(clock.now))
    }

    // feature: drum-trainer
    @Test
    fun releaseStopsAndFreesTheSound() {
        player.start()

        player.release()

        assertFalse(player.isPlaying)
        assertTrue(sound.isReleased)
    }

    // feature: drum-trainer
    @Test
    fun losingAudioFocusStops() {
        player.start()

        sound.onFocusLost?.invoke()

        assertFalse(player.isPlaying)
    }

    // feature: drum-trainer
    @Test
    fun ballRestsOnBeatOneWhileStopped() {
        clock.advanceSeconds(3.0)

        assertEquals(BallPosition(0, 0.0, 0.0), player.ballPosition(clock.now))
    }

    // feature: drum-trainer
    @Test
    fun ballFollowsTheClockWhilePlaying() {
        player.tempo = 60.0
        player.start()
        // Basic Rock counts eighths: at 60 BPM each count lasts half a second, so 1.25 s is
        // halfway from the third count to the fourth, at the top of the bounce.
        clock.advanceSeconds(1.25)

        val ball = player.ballPosition(clock.now)

        assertEquals(2, ball.step)
        assertEquals(0.5, ball.travel, 1e-9)
        assertEquals(1.0, ball.bounce, 1e-9)
    }

    // feature: drum-trainer
    @Test
    fun tempoIsKeptInsideItsRange() {
        player.tempo = 10.0
        assertEquals(DrumPlayer.tempoRange.start, player.tempo, 0.0)

        player.tempo = 500.0
        assertEquals(DrumPlayer.tempoRange.endInclusive, player.tempo, 0.0)
    }
}
