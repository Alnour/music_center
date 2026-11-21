package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
private val ONE_ROW_MINIMUM_WIDTH = 680.dp

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
 * The pills on their own line under a narrow phone's menus. Three instruments and three label
 * styles are wider than a phone when every segment is as wide as the longest word, so here
 * the pills and their segments share the width by the length of their words
 * ([wordWeight]), as the iPhone's folded toolbar does.
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
        var instrumentShare = 0f
        for (instrument in Instrument.entries) {
            instrumentShare += wordWeight(instrument.title)
        }
        var labelShare = 0f
        for (style in NoteLabelStyle.entries) {
            labelShare += wordWeight(style.title)
        }
        InstrumentPill(
            state = state,
            session = session,
            modifier = Modifier.weight(instrumentShare),
            sharesWidthByWord = true,
        )
        LabelPill(
            state = state,
            session = session,
            modifier = Modifier.weight(labelShare),
            sharesWidthByWord = true,
        )
    }
}

/** The letters' worth of width a segment's padding takes, added to every word's length. */
private const val SEGMENT_PADDING_IN_LETTERS = 2f

/**
 * The share of a pill's width a segment with this word gets: its letters plus its padding.
 * Counting letters stands in for measuring the text, which is close enough for six short
 * words and keeps the row a plain weighted row.
 */
private fun wordWeight(title: String): Float = title.length + SEGMENT_PADDING_IN_LETTERS

/** Every segment equally wide, the control's own default. */
private const val EQUAL_SEGMENT_WEIGHT = 1f

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

/** Guitar | Bass | Keyboard. */
@Composable
private fun InstrumentPill(
    state: SessionState,
    session: PracticeSession,
    modifier: Modifier = Modifier,
    sharesWidthByWord: Boolean = false,
) {
    IosSegmentedControl(
        options = Instrument.entries,
        selected = state.settings.selectedInstrument,
        label = { instrument -> instrument.title },
        onSelect = { instrument ->
            session.updateSettings { settings -> settings.withSelectedInstrument(instrument) }
        },
        modifier = modifier.testTag("instrumentPill"),
        segmentWeight = { instrument ->
            if (sharesWidthByWord) wordWeight(instrument.title) else EQUAL_SEGMENT_WEIGHT
        },
    )
}

/** Notes | Degrees | Dots: what the circles on the board show. */
@Composable
private fun LabelPill(
    state: SessionState,
    session: PracticeSession,
    modifier: Modifier = Modifier,
    sharesWidthByWord: Boolean = false,
) {
    IosSegmentedControl(
        options = NoteLabelStyle.entries,
        selected = state.settings.labelStyle,
        label = { style -> style.title },
        onSelect = { style ->
            session.updateSettings { settings -> settings.copy(labelStyle = style) }
        },
        modifier = modifier.testTag("labelPill"),
        segmentWeight = { style ->
            if (sharesWidthByWord) wordWeight(style.title) else EQUAL_SEGMENT_WEIGHT
        },
    )
}
