package com.alnourkhalifa.scales.app.tv

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/**
 * The Chord tables route's slot in `TvApp`: the major and minor key tables, full screen.
 * `TvApp` calls this by name, so the real screen (`TvChordsScreen`) replaces the body here
 * and nothing else changes. Back is `TvApp`'s and returns to the panel.
 */
@Composable
fun TvChordsTab(modifier: Modifier = Modifier) {
    TvPlaceholderScreen(title = "Chord tables", tag = "tvChords", modifier = modifier)
}

/**
 * A full-screen stand-in for a screen not ported yet. It holds focus, so the remote still
 * has somewhere to be and Back still reaches the activity.
 */
@Composable
internal fun TvPlaceholderScreen(
    title: String,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        requestFocusOnceLaidOut(requester)
    }
    Surface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(tag)
                    .focusRequester(requester)
                    .focusable(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
