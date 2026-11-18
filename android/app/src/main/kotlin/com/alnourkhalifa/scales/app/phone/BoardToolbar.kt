package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.session.NoteLabelStyle
import com.alnourkhalifa.scales.core.session.PracticeSession
import com.alnourkhalifa.scales.core.session.SessionState

/**
 * The narrowest toolbar that keeps everything on one line: menus, both pills and the wand.
 * It grows with the system font size, since the labels do, so large text folds the row
 * rather than squeezing the pills.
 */
private val ONE_ROW_MINIMUM_WIDTH = 600.dp

/**
 * One slim row above the board with everything a player changes while practising: the key
 * and scale as menus, the instrument and the labels as segmented pills, and the note-change
 * menu. On a narrow phone the row folds into two lines, menus and wand above, pills below.
 * Twin of `BoardToolbar` in `Scales/Views/ContentView.swift`.
 *
 * What differs: SwiftUI's `ViewThatFits` tries the one-line row and falls back when it does
 * not fit; Compose cannot measure a layout twice to compare, so the fold happens below a
 * fixed width ([ONE_ROW_MINIMUM_WIDTH] times the font scale) that the one-line row needs.
 * Actions go straight to [session]; a note change goes through [onEdit] because saving it
 * can fail and the screen shows the error.
 */
@Composable
fun BoardToolbar(
    state: SessionState,
    customScales: List<CustomScale>,
    session: PracticeSession,
    onEdit: (ScaleEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fitsOneRow = maxWidth >= ONE_ROW_MINIMUM_WIDTH * fontScale
        if (fitsOneRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScaleMenus(state, customScales, session)
                Spacer(modifier = Modifier.weight(1f))
                InstrumentPill(state, session)
                LabelPill(state, session)
                ChangeNoteMenu(scale = state.scale, onPick = onEdit)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScaleMenus(state, customScales, session)
                    Spacer(modifier = Modifier.weight(1f))
                    ChangeNoteMenu(scale = state.scale, onPick = onEdit)
                }
                FoldedPills(state, session)
            }
        }
    }
}

/**
 * The pills on their own line under a narrow phone's menus. They share the width by their
 * segment counts (two for the instrument, three for the labels), so every segment is the
 * same width, as on the iPhone.
 */
@Composable
private fun FoldedPills(
    state: SessionState,
    session: PracticeSession,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val instrumentShare =
            Instrument.entries.size
                .toFloat()
        val labelShare =
            NoteLabelStyle.entries.size
                .toFloat()
        InstrumentPill(state, session, Modifier.weight(instrumentShare))
        LabelPill(state, session, Modifier.weight(labelShare))
    }
}

/** The Root and Scale menus side by side. */
@Composable
private fun ScaleMenus(
    state: SessionState,
    customScales: List<CustomScale>,
    session: PracticeSession,
) {
    val settings = state.settings
    Row(verticalAlignment = Alignment.CenterVertically) {
        RootMenu(
            root = settings.root,
            onChoose = { root -> session.choose(settings.scaleReference, root) },
        )
        // The board's scale goes through the session so a saved scale is copied in from
        // the library, as Swift's `scaleSelection` binding does.
        ScaleMenu(
            selection = settings.scaleReference,
            selectionName = settings.scaleDefinition.name,
            customScales = customScales,
            onChoose = { reference -> session.choose(reference) },
        )
    }
}

/** Guitar | Keyboard. */
@Composable
private fun InstrumentPill(
    state: SessionState,
    session: PracticeSession,
    modifier: Modifier = Modifier,
) {
    SegmentedPill(
        options = Instrument.entries,
        selected = state.settings.selectedInstrument,
        title = { instrument -> instrument.title },
        onSelect = { instrument ->
            session.updateSettings { settings -> settings.withSelectedInstrument(instrument) }
        },
        modifier = modifier.testTag("instrumentPill"),
    )
}

/** Notes | Degrees | Dots: what the circles on the board show. */
@Composable
private fun LabelPill(
    state: SessionState,
    session: PracticeSession,
    modifier: Modifier = Modifier,
) {
    SegmentedPill(
        options = NoteLabelStyle.entries,
        selected = state.settings.labelStyle,
        title = { style -> style.title },
        onSelect = { style ->
            session.updateSettings { settings -> settings.copy(labelStyle = style) }
        },
        modifier = modifier.testTag("labelPill"),
    )
}

/** The height of a segmented pill: iOS's small segmented control, a little taller to tap. */
private val PILL_HEIGHT: Dp = 36.dp

/**
 * A segmented control for one choice out of a few, the Material twin of SwiftUI's
 * `.pickerStyle(.segmented)`. Shared by the toolbar and the board settings (frets, octaves).
 *
 * The selected segment shows no tick: Material's tick widens the segment it lands on, which
 * would shift the labels each time the choice changes, and iOS marks the choice by its fill
 * alone. Each segment is a radio button to TalkBack, and the selected one reads as selected.
 */
@Composable
internal fun <T> SegmentedPill(
    options: List<T>,
    selected: T,
    title: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = pillColors()
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        for ((index, option) in options.withIndex()) {
            val shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = shape,
                modifier = Modifier.height(PILL_HEIGHT),
                colors = colors,
                contentPadding = PaddingValues(horizontal = 8.dp),
                icon = {},
                label = { Text(text = title(option), maxLines = 1, softWrap = false) },
            )
        }
    }
}

/** The selected segment in the grey fill iOS gives its thumb, the rest on the screen colour. */
@Composable
private fun pillColors(): SegmentedButtonColors {
    val scheme = MaterialTheme.colorScheme
    return SegmentedButtonDefaults.colors(
        activeContainerColor = scheme.secondaryContainer,
        activeContentColor = scheme.onSurface,
        activeBorderColor = scheme.outline,
        inactiveContainerColor = scheme.surface,
        inactiveContentColor = scheme.onSurface,
        inactiveBorderColor = scheme.outline,
    )
}
