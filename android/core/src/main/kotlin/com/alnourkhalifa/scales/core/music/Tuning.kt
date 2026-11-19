package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/Tuning.swift`: common six-string tunings. [rawValue] is Swift's raw
 * string ("halfStepDown"), the value saved settings carry. Swift's `switch` tables are the
 * entries' named constructor arguments.
 */
enum class Tuning(
    val rawValue: String,
    val displayName: String,
    val shortName: String,
    /** Open-string notes from the thickest (6th) string to the thinnest (1st). */
    val openStrings: List<NoteName>,
) {
    STANDARD(
        rawValue = "standard",
        displayName = "Standard (E A D G B E)",
        shortName = "Standard tuning",
        openStrings = naturals(Letter.E, Letter.A, Letter.D, Letter.G, Letter.B, Letter.E),
    ),
    HALF_STEP_DOWN(
        rawValue = "halfStepDown",
        displayName = "Half Step Down (E♭ A♭ D♭ G♭ B♭ E♭)",
        shortName = "Half step down",
        openStrings =
            listOf(
                NoteName(Letter.E, -1),
                NoteName(Letter.A, -1),
                NoteName(Letter.D, -1),
                NoteName(Letter.G, -1),
                NoteName(Letter.B, -1),
                NoteName(Letter.E, -1),
            ),
    ),
    DROP_D(
        rawValue = "dropD",
        displayName = "Drop D (D A D G B E)",
        shortName = "Drop D",
        openStrings = naturals(Letter.D, Letter.A, Letter.D, Letter.G, Letter.B, Letter.E),
    ),
    DADGAD(
        rawValue = "dadgad",
        displayName = "DADGAD",
        shortName = "DADGAD",
        openStrings = naturals(Letter.D, Letter.A, Letter.D, Letter.G, Letter.A, Letter.D),
    ),
    OPEN_G(
        rawValue = "openG",
        displayName = "Open G (D G D G B D)",
        shortName = "Open G",
        openStrings = naturals(Letter.D, Letter.G, Letter.D, Letter.G, Letter.B, Letter.D),
    ),
    OPEN_D(
        rawValue = "openD",
        displayName = "Open D (D A D F♯ A D)",
        shortName = "Open D",
        openStrings =
            listOf(
                NoteName(Letter.D),
                NoteName(Letter.A),
                NoteName(Letter.D),
                NoteName(Letter.F, 1),
                NoteName(Letter.A),
                NoteName(Letter.D),
            ),
    ),
    ;

    /** Decoding from the raw string, Swift's `init(rawValue:)`. */
    companion object {
        /** The tuning Swift would decode from this raw string, or null for an unknown one. */
        fun fromRawValue(rawValue: String): Tuning? {
            for (tuning in entries) {
                if (tuning.rawValue == rawValue) {
                    return tuning
                }
            }
            return null
        }
    }
}

/** Open strings with no sharps or flats, listed thick to thin, so the tunings that need no
 *  accidentals read as their letters. */
private fun naturals(vararg letters: Letter): List<NoteName> {
    val notes = mutableListOf<NoteName>()
    for (letter in letters) {
        notes.add(NoteName(letter))
    }
    return notes
}
