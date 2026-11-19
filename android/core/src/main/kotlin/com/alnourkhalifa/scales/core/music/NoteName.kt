package com.alnourkhalifa.scales.core.music

/**
 * Twin of the `Letter` enum in `Scales/Music/NoteName.swift`: the seven letter names of
 * standard notation. Swift's `Int` raw value is [rawValue] (C = 0 through B = 6), which is
 * how settings and saved scales store a letter.
 */
enum class Letter(
    val rawValue: Int,
    /** The pitch class of the letter with no sharp or flat. */
    val naturalPitchClass: Int,
    val symbol: String,
) {
    C(rawValue = 0, naturalPitchClass = 0, symbol = "C"),
    D(rawValue = 1, naturalPitchClass = 2, symbol = "D"),
    E(rawValue = 2, naturalPitchClass = 4, symbol = "E"),
    F(rawValue = 3, naturalPitchClass = 5, symbol = "F"),
    G(rawValue = 4, naturalPitchClass = 7, symbol = "G"),
    A(rawValue = 5, naturalPitchClass = 9, symbol = "A"),
    B(rawValue = 6, naturalPitchClass = 11, symbol = "B"),
    ;

    /** The letter a number of steps higher, wrapping from B back to C (and below C to B). */
    fun advanced(by: Int): Letter {
        val letterCount = entries.size
        val shifted = (rawValue + by) % letterCount
        val wrappedIndex = (shifted + letterCount) % letterCount
        return entries[wrappedIndex]
    }

    /** Decoding from the raw number, Swift's `init(rawValue:)`. */
    companion object {
        /** The letter Swift would decode from this raw value, or null for any other number. */
        fun fromRawValue(rawValue: Int): Letter? {
            for (letter in entries) {
                if (letter.rawValue == rawValue) {
                    return letter
                }
            }
            return null
        }
    }
}

/**
 * Twin of `NoteName` in `Scales/Music/NoteName.swift`: a spelled note such as F♯ or B♭.
 *
 * Spelling is kept, rather than only a pitch number, because musicians read G major as
 * containing F♯ and F major as containing B♭, never "G♭" or "A♯". The scale model picks the
 * letter for each degree and this type records the sharps or flats that letter needs.
 */
data class NoteName(
    val letter: Letter,
    /** Negative values are flats, positive values are sharps (-2 is a double flat). */
    val accidental: Int = 0,
) {
    val pitchClass: Int
        get() = PitchMath.wrap(letter.naturalPitchClass + accidental)

    val displayName: String
        get() = letter.symbol + accidentalSymbol

    /** Single and double accidentals have their own glyphs; anything larger, which no
     *  built-in scale produces, repeats the single sign. */
    private val accidentalSymbol: String
        get() =
            when (accidental) {
                DOUBLE_FLAT -> "𝄫"
                -1 -> "♭"
                0 -> ""
                1 -> "♯"
                DOUBLE_SHARP -> "𝄪"
                else -> repeatedSymbol()
            }

    private fun repeatedSymbol(): String {
        if (accidental < 0) {
            return "♭".repeat(-accidental)
        }
        return "♯".repeat(accidental)
    }

    /** Swift's static `rootChoices`, plus the accidental counts with their own glyphs. */
    companion object {
        private const val DOUBLE_FLAT = -2
        private const val DOUBLE_SHARP = 2

        /** Roots offered in the picker. Both spellings are listed for the black keys so that
         *  keys like D♭ major and C♯ minor each come out spelled the way they are written. */
        val rootChoices: List<NoteName> =
            listOf(
                NoteName(Letter.C),
                NoteName(Letter.C, 1),
                NoteName(Letter.D, -1),
                NoteName(Letter.D),
                NoteName(Letter.D, 1),
                NoteName(Letter.E, -1),
                NoteName(Letter.E),
                NoteName(Letter.F),
                NoteName(Letter.F, 1),
                NoteName(Letter.G, -1),
                NoteName(Letter.G),
                NoteName(Letter.G, 1),
                NoteName(Letter.A, -1),
                NoteName(Letter.A),
                NoteName(Letter.A, 1),
                NoteName(Letter.B, -1),
                NoteName(Letter.B),
            )
    }
}
