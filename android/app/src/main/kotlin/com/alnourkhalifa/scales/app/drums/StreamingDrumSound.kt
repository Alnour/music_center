package com.alnourkhalifa.scales.app.drums

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Loops a rendered bar of drums through one streaming `AudioTrack`, opened when the drums
 * screen appears and released when it goes. Stands in for the `AVAudioEngine` and
 * `AVAudioPlayerNode` that `Scales/Drums/DrumPlayer.swift` loops a buffer on.
 *
 * Why streaming: a static track would hold the whole bar in the audio buffer (over half a
 * megabyte at 40 BPM), which a 1 GB Fire TV stick may refuse. Instead a writer coroutine
 * feeds 20 ms chunks from the bar, wrapping at its end, so the loop is gapless and the track's
 * buffer stays at four times the device minimum.
 *
 * Why every track call happens on the writer: `AudioTrack` must not be flushed while another
 * thread writes to it, or a few milliseconds of the old bar would sound after the new one.
 * Each [play] and [stop] cancels the running writer and starts a new coroutine that first
 * waits for it to finish, so pausing, flushing, swapping the bar and writing never overlap.
 *
 * Audio focus: iOS plays in the `.playback` category without mixing, which silences other
 * apps, so this asks for `AUDIOFOCUS_GAIN` to match. Losing focus calls [onFocusLost];
 * stopping gives focus back.
 *
 * Any failure (no track, a refused focus request, a write error) leaves the drums silent and
 * is logged; the ball keeps running, as on iOS when the engine cannot start.
 */
