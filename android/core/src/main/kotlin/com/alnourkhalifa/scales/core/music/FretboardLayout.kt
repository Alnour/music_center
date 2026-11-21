package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/FretboardLayout.swift`: which scale tone (if any) sits at every string
 * and fret, ready to be drawn.
 *
 * The layout is pure data so the phone screen and the TV screen draw exactly the same board,
 * and so tests can check note positions without rendering anything.
 *
 * The primary constructor takes any set of open strings, thickest first: the guitar's six,
 * or the bass guitar's four or five ([BassTuning.openStrings]).
 *
 * A capo presses every string at its fret, so the frets behind it cannot be played: they
 * carry no tones, and the tones on the capo's own fret are the new open strings.
 */
class FretboardLayout(
    scale: Scale,
    openStrings: List<NoteName>,
    val fretCount: Int,
    capoFret: Int = 0,
) {
    /** The guitar's neck in one of its six-string tunings. */
    constructor(
        scale: Scale,
        tuning: Tuning,
        fretCount: Int,
        capoFret: Int = 0,
    ) : this(scale, tuning.openStrings, fretCount, capoFret)

    /** Twin of `FretboardLayout.GuitarString`: one string's open note and what each fret
     *  holds. */
    data class GuitarString(
        val openNote: NoteName,
        /** Indexed by fret number; index 0 is the open string. */
        val tonesByFret: List<ScaleTone?>,
    )

    /** The fret the capo is on, 0 for no capo. Always below [fretCount]. */
    val capoFret: Int

    /** Thickest string first, matching [Tuning.openStrings]. */
    val strings: List<GuitarString>

    init {
        val highestCapoFret = fretCount - 1
        val playableCapoFret = maxOf(0, minOf(capoFret, highestCapoFret))
        val strings = mutableListOf<GuitarString>()
        for (openNote in openStrings) {
            val tonesByFret = mutableListOf<ScaleTone?>()
            for (fret in 0..fretCount) {
                if (fret < playableCapoFret) {
                    tonesByFret.add(null)
                    continue
                }
                val pitchClass = PitchMath.wrap(openNote.pitchClass + fret)
                tonesByFret.add(scale.tone(pitchClass))
            }
            strings.add(GuitarString(openNote, tonesByFret))
        }
        this.capoFret = playableCapoFret
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
