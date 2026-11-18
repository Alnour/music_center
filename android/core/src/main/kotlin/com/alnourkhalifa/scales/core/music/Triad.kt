package com.alnourkhalifa.scales.core.music

/**
 * Twin of `TriadQuality` in `Scales/Music/Triad.swift`: the kinds of three-note chord a scale
 * can stack on one of its notes, told apart by the size of the middle note and the fifth above
 * the root.
 *
 * Beyond the four textbook triads, a scale edit can leave a chord with an altered fifth
 * (C E G♭ is C(♭5), C E♭ G♯ is Cm(♯5)) or with no third at all, where the second or the
 * fourth stands in (Csus2, Csus4). Each is named the way chord charts print it.
 *
 * Differs from Swift: the `switch` tables are the entries' named constructor arguments, the
 * failable `init?(thirdSemitones:fifthSemitones:)` is [fromSemitones], and [swiftName] is
 * Swift's case name (`String(describing:)`), which the answers file records.
 */
enum class TriadQuality(
    val swiftName: String,
    /** Semitones from the root to the middle note: a major third is 4, a minor third 3, a
     *  fourth 5, a second 2. */
    private val middleSemitones: Int,
    /** Semitones from the root to the fifth: perfect 7, diminished 6, augmented 8. */
    private val fifthSemitones: Int,
    /** Appended to the root in a chord name: C, Cm, C°, C+, C(♭5), Cm(♯5), Csus4, Csus2(♭5). */
    val nameSuffix: String,
    /** The quality as it is said aloud: "C minor", "B diminished", "D suspended fourth". */
    val spokenName: String,
) {
    MAJOR(
        swiftName = "major",
        middleSemitones = 4,
        fifthSemitones = 7,
        nameSuffix = "",
        spokenName = "major",
    ),
    MINOR(
        swiftName = "minor",
        middleSemitones = 3,
        fifthSemitones = 7,
        nameSuffix = "m",
        spokenName = "minor",
    ),
    DIMINISHED(
        swiftName = "diminished",
        middleSemitones = 3,
        fifthSemitones = 6,
        nameSuffix = "°",
        spokenName = "diminished",
    ),
    AUGMENTED(
        swiftName = "augmented",
        middleSemitones = 4,
        fifthSemitones = 8,
        nameSuffix = "+",
        spokenName = "augmented",
    ),

    /** A major third over a diminished fifth: C E G♭. */
    MAJOR_FLAT_FIVE(
        swiftName = "majorFlatFive",
        middleSemitones = 4,
        fifthSemitones = 6,
        nameSuffix = "(♭5)",
        spokenName = "major flat five",
    ),

    /** A minor third over an augmented fifth: C E♭ G♯. */
    MINOR_SHARP_FIVE(
        swiftName = "minorSharpFive",
        middleSemitones = 3,
        fifthSemitones = 8,
        nameSuffix = "m(♯5)",
        spokenName = "minor sharp five",
    ),

    /** The fourth in place of the third: C F G. */
    SUSPENDED_FOURTH(
        swiftName = "suspendedFourth",
        middleSemitones = 5,
        fifthSemitones = 7,
        nameSuffix = "sus4",
        spokenName = "suspended fourth",
    ),

    /** The second in place of the third: C D G. */
    SUSPENDED_SECOND(
        swiftName = "suspendedSecond",
        middleSemitones = 2,
        fifthSemitones = 7,
        nameSuffix = "sus2",
        spokenName = "suspended second",
    ),
    SUSPENDED_FOURTH_FLAT_FIVE(
        swiftName = "suspendedFourthFlatFive",
        middleSemitones = 5,
        fifthSemitones = 6,
        nameSuffix = "sus4(♭5)",
        spokenName = "suspended fourth flat five",
    ),
    SUSPENDED_SECOND_FLAT_FIVE(
        swiftName = "suspendedSecondFlatFive",
        middleSemitones = 2,
        fifthSemitones = 6,
        nameSuffix = "sus2(♭5)",
        spokenName = "suspended second flat five",
    ),
    SUSPENDED_FOURTH_SHARP_FIVE(
        swiftName = "suspendedFourthSharpFive",
        middleSemitones = 5,
        fifthSemitones = 8,
        nameSuffix = "sus4(♯5)",
        spokenName = "suspended fourth sharp five",
    ),
    SUSPENDED_SECOND_SHARP_FIVE(
        swiftName = "suspendedSecondSharpFive",
        middleSemitones = 2,
        fifthSemitones = 8,
        nameSuffix = "sus2(♯5)",
        spokenName = "suspended second sharp five",
    ),
    ;

    /** True when the chord has a minor third, which chord charts show by a lower-case
     *  numeral and an "m" in the name. */
    val hasMinorThird: Boolean
        get() =
            when (this) {
                MINOR, DIMINISHED, MINOR_SHARP_FIVE -> true
                else -> false
            }

    /** True when a second or fourth stands in for the third. */
    val isSuspended: Boolean
        get() =
            when (this) {
                SUSPENDED_FOURTH,
                SUSPENDED_SECOND,
                SUSPENDED_FOURTH_FLAT_FIVE,
                SUSPENDED_SECOND_FLAT_FIVE,
                SUSPENDED_FOURTH_SHARP_FIVE,
                SUSPENDED_SECOND_SHARP_FIVE,
                -> true

                else -> false
            }

    /**
     * Writes a Roman numeral the way the quality is shown: lower case for chords with a minor
     * third, upper case otherwise, with the chart suffix after it (vii°, III+, I(♭5), i(♯5),
     * IVsus2). The "m" of a minor chord is carried by the lower case alone.
     */
    fun numeral(base: String): String {
        val cased = if (hasMinorThird) base.lowercase() else base
        if (this == MINOR) {
            return cased
        }
        if (this == MINOR_SHARP_FIVE) {
            return "$cased(♯5)"
        }
        return cased + nameSuffix
    }

    /** Swift's failable initializer. */
    companion object {
        /** Identifies a chord from the semitone distances of its middle note and fifth above
         *  the root, or null when no chart name fits (a diminished fifth under an augmented
         *  third, for instance). */
        fun fromSemitones(
            thirdSemitones: Int,
            fifthSemitones: Int,
        ): TriadQuality? {
            for (quality in entries) {
                val middleMatches = quality.middleSemitones == thirdSemitones
                val fifthMatches = quality.fifthSemitones == fifthSemitones
                if (middleMatches && fifthMatches) {
                    return quality
                }
            }
            return null
        }
    }
}

