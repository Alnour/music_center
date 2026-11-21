package com.alnourkhalifa.scales.app.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.alnourkhalifa.scales.app.plainCapoTitle
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.session.PracticeSettings
import com.alnourkhalifa.scales.ui.MusicFontFamily

/** The share of the screen's width the panel takes, leaving most of the board in view. */
private const val PANEL_WIDTH_SHARE = 0.38f

/**
 * The panel's rows, in the order the panel lists them. A row tied to some instruments
 * ([appliesTo]) is hidden while another is chosen (no tuning for a piano, no capo for a bass,
 * no octaves for a guitar), the same rule the phone's settings follow.
 */
enum class TvPanelRow(
    val title: String,
    private val appliesTo: (Instrument) -> Boolean = { true },
) {
    ROOT("Root"),
    SCALE("Scale"),
    INSTRUMENT("Instrument"),
    TUNING("Tuning", { instrument -> instrument.isFretted }),
    CAPO("Capo", { instrument -> instrument == Instrument.GUITAR }),
    FRETS("Frets", { instrument -> instrument.isFretted }),
    LEFT_HANDED("Left-handed", { instrument -> instrument.isFretted }),
    THIN_STRING_ON_TOP("Thin string on top", { instrument -> instrument.isFretted }),
    OCTAVES("Octaves", { instrument -> instrument == Instrument.KEYBOARD }),
    LABELS("Labels"),
    DARK_BOARD("Dark board"),
    CHANGE_A_NOTE("Change a note"),
    KEEP_BOARD_ON("Keep board on"),
    CHORD_TABLES("Chord tables"),
    DRUMS("Drums"),
    ;

    /** "row-thin-string-on-top": the title in kebab case, so tests read like the screen. */
    val testTag: String
        get() {
            val kebab = title.lowercase().replace(' ', '-')
            return "row-$kebab"
        }

    /** True when the row applies to the instrument on the board. */
    fun isShown(chosen: Instrument): Boolean = appliesTo(chosen)

    /** Rows whose OK opens a [TvChoiceList] in the panel's place. */
    val opensChoiceList: Boolean
        get() =
            when (this) {
                ROOT, SCALE, INSTRUMENT, TUNING, CAPO, FRETS, OCTAVES, LABELS, KEEP_BOARD_ON -> true
                else -> false
            }
}

/**
 * What the panel remembers between openings while the activity lives: the row the player
 * left it from and how far its list was scrolled. The panel's rows leave the composition
 * whenever a list or screen replaces them, which would lose `focusRestorer`'s memory, so the
 * row is kept here and the panel puts focus back on it when it returns. Closing the panel
 * forgets both, so every opening from the board starts on Root.
 */
@Stable
class TvPanelMemory {
    var lastRow: TvPanelRow by mutableStateOf(TvPanelRow.ROOT)

    var listState: LazyListState by mutableStateOf(LazyListState())
        private set

    /** Forgets the row and scroll position; called when the board is showing again. */
    fun reset() {
        lastRow = TvPanelRow.ROOT
        listState = LazyListState()
    }
}

/** One [TvPanelMemory] for the TV screen's composition. */
@Composable
fun rememberTvPanelMemory(): TvPanelMemory = remember { TvPanelMemory() }

/**
 * The options panel on the right of the board: the board stays visible on the left and
 * redraws as choices are made. The panel's body follows [route]: its rows, the choice list of
 * one row, or the note-change lists.
 *
 * Focus is trapped inside: leaving in any direction is cancelled, so Left can't drop focus
 * onto the board underneath, which can't show that it has it. Back is the only way out and
 * belongs to `TvApp`.
 */
@Composable
fun TvOptionsPanel(
    route: TvRoute,
    navigation: TvNavigation,
    memory: TvPanelMemory,
    timer: TvPracticeTimer,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxHeight()
                .fillMaxWidth(PANEL_WIDTH_SHARE)
                .testTag("panel"),
        colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        // Google's overscan-safe insets: 27 dp top and bottom, 48 dp at the screen's edge.
        val bodyModifier =
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 48.dp, top = 27.dp, bottom = 27.dp)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup()
        Box(modifier = bodyModifier) {
            PanelBody(route, navigation, memory, timer)
        }
    }
}

/** The rows, or whatever the current route puts in their place. */
@Composable
private fun PanelBody(
    route: TvRoute,
    navigation: TvNavigation,
    memory: TvPanelMemory,
    timer: TvPracticeTimer,
) {
    when (route) {
        is TvRoute.ChoiceList -> {
            TvPanelChoices(route.row, navigation, timer)
        }

        TvRoute.EditNoteNotes -> {
            TvEditNoteNotes(navigation)
        }

        is TvRoute.EditNoteEdits -> {
            TvEditNoteEdits(route.degree, navigation)
        }

        else -> {
            PanelRows(navigation, memory, timer)
        }
    }
}

