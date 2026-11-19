package com.alnourkhalifa.scales.core.drums

import com.alnourkhalifa.scales.core.music.DrumClock
import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.core.music.DrumVoice
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The kit's recipe, digit for digit from the Swift renderer. Each sound is a decaying
// burst; `*_DECAY` is the rate in exp(-time * rate), so a larger number dies away faster.

/** The kick lasts 180 ms. */
private const val KICK_SECONDS = 0.18

/** The kick's pitch settles on 50 Hz... */
private const val KICK_LOW_HZ = 50.0

/** ...after starting 100 Hz higher, at 150 Hz. */
private const val KICK_SWEEP_HZ = 100.0

/** How fast the kick's pitch falls; the fast drop is what makes it a thump. */
private const val KICK_PITCH_FALL = 30.0
private const val KICK_DECAY = 14.0
private const val KICK_GAIN = 0.9

/** The snare lasts 160 ms: a 185 Hz tone under a burst of noise. */
private const val SNARE_SECONDS = 0.16
private const val SNARE_TONE_HZ = 185.0
private const val SNARE_TONE_DECAY = 25.0
private const val SNARE_TONE_GAIN = 0.35
private const val SNARE_RATTLE_DECAY = 18.0
private const val SNARE_RATTLE_GAIN = 0.45

/** The closed hi-hat is a 50 ms click of bright noise. */
private const val HI_HAT_SECONDS = 0.05
private const val HI_HAT_DECAY = 80.0
private const val HI_HAT_GAIN = 0.25

/** Fixed seeds, so the snare and the hat sound the same on every render, on both platforms. */
private const val SNARE_NOISE_SEED = 7
private const val HI_HAT_NOISE_SEED = 11

/** The loudest a sample may be once overlapping hits are summed. */
private const val FULL_SCALE = 1.0f

/**
 * Synthesizes one bar of a groove as mono samples, so the kit needs no sound files.
 *
 * Twin of `DrumBarRenderer` in `Scales/Drums/DrumBarRenderer.swift`. All maths runs in
 * `Double` and each sample narrows to `Float` at the point Swift calls `Float(...)`, so the
 * bar matches the iOS bar sample for sample. One difference: each voice is synthesized once
 * per [render] call and reused for every hit of that voice; Swift synthesizes it per hit,
 * but the noise re-seeds each time, so the samples are identical.
 *
 * Each voice is a short burst shaped like the real drum: the kick is a sine whose pitch falls
 * from 150 Hz to 50 Hz, the snare a tone under a burst of noise, the hi-hat a click of
 * bright noise. The bar is rendered whole so the player can loop it sample-accurately.
 */
class DrumBarRenderer(
    val sampleRate: Double,
) {
    /**
     * The samples for one bar of the groove at the tempo. Hits that overlap are summed in
     * `Float` and the result is kept inside ±1; a sound running past the bar line is cut
     * there, as on iOS.
     */
    fun render(
        groove: DrumGroove,
        tempo: Double,
    ): FloatArray {
        val duration = DrumClock.barDuration(tempo = tempo, beatsPerBar = groove.beatsPerBar)
        val frameCount = (duration * sampleRate).toInt()
        val stepFrames = frameCount.toDouble() / groove.stepsPerBar.toDouble()
        val samples = FloatArray(frameCount)
        val sounds = mutableMapOf<DrumVoice, FloatArray>()
        for (hit in groove.hits) {
            val sound = sounds.getOrPut(hit.voice) { sound(hit.voice) }
            val start = (hit.step.toDouble() * stepFrames).toInt()
            mix(sound, samples, start)
        }
        for (index in samples.indices) {
            samples[index] = max(-FULL_SCALE, min(FULL_SCALE, samples[index]))
        }
        return samples
    }

    private fun mix(
        sound: FloatArray,
        samples: FloatArray,
        start: Int,
    ) {
        for (offset in sound.indices) {
            val index = start + offset
            if (index < samples.size) {
                samples[index] += sound[offset]
            }
        }
    }

    private fun sound(voice: DrumVoice): FloatArray =
        when (voice) {
            DrumVoice.KICK -> kick()
            DrumVoice.SNARE -> snare()
            DrumVoice.HI_HAT -> hiHat()
        }

    private fun kick(): FloatArray {
        val length = (KICK_SECONDS * sampleRate).toInt()
        val samples = FloatArray(length)
        var phase = 0.0
        for (frame in 0 until length) {
            val time = frame.toDouble() / sampleRate
            // The pitch drops fast, which is what makes a kick sound like a thump.
            val frequency = KICK_LOW_HZ + KICK_SWEEP_HZ * exp(-time * KICK_PITCH_FALL)
            phase += 2 * PI * frequency / sampleRate
            val envelope = exp(-time * KICK_DECAY)
            samples[frame] = (sin(phase) * envelope * KICK_GAIN).toFloat()
        }
        return samples
    }

    private fun snare(): FloatArray {
        val length = (SNARE_SECONDS * sampleRate).toInt()
        val noise = NoiseSource(seed = SNARE_NOISE_SEED)
        val samples = FloatArray(length)
        for (frame in 0 until length) {
            val time = frame.toDouble() / sampleRate
            val toneWave = sin(2 * PI * SNARE_TONE_HZ * time)
            val tone = toneWave * exp(-time * SNARE_TONE_DECAY) * SNARE_TONE_GAIN
            val rattle = noise.next() * exp(-time * SNARE_RATTLE_DECAY) * SNARE_RATTLE_GAIN
            samples[frame] = (tone + rattle).toFloat()
        }
        return samples
    }

    private fun hiHat(): FloatArray {
        val length = (HI_HAT_SECONDS * sampleRate).toInt()
        val noise = NoiseSource(seed = HI_HAT_NOISE_SEED)
        val samples = FloatArray(length)
        var previous = 0.0
        for (frame in 0 until length) {
            val time = frame.toDouble() / sampleRate
            val value = noise.next()
            // Differencing the noise keeps only its highs, the "ts" of a closed hat.
            val bright = value - previous
            previous = value
            samples[frame] = (bright * exp(-time * HI_HAT_DECAY) * HI_HAT_GAIN).toFloat()
        }
        return samples
    }
}

/**
 * White noise in -1...1 from a fixed seed (a linear congruential generator), so the drum
 * sounds are the same every time they are rendered. Twin of `NoiseSource` in
 * `Scales/Drums/DrumBarRenderer.swift`.
 *
 * Swift keeps the state in a `UInt32` and steps it with wrapping `&*` and `&+`. Kotlin has
 * the same 32 bits in an `Int`, whose `*` and `+` also wrap modulo 2^32, so the bit pattern
 * of [state] follows Swift's exactly; it is read as unsigned only when turned into a value.
 * Internal, with a readable [state], so the answers test can pin the raw sequence.
 */
internal class NoiseSource(
    seed: Int,
) {
    var state: Int = seed
        private set

    fun next(): Double {
        state = state * LCG_MULTIPLIER + LCG_INCREMENT
        val unsigned = state.toUInt().toDouble()
        return unsigned / UINT32_MAX * 2 - 1
    }

    private companion object {
        /** The Numerical Recipes constants, as in the Swift generator. */
        const val LCG_MULTIPLIER = 1_664_525
        const val LCG_INCREMENT = 1_013_904_223

        /** `Double(UInt32.max)`. */
        const val UINT32_MAX = 4_294_967_295.0
    }
}
