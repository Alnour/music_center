package com.alnourkhalifa.scales.core.music

import kotlin.math.abs

/**
 * Twin of `ChordFingering` in `Scales/Music/Capo.swift`: how the hand plays a chord shape.
 * [rawValue] is Swift's raw string, which the answers file records.
 */
enum class ChordFingering(
    val rawValue: String,
) {
    /** One of the first chords every guitarist learns, with strings left ringing open (or,
     *  with a capo on, ringing at the capo). */
    OPEN("open"),

    /** A major, minor, seventh or suspended chord with no open shape: the first finger lies
     *  across the strings. */
    BARRE("barre"),

    /** A diminished, augmented or altered chord, played as a small movable shape. */
    MOVABLE("movable"),
}

/**
 * Twin of `CapoChordShape` in `Scales/Music/Capo.swift`: the chord a guitarist fingers,
 * counted from the capo, to sound one of the scale's chords. With a capo on fret 3, B♭ is
 * fingered as G.
 */
data class CapoChordShape(
    val triad: Triad,
    val isDominantSeventh: Boolean,
) {
    /** The shape's name as chord charts print it: G, Am, B7. */
    val name: String
        get() {
            val seventhSuffix = if (isDominantSeventh) "7" else ""
            return triad.name + seventhSuffix
        }

    /** "G major", "B dominant seventh", for TalkBack. */
    val spokenName: String
        get() {
            if (isDominantSeventh) {
                return "${triad.root.displayName} dominant seventh"
            }
            return triad.spokenName
        }

    val fingering: ChordFingering
        get() {
            if (isOpenShape(triad, isDominantSeventh)) {
                return ChordFingering.OPEN
            }
            if (isDominantSeventh || triad.quality in barreQualities) {
                return ChordFingering.BARRE
            }
            return ChordFingering.MOVABLE
        }

    /** The line under the chord's name on a card: "G shape" for an open chord, "Bm barre"
     *  when the shape needs a barre, so the player sees at a glance which chords are easy. */
    val caption: String
        get() =
            when (fingering) {
                ChordFingering.OPEN, ChordFingering.MOVABLE -> "$name shape"
                ChordFingering.BARRE -> "$name barre"
            }

    /** The same, as a sentence for TalkBack: "Fingered as G major, an open chord." */
    val spokenSentence: String
        get() =
            when (fingering) {
                ChordFingering.OPEN -> "Fingered as $spokenName, an open chord."
                ChordFingering.BARRE -> "Fingered as $spokenName, a barre chord."
                ChordFingering.MOVABLE -> "Fingered as $spokenName."
            }

    /** Swift's private static tables of open shapes. */
    private companion object {
        // Pitch classes of the roots that have an open shape, per kind of chord. Roots are
        // compared by pitch rather than by spelling, so a shape spelled either way counts.
        val openMajorRoots: Set<Int> = setOf(0, 2, 4, 7, 9)
        val openMinorRoots: Set<Int> = setOf(2, 4, 9)
        val openSeventhRoots: Set<Int> = setOf(0, 2, 4, 7, 9, 11)
        val openSuspendedFourthRoots: Set<Int> = setOf(2, 4, 9)
        val openSuspendedSecondRoots: Set<Int> = setOf(2, 9)

        /** The chord kinds played as barre chords when they have no open shape. */
        val barreQualities: Set<TriadQuality> =
            setOf(
                TriadQuality.MAJOR,
                TriadQuality.MINOR,
                TriadQuality.SUSPENDED_FOURTH,
                TriadQuality.SUSPENDED_SECOND,
            )

        /** The open chords of the beginner books: C A G E D, Am Em Dm, the open sevenths (A7
         *  B7 C7 D7 E7 G7) and the open suspended chords. F is left out because its usual
         *  shape is a small barre. */
        fun isOpenShape(
            triad: Triad,
            isDominantSeventh: Boolean,
        ): Boolean {
            val rootPitchClass = triad.root.pitchClass
            if (isDominantSeventh) {
                return rootPitchClass in openSeventhRoots
            }
            return when (triad.quality) {
                TriadQuality.MAJOR -> rootPitchClass in openMajorRoots
                TriadQuality.MINOR -> rootPitchClass in openMinorRoots
                TriadQuality.SUSPENDED_FOURTH -> rootPitchClass in openSuspendedFourthRoots
                TriadQuality.SUSPENDED_SECOND -> rootPitchClass in openSuspendedSecondRoots
                else -> false
            }
        }
    }
}

