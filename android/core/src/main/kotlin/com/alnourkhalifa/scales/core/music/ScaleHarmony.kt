// Named after its Swift twin, ScaleHarmony.swift, so `make mirror` pairs the two files.
@file:Suppress("MatchingDeclarationName")

package com.alnourkhalifa.scales.core.music

/**
 * Twin of `HarmonicHome` in `Scales/Music/ScaleHarmony.swift`: where a scale's chords come
 * from.
 *
 * A seven-note scale is harmonised the textbook way: a chord stacked on each of its notes
 * ([letterTriads]). Pentatonic and blues scales are melody scales, five or six notes picked
 * out of a key, and the theory books do not harmonise them on their own notes (stacking gives
 * only two real triads and a handful of suspended chords). They are played over the chords of
 * the key they come from: the major pentatonic over its major key, the minor pentatonic over
 * its minor key, and the blues scales over the three dominant seventh chords of a twelve-bar
 * blues (I7, IV7, V7), where the scale's ♭3 and ♭5 rub against the chords' major thirds and
 * fifths to make the blue notes.
 */
enum class HarmonicHome {
    /** The major key on the same root, whose seven chords the scale is played over. */
    MAJOR_KEY,

    /** The natural minor key on the same root. */
    MINOR_KEY,

    /** The twelve-bar blues on the same root: I7, IV7 and V7. */
    TWELVE_BAR_BLUES,
}

/** The key or progression a scale borrows its chords from, or null for a scale that is
 *  harmonised on its own notes. */
val ScaleType.harmonicHome: HarmonicHome?
    get() =
        when (this) {
            ScaleType.MAJOR_PENTATONIC -> HarmonicHome.MAJOR_KEY
            ScaleType.MINOR_PENTATONIC -> HarmonicHome.MINOR_KEY
            ScaleType.BLUES, ScaleType.MAJOR_BLUES -> HarmonicHome.TWELVE_BAR_BLUES
            else -> null
        }

/**
 * The built-in scale this one is, when it is one of them unchanged. A saved scale or an edited
 * one is harmonised on its own notes even when it happens to sound like a built-in scale,
 * because its chords are what the edit lesson talks about.
 */
val Scale.builtInType: ScaleType?
    get() = ScaleType.entries.firstOrNull { type -> type.definition == definition }

/** The chords shown under the board: the scale's own stacked chords, or, for a pentatonic or
 *  blues scale, the chords of its harmonic home (see [HarmonicHome]). */
fun Scale.boardChords(): List<ScaleChord> =
    when (builtInType?.harmonicHome) {
        null -> letterTriads()
        HarmonicHome.MAJOR_KEY -> Scale(root, ScaleType.MAJOR).letterTriads()
        HarmonicHome.MINOR_KEY -> Scale(root, ScaleType.NATURAL_MINOR).letterTriads()
        HarmonicHome.TWELVE_BAR_BLUES -> twelveBarBluesChords()
    }

/** Degrees of the dominant seventh chords of a twelve-bar blues, each read against the major
 *  scale of the key: I7 is 1 3 5 ♭7, IV7 is 4 6 1 ♭3, V7 is 5 7 2 4. */
private val twelveBarBluesFormulas =
    listOf(
        listOf("1", "3", "5", "♭7"),
        listOf("4", "6", "1", "♭3"),
        listOf("5", "7", "2", "4"),
    )

/** I7, IV7 and V7 on this scale's root, each spelled from its formula above the root. */
private fun Scale.twelveBarBluesChords(): List<ScaleChord> {
    val chords = mutableListOf<ScaleChord>()
    for (formula in twelveBarBluesFormulas) {
        val notes = mutableListOf<NoteName>()
        for (symbol in formula) {
            val degree = ScaleDegree.fromSymbol(symbol)
            notes.add(Scale.spell(degree, above = root))
        }
        val chord =
            ScaleChord(
                degree = ScaleDegree.fromSymbol(formula[0]),
                triad = Triad(notes[0], TriadQuality.MAJOR),
                notes = notes,
                isDominantSeventh = true,
            )
        chords.add(chord)
    }
    return chords
}

/** One line under the chords saying where they come from, for a scale that borrows them;
 *  null for a scale harmonised on its own notes. */
val Scale.harmonyNote: String?
    get() {
        val type = builtInType ?: return null
        val home = type.harmonicHome ?: return null
        val keyName = root.displayName
        return when (home) {
            HarmonicHome.MAJOR_KEY -> {
                "${type.displayName} is $keyName major without its 4 and 7, " +
                    "so it is played over $keyName major's chords."
            }

            HarmonicHome.MINOR_KEY -> {
                "${type.displayName} is $keyName minor without its 2 and ♭6, " +
                    "so it is played over $keyName minor's chords."
            }

            HarmonicHome.TWELVE_BAR_BLUES -> {
                "${type.displayName} is played over the three seventh chords of a " +
                    "twelve-bar blues in $keyName; the scale's notes outside them are the " +
                    "blue notes."
            }
        }
    }
