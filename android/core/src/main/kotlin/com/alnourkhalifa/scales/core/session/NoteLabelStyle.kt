package com.alnourkhalifa.scales.core.session

/**
 * Twin of `NoteLabelStyle` in `Scales/Session/PracticeSettings.swift`: what goes inside each
 * note circle. [rawValue] is Swift's raw string, the value saved settings carry.
 */
enum class NoteLabelStyle(
    val rawValue: String,
    val title: String,
    /** What the circles on the board show, read by TalkBack as the board's value, so a player
     *  who cannot see the board still hears what it holds. */
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
