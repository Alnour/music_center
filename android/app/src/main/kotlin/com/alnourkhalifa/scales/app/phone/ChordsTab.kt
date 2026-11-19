package com.alnourkhalifa.scales.app.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Chords tab's slot in RootView: the key chord tables. The slot keeps its wave-2 name
 * because `PhoneActivity` wires the tabs by these names; the screen itself is
 * [ChordTablesScreen], twin of `ChordTablesView` in `Scales/Views/ChordTablesView.swift`.
 */
@Composable
fun ChordsTabStub(modifier: Modifier = Modifier) {
    ChordTablesScreen(modifier = modifier)
}
