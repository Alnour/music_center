package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.session.SessionState
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.KeyboardView
import com.alnourkhalifa.scales.ui.MusicFontFamily
import com.alnourkhalifa.scales.ui.PhoneFretboard
import com.alnourkhalifa.scales.ui.ScaleNotesRow

/*
 * The Scales tab's slot in RootView; replaced by the ported ContentView in wave 3.
 */

/**
 * The saved board the way the iPhone lays it out: the scale's name, the notes row, then the
 * fretboard or keyboard, with the lower third left for the chord cards. Reads the session
 * snapshot, so any change made through the session redraws it.
 */
@Composable
fun ScalesTabStub(modifier: Modifier = Modifier) {
    val app = rememberScalesApplication()
    val state = app.sessionState.value
    val palette = FretboardPalette.matching(isSystemInDarkTheme())
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(palette.background)
                .safeDrawingPadding()
                .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.scale.title,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
            color = palette.title,
        )
        ScaleNotesRow(
            scale = state.scale,
            noteSize = 14.dp,
            palette = palette,
            highlightedNotes = state.ringedNotes,
        )
        SavedBoard(
            state = state,
            palette = palette,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(2f),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

/** The guitar neck or the keyboard, whichever the player picked, with every saved option. */
@Composable
private fun SavedBoard(
    state: SessionState,
    palette: FretboardPalette,
    modifier: Modifier,
) {
    val settings = state.settings
    val title = state.scale.title
    when (settings.selectedInstrument) {
        Instrument.GUITAR -> {
            PhoneFretboard(
                layout = state.layout,
                scaleTitle = title,
                labelStyle = settings.labelStyle,
                palette = palette,
                modifier = modifier,
                ringedNotes = state.ringedNotes,
                thinStringOnTop = settings.thinStringOnTop,
                isLeftHanded = settings.isLeftHanded,
            )
        }

        Instrument.KEYBOARD -> {
            KeyboardView(
                layout = state.keyboardLayout,
                scaleTitle = title,
                labelStyle = settings.labelStyle,
                palette = palette,
                modifier = modifier,
                ringedNotes = state.ringedNotes,
            )
        }
    }
}
