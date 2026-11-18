package com.alnourkhalifa.scales.core.music

/** Twin of `ScaleMatch` in `Scales/Music/ScaleIdentifier.swift`: a scale the app already
 *  knows by name. */
sealed class ScaleMatch {
    /** One of the app's own scales. */
    data class BuiltIn(
        val type: ScaleType,
    ) : ScaleMatch()

    /** One of the player's saved scales. */
    data class Custom(
        val scale: CustomScale,
    ) : ScaleMatch()

    val name: String
        get() =
            when (this) {
                is BuiltIn -> type.displayName
                is Custom -> scale.definition.name
            }

    /** Where a lesson that ends on this scale should be filed. */
    val reference: ScaleReference
        get() =
            when (this) {
                is BuiltIn -> ScaleReference.BuiltIn(type)
                is Custom -> ScaleReference.Custom(scale.id)
            }
}

/** Twin of `ScaleIdentifier` in `Scales/Music/ScaleIdentifier.swift`: names a scale from its
 *  sound, so an edit that lands on a known scale is called by name. Swift's `among:` label is
 *  the `customScales` parameter. */
object ScaleIdentifier {
    /**
     * The known scale holding the same pitches above the root, or null for a new scale.
     *
     * Built-in scales are checked first in picker order, so the major scale is called Major
     * rather than Ionian and the natural minor Natural Minor rather than Aeolian. The player's
     * saved scales come after. Pitches are compared rather than spellings, so a scale written
     * with ♯4 is still recognised as one written with ♭5.
     */
    fun match(
        definition: ScaleDefinition,
        customScales: List<CustomScale>,
    ): ScaleMatch? {
        for (family in ScaleFamily.entries) {
            for (type in family.scales) {
                if (type.definition.hasSamePitches(definition)) {
                    return ScaleMatch.BuiltIn(type)
                }
            }
        }
        for (customScale in customScales) {
            if (customScale.definition.hasSamePitches(definition)) {
                return ScaleMatch.Custom(customScale)
            }
        }
        return null
    }
}