class StreamingDrumSound(
    context: Context,
) : DrumSound {
    private val audioManager: AudioManager? =
        context.applicationContext.getSystemService(AudioManager::class.java)

    override val sampleRate: Int = nativeSampleRate()

    override var onFocusLost: (() -> Unit)? = null

    private val attributes: AudioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

    private val focusRequest: AudioFocusRequest =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
            .build()

    private val track: AudioTrack? = openTrack()

    /** Runs the writer and the pause-flush-swap steps one after another, off the main thread. */
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The latest writer or stop step; the next one waits for it before touching the track. */
    private var writerJob: Job? = null

    private var isReleased = false

    override fun play(
        samples: FloatArray,
        sampleRate: Int,
    ) {
        val openTrack = track ?: return
        if (isReleased || samples.isEmpty()) {
            return
        }
        if (sampleRate != this.sampleRate) {
            Log.w(TAG, "Bar rendered at $sampleRate Hz plays on a ${this.sampleRate} Hz track")
        }
        if (!requestFocus()) {
            Log.w(TAG, "Audio focus refused; the drums stay silent")
            return
        }
        val bar = toPcm16(samples)
        val previous = writerJob
        previous?.cancel()
        writerJob =
            writerScope.launch {
                previous?.join()
                restartTrack(openTrack)
                writeLoop(openTrack, bar)
            }
    }

    override fun stop() {
        abandonFocus()
        val openTrack = track ?: return
        val previous = writerJob
        previous?.cancel()
        writerJob =
            writerScope.launch {
                previous?.join()
                silence(openTrack)
            }
    }

    override fun release() {
        if (isReleased) {
            return
        }
        isReleased = true
        onFocusLost = null
        abandonFocus()
        val openTrack = track ?: return
        val previous = writerJob
        previous?.cancel()
        writerJob =
            writerScope.launch {
                previous?.join()
                silence(openTrack)
                openTrack.release()
            }
    }

    /** The device's own output rate, so the mixer does not resample every chunk. */
    private fun nativeSampleRate(): Int {
        val reported = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val rate = reported?.toIntOrNull()
        if (rate == null || rate <= 0) {
            return FALLBACK_SAMPLE_RATE
        }
        return rate
    }

    private fun openTrack(): AudioTrack? =
        try {
            val minimumBytes =
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            val format =
                AudioFormat
                    .Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            val built =
                AudioTrack
                    .Builder()
                    .setAudioAttributes(attributes)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(minimumBytes * BUFFER_MULTIPLE)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            initializedOrNull(built)
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Could not open the drum track", error)
            null
        } catch (error: UnsupportedOperationException) {
            Log.e(TAG, "Could not open the drum track", error)
            null
        }

    /** A track the system could not set up is released at once and treated as absent. */
    private fun initializedOrNull(built: AudioTrack): AudioTrack? {
        if (built.state == AudioTrack.STATE_INITIALIZED) {
            return built
        }
        Log.e(TAG, "The drum track did not initialise (state ${built.state})")
        built.release()
        return null
    }

    /** Drops whatever the old bar left in the buffer and starts the track again. */
    private fun restartTrack(openTrack: AudioTrack) {
        try {
            openTrack.pause()
            openTrack.flush()
            openTrack.play()
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Could not restart the drum track", error)
        }
    }

    private fun silence(openTrack: AudioTrack) {
        try {
            openTrack.pause()
            openTrack.flush()
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Could not stop the drum track", error)
        }
    }

    /**
     * Feeds the bar to the track in 20 ms chunks, wrapping to its start at the bar line, until
     * cancelled. Writes do not block, so a cancel is noticed within one chunk; when the
     * buffer is full the writer waits a few milliseconds for the track to drain.
     */
    private suspend fun writeLoop(
        openTrack: AudioTrack,
        bar: ShortArray,
    ) {
        val chunkFrames = sampleRate * CHUNK_MILLISECONDS / MILLISECONDS_PER_SECOND
        val chunk = ShortArray(chunkFrames)
        var barPosition = 0
        var pending = 0
        var chunkOffset = 0
        while (currentCoroutineContext().isActive) {
            if (pending == 0) {
                barPosition = fillChunk(chunk, bar, barPosition)
                pending = chunk.size
                chunkOffset = 0
            }
            val written =
                openTrack.write(
                    chunk,
                    chunkOffset,
                    pending,
                    AudioTrack.WRITE_NON_BLOCKING,
                )
            if (written < 0) {
                Log.e(TAG, "Drum track write failed with code $written")
                return
            }
            pending -= written
            chunkOffset += written
            if (pending > 0) {
                delay(FULL_BUFFER_WAIT_MILLISECONDS)
            }
        }
    }

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return false
        val result = manager.requestAudioFocus(focusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        audioManager?.abandonAudioFocusRequest(focusRequest)
    }

    /** A lasting or passing loss (a call, another player) stops the drums; ducking does not. */
    private fun onFocusChange(change: Int) {
        val isLoss =
            change == AudioManager.AUDIOFOCUS_LOSS ||
                change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
        if (isLoss) {
            onFocusLost?.invoke()
        }
    }

    private companion object {
        const val TAG = "DrumSound"
        const val FALLBACK_SAMPLE_RATE = 44_100
        const val BUFFER_MULTIPLE = 4
        const val CHUNK_MILLISECONDS = 20
        const val MILLISECONDS_PER_SECOND = 1000
        const val FULL_BUFFER_WAIT_MILLISECONDS = 5L
    }
}

/**
 * Copies the next chunk of the bar, wrapping at its end, and returns where the following
 * chunk starts.
 */
private fun fillChunk(
    chunk: ShortArray,
    bar: ShortArray,
    start: Int,
): Int {
    var position = start
    for (index in chunk.indices) {
        chunk[index] = bar[position]
        position += 1
        if (position == bar.size) {
            position = 0
        }
    }
    return position
}

/** The renderer's ±1 floats as the 16-bit samples the track takes. */
private fun toPcm16(samples: FloatArray): ShortArray {
    val pcm = ShortArray(samples.size)
    for (index in samples.indices) {
        val clamped = samples[index].coerceIn(-1f, 1f)
        val scaled = clamped * Short.MAX_VALUE
        pcm[index] = scaled.toInt().toShort()
    }
    return pcm
}
