package com.alnourkhalifa.scales.ui

import androidx.compose.ui.graphics.Color

/**
 * Colours for the board and the note chips. Twin of `Scales/Views/FretboardPalette.swift`,
 * with the same members and hex values; Swift's `matching(_ colorScheme:)` takes `isDark`
 * here, because the ui module has no Material theme to read a colour scheme from. Swift's
 * `Color(hex:)` is Compose's own `Color(0xFFRRGGBB)`, so it has no twin.
 *
 * The light palette follows the reference chart: grey-blue circles with the roots in deep
 * navy. Navy disappears on a dark background, so the dark palette marks roots in gold.
 * Text colours were picked for at least 4.5:1 contrast against their circles.
 */
data class FretboardPalette(
    val background: Color,
    val fretWire: Color,
    val nut: Color,
    val string: Color,
    val toneFill: Color,
    val toneText: Color,
    val rootFill: Color,
    val rootText: Color,
    val fretNumber: Color,
    val inlay: Color,
    val title: Color,
    val secondaryText: Color,
    /**
     * Piano keys outside the scale keep their natural colours; keys in the scale take
     * [toneFill] or [rootFill], the same colours as the fretboard circles.
     */
    val whiteKey: Color,
    val blackKey: Color,
    val keyOutline: Color,
    /**
     * Notes of the chord the player picked. Green sits far from both the grey-blue scale
     * tones and the root (navy in light, gold in dark), so the three kinds never read alike;
     * the ring uses the same hue so a ringed root still says "chord".
     */
    val chordFill: Color,
    val chordText: Color,
    val chordRing: Color,
    /** The capo's bar: darker than the strings and fret wires it clamps, lighter than the
     *  note circles drawn over it. */
    val capo: Color,
) {
    /** The two palettes and the choice between them. */
    companion object {
        val light: FretboardPalette =
            FretboardPalette(
                background = Color(0xFFFFFFFF),
                fretWire = Color(0xFF8A8F9C),
                nut = Color(0xFF3A3F4B),
                string = Color(0xFF7A7F8C),
                toneFill = Color(0xFF8E9AB5),
                toneText = Color(0xFF1C2440),
                rootFill = Color(0xFF3D4A73),
                rootText = Color(0xFFFFFFFF),
                fretNumber = Color(0xFF5F6472),
                inlay = Color(0xFF2B2E36),
                title = Color(0xFF1C2440),
                secondaryText = Color(0xFF5B6275),
                whiteKey = Color(0xFFFAFAF7),
                blackKey = Color(0xFF2B2E36),
                keyOutline = Color(0xFF8A8F9C),
                chordFill = Color(0xFF2E9E6B),
                chordText = Color(0xFFFFFFFF),
                chordRing = Color(0xFF1F7A50),
                capo = Color(0xFF4A4F5C),
            )

        val dark: FretboardPalette =
            FretboardPalette(
                background = Color(0xFF12151C),
                fretWire = Color(0xFF4A5162),
                nut = Color(0xFFC9CED8),
                string = Color(0xFF6A7184),
                toneFill = Color(0xFF8E9AB5),
                toneText = Color(0xFF10141E),
                rootFill = Color(0xFFF2C14E),
                rootText = Color(0xFF1A1408),
                fretNumber = Color(0xFF9AA1B2),
                inlay = Color(0xFF9AA1B2),
                title = Color(0xFFF2F4F8),
                secondaryText = Color(0xFF9AA1B2),
                whiteKey = Color(0xFFE4E7EE),
                blackKey = Color(0xFF1C2029),
                keyOutline = Color(0xFF4A5162),
                chordFill = Color(0xFF4CD98F),
                chordText = Color(0xFF07261A),
                chordRing = Color(0xFF7CF0B4),
                capo = Color(0xFF5C6478),
            )

        /** The palette for a dark or a light screen. */
        fun matching(isDark: Boolean): FretboardPalette {
            if (isDark) {
                return dark
            }
            return light
        }
    }
}
