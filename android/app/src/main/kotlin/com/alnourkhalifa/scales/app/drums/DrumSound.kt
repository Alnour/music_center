package com.alnourkhalifa.scales.app.drums

/**
 * Something that loops one rendered bar of drums until told to stop. [DrumPlayer] talks to
 * this rather than to `AudioTrack` so its timing rules run in JVM tests with a fake.
 *
 * Swift has no twin: `Scales/Drums/DrumPlayer.swift` drives `AVAudioEngine` directly. The
 * production sound is [StreamingDrumSound].
 */
interface DrumSound {
    /** The rate the device plays at, so the bar is rendered without resampling. */
    val sampleRate: Int

    /**
     * Called when another app takes the audio output (a call, a video). The player stops
     * then, as iOS stops when its audio session is interrupted.
     */
    var onFocusLost: (() -> Unit)?

    /**
     * Replaces whatever is playing with [samples] (one bar, mono, ±1) looped without a gap,
     * starting from its first sample.
     */
    fun play(
        samples: FloatArray,
        sampleRate: Int,
    )

    /** Silences the loop and gives the audio output back to other apps. */
    fun stop()

    /** Frees the audio output for good; the sound plays nothing afterwards. */
    fun release()
}