/**
 * Twin of `CapoTransposition` in `Scales/Music/Capo.swift`: turns the chords a scale sounds
 * into the shapes fingered with a capo on.
 *
 * A capo on fret 3 raises every shape three semitones, so the shape for a chord is that chord
 * three semitones lower. The whole key moves together: the shapes are spelled as the lower
 * key spells them (B♭ major with a capo on 3 is fingered as G major: G, Am, Bm, C, D, Em,
 * F♯°), which is why one interval, a letter count and a semitone count, is worked out from
 * the scale's root and then applied to every chord.
 *
 * Differs from Swift: the failable `init?(scale:capoFret:tuning:)` is [create].
 */
class CapoTransposition(
    scale: Scale,
    /** The fret the capo is on; 0 for no capo. */
    val capoFret: Int,
    /** How many semitones below its sound each shape is named. The capo's fret, less one when
     *  the guitar is tuned a half step down. */
    val semitonesDown: Int,
) {
    /** The root of the key the shapes belong to: G for B♭ major with a capo on 3. */
    val shapeRoot: NoteName = shapeRoot(scale, semitonesDown)

    /** The name of the scale on the board, to say which key's chords are fingered. */
    private val scaleName: String = scale.definition.name

    /** Letters from a sounding note down to its shape: B♭ to G is two letters. */
    private val letterStepsDown: Int

    init {
        val letterCount = Letter.entries.size
        val letterDistance = scale.root.letter.rawValue - shapeRoot.letter.rawValue
        val wrappedDistance = letterDistance % letterCount + letterCount
        letterStepsDown = wrappedDistance % letterCount
    }

    /** The key the shapes belong to: "G Major". */
    val shapeScaleTitle: String
        get() = "${shapeRoot.displayName} $scaleName"

    /** One line under the chord cards saying what their shape lines mean. */
    val note: String
        get() = "Capo on fret $capoFret: finger the chords as in $shapeScaleTitle."

    /** A sounding note moved down to the note the hand plays, keeping the interval from the
     *  key's root so every shape is spelled within the same key. */
    fun shapeNote(sounding: NoteName): NoteName {
        val letter = sounding.letter.advanced(by = -letterStepsDown)
        val targetPitchClass = PitchMath.wrap(sounding.pitchClass - semitonesDown)
        val accidental =
            PitchMath.signedDistance(
                from = letter.naturalPitchClass,
                to = targetPitchClass,
            )
        return NoteName(letter, accidental)
    }

    fun shape(chord: ScaleChord): CapoChordShape {
        val shapeTriadRoot = shapeNote(chord.triad.root)
        val shapeTriad = Triad(shapeTriadRoot, chord.triad.quality)
        return CapoChordShape(shapeTriad, chord.isDominantSeventh)
    }

    /** Swift's failable initializer and the private static `shapeRoot(for:semitonesDown:)`. */
    companion object {
        /** Null when there is nothing to rename: no capo, or a tuning whose chord shapes are
         *  not the standard ones (see [Tuning.standardShapeOffset]). */
        fun create(
            scale: Scale,
            capoFret: Int,
            tuning: Tuning,
        ): CapoTransposition? {
            if (capoFret <= 0) {
                return null
            }
            val offset = tuning.standardShapeOffset ?: return null
            return CapoTransposition(scale, capoFret, semitonesDown = capoFret + offset)
        }

        /** The root the key takes once it is moved down. A black key has two spellings (C♯
         *  and D♭); the one whose scale needs fewer sharps and flats wins, as a musician
         *  would name it: D major one semitone down is fingered as D♭ major, not C♯ major. */
        private fun shapeRoot(
            scale: Scale,
            semitonesDown: Int,
        ): NoteName {
            val targetPitchClass = PitchMath.wrap(scale.root.pitchClass - semitonesDown)
            if (targetPitchClass == scale.root.pitchClass) {
                return scale.root
            }
            var best = scale.root
            var bestAccidentalCount = Int.MAX_VALUE
            for (candidate in NoteName.rootChoices) {
                if (candidate.pitchClass != targetPitchClass) {
                    continue
                }
                val candidateScale = Scale(candidate, scale.definition)
                var accidentalCount = 0
                for (tone in candidateScale.tones) {
                    accidentalCount += abs(tone.name.accidental)
                }
                if (accidentalCount < bestAccidentalCount) {
                    bestAccidentalCount = accidentalCount
                    best = candidate
                }
            }
            return best
        }
    }
}

