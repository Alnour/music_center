package com.alnourkhalifa.scales.app.tv

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * [content] in tv-material's dark colours, for tests that host one TV screen without
 * `TvApp`. The Chords and Drums screens take their theme from `TvApp`, which a test that
 * calls `setContent` with the screen alone does not include; tv-material's default scheme is
 * light, so without this the screens would be tested in colours the TV opens with only when
 * the player turns the dark TV theme off.
 */
@Composable
fun InDarkTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
