package com.alnourkhalifa.scales.app.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Scales tab's slot in RootView: the ported Scales screen, [ContentView]. The name stays
 * as PhoneActivity calls it, so the activity file is left to its owner.
 */
@Composable
fun ScalesTabStub(modifier: Modifier = Modifier) {
    ContentView(modifier = modifier)
}