/**
 * Twin of `CapoSuggestion` in `Scales/Music/Capo.swift`: one row of the capo picker, a fret,
 * the key its shapes come from, and how many of the scale's chords become open chords there.
 */
data class CapoSuggestion(
    /** 0 for no capo. */
    val capoFret: Int,
    /** The key the chords are fingered in: "G Major". */
    val shapeScaleTitle: String,
    val openChordCount: Int,
    val chordCount: Int,
) {
    /** "No capo" or "Fret 3". */
    val title: String
        get() = if (capoFret == 0) "No capo" else "Fret $capoFret"

    /** "as G Major · 5 of 7 open". With no capo the key is the one on the board, so only the
     *  count is given. */
    val detail: String
        get() {
            val openCount = "$openChordCount of $chordCount open"
            if (capoFret == 0) {
                return openCount
            }
            return "as $shapeScaleTitle · $openCount"
        }

    /** The whole row as one line, for a menu item. */
    val label: String
        get() = "$title · $detail"
}

/** Twin of `Capo` in `Scales/Music/Capo.swift`: where the capo may sit and which fret suits
 *  the scale on the board. */
object Capo {
    /** Past the ninth fret the neck left to play on is too short to be useful. */
    private const val HIGHEST_CAPO_FRET = 9

    /** Frets offered in the picker; 0 is no capo. */
    val fretChoices: List<Int> = (0..HIGHEST_CAPO_FRET).toList()

    /** What the capo picker shows when the tuning's chord shapes are not the standard ones.
     *  A plain val so it keeps Swift's camelCase name, as `FretboardGeometry`'s shares do. */
    @Suppress("MayBeConstant")
    val unsupportedTuningAdvice: String =
        "Chord shapes are named for standard tuning, so this tuning shows the capo only."

    /**
     * One suggestion per fret choice, in fret order: the players' question is "where do I put
     * the capo to play this key with easy chords?", and the open chord count per fret answers
     * it. Empty for a tuning whose shapes are not the standard ones.
     */
    fun suggestions(
        scale: Scale,
        tuning: Tuning,
    ): List<CapoSuggestion> {
        val offset = tuning.standardShapeOffset ?: return emptyList()
        val chords = scale.boardChords()
        val suggestions = mutableListOf<CapoSuggestion>()
        for (fret in fretChoices) {
            val transposition = CapoTransposition(scale, fret, semitonesDown = fret + offset)
            var openChordCount = 0
            for (chord in chords) {
                if (transposition.shape(chord).fingering == ChordFingering.OPEN) {
                    openChordCount += 1
                }
            }
            val suggestion =
                CapoSuggestion(
                    capoFret = fret,
                    shapeScaleTitle = transposition.shapeScaleTitle,
                    openChordCount = openChordCount,
                    chordCount = chords.size,
                )
            suggestions.add(suggestion)
        }
        return suggestions
    }

    /** One sentence naming the frets that give the most open chords: "Most open chords: capo
     *  on fret 3 (as C Major) or fret 8 (as G Major)." */
    fun advice(
        scale: Scale,
        tuning: Tuning,
    ): String {
        val suggestions = suggestions(scale, tuning)
        val noCapo = suggestions.firstOrNull() ?: return unsupportedTuningAdvice
        var bestCount = 0
        for (suggestion in suggestions) {
            if (suggestion.capoFret > 0) {
                bestCount = maxOf(bestCount, suggestion.openChordCount)
            }
        }
        if (bestCount <= noCapo.openChordCount) {
            return "No capo already gives the most open chords."
        }
        val bestFrets = mutableListOf<String>()
        for (suggestion in suggestions) {
            val isBest = suggestion.capoFret > 0 && suggestion.openChordCount == bestCount
            if (isBest) {
                bestFrets.add("fret ${suggestion.capoFret} (as ${suggestion.shapeScaleTitle})")
            }
        }
        val frets = bestFrets.joinToString(" or ")
        return "Most open chords: capo on $frets."
    }
}
