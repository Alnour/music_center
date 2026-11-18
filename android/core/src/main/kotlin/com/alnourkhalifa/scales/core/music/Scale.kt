package com.alnourkhalifa.scales.core.music

/** Twin of `ScaleTone` in `Scales/Music/Scale.swift`: one note of a scale, how it is spelled
 *  and which degree it plays. */
data class ScaleTone(
    val name: NoteName,
    val degree: ScaleDegree,
) {
    val isRoot: Boolean
        get() = degree.isRoot
}

/**
 * Twin of `Scale` in `Scales/Music/Scale.swift`: a concrete scale, such as G major, with every
 * tone spelled. Swift's `tone(forPitchClass:)` is `tone(pitchClass)` and
 * `spell(_:above:)` is `spell(degree, above)`.
 */
class Scale(
    val root: NoteName,
    /** The scale's name and formula, which may be built in or one the player saved. */
    val definition: ScaleDefinition,
) {
    /** Tones in ascending degree order, starting at the root. */
    val tones: List<ScaleTone>

    /** Answers "is this fretted pitch in the scale, and which tone is it?" in one lookup,
     *  which the fretboard asks for every string and fret. */
    private val toneByPitchClass: Map<Int, ScaleTone>

    init {
        val tones = mutableListOf<ScaleTone>()
        val toneByPitchClass = mutableMapOf<Int, ScaleTone>()
        for (degree in definition.degrees) {
            val name = spell(degree, above = root)
            val tone = ScaleTone(name, degree)
            tones.add(tone)
            toneByPitchClass[name.pitchClass] = tone
        }
        this.tones = tones
        this.toneByPitchClass = toneByPitchClass
    }

    constructor(root: NoteName, type: ScaleType) : this(root, type.definition)

    val title: String
        get() = "${root.displayName} ${definition.name}"

    fun tone(pitchClass: Int): ScaleTone? = toneByPitchClass[pitchClass]

    /** Swift's static members of `Scale`. */
    companion object {
        /** Spells a degree above a root: the degree number picks the letter (the 3rd of G is
         *  some kind of B), and the semitone distance decides how many sharps or flats that
         *  letter needs to reach the right pitch. */
        fun spell(
            degree: ScaleDegree,
            above: NoteName,
        ): NoteName {
            val letter = above.letter.advanced(by = degree.number - 1)
            val targetPitchClass = PitchMath.wrap(above.pitchClass + degree.semitonesAboveRoot)
            val accidental =
                PitchMath.signedDistance(from = letter.naturalPitchClass, to = targetPitchClass)
            return NoteName(letter, accidental)
        }
    }
}
