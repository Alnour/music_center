package com.alnourkhalifa.scales.core.session

/**
 * What the circles on the board show: the note's name, its scale degree, or nothing. Twin of
 * the `NoteLabelStyle` enum inside `Scales/Session/PracticeSettings.swift`, split into its own
 * file because the board painters needed it before `PracticeSettings` was ported; the Kotlin
 * `PracticeSettings` refers to this one rather than declaring its own. [rawValue] is Swift's
 * raw string, the value saved settings carry, and Swift's `title` keeps its name.
 */
enum class NoteLabelStyle(
    val rawValue: String,
    /** The word on the segmented pill that picks the style. */
    val title: String,
    /**
     * What the circles on the board show, read by TalkBack (VoiceOver on iOS) as the board's
     * state, so a player who cannot see the board still hears what it holds.
     */
    val accessibilityDescription: String,
) {
    NOTE_NAMES(
        rawValue = "noteNames",
        title = "Notes",
        accessibilityDescription = "Circles show note names",
    ),
    DEGREES(
        rawValue = "degrees",
        title = "Degrees",
        accessibilityDescription = "Circles show scale degrees",
    ),
    NONE(
        rawValue = "none",
        title = "Dots",
        accessibilityDescription = "Plain circles",
    ),
    ;

    /** Decoding from the raw string, Swift's `init(rawValue:)`. */
    companion object {
        /** The style Swift would decode from this raw string, or null for another. */
        fun fromRawValue(rawValue: String): NoteLabelStyle? {
            for (style in entries) {
                if (style.rawValue == rawValue) {
                    return style
                }
            }
            return null
        }
    }
}
