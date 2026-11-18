package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/ScaleDegree.swift`: a step of a scale described relative to the major
 * scale, such as 3, ♭3 or ♯4.
 *
 * Describing scales this way gives two things at once: the semitone distance from the root
 * (for finding notes on the neck) and the letter offset (for spelling them correctly). A ♭3
 * is always a third-letter note, so in A it is C, never B♯.
 *
 * Degrees sort in letter order (by number, then flats before sharps), the order a formula is
 * written in. Swift's `init(symbol:)` is [fromSymbol] here, because a Kotlin data class
 * cannot compute its fields in a secondary constructor.
 */
data class ScaleDegree(
    /** 1 through 7. */
    val number: Int,
    /** -1 lowers the major-scale step by a semitone, +1 raises it. */
    val alteration: Int,
) : Comparable<ScaleDegree> {
    val semitonesAboveRoot: Int
        get() = majorScaleSemitones[number - 1] + alteration

    /** Semitones above the root folded into one octave, so a ♯7 lands on the root itself.
     *  Two degrees with the same value are the same pitch, whatever their spelling. */
    val pitchClassAboveRoot: Int
        get() = PitchMath.wrap(semitonesAboveRoot)

    val isRoot: Boolean
        get() = number == 1 && alteration == 0

    /** One sign whatever the size of the alteration, as Swift writes it: a doubly lowered
     *  step still reads ♭. */
    val label: String
        get() {
            val prefix =
                when {
                    alteration < 0 -> "♭"
                    alteration > 0 -> "♯"
                    else -> ""
                }
            return prefix + number.toString()
        }

    /** The same step a semitone lower, keeping its letter: 7 becomes ♭7, ♯4 becomes 4. */
    fun lowered(): ScaleDegree = ScaleDegree(number, alteration - 1)

    /** The same step a semitone higher, keeping its letter: 4 becomes ♯4, ♭3 becomes 3. */
    fun raised(): ScaleDegree = ScaleDegree(number, alteration + 1)

    /** Letter order: 2 before ♭3 before 3 before ♯4. Within one letter, flat comes first. */
    override fun compareTo(other: ScaleDegree): Int {
        if (number != other.number) {
            return number.compareTo(other.number)
        }
        return alteration.compareTo(other.alteration)
    }

    /** Swift's static members of `ScaleDegree`, and its `init(symbol:)`. */
    companion object {
        private val majorScaleSemitones = listOf(0, 2, 4, 5, 7, 9, 11)

        /** Degree numbers a scale can use: one per letter of the scale. */
        val numbers: IntRange = 1..7

        /** The root, which every scale starts on and no edit may move. */
        val root: ScaleDegree = ScaleDegree(number = 1, alteration = 0)

        /** Reads one symbol of a scale formula, such as "5", "♭3" or "♯4". Formulas are fixed
         *  in the source and covered by tests, so a malformed one is a programming error. */
        fun fromSymbol(symbol: String): ScaleDegree {
            var alteration = 0
            var digits = symbol
            if (digits.startsWith("♭")) {
                alteration = -1
                digits = digits.removePrefix("♭")
            } else if (digits.startsWith("♯")) {
                alteration = 1
                digits = digits.removePrefix("♯")
            }
            val number = digits.toIntOrNull()
            check(number != null && number in numbers) { "Malformed scale degree symbol: $symbol" }
            return ScaleDegree(number, alteration)
        }
    }
}
