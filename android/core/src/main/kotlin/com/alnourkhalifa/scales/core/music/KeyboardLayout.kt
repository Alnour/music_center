package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/KeyboardLayout.swift`: which scale tone (if any) sits on each piano
 * key, ready to be drawn.
 *
 * The keys run from C to a closing C, the way a keyboard is read, whatever the scale's root:
 * the root is picked out by its colour wherever it falls. Like [FretboardLayout] this is pure
 * data, so the phone and the TV draw the same keys and tests can check membership without
 * rendering.
 */
class KeyboardLayout(
    scale: Scale,
    octaveCount: Int = 1,
) {
    /** Twin of `KeyboardLayout.Key`: one piano key and the scale tone it carries. */
    data class Key(
        /** Semitones above C: 0 is C, 11 is B. The closing key is C again, so it is 0 too. */
        val pitchClass: Int,
        val isBlack: Boolean,
        /** The scale tone on this key, or null when the key is outside the scale. */
        val tone: ScaleTone?,
    )

    /** C D E F G A B for each octave and then the closing C, with the black keys in between,
     *  left to right: 13 keys for one octave, 25 for two. Fewer than one octave is drawn as
     *  one. */
    val keys: List<Key>

    init {
        val pitchClasses = mutableListOf<Int>()
        val drawnOctaves = maxOf(1, octaveCount)
        repeat(drawnOctaves) {
            pitchClasses.addAll(octavePitchClasses)
        }
        pitchClasses.add(0)
        val keys = mutableListOf<Key>()
        for (pitchClass in pitchClasses) {
            val isBlack = pitchClass in blackPitchClasses
            val key = Key(pitchClass, isBlack, scale.tone(pitchClass))
            keys.add(key)
        }
        this.keys = keys
    }

    /** The white keys, left to right. Drawn first because the black keys sit on them. */
    val whiteKeys: List<Key>
        get() = keys.filter { key -> !key.isBlack }

    /** The black keys, left to right. */
    val blackKeys: List<Key>
        get() = keys.filter { key -> key.isBlack }

    /** Swift's static members of `KeyboardLayout`. */
    companion object {
        /** The pitch classes no natural letter lands on: C♯/D♭, D♯/E♭, F♯/G♭, G♯/A♭, A♯/B♭. */
        val blackPitchClasses: Set<Int> = setOf(1, 3, 6, 8, 10)

        /** Pitch classes of one octave's twelve keys, white and black, in keyboard order. */
        private val octavePitchClasses: List<Int> =
            (0 until PitchMath.SEMITONES_PER_OCTAVE)
                .toList()
    }
}
