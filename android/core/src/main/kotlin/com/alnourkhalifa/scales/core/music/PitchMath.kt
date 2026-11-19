package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/PitchMath.swift`: arithmetic on pitch classes, the twelve semitone
 * positions of the octave numbered 0 (C) through 11 (B). Octaves don't matter for scale
 * membership, so every calculation here wraps around twelve.
 *
 * Differs from Swift only in naming: `semitonesPerOctave` is the constant
 * [SEMITONES_PER_OCTAVE], as Kotlin spells constants.
 */
object PitchMath {
    const val SEMITONES_PER_OCTAVE = 12

    /** Folds any semitone count into 0..11, including negative counts. Kotlin's `%` keeps
     *  the sign of the dividend, as Swift's does, so a negative remainder is lifted. */
    fun wrap(semitones: Int): Int {
        val remainder = semitones % SEMITONES_PER_OCTAVE
        if (remainder < 0) {
            return remainder + SEMITONES_PER_OCTAVE
        }
        return remainder
    }

    /** The shortest signed step from one pitch class to another, in -5..6. Used to turn
     *  "this letter needs to land on that pitch" into a sharp or flat count. */
    fun signedDistance(
        from: Int,
        to: Int,
    ): Int {
        val upward = wrap(to - from)
        if (upward > SEMITONES_PER_OCTAVE / 2) {
            return upward - SEMITONES_PER_OCTAVE
        }
        return upward
    }
}
