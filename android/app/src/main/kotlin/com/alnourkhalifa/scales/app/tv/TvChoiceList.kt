package com.alnourkhalifa.scales.app.tv

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleFamily
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.Tuning
import com.alnourkhalifa.scales.core.session.NoteLabelStyle
import com.alnourkhalifa.scales.core.session.PracticeSession
import com.alnourkhalifa.scales.core.session.PracticeSettings
import com.alnourkhalifa.scales.ui.MusicFontFamily

/**
 * One line of a [TvChoiceList]. [id] is what the caller gets back when the player picks it,
 * so labels can change wording without breaking the wiring. A disabled choice is shown but
 * can't take focus, so the D-pad skips it.
 */
data class TvChoice(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
)

/** Choices under one heading ("Modes", "My Scales"); a null [header] draws no heading. */
data class TvChoiceGroup(
    val header: String?,
    val choices: List<TvChoice>,
)

/** A list with no headings, the shape of every list but Scale. */
@Composable
fun TvChoiceList(
    title: String,
    choices: List<TvChoice>,
    selectedId: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val single = listOf(TvChoiceGroup(header = null, choices = choices))
    TvChoiceList(title, single, selectedId, onPick, modifier)
}

/**
 * The list of choices behind one panel row, drawn in the panel's place.
 *
 * Focus lands on the current value ([selectedId]) on arrival, so OK straight away changes
 * nothing and the player sees where they are; with no current value it lands on the first
 * choice that can take focus. `focusRestorer` keeps focus inside the list when it scrolls.
 * [onPick] runs on OK; the caller applies the choice and pops back to the panel, because only
 * the caller knows which setting the list edits.
 *
 * The JVM name differs from the ungrouped overload's because both lists erase to `List`.
 */
@Composable
@JvmName("TvGroupedChoiceList")
fun TvChoiceList(
    title: String,
    groups: List<TvChoiceGroup>,
    selectedId: String?,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val arrivalRequester = remember { FocusRequester() }
    val arrivalId = arrivalChoiceId(groups, selectedId)
    val arrivalIndex = lazyIndexOf(groups, arrivalId)
    LaunchedEffect(Unit) {
        // Two rows of context above the current value, so a heading stays in view.
        val firstVisible = (arrivalIndex - 2).coerceAtLeast(0)
        listState.scrollToItem(firstVisible)
        requestFocusOnceLaidOut(arrivalRequester)
    }
    Column(
        modifier = modifier.testTag("choiceList"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRestorer(arrivalRequester)
                    .focusGroup(),
        ) {
            for (group in groups) {
                choiceGroup(group, selectedId, arrivalId, arrivalRequester, onPick)
            }
        }
    }
}

/** One group's heading, if any, and its choices. */
private fun LazyListScope.choiceGroup(
    group: TvChoiceGroup,
    selectedId: String?,
    arrivalId: String?,
    arrivalRequester: FocusRequester,
    onPick: (String) -> Unit,
) {
    val header = group.header
    if (header != null) {
        item(key = "header-$header") {
            Text(
                text = header,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
            )
        }
    }
    for (choice in group.choices) {
        item(key = choice.id) {
            val isArrival = choice.id == arrivalId
            ChoiceRow(
                choice = choice,
                isSelected = choice.id == selectedId,
                arrivalRequester = if (isArrival) arrivalRequester else null,
                onPick = onPick,
            )
        }
    }
}

/** One choice; the current value carries a tick and the list's selected colours. */
@Composable
private fun ChoiceRow(
    choice: TvChoice,
    isSelected: Boolean,
    arrivalRequester: FocusRequester?,
    onPick: (String) -> Unit,
) {
    var rowModifier =
        Modifier
            .fillMaxWidth()
            .testTag("choice-${choice.id}")
            .focusProperties { canFocus = choice.enabled }
    if (arrivalRequester != null) {
        rowModifier = rowModifier.focusRequester(arrivalRequester)
    }
    ListItem(
        selected = isSelected,
        onClick = { onPick(choice.id) },
        enabled = choice.enabled,
        modifier = rowModifier,
        headlineContent = {
            Text(text = choice.label, fontFamily = MusicFontFamily)
        },
        trailingContent =
            if (isSelected) {
                { Text(text = "✓") }
            } else {
                null
            },
    )
}

/** Where focus lands on arrival: the current value if it can take focus, else the first
 *  choice that can. */
