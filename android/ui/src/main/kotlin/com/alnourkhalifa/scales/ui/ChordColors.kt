package com.alnourkhalifa.scales.ui

import androidx.compose.ui.graphics.Color
import com.alnourkhalifa.scales.core.music.TriadQuality

/**
 * One colour per chord quality, shared by the key tables and the lessons so major, minor,
 * diminished and suspended read the same at a glance everywhere in the app. Twin of
 * `Scales/Views/ChordColors.swift`.
 *
 * What differs: SwiftUI's `.primary`, `.blue`, `.orange` and `.teal` adapt to light and dark
 * by themselves, and the ui module has no Material theme to do that. So the caller passes the
 * screen's text colour as `primary` (Material's `onSurface` on the phone, the TV theme's on
 * the TV) and says whether the screen is dark; the other three are Apple's system colours,
 * copied here for each appearance so the tables look the same on both platforms.
 */
object ChordColors {
    /** Apple's `systemBlue`, light and dark. */
    val blueLight: Color = Color(0xFF007AFF)
    val blueDark: Color = Color(0xFF0A84FF)

    /** Apple's `systemOrange`, light and dark. */
    val orangeLight: Color = Color(0xFFFF9500)
    val orangeDark: Color = Color(0xFFFF9F0A)

    /** Apple's `systemTeal`, light and dark. */
    val tealLight: Color = Color(0xFF30B0C7)
    val tealDark: Color = Color(0xFF40C8E0)

    /** Minor chords. */
    fun blue(isDark: Boolean): Color = if (isDark) blueDark else blueLight

    /** Diminished, augmented and the altered-fifth chords. */
    fun orange(isDark: Boolean): Color = if (isDark) orangeDark else orangeLight

    /** Suspended chords. */
    fun teal(isDark: Boolean): Color = if (isDark) tealDark else tealLight
}

/**
 * Swift's `TriadQuality.tint`. Major chords take the plain text colour, so the eye lands on
 * the coloured ones: minor in blue, the unstable fifths in orange, suspended in teal.
 */
fun TriadQuality.tint(
    primary: Color,
    isDark: Boolean,
): Color =
    when (this) {
        TriadQuality.MAJOR -> primary

        TriadQuality.MINOR -> ChordColors.blue(isDark)

        TriadQuality.DIMINISHED,
        TriadQuality.AUGMENTED,
        TriadQuality.MAJOR_FLAT_FIVE,
        TriadQuality.MINOR_SHARP_FIVE,
        -> ChordColors.orange(isDark)

        TriadQuality.SUSPENDED_FOURTH,
        TriadQuality.SUSPENDED_SECOND,
        TriadQuality.SUSPENDED_FOURTH_FLAT_FIVE,
        TriadQuality.SUSPENDED_SECOND_FLAT_FIVE,
        TriadQuality.SUSPENDED_FOURTH_SHARP_FIVE,
        TriadQuality.SUSPENDED_SECOND_SHARP_FIVE,
        -> ChordColors.teal(isDark)
    }
