package com.alnourkhalifa.scales.app.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Drums tab's slot in RootView, now filled by the drum trainer ([DrumTrainerView], twin
 * of `Scales/Views/DrumTrainerView.swift`). The slot keeps the name `PhoneActivity` calls so
 * each tab's screen lands in its own file without touching the activity.
 */
@Composable
fun DrumsTabStub(modifier: Modifier = Modifier) {
    DrumTrainerView(modifier = modifier)
}
