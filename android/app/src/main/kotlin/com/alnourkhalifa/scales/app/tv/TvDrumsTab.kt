package com.alnourkhalifa.scales.app.tv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Drums route's slot in `TvApp`: the drum trainer, full screen. The screen catches the
 * Back key itself (so a remote's Back closes its groove list first) and reports it through
 * [onBack], which `TvApp` turns into one step back to the panel.
 */
@Composable
fun TvDrumsTab(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvDrumsScreen(onBack = onBack, modifier = modifier)
}
