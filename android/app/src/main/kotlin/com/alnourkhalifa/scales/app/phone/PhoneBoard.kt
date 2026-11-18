package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.session.SessionState
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.KeyboardView
import com.alnourkhalifa.scales.ui.PhoneFretboard

/** The board's rounded corners, as on the iPhone. */
private val BOARD_CORNER_RADIUS = 12.dp

/**
 * The fretboard or the piano keyboard, whichever instrument the player chose, filling the
 * height the toolbar, notes row and chords leave it. Twin of `phoneBoard(palette:)` and the
 * private `PhoneKeyboard` in `Scales/Views/ContentView.swift`; the fretboard half is
 * `PhoneFretboard` in the ui module.
 *
 * The keyboard always fits the width, so it gets a 12 dp margin on the palette's colour and
 * the keys keep their shape inside it; the fretboard scrolls sideways instead, so it runs to
 * the board's edges. Both carry their spoken label and `board` test tag from the ui module
 * ("Fretboard showing G Major", state "Circles show note names").
 */
@Composable
fun PhoneBoard(
    state: SessionState,
    palette: FretboardPalette,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    val title = state.scale.title
    val boardModifier =
        modifier
            .clip(RoundedCornerShape(BOARD_CORNER_RADIUS))
            .background(palette.background)
    when (settings.selectedInstrument) {
        Instrument.GUITAR -> {
            PhoneFretboard(
                layout = state.layout,
                scaleTitle = title,
                labelStyle = settings.labelStyle,
                palette = palette,
                modifier = boardModifier,
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
                modifier =
                    boardModifier
                        .padding(12.dp)
                        .fillMaxSize(),
                ringedNotes = state.ringedNotes,
            )
        }
    }
}
