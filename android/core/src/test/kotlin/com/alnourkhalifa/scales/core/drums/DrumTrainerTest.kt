package com.alnourkhalifa.scales.core.drums

import com.alnourkhalifa.scales.core.music.DrumClock
import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.core.music.DrumVoice
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * Twin of `ScalesTests/DrumTrainerTests.swift`, test for test: the grooves and how they are
 * counted, the clock that turns time into a ball position, and the synthesized bar. Swift
 * groups them in three suites; here they share one class, in the same order.
 */
class DrumTrainerTest {
    // Grooves: the patterns and how they are counted.

    // feature: drum-trainer
    @Test
    fun everyBuiltInGrooveFitsItsBar() {
        for (groove in DrumGroove.builtIn) {
            for (hit in groove.hits) {
                val inBar = hit.step >= 0 && hit.step < groove.stepsPerBar
                assertTrue("${groove.name} overflows", inBar)
            }
            val distinct = groove.hits.toSet()
            assertEquals("${groove.name} repeats a hit", groove.hits.size, distinct.size)
        }
    }

    // feature: drum-trainer
    @Test
    fun builtInNamesAreUnique() {
        val names = mutableSetOf<String>()
        for (groove in DrumGroove.builtIn) {
            names.add(groove.name)
        }
        assertEquals(DrumGroove.builtIn.size, names.size)
    }

    // feature: drum-trainer
    @Test
    fun basicRockPutsTheBackbeatOnTwoAndFour() {
        val rock = DrumGroove.builtIn[0]
        assertEquals(8, rock.stepsPerBar)
        assertEquals(setOf(DrumVoice.HI_HAT, DrumVoice.KICK), rock.voices(atStep = 0))
        assertEquals(setOf(DrumVoice.HI_HAT, DrumVoice.SNARE), rock.voices(atStep = 2))
        assertEquals(setOf(DrumVoice.HI_HAT), rock.voices(atStep = 1))
        assertEquals(setOf(DrumVoice.HI_HAT, DrumVoice.SNARE), rock.voices(atStep = 6))
    }

    // feature: drum-trainer
    @Test
    fun eighthsAreCountedWithAnds() {
        val groove = DrumGroove(name = "Eighths", beatsPerBar = 4, subdivision = 2, hits = listOf())
        val labels = mutableListOf<String>()
        for (step in 0 until groove.stepsPerBar) {
            labels.add(groove.countLabel(forStep = step))
        }
        assertEquals(listOf("1", "+", "2", "+", "3", "+", "4", "+"), labels)
    }

    // feature: drum-trainer
    @Test
    fun sixteenthsAreCountedOneEAndA() {
        val groove =
            DrumGroove(name = "Sixteenths", beatsPerBar = 2, subdivision = 4, hits = listOf())
        val labels = mutableListOf<String>()
        for (step in 0 until groove.stepsPerBar) {
            labels.add(groove.countLabel(forStep = step))
        }
        assertEquals(listOf("1", "e", "+", "a", "2", "e", "+", "a"), labels)
    }

    // The clock: turning time into a place in the bar and a ball position.

    // feature: drum-trainer
    @Test
    fun aBarAtOneTwentyLastsTwoSeconds() {
        assertEquals(2.0, DrumClock.barDuration(tempo = 120.0, beatsPerBar = 4), 0.0)
    }

    // feature: drum-trainer
    @Test
    fun progressWrapsAtTheBarLine() {
        assertEquals(0.0, progressAt(elapsed = 0.0), 0.0)
        assertEquals(0.5, progressAt(elapsed = 1.0), 0.0)
        assertEquals(0.0, progressAt(elapsed = 2.0), 0.0)
        assertEquals(0.25, progressAt(elapsed = 2.5), 0.0)
    }

    // feature: drum-trainer
    @Test
    fun negativeTimeRestsOnBeatOne() {
        assertEquals(0.0, progressAt(elapsed = -1.0), 0.0)
    }

    // feature: drum-trainer
    @Test
    fun theBallTouchesDownOnEveryCount() {
        val onCount = DrumClock.ballPosition(progress = 0.25, stepsPerBar = 8)
        assertEquals(2, onCount.step)
        assertEquals(0.0, onCount.travel, 0.0)
        assertTrue(abs(onCount.bounce) < 1e-9)
    }

    // feature: drum-trainer
    @Test
    fun theBallPeaksHalfwayBetweenCounts() {
        val midway = DrumClock.ballPosition(progress = 3.5 / 8, stepsPerBar = 8)
        assertEquals(3, midway.step)
        assertEquals(0.5, midway.travel, 0.0)
        assertTrue(abs(midway.bounce - 1) < 1e-9)
    }

    // feature: drum-trainer
    @Test
    fun theBallNeverLeavesTheBar() {
        val end = DrumClock.ballPosition(progress = 0.999_999_999, stepsPerBar = 8)
        assertEquals(7, end.step)
        val overflow = DrumClock.ballPosition(progress = 1.0, stepsPerBar = 8)
        assertEquals(7, overflow.step)
    }

    // The sound: one bar of synthesized kit.

    // feature: drum-trainer
    @Test
    fun theBarIsExactlyOneBarLong() {
        val renderer = DrumBarRenderer(sampleRate = 1000.0)
        val samples = renderer.render(DrumGroove.builtIn[0], tempo = 120.0)
        assertEquals(2000, samples.size)
    }

    // feature: drum-trainer
    @Test
    fun everyHitMakesSoundAndNothingClips() {
        val renderer = DrumBarRenderer(sampleRate = 8000.0)
        val rock = DrumGroove.builtIn[0]
        val samples = renderer.render(rock, tempo = 120.0)
        val stepFrames = samples.size / rock.stepsPerBar
        for (step in 0 until rock.stepsPerBar) {
            val windowStart = step * stepFrames
            var loudest = 0.0f
            for (index in windowStart until windowStart + 100) {
                loudest = max(loudest, abs(samples[index]))
            }
            assertTrue("step $step is silent", loudest > 0.01f)
            assertTrue("step $step clips", loudest <= 1)
        }
    }

    // feature: drum-trainer
    @Test
    fun aSilentGrooveRendersSilence() {
        val renderer = DrumBarRenderer(sampleRate = 1000.0)
        val silent = DrumGroove(name = "Rest", beatsPerBar = 1, subdivision = 2, hits = listOf())
        val samples = renderer.render(silent, tempo = 60.0)
        for ((index, sample) in samples.withIndex()) {
            assertEquals("sample $index", 0.0f, sample, 0.0f)
        }
    }

    // feature: drum-trainer
    @Test
    fun renderingIsDeterministic() {
        val renderer = DrumBarRenderer(sampleRate = 4000.0)
        val first = renderer.render(DrumGroove.builtIn[0], tempo = 100.0)
        val second = renderer.render(DrumGroove.builtIn[0], tempo = 100.0)
        assertArrayEquals(first, second, 0.0f)
    }

    /** Swift's progress checks all use a 4-beat bar at 120 BPM, two seconds long. */
    private fun progressAt(elapsed: Double): Double =
        DrumClock.barProgress(elapsed = elapsed, tempo = 120.0, beatsPerBar = 4)
}
