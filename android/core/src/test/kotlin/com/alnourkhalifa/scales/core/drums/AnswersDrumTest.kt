package com.alnourkhalifa.scales.core.drums

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertClose
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.double
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.music.DrumClock
import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.core.music.DrumVoice
import com.alnourkhalifa.scales.core.obj
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Test
import kotlin.math.abs

/**
 * Reads the `drums` and `drumRender` sections of `expected/answers.json`, written by the
 * Swift generator, and checks the Kotlin grooves, clock and synthesized bars against them.
 *
 * The rendered bars are the strictest check: the Swift sound and this one must agree to six
 * decimals at every sampled index, so a player switching from iPhone to Android hears the
 * same kit.
 */
class AnswersDrumTest {
    private val drums: JsonObject = Answers.section("drums").jsonObject
    private val drumRender: JsonObject = Answers.section("drumRender").jsonObject

    // feature: drum-trainer
    @Test
    fun voicesMatchSwift() {
        val expected = drums.array("voices")
        assertSame("voice count", expected.size, DrumVoice.entries.size)
        for ((index, voice) in DrumVoice.entries.withIndex()) {
            val answer = expected[index].jsonObject
            val case = "voice $index"
            assertSame("$case id", answer.string("id"), voiceId(voice))
            assertSame("$case name", answer.string("name"), voice.displayName)
        }
    }

    // feature: drum-trainer
    @Test
    fun groovesMatchSwift() {
        val expected = drums.array("grooves")
        assertSame("groove count", expected.size, DrumGroove.builtIn.size)
        for ((index, groove) in DrumGroove.builtIn.withIndex()) {
            val answer = expected[index].jsonObject
            val case = "groove ${answer.string("name")}"
            assertSame("$case name", answer.string("name"), groove.name)
            assertSame("$case beatsPerBar", answer.int("beatsPerBar"), groove.beatsPerBar)
            assertSame("$case subdivision", answer.int("subdivision"), groove.subdivision)
            assertSame("$case stepsPerBar", answer.int("stepsPerBar"), groove.stepsPerBar)
            assertSame("$case hits", hitsOf(answer.array("hits")), hitsOf(groove))
            checkCounting(case, answer, groove)
        }
    }

    // feature: drum-trainer
    @Test
    fun barDurationsMatchSwift() {
        for (element in drums.array("barDurations")) {
            val answer = element.jsonObject
            val tempo = answer.double("tempo")
            val beatsPerBar = answer.int("beatsPerBar")
            val seconds = DrumClock.barDuration(tempo = tempo, beatsPerBar = beatsPerBar)
            assertClose("bar duration at $tempo BPM", answer.double("seconds"), seconds)
        }
    }

    // feature: drum-trainer
    @Test
    fun barProgressMatchesSwift() {
        for (element in drums.array("barProgress")) {
            val answer = element.jsonObject
            val elapsed = answer.double("elapsed")
            val progress =
                DrumClock.barProgress(
                    elapsed = elapsed,
                    tempo = answer.double("tempo"),
                    beatsPerBar = answer.int("beatsPerBar"),
                )
            assertClose("bar progress at $elapsed s", answer.double("progress"), progress)
        }
    }

    // feature: drum-trainer
    @Test
    fun ballTracksMatchSwift() {
        for (element in drums.array("ballTracks")) {
            val track = element.jsonObject
            val stepsPerBar = track.int("stepsPerBar")
            val positions = track.array("positions")
            for ((k, position) in positions.withIndex()) {
                val answer = position.jsonObject
                val progress = k.toDouble() / 64
                val case = "ball on $stepsPerBar steps at $k/64"
                val ball = DrumClock.ballPosition(progress = progress, stepsPerBar = stepsPerBar)
                assertClose("$case progress", answer.double("progress"), progress)
                assertSame("$case step", answer.int("step"), ball.step)
                assertClose("$case travel", answer.double("travel"), ball.travel)
                assertClose("$case bounce", answer.double("bounce"), ball.bounce)
            }
        }
    }

    // feature: drum-trainer
    @Test
    fun renderedBarsMatchSwift() {
        for (element in drumRender.array("fixtures")) {
            val answer = element.jsonObject
            val grooveName = answer.string("groove")
            val tempo = answer.double("tempo")
            val sampleRate = answer.double("sampleRate")
            val case = "render $grooveName at $tempo BPM, $sampleRate Hz"
            val groove = builtInGroove(grooveName)
            val samples = DrumBarRenderer(sampleRate = sampleRate).render(groove, tempo = tempo)
            checkSummary(case, answer, samples)
            checkHead(case, answer.array("head"), samples)
            checkProbe(case, answer.array("probe"), samples)
        }
    }

