package com.alnourkhalifa.scales.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * The font every note name, chord chip and scale title uses, so ♭ ♯ ♮ 𝄪 𝄫 look the same on
 * every phone, tablet and TV.
 *
 * Why it is bundled: system fonts differ by device, and Fire OS 7 (the oldest system the app
 * supports) has no glyph for the double sharp 𝄪 or double flat 𝄫, so C𝄪 would show a tofu
 * box. The file holds ♭ ♮ ♯ from Noto Sans Symbols at weight 700 (their strokes match Roboto
 * Bold) and 𝄪 𝄫 from Noto Music, both SIL Open Font License 1.1 (text in assets/licenses),
 * about 2.7 KB each. `scripts/build_music_font.py` builds them: each glyph is lifted to where
 * San Francisco draws it beside a capital, and the font takes Roboto's vertical metrics so a
 * line holding one accidental is no taller than its neighbours.
 *
 * Why two files: Android chooses the fallback font for the borrowed letters by the declared
 * weight of the font it was asked for, so a single file flagged bold would set every "G B D"
 * in Roboto Bold. A regular and a bold file let each text keep its own weight.
 *
 * How the other characters render: Android picks a font per glyph. Letters, digits and
 * punctuation are missing from this subset, so they fall back to the system font, and only
 * the accidentals come from the bundled file.
 */
val MusicFontFamily =
    FontFamily(
        Font(R.font.accidentals_regular, FontWeight.Normal),
        Font(R.font.accidentals_bold, FontWeight.Bold),
    )
