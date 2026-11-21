package com.alnourkhalifa.scales.app.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.SelectableSurfaceBorder
import androidx.tv.material3.SelectableSurfaceDefaults
import com.alnourkhalifa.scales.core.music.CapoChordShape
import com.alnourkhalifa.scales.core.music.ChordFingering
import com.alnourkhalifa.scales.ui.ChordChipColors
import com.alnourkhalifa.scales.ui.MusicFontFamily

// Pieces of the TV chord row kept beside `TvChordRow.kt` so that file stays within detekt's
// function and length limits: a chip's borders, its capo shape line, and the footnotes under
// the chips.

/** The capo shape line's font size in screen units, a step below the notes above it. */
private const val CAPO_SHAPE_UNITS = 2f

/**
 * A chip's edges: the ringed chip in the board's chord colour, a focused one in the text
 * colour, and a focused ringed one in a thicker chord-coloured edge, so focus and ring read
 * apart from the sofa.
 */
@Composable
internal fun tvChipBorders(
    colors: ChordChipColors,
    shape: RoundedCornerShape,
): SelectableSurfaceBorder {
    val ringBorder = Border(BorderStroke(2.dp, colors.ring), shape = shape)
    val focusBorder = Border(BorderStroke(2.dp, colors.primary), shape = shape)
    val focusedRingBorder = Border(BorderStroke(3.dp, colors.ring), shape = shape)
    return SelectableSurfaceDefaults.border(
        focusedBorder = focusBorder,
        pressedBorder = focusBorder,
        selectedBorder = ringBorder,
        focusedSelectedBorder = focusedRingBorder,
        pressedSelectedBorder = focusedRingBorder,
    )
}

/**
 * The shape to finger with the capo on ("G shape", "Bm barre"), small, under the notes; it
 * draws nothing without a capo. An open shape is in the board's chord green so the easy
 * chords stand out from the sofa, as on the phone; a barre or movable shape stays grey.
 */
@Composable
internal fun TvCapoShapeLine(
    shape: CapoChordShape?,
    colors: ChordChipColors,
    unit: Dp,
) {
    if (shape == null) {
        return
    }
    val isOpen = shape.fingering == ChordFingering.OPEN
    val color = if (isOpen) colors.ring else colors.secondary
    val style =
        unitStyle(unit, CAPO_SHAPE_UNITS).copy(
            color = color,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.SemiBold,
        )
    BasicText(
        text = shape.caption,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** One line of small grey text under the chips (the harmony note, the capo note). It takes
 *  no focus, so D-pad moves are unchanged. */
@Composable
internal fun TvChipFootnote(
    text: String,
    tag: String,
    colors: ChordChipColors,
    unit: Dp,
) {
    val noteStyle =
        unitStyle(unit, SMALL_TEXT_UNITS).copy(
            color = colors.secondary,
            fontFamily = MusicFontFamily,
        )
    BasicText(
        text = text,
        style = noteStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = unit * 0.8f).testTag(tag),
    )
}