private fun arrivalChoiceId(
    groups: List<TvChoiceGroup>,
    selectedId: String?,
): String? {
    val allChoices = groups.flatMap { group -> group.choices }
    val focusable = allChoices.filter { choice -> choice.enabled }
    val selected = focusable.firstOrNull { choice -> choice.id == selectedId }
    val arrival = selected ?: focusable.firstOrNull()
    return arrival?.id
}

/** The position of a choice among the lazy list's items, headings included. */
private fun lazyIndexOf(
    groups: List<TvChoiceGroup>,
    id: String?,
): Int {
    var index = 0
    for (group in groups) {
        if (group.header != null) {
            index += 1
        }
        for (choice in group.choices) {
            if (choice.id == id) {
                return index
            }
            index += 1
        }
    }
    return 0
}

/** Frames to wait for a lazy row to be composed and attached before giving up on focus. */
private const val FOCUS_ATTEMPT_FRAMES = 10

/**
 * Moves focus to [requester] once its node exists. A lazy list composes its rows during
 * layout, after the effect that asks for focus has started, so the first attempt can find
 * nothing attached; waiting a frame between attempts lets the row arrive. Returns false when
 * the node never arrived (a row scrolled out of a lazy list), so the caller can scroll to it.
 */
internal suspend fun requestFocusOnceLaidOut(requester: FocusRequester): Boolean {
    repeat(FOCUS_ATTEMPT_FRAMES) {
        if (requester.requestFocus(FocusDirection.Enter)) {
            return true
        }
        withFrameNanos { }
    }
    return false
}

/*
 * The panel's lists: what each choice row offers and how a pick reaches the session.
 */

/**
 * The choice list behind one panel row. OK applies the choice through the session (which
 * saves it, as on the phone) and climbs back to the panel, with focus on the row just left.
 * Keep board on goes to the [TvPracticeTimer] instead, because that choice is never saved.
 */
@Composable
fun TvPanelChoices(
    row: TvPanelRow,
    navigation: TvNavigation,
    timer: TvPracticeTimer,
    modifier: Modifier = Modifier,
) {
    val app = rememberScalesApplication()
    val settings = app.sessionState.value.settings
    // Read so a scale saved or renamed elsewhere redraws the Scale list.
    app.libraryVersion.value
    val list =
        when (row) {
            TvPanelRow.ROOT -> PanelLists.rootList(settings, app.session)
            TvPanelRow.SCALE -> PanelLists.scaleList(settings, app.session, app.library.scales)
            TvPanelRow.INSTRUMENT -> PanelLists.instrumentList(settings, app.session)
            TvPanelRow.TUNING -> PanelLists.tuningList(settings, app.session)
            TvPanelRow.FRETS -> PanelLists.fretList(settings, app.session)
            TvPanelRow.OCTAVES -> PanelLists.octaveList(settings, app.session)
            TvPanelRow.LABELS -> PanelLists.labelList(settings, app.session)
            else -> PanelLists.keepOnList(timer)
        }
    TvChoiceList(
        title = list.title,
        groups = list.groups,
        selectedId = list.selectedId,
        onPick = { id ->
            list.apply(id)
            navigation.pop()
        },
        modifier = modifier,
    )
}

/** One row's list: its title, choices, current value and what a pick does. */
private class PanelList(
    val title: String,
    val groups: List<TvChoiceGroup>,
    val selectedId: String?,
    val apply: (String) -> Unit,
)

/** Id of the Off choice; the timed choices use their minutes as id. */
private const val KEEP_ON_OFF_ID = "off"

/** One builder per choice row, gathered so the list file stays a list file. */
private object PanelLists {
    /** A list with no headings. */
    private fun ungrouped(choices: List<TvChoice>): List<TvChoiceGroup> =
        listOf(TvChoiceGroup(header = null, choices = choices))

    /** The 17 roots, both spellings of every black key. */
    fun rootList(
        settings: PracticeSettings,
        session: PracticeSession,
    ): PanelList {
        val choices = mutableListOf<TvChoice>()
        val notesById = mutableMapOf<String, NoteName>()
        for (note in NoteName.rootChoices) {
            choices.add(TvChoice(id = note.displayName, label = note.displayName))
            notesById[note.displayName] = note
        }
        return PanelList("Root", ungrouped(choices), settings.root.displayName) { id ->
            val note = notesById.getValue(id)
            session.updateSettings { it.copy(root = note) }
        }
    }

