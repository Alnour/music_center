package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/BassTuning.swift`: common bass guitar tunings. [rawValue] is Swift's
 * raw string ("halfStepDown"), the value saved settings carry.
 *
 * The bass has its own list rather than sharing [Tuning]: a four-string bass is tuned like
 * the guitar's four thickest strings, so only the guitar tunings that keep those strings a
 * fourth apart (standard, half step down) or drop the lowest one (drop D) are used on it,
 * and the five-string bass adds a low B the guitar never has.
 */
enum class BassTuning(
    val rawValue: String,
    val displayName: String,
    val shortName: String,
    /** Open-string notes from the thickest string to the thinnest. */
    val openStrings: List<NoteName>,
) {
    STANDARD(
        rawValue = "standard",
        displayName = "Standard (E A D G)",
        shortName = "Standard tuning",
        openStrings =
            listOf(
                NoteName(Letter.E),
                NoteName(Letter.A),
                NoteName(Letter.D),
                NoteName(Letter.G),
            ),
    ),
    HALF_STEP_DOWN(
        rawValue = "halfStepDown",
        displayName = "Half Step Down (E♭ A♭ D♭ G♭)",
        shortName = "Half step down",
        openStrings =
            listOf(
                NoteName(Letter.E, -1),
                NoteName(Letter.A, -1),
                NoteName(Letter.D, -1),
                NoteName(Letter.G, -1),
            ),
    ),
    DROP_D(
        rawValue = "dropD",
        displayName = "Drop D (D A D G)",
        shortName = "Drop D",
        openStrings =
            listOf(
                NoteName(Letter.D),
                NoteName(Letter.A),
                NoteName(Letter.D),
                NoteName(Letter.G),
            ),
    ),
    FIVE_STRING(
        rawValue = "fiveString",
        displayName = "Five-string (B E A D G)",
        shortName = "Five-string",
        openStrings =
            listOf(
                NoteName(Letter.B),
                NoteName(Letter.E),
                NoteName(Letter.A),
                NoteName(Letter.D),
                NoteName(Letter.G),
            ),
    ),
    ;

    /** Decoding from the raw string, Swift's `init(rawValue:)`. */
    companion object {
        /** The tuning Swift would decode from this raw string, or null for an unknown one. */
        fun fromRawValue(rawValue: String): BassTuning? {
            for (tuning in entries) {
                if (tuning.rawValue == rawValue) {
                    return tuning
                }
            }
            return null
        }
    }
}
