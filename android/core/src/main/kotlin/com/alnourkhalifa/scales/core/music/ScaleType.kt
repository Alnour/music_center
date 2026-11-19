package com.alnourkhalifa.scales.core.music

/**
 * Twin of `ScaleType` in `Scales/Music/ScaleType.swift`: the built-in scale shapes the app can
 * draw, each defined by its formula. [rawValue] is Swift's raw string ("naturalMinor"), the
 * value saved settings and the TV clip key carry, so it must never change. Swift's `switch`
 * tables are the entries' named constructor arguments, one scale per line.
 */
enum class ScaleType(
    val rawValue: String,
    val displayName: String,
    /** The formula as musicians write it, read against the major scale. */
    val formula: String,
) {
    MAJOR(rawValue = "major", displayName = "Major", formula = "1 2 3 4 5 6 7"),
    NATURAL_MINOR(
        rawValue = "naturalMinor",
        displayName = "Natural Minor",
        formula = "1 2 ♭3 4 5 ♭6 ♭7",
    ),
    HARMONIC_MINOR(
        rawValue = "harmonicMinor",
        displayName = "Harmonic Minor",
        formula = "1 2 ♭3 4 5 ♭6 7",
    ),
    MELODIC_MINOR(
        rawValue = "melodicMinor",
        displayName = "Melodic Minor",
        formula = "1 2 ♭3 4 5 6 7",
    ),
    MAJOR_PENTATONIC(
        rawValue = "majorPentatonic",
        displayName = "Major Pentatonic",
        formula = "1 2 3 5 6",
    ),
    MINOR_PENTATONIC(
        rawValue = "minorPentatonic",
        displayName = "Minor Pentatonic",
        formula = "1 ♭3 4 5 ♭7",
    ),
    BLUES(rawValue = "blues", displayName = "Blues", formula = "1 ♭3 4 ♭5 5 ♭7"),
    MAJOR_BLUES(rawValue = "majorBlues", displayName = "Major Blues", formula = "1 2 ♭3 3 5 6"),
    IONIAN(rawValue = "ionian", displayName = "Ionian", formula = "1 2 3 4 5 6 7"),
    DORIAN(rawValue = "dorian", displayName = "Dorian", formula = "1 2 ♭3 4 5 6 ♭7"),
    PHRYGIAN(rawValue = "phrygian", displayName = "Phrygian", formula = "1 ♭2 ♭3 4 5 ♭6 ♭7"),
    LYDIAN(rawValue = "lydian", displayName = "Lydian", formula = "1 2 3 ♯4 5 6 7"),
    MIXOLYDIAN(rawValue = "mixolydian", displayName = "Mixolydian", formula = "1 2 3 4 5 6 ♭7"),
    AEOLIAN(rawValue = "aeolian", displayName = "Aeolian", formula = "1 2 ♭3 4 5 ♭6 ♭7"),
    LOCRIAN(rawValue = "locrian", displayName = "Locrian", formula = "1 ♭2 ♭3 4 ♭5 ♭6 ♭7"),
    PHRYGIAN_DOMINANT(
        rawValue = "phrygianDominant",
        displayName = "Phrygian Dominant (Hijaz)",
        formula = "1 ♭2 3 4 5 ♭6 ♭7",
    ),
    WHOLE_TONE(rawValue = "wholeTone", displayName = "Whole Tone", formula = "1 2 3 ♯4 ♯5 ♭7"),
    ;

    /** The formula read symbol by symbol, so the formula string stays the one source. */
    val degrees: List<ScaleDegree>
        get() {
            val degrees = mutableListOf<ScaleDegree>()
            for (symbol in formula.split(" ")) {
                degrees.add(ScaleDegree.fromSymbol(symbol))
            }
            return degrees
        }

    /** Decoding from the raw string, Swift's `init(rawValue:)`. */
    companion object {
        /** The scale Swift would decode from this raw string, or null for an unknown one. */
        fun fromRawValue(rawValue: String): ScaleType? {
            for (type in entries) {
                if (type.rawValue == rawValue) {
                    return type
                }
            }
            return null
        }
    }
}

/**
 * Twin of `ScaleFamily` in `Scales/Music/ScaleType.swift`: groups for the scale picker, so
 * the menu reads like a practice book's table of contents.
 */
enum class ScaleFamily(
    val rawValue: String,
    val title: String,
    /** The family's scales in the order the picker lists them. */
    val scales: List<ScaleType>,
) {
    EVERYDAY(
        rawValue = "everyday",
        title = "Major & Minor",
        scales =
            listOf(
                ScaleType.MAJOR,
                ScaleType.NATURAL_MINOR,
                ScaleType.HARMONIC_MINOR,
                ScaleType.MELODIC_MINOR,
            ),
    ),
    PENTATONIC_AND_BLUES(
        rawValue = "pentatonicAndBlues",
        title = "Pentatonic & Blues",
        scales =
            listOf(
                ScaleType.MAJOR_PENTATONIC,
                ScaleType.MINOR_PENTATONIC,
                ScaleType.BLUES,
                ScaleType.MAJOR_BLUES,
            ),
    ),
    MODES(
        rawValue = "modes",
        title = "Modes",
        scales =
            listOf(
                ScaleType.IONIAN,
                ScaleType.DORIAN,
                ScaleType.PHRYGIAN,
                ScaleType.LYDIAN,
                ScaleType.MIXOLYDIAN,
                ScaleType.AEOLIAN,
                ScaleType.LOCRIAN,
            ),
    ),
    COLOUR(
        rawValue = "colour",
        title = "Other Colours",
        scales =
            listOf(
                ScaleType.PHRYGIAN_DOMINANT,
                ScaleType.WHOLE_TONE,
            ),
    ),
}