/** Twin of `Triad` in `Scales/Music/Triad.swift`: a three-note chord such as Dm, B° or Gsus4. */
data class Triad(
    val root: NoteName,
    val quality: TriadQuality,
) {
    val name: String
        get() = root.displayName + quality.nameSuffix

    /** "D minor", for sentences and accessibility labels. */
    val spokenName: String
        get() = "${root.displayName} ${quality.spokenName}"
}

/** Twin of `DiatonicChord` in `Scales/Music/Triad.swift`: a chord built on one step of a key,
 *  with its Roman numeral (IV, vi, vii°, …). */
data class DiatonicChord(
    val numeral: String,
    val triad: Triad,
)

/** The key tables need all seven chords; any other scale size has no table. */
private const val HEPTATONIC_TONE_COUNT = 7

/**
 * The seven chords of a key for the key tables, each numbered from the scale's own first note
 * (so A minor starts at i and its chord on C is III). Only seven-note scales have a full
 * table; other sizes give an empty list. Spelling comes from the scale, so F♯ major yields
 * E♯° rather than F°.
 */
fun Scale.diatonicTriads(): List<DiatonicChord> {
    if (tones.size != HEPTATONIC_TONE_COUNT) {
        return emptyList()
    }
    val chords = mutableListOf<DiatonicChord>()
    for (chord in letterTriads()) {
        chords.add(DiatonicChord(chord.numeral, chord.triad))
    }
    return chords
}

/** The key signature as musicians label it: "—", "3♯" or "2♭". Sharps win when a scale
 *  carries both, as in Swift. */
val Scale.keySignature: String
    get() {
        var sharps = 0
        var flats = 0
        for (tone in tones) {
            val accidental = tone.name.accidental
            if (accidental > 0) {
                sharps += accidental
            } else if (accidental < 0) {
                flats -= accidental
            }
        }
        if (sharps > 0) {
            return "$sharps♯"
        }
        if (flats > 0) {
            return "$flats♭"
        }
        return "—"
    }
