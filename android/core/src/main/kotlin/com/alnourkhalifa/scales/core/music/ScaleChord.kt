package com.alnourkhalifa.scales.core.music

import kotlin.math.abs

/**
 * Twin of `ScaleChord` in `Scales/Music/ScaleChord.swift`: a chord built from a scale's own
 * notes on one of its tones, with the notes it uses.
 *
 * Differs from Swift: a data class over the four stored fields; the two numerals are
 * derived from them, as Swift's initializer derives them, so equality is unchanged (Swift's
 * synthesized `Hashable` also covers [isDominantSeventh], so A7 and A are different chords).
 */
data class ScaleChord(
    /** The degree the chord is built on. */
    val degree: ScaleDegree,
    val triad: Triad,
    /** Root, middle note (the third, or the fourth or second of a suspended chord) and fifth,
     *  spelled as the scale spells them, then the seventh of a seventh chord. */
    val notes: List<NoteName>,
    /** True for a dominant seventh chord (A7): the triad with a minor seventh on top, the
     *  chord of a twelve-bar blues. Triads are the rule everywhere else. */
    val isDominantSeventh: Boolean = false,
) {
    /** The numeral counted from the scale's own steps, as key tables print it: the chord on C
     *  in A minor is III. A seventh chord carries a 7 (IV7). */
    val numeral: String =
        triad.quality.numeral(romanNumerals[degree.number - 1]) + seventhSuffix(isDominantSeventh)

    /** The numeral with the degree's flat or sharp, read against the major scale the way
     *  formulas are: the chord on C in A minor is ♭III, the chord on ♭2 in Phrygian is ♭II. */
    val formulaNumeral: String = accidentalPrefix(degree.alteration) + numeral

    /** The name as chord charts print it: Dm, Gsus4, A7. */
    val name: String
        get() = triad.name + seventhSuffix(isDominantSeventh)

    /** "D minor", "A dominant seventh", for sentences and TalkBack. The seventh chord is
     *  named in full because "A major seventh" is a different chord (Amaj7). */
    val spokenName: String
        get() {
            if (isDominantSeventh) {
                return "${triad.root.displayName} dominant seventh"
            }
            return triad.spokenName
        }

    /** "V, G major", for the sentences of a lesson. */
    val spokenTitle: String
        get() = "$formulaNumeral, $spokenName"

    private companion object {
        val romanNumerals = listOf("I", "II", "III", "IV", "V", "VI", "VII")

        /** The "7" a dominant seventh chord's name and numeral end with (A7, IV7). */
        fun seventhSuffix(isDominantSeventh: Boolean): String = if (isDominantSeventh) "7" else ""

        /** One sign whatever the size of the alteration, as Swift writes it. */
        fun accidentalPrefix(alteration: Int): String =
            when {
                alteration < 0 -> "♭"
                alteration > 0 -> "♯"
                else -> ""
            }
    }
}

/** Letters above the root that can take the middle place of a chord, tried in order: the
 *  third first, then the fourth and the second for a suspended chord. */
private const val THIRD_LETTER_STEP = 2
private const val FOURTH_LETTER_STEP = 3
private const val SECOND_LETTER_STEP = 1
private val middleLetterSteps = listOf(THIRD_LETTER_STEP, FOURTH_LETTER_STEP, SECOND_LETTER_STEP)

/** The fifth sits four letters above the root. */
private const val FIFTH_LETTER_STEP = 4

/**
 * The chords this scale makes from its own notes, for a scale of any size.
 *
 * A chord is stacked by letter: on a tone, take the scale's tone two letters up (its third)
 * and four letters up (its fifth). An altered third or fifth is still a chord with a chart
 * name (C(♭5), Cm(♯5)), so an edit that moves one note renames the chord rather than losing
 * it. When the scale has no tone on the third's letter, the fourth stands in for it, then the
 * second (Csus4, Csus2), which is how players voice a chord in a scale that skips the third.
 * When neither the fifth nor a middle note is there, or the three notes have no chart name,
 * that tone has no chord. This is the textbook rule for seven-note scales, and it also shows
 * what happens when a note is removed (chords that needed it disappear) or added (new ones
 * form).
 *
 * A scale can hold two tones on one letter, as blues does with ♭5 and 5. Every pairing is
 * tried and the one with the fewest flats and sharps against the major scale wins, so C blues
 * gives C minor on C rather than C diminished.
 */
fun Scale.letterTriads(): List<ScaleChord> {
    val chords = mutableListOf<ScaleChord>()
    for (rootTone in tones) {
        val chord = chordOn(rootTone)
        if (chord != null) {
            chords.add(chord)
        }
    }
    return chords
}

/** The chord on one tone: the fifth's candidates and the scale grouped by letter are found
 *  in one pass, then each middle letter is tried in order until one makes a chord. */
private fun Scale.chordOn(rootTone: ScaleTone): ScaleChord? {
    val fifthNumber = letterNumber(rootTone.degree.number + FIFTH_LETTER_STEP)
    val fifths = mutableListOf<ScaleTone>()
    val tonesByLetter = mutableMapOf<Int, MutableList<ScaleTone>>()
    for (tone in tones) {
        if (tone.degree.number == fifthNumber) {
            fifths.add(tone)
        }
        val letterTones = tonesByLetter.getOrPut(tone.degree.number) { mutableListOf() }
        letterTones.add(tone)
    }

    for (step in middleLetterSteps) {
        val middleNumber = letterNumber(rootTone.degree.number + step)
        val middles = tonesByLetter[middleNumber] ?: continue
        val chord = bestChord(rootTone, middles, fifths)
        if (chord != null) {
            return chord
        }
    }
    return null
}

/** The chord on a root from one set of candidate middle notes and fifths, favouring the
 *  pairing with the fewest flats and sharps. On a tie the first pairing found is kept. */
private fun bestChord(
    rootTone: ScaleTone,
    middles: List<ScaleTone>,
    fifths: List<ScaleTone>,
): ScaleChord? {
    var best: ScaleChord? = null
    var bestAccidentalCount = Int.MAX_VALUE
    for (middle in middles) {
        for (fifth in fifths) {
            val quality = qualityOf(rootTone, middle, fifth)
            val accidentalCount = abs(middle.degree.alteration) + abs(fifth.degree.alteration)
            val isFewerAccidentals = accidentalCount < bestAccidentalCount
            if (quality != null && isFewerAccidentals) {
                bestAccidentalCount = accidentalCount
                val notes = listOf(rootTone.name, middle.name, fifth.name)
                best = ScaleChord(rootTone.degree, Triad(rootTone.name, quality), notes)
            }
        }
    }
    return best
}

/** The chart quality of three tones, measured in semitones above the root. */
private fun qualityOf(
    rootTone: ScaleTone,
    middle: ScaleTone,
    fifth: ScaleTone,
): TriadQuality? {
    val rootPitchClass = rootTone.name.pitchClass
    val middleSemitones = PitchMath.wrap(middle.name.pitchClass - rootPitchClass)
    val fifthSemitones = PitchMath.wrap(fifth.name.pitchClass - rootPitchClass)
    return TriadQuality.fromSemitones(middleSemitones, fifthSemitones)
}

/** Wraps a degree number past 7 back round to 1, as letters wrap from B to C. */
private fun letterNumber(number: Int): Int {
    val letterCount = ScaleDegree.numbers.count()
    return (number - 1) % letterCount + 1
}
