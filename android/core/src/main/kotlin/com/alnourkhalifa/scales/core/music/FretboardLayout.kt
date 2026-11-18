package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/FretboardLayout.swift`: which scale tone (if any) sits at every string
 * and fret, ready to be drawn.
 *
 * The layout is pure data so the phone screen and the TV screen draw exactly the same board,
 * and so tests can check note positions without rendering anything.
 */
class FretboardLayout(
    scale: Scale,
    tuning: Tuning,
    val fretCount: Int,
) {
    /** Twin of `FretboardLayout.GuitarString`: one string's open note and what each fret
     *  holds. */
    data class GuitarString(
        val openNote: NoteName,
        /** Indexed by fret number; index 0 is the open string. */
        val tonesByFret: List<ScaleTone?>,
    )

    /** Thickest string first, matching [Tuning.openStrings]. */
    val strings: List<GuitarString>

    init {
        val strings = mutableListOf<GuitarString>()
        for (openNote in tuning.openStrings) {
            val tonesByFret = mutableListOf<ScaleTone?>()
            for (fret in 0..fretCount) {
                val pitchClass = PitchMath.wrap(openNote.pitchClass + fret)
                tonesByFret.add(scale.tone(pitchClass))
            }
            strings.add(GuitarString(openNote, tonesByFret))
        }
        this.strings = strings
    }

    /** Fret numbers that carry a scale tone on the given string (0 is the open string). */
    fun frets(withToneOnString: Int): List<Int> {
        val frets = mutableListOf<Int>()
        val tonesByFret = strings[withToneOnString].tonesByFret
        for (fret in tonesByFret.indices) {
            if (tonesByFret[fret] != null) {
                frets.add(fret)
            }
        }
        return frets
    }
}