    /** The built-in scales by family, then the player's own scales under "My Scales". */
    fun scaleList(
        settings: PracticeSettings,
        session: PracticeSession,
        savedScales: List<CustomScale>,
    ): PanelList {
        val groups = mutableListOf<TvChoiceGroup>()
        val referencesById = mutableMapOf<String, ScaleReference>()
        for (family in ScaleFamily.entries) {
            val choices = mutableListOf<TvChoice>()
            for (type in family.scales) {
                val reference = ScaleReference.BuiltIn(type)
                choices.add(TvChoice(id = reference.swiftDescription, label = type.displayName))
                referencesById[reference.swiftDescription] = reference
            }
            groups.add(TvChoiceGroup(header = family.title, choices = choices))
        }
        if (savedScales.isNotEmpty()) {
            val choices = mutableListOf<TvChoice>()
            for (saved in savedScales) {
                val reference = ScaleReference.Custom(saved.id)
                choices.add(
                    TvChoice(id = reference.swiftDescription, label = saved.definition.name),
                )
                referencesById[reference.swiftDescription] = reference
            }
            groups.add(TvChoiceGroup(header = "My Scales", choices = choices))
        }
        val selectedId = settings.scaleReference.swiftDescription
        return PanelList("Scale", groups, selectedId) { id ->
            val reference = referencesById.getValue(id)
            session.choose(reference)
        }
    }

    fun instrumentList(
        settings: PracticeSettings,
        session: PracticeSession,
    ): PanelList {
        val choices = mutableListOf<TvChoice>()
        for (instrument in Instrument.entries) {
            choices.add(TvChoice(id = instrument.rawValue, label = instrument.title))
        }
        val selectedId = settings.selectedInstrument.rawValue
        return PanelList("Instrument", ungrouped(choices), selectedId) { id ->
            val instrument = Instrument.fromRawValue(id) ?: Instrument.GUITAR
            session.updateSettings { it.withSelectedInstrument(instrument) }
        }
    }

    fun tuningList(
        settings: PracticeSettings,
        session: PracticeSession,
    ): PanelList {
        val choices = mutableListOf<TvChoice>()
        for (tuning in Tuning.entries) {
            choices.add(TvChoice(id = tuning.rawValue, label = tuning.displayName))
        }
        return PanelList("Tuning", ungrouped(choices), settings.tuning.rawValue) { id ->
            val tuning = Tuning.fromRawValue(id) ?: Tuning.STANDARD
            session.updateSettings { it.copy(tuning = tuning) }
        }
    }

    fun fretList(
        settings: PracticeSettings,
        session: PracticeSession,
    ): PanelList {
        val choices = mutableListOf<TvChoice>()
        for (count in PracticeSettings.fretCountChoices) {
            choices.add(TvChoice(id = count.toString(), label = count.toString()))
        }
        return PanelList("Frets", ungrouped(choices), settings.fretCount.toString()) { id ->
            val count = id.toInt()
            session.updateSettings { it.copy(fretCount = count) }
        }
    }

    fun octaveList(
        settings: PracticeSettings,
        session: PracticeSession,
    ): PanelList {
        val choices = mutableListOf<TvChoice>()
        for (octaves in PracticeSettings.keyboardOctaveChoices) {
            choices.add(TvChoice(id = octaves.toString(), label = octavesLabel(octaves)))
        }
        val selectedId = settings.selectedKeyboardOctaves.toString()
        return PanelList("Octaves", ungrouped(choices), selectedId) { id ->
            val octaves = id.toInt()
            session.updateSettings { it.withSelectedKeyboardOctaves(octaves) }
        }
    }

    fun labelList(
        settings: PracticeSettings,
        session: PracticeSession,
    ): PanelList {
        val choices = mutableListOf<TvChoice>()
        for (style in NoteLabelStyle.entries) {
            choices.add(TvChoice(id = style.rawValue, label = style.title))
        }
        return PanelList("Labels", ungrouped(choices), settings.labelStyle.rawValue) { id ->
            val style = NoteLabelStyle.fromRawValue(id) ?: NoteLabelStyle.NOTE_NAMES
            session.updateSettings { it.copy(labelStyle = style) }
        }
    }

    fun keepOnList(timer: TvPracticeTimer): PanelList {
        val choices = mutableListOf(TvChoice(id = KEEP_ON_OFF_ID, label = keepOnLabel(null)))
        for (minutes in TvPracticeTimer.choices) {
            choices.add(TvChoice(id = minutes.toString(), label = keepOnLabel(minutes)))
        }
        val selectedId = timer.minutes?.toString() ?: KEEP_ON_OFF_ID
        return PanelList("Keep board on", ungrouped(choices), selectedId) { id ->
            timer.choose(id.toIntOrNull())
        }
    }
}
