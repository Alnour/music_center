package com.alnourkhalifa.scales.app.tv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Drums route's slot in `TvApp`: the drum trainer, full screen. Back is `TvApp`'s and
 * returns to the panel; the screen only closes its groove list itself.
 */
@Composable
fun TvDrumsTab(modifier: Modifier = Modifier) {
    TvDrumsScreen(modifier = modifier)
}
