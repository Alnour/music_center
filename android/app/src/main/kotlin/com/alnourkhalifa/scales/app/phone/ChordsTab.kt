package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** The Chords tab's slot in RootView; replaced by the ported screen in wave 3. */
@Composable
fun ChordsTabStub(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Chords", style = MaterialTheme.typography.headlineMedium)
    }
}
