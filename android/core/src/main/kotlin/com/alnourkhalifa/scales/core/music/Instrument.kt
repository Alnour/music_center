package com.alnourkhalifa.scales.core.music

/**
 * Twin of `Scales/Music/Instrument.swift`: which instrument the board is drawn for.
 *
 * The guitar and the bass guitar get the fretboard; the keyboard gets piano octaves. The
 * choice only changes how the scale is pictured, so the scale, root and labels are shared.
 * [rawValue] is Swift's raw string, the value saved settings carry.
 */
enum class Instrument(
    val rawValue: String,
    val title: String,
    /**
     * Whether the board is a neck with strings and frets. The fret count, the string order
     * and the left-handed switch apply to both fretted instruments, so the screens ask this
     * rather than naming the guitar and the bass each time.
     */
    val isFretted: Boolean,
) {
    GUITAR(rawValue = "guitar", title = "Guitar", isFretted = true),
    BASS(rawValue = "bass", title = "Bass", isFretted = true),
    KEYBOARD(rawValue = "keyboard", title = "Keyboard", isFretted = false),
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
