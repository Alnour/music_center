package com.alnourkhalifa.scales.app.tv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Drums route's slot in `TvApp`: the drum trainer, full screen. `TvApp` calls this by
 * name, so the real screen (`TvDrumsScreen`) replaces the body here and nothing else changes.
 * Back is `TvApp`'s and returns to the panel.
 */
@Composable
fun TvDrumsTab(modifier: Modifier = Modifier) {
    TvPlaceholderScreen(title = "Drums", tag = "tvDrums", modifier = modifier)
}