/** The list of rows, focus on the row the player last left (Root on a fresh opening). */
@Composable
private fun PanelRows(
    navigation: TvNavigation,
    memory: TvPanelMemory,
    timer: TvPracticeTimer,
) {
    val app = rememberScalesApplication()
    val settings = app.sessionState.value.settings
    val instrument = settings.selectedInstrument
    val visibleRows = mutableListOf<TvPanelRow>()
    for (row in TvPanelRow.entries) {
        if (row.isShown(instrument)) {
            visibleRows.add(row)
        }
    }
    val requesters = remember { TvPanelRow.entries.associateWith { FocusRequester() } }
    val arrivalRow = if (memory.lastRow in visibleRows) memory.lastRow else TvPanelRow.ROOT
    val arrivalRequester = requesters.getValue(arrivalRow)
    val listState = memory.listState
    LaunchedEffect(Unit) {
        val focused = requestFocusOnceLaidOut(arrivalRequester)
        if (!focused) {
            listState.scrollToItem(visibleRows.indexOf(arrivalRow))
            requestFocusOnceLaidOut(arrivalRequester)
        }
    }
    val onRow = { row: TvPanelRow ->
        memory.lastRow = row
        openRow(row, navigation) { transform -> app.session.updateSettings(transform) }
    }
    // The restorer's fallback is the arrival row, not Root: a fresh list has nothing saved to
    // restore, and focus requested for a row enters through the restorer, which would
    // otherwise send it to its fallback instead.
    LazyColumn(
        state = listState,
        modifier =
            Modifier
                .fillMaxSize()
                .focusRestorer(arrivalRequester)
                .focusGroup(),
    ) {
        items(visibleRows, key = { row -> row.name }) { row ->
            PanelRowItem(
                row = row,
                settings = settings,
                timer = timer,
                requester = requesters.getValue(row),
                onClick = { onRow(row) },
            )
        }
    }
}

/** What OK on a row does: open its list or screen, or flip its switch. */
private fun openRow(
    row: TvPanelRow,
    navigation: TvNavigation,
    updateSettings: ((PracticeSettings) -> PracticeSettings) -> Unit,
) {
    if (row.opensChoiceList) {
        navigation.push(TvRoute.ChoiceList(row))
        return
    }
    when (row) {
        TvPanelRow.LEFT_HANDED -> {
            updateSettings { it.copy(isLeftHanded = !it.isLeftHanded) }
        }

        TvPanelRow.THIN_STRING_ON_TOP -> {
            updateSettings {
                it.copy(
                    thinStringOnTop = !it.thinStringOnTop,
                )
            }
        }

        TvPanelRow.DARK_BOARD -> {
            updateSettings { it.copy(tvUsesDarkTheme = !it.tvUsesDarkTheme) }
        }

        TvPanelRow.CHANGE_A_NOTE -> {
            navigation.push(TvRoute.EditNoteNotes)
        }

        TvPanelRow.CHORD_TABLES -> {
            navigation.push(TvRoute.Chords)
        }

        TvPanelRow.DRUMS -> {
            navigation.push(TvRoute.Drums)
        }

        else -> {
            // Every other row opens a choice list, handled above.
        }
    }
}

/** One row: its title on the left, and on the right its current value, a switch or "›". */
@Composable
private fun PanelRowItem(
    row: TvPanelRow,
    settings: PracticeSettings,
    timer: TvPracticeTimer,
    requester: FocusRequester,
    onClick: () -> Unit,
) {
    val switchValue = switchValue(row, settings)
    var rowModifier =
        Modifier
            .fillMaxWidth()
            .testTag(row.testTag)
            .focusRequester(requester)
    if (switchValue != null) {
        // TalkBack and tests read the row itself as on or off; the drawn switch is decoration.
        rowModifier = rowModifier.semantics { toggleableState = ToggleableState(switchValue) }
    }
    ListItem(
        selected = false,
        onClick = onClick,
        modifier = rowModifier,
        headlineContent = { Text(text = row.title) },
        trailingContent = {
            if (switchValue != null) {
                Switch(checked = switchValue, onCheckedChange = null)
            } else {
                Text(text = rowValue(row, settings, timer), fontFamily = MusicFontFamily)
            }
        },
    )
}

/** The on/off state of a switch row, or null for a row that is not a switch. */
private fun switchValue(
    row: TvPanelRow,
    settings: PracticeSettings,
): Boolean? =
    when (row) {
        TvPanelRow.LEFT_HANDED -> settings.isLeftHanded
        TvPanelRow.THIN_STRING_ON_TOP -> settings.thinStringOnTop
        TvPanelRow.DARK_BOARD -> settings.tvUsesDarkTheme
        else -> null
    }

/** The words on the right of a row: its current value, or "›" for a row that opens a screen. */
private fun rowValue(
    row: TvPanelRow,
    settings: PracticeSettings,
    timer: TvPracticeTimer,
): String {
    val guitarValue = guitarRowValue(row, settings)
    if (guitarValue != null) {
        return guitarValue
    }
    return when (row) {
        TvPanelRow.ROOT -> settings.root.displayName
        TvPanelRow.SCALE -> settings.scaleDefinition.name
        TvPanelRow.INSTRUMENT -> settings.selectedInstrument.title
        TvPanelRow.OCTAVES -> octavesLabel(settings.selectedKeyboardOctaves)
        TvPanelRow.LABELS -> settings.labelStyle.title
        TvPanelRow.KEEP_BOARD_ON -> keepOnLabel(timer.minutes)
        else -> "›"
    }
}

/** The value of a guitar or bass row with a list behind it, or null for any other row. */
private fun guitarRowValue(
    row: TvPanelRow,
    settings: PracticeSettings,
): String? =
    when (row) {
        TvPanelRow.TUNING -> tuningRowValue(settings)
        TvPanelRow.CAPO -> plainCapoTitle(settings.selectedCapoFret)
        TvPanelRow.FRETS -> settings.fretCount.toString()
        else -> null
    }

/** The tuning of the neck on the board: the bass guitar's own while the bass is chosen. */
private fun tuningRowValue(settings: PracticeSettings): String {
    if (settings.selectedInstrument == Instrument.BASS) {
        return settings.selectedBassTuning.shortName
    }
    return settings.tuning.shortName
}

/** "1 octave", "2 octaves". */
internal fun octavesLabel(octaves: Int): String {
    if (octaves == 1) {
        return "1 octave"
    }
    return "$octaves octaves"
}

/** "Off", "30 min". */
internal fun keepOnLabel(minutes: Int?): String {
    if (minutes == null) {
        return "Off"
    }
    return "$minutes min"
}
