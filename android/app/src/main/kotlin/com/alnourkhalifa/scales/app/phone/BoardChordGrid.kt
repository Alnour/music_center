package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.core.board.BoardChordChipContent
import com.alnourkhalifa.scales.core.music.ScaleChord
import com.alnourkhalifa.scales.core.music.harmonyNote
import com.alnourkhalifa.scales.core.session.SessionState
import com.alnourkhalifa.scales.ui.ChordChipColors
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.ScaleChordsRow

/**
 * The largest share of the screen the chord cards may take, so the board always keeps most
 * of the height even when a changed scale lists every chord it had and has. Swift's
 * `chordsHeightShare`.
 */
const val CHORDS_HEIGHT_SHARE: Float = 0.34f

/**
 * The chord cards under the board on the Scales screen, and the line under them saying where a
 * pentatonic or blues scale's chords come from. Twin of the `ScrollView` holding
 * `ScaleChordsRow` in `Scales/Views/ContentView.swift`.
 *
 * The grid is as tall as its cards up to [CHORDS_HEIGHT_SHARE] of [screenHeight], and
 * scrolls inside that when a changed scale lists more chords than fit; it is a lazy grid, so
 * it must never sit inside another vertical scroller. Wide windows get roomier cards (120 dp
 * against 84 dp), so a tablet shows all seven chords of a major scale in one row.
 *
 * @param screenHeight the height of the whole screen below the title bar, as Swift's
 *   `GeometryReader` measures it.
 */
@Composable
fun BoardChordGrid(
    state: SessionState,
    screenHeight: Dp,
    isWide: Boolean,
    onTap: (ScaleChord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = state.scale
    val change = state.currentBoardChange
    // Keyed on what the cards depend on, so ringing a chord or flipping a pill does not
    // work the chords out again.
    val root = state.settings.root
    val definition = state.settings.scaleDefinition
    val chips = remember(root, definition, change) { BoardChordChipContent.chips(scale, change) }
    // The line saying where borrowed chords come from shows only while no change is marked
    // on the cards, as on iOS.
    var harmonyNote: String? = null
    if (change == null) {
        harmonyNote = scale.harmonyNote
    }
    val palette = FretboardPalette.matching(isSystemInDarkTheme())
    val colors = phoneChordChipColors(palette)
    val cardMinimumWidth = if (isWide) 120.dp else 84.dp
    val maximumHeight = screenHeight * CHORDS_HEIGHT_SHARE
    ScaleChordsRow(
        chips = chips,
        ringedChord = state.ringedChord,
        onTap = onTap,
        colors = colors,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = maximumHeight),
        cardMinimumWidth = cardMinimumWidth,
        harmonyNote = harmonyNote,
    )
}

/**
 * The chord cards' colours on the phone: Apple's system colours from the ui module, with the
 * theme's text colours and accent, and the ring in the board's chord colour. Shared by the
 * change row above the cards, so its sentence and button match.
 */
@Composable
fun phoneChordChipColors(palette: FretboardPalette): ChordChipColors {
    val scheme = MaterialTheme.colorScheme
    return ChordChipColors.matching(
        isDark = isSystemInDarkTheme(),
        primary = scheme.onSurface,
        secondary = scheme.onSurfaceVariant,
        accent = scheme.primary,
        palette = palette,
    )
}
