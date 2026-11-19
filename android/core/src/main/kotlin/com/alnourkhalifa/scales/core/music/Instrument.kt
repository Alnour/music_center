package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/Instrument.swift`: which instrument the board is drawn for.
 *
 * The guitar gets the fretboard; the keyboard gets piano octaves. The choice only changes how
 * the scale is pictured, so the scale, root and labels are shared. [rawValue] is Swift's raw
 * string, the value saved settings carry.
 */
enum class Instrument(
    val rawValue: String,
    val title: String,
) {
    GUITAR(rawValue = "guitar", title = "Guitar"),
    KEYBOARD(rawValue = "keyboard", title = "Keyboard"),
    ;

    /** Decoding from the raw string, Swift's `init(rawValue:)`. */
    companion object {
        /** The instrument Swift would decode from this raw string, or null for another. */
        fun fromRawValue(rawValue: String): Instrument? {
            for (instrument in entries) {
                if (instrument.rawValue == rawValue) {
                    return instrument
                }
            }
            return null
        }
    }
}