    // feature: drum-trainer
    @Test
    fun noiseMatchesSwiftBitForBit() {
        for (element in drumRender.array("noise")) {
            val answer = element.jsonObject
            val seed = answer.int("seed")
            val states = answer.array("states")
            val values = answer.array("values")
            val noise = NoiseSource(seed = seed)
            for (index in states.indices) {
                val case = "noise seed $seed call $index"
                val value = noise.next()
                val expectedState = states[index].jsonPrimitive.long
                val actualState =
                    noise.state
                        .toUInt()
                        .toLong()
                assertSame("$case state", expectedState, actualState)
                // The generator rounds values to six decimals, so they match to that.
                assertClose("$case value", values[index].jsonPrimitive.double, value)
            }
        }
    }

    /** Count labels and the voices on each step, the voices in [DrumVoice] order. */
    private fun checkCounting(
        case: String,
        answer: JsonObject,
        groove: DrumGroove,
    ) {
        val labels = answer.array("countLabels")
        val voicesPerStep = answer.array("voicesPerStep")
        for (step in 0 until groove.stepsPerBar) {
            val expectedLabel = labels[step].jsonPrimitive.content
            assertSame("$case count label at $step", expectedLabel, groove.countLabel(step))
            val expectedVoices = stringsOf(voicesPerStep[step].jsonArray)
            val actualVoices = orderedVoiceIds(groove.voices(step))
            assertSame("$case voices at $step", expectedVoices, actualVoices)
        }
    }

    /** Frame count and peak position exactly; the sums and peak to a thousandth. */
    private fun checkSummary(
        case: String,
        answer: JsonObject,
        samples: FloatArray,
    ) {
        var sum = 0.0
        var sumAbs = 0.0
        var peak = 0.0
        var peakIndex = 0
        for ((index, sample) in samples.withIndex()) {
            val value = sample.toDouble()
            sum += value
            sumAbs += abs(value)
            if (abs(value) > peak) {
                peak = abs(value)
                peakIndex = index
            }
        }
        assertSame("$case frames", answer.int("frames"), samples.size)
        assertSame("$case peakIndex", answer.int("peakIndex"), peakIndex)
        assertClose("$case sum", answer.double("sum"), sum, SUMMARY_TOLERANCE)
        assertClose("$case sumAbs", answer.double("sumAbs"), sumAbs, SUMMARY_TOLERANCE)
        assertClose("$case peak", answer.double("peak"), peak, SUMMARY_TOLERANCE)
    }

    private fun checkHead(
        case: String,
        head: JsonArray,
        samples: FloatArray,
    ) {
        for (index in head.indices) {
            val expected = head[index].jsonPrimitive.double
            val actual = samples[index].toDouble()
            assertClose("$case head sample $index", expected, actual)
        }
    }

    /** 64 samples spread over the bar, at index slot * frames / 64 as the generator took them. */
    private fun checkProbe(
        case: String,
        probe: JsonArray,
        samples: FloatArray,
    ) {
        for (slot in probe.indices) {
            val index = slot * samples.size / probe.size
            val expected = probe[slot].jsonPrimitive.double
            val actual = samples[index].toDouble()
            assertClose("$case probe slot $slot (sample $index)", expected, actual)
        }
    }

    private fun builtInGroove(name: String): DrumGroove {
        val groove = DrumGroove.builtIn.firstOrNull { candidate -> candidate.name == name }
        requireNotNull(groove) { "no built-in groove named $name" }
        return groove
    }

    private fun hitsOf(hits: JsonArray): List<String> {
        val labels = mutableListOf<String>()
        for (element in hits) {
            val hit = element.jsonObject
            labels.add("${hit.string("voice")}@${hit.int("step")}")
        }
        return labels
    }

    private fun hitsOf(groove: DrumGroove): List<String> {
        val labels = mutableListOf<String>()
        for (hit in groove.hits) {
            labels.add("${voiceId(hit.voice)}@${hit.step}")
        }
        return labels
    }

    private fun stringsOf(array: JsonArray): List<String> {
        val strings = mutableListOf<String>()
        for (element in array) {
            strings.add(element.jsonPrimitive.content)
        }
        return strings
    }

    /** The generator sorts a voice set by `DrumVoice.allCases`, the enum order here too. */
    private fun orderedVoiceIds(voices: Set<DrumVoice>): List<String> {
        val ids = mutableListOf<String>()
        for (voice in DrumVoice.entries) {
            if (voice in voices) {
                ids.add(voiceId(voice))
            }
        }
        return ids
    }

    /** The Swift case name the generator writes for each voice. */
    private fun voiceId(voice: DrumVoice): String =
        when (voice) {
            DrumVoice.HI_HAT -> "hiHat"
            DrumVoice.SNARE -> "snare"
            DrumVoice.KICK -> "kick"
        }

    private companion object {
        /** Sums over thousands of samples gather rounding, so they match to a thousandth. */
        const val SUMMARY_TOLERANCE = 1.0e-3
    }
}
