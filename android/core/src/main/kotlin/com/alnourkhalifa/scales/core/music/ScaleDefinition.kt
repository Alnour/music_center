package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/ScaleDefinition.swift`: what makes a scale that scale, whatever key it
 * is played in, a name and its degrees.
 *
 * Built-in scales and the player's own scales are both described this way, so the fretboard,
 * the chord builder and the lessons treat them alike. A saved scale keeps only its definition;
 * the key it is shown in comes from the root the player picks.
 *
 * Differs from Swift: a plain class rather than a data class, because the degrees are sorted
 * on the way in and a data class constructor cannot do that; [equals], [hashCode] and [copy]
 * are written out to behave like a data class. Swift's mutable `name` becomes
 * `copy(name = …)`, and `hasSamePitches(as:)` takes `other` since `as` is a Kotlin keyword.
 */
class ScaleDefinition(
    val name: String,
    degrees: List<ScaleDegree>,
) {
    /** In letter order, starting with the root, with no two degrees on the same pitch. */
    val degrees: List<ScaleDegree> = degrees.sorted()

    /** The formula as musicians write it: "1 2 ♭3 4 5 6 ♭7". */
    val formula: String
        get() {
            val labels = mutableListOf<String>()
            for (degree in degrees) {
                labels.add(degree.label)
            }
            return labels.joinToString(" ")
        }

    /** True when both scales sound the same: they hold the same pitches above the root, even
     *  if one spells a note differently (♯4 against ♭5) or carries another name. */
    fun hasSamePitches(other: ScaleDefinition): Boolean =
        pitchClassesAboveRoot() == other.pitchClassesAboveRoot()

    private fun pitchClassesAboveRoot(): Set<Int> {
        val pitchClasses = mutableSetOf<Int>()
        for (degree in degrees) {
            pitchClasses.add(degree.pitchClassAboveRoot)
        }
        return pitchClasses
    }

    fun copy(
        name: String = this.name,
        degrees: List<ScaleDegree> = this.degrees,
    ): ScaleDefinition = ScaleDefinition(name, degrees)

    override fun equals(other: Any?): Boolean {
        if (other !is ScaleDefinition) {
            return false
        }
        return name == other.name && degrees == other.degrees
    }

    override fun hashCode(): Int {
        val nameHash = name.hashCode()
        return HASH_MULTIPLIER * nameHash + degrees.hashCode()
    }

    override fun toString(): String = "ScaleDefinition(name=$name, formula=$formula)"

    private companion object {
        /** The multiplier a Kotlin data class uses to combine field hashes. */
        const val HASH_MULTIPLIER = 31
    }
}

/** The built-in scale as a definition, named the way the scale picker names it. */
val ScaleType.definition: ScaleDefinition
    get() = ScaleDefinition(displayName, degrees)
