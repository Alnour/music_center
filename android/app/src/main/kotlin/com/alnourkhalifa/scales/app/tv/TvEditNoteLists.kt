package com.alnourkhalifa.scales.app.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.alnourkhalifa.scales.app.phone.playerMessage
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.EditChoices
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleDegree
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleTone
import com.alnourkhalifa.scales.core.music.menuTitle
import com.alnourkhalifa.scales.core.session.PracticeSession
import com.alnourkhalifa.scales.core.session.ScaleLibraryError

/*
 * "Change a note" on the TV: two lists in the panel's place, the scale's notes and then the
 * changes one note allows. Twin of `CompactScaleEditMenu` in
 * `Scales/Views/ScaleEditMenuViews.swift`: iOS nests a submenu per note inside one menu; the
 * remote climbs the same two levels as two routes, so Back steps out of one level at a time.
 */

/** Title of the first list, and of the panel row that opens it. */
private const val CHANGE_A_NOTE = "Change a note"

/** The last row of the first list, and the title of the list it opens. */
private const val ADD_A_NOTE = "Add a note"

/** Id of the "Add a note" row; the note rows use their degree's label ("7", "♭3"). */
private const val ADD_ID = "add"

/**
 * The scale's notes, one row each as iOS titles its submenus ("F♯ (7)"), then "Add a note".
 *
 * The root has no row: moving it would put the scale in another key, so the core never
 * offers a change to it. A note with nothing to offer, or "Add a note" when every degree is
 * taken, stays in the list greyed out and skipped by the D-pad, so the list always spells
 * the whole scale. OK on a note opens `TvRoute.EditNoteEdits(degree)`; on "Add a note",
 * `TvRoute.EditNoteEdits(null)`. Focus lands on the first note that can change.
 */
@Composable
fun TvEditNoteNotes(
    navigation: TvNavigation,
    modifier: Modifier = Modifier,
) {
    val app = rememberScalesApplication()
    val scale = app.sessionState.value.scale
    val choices = EditChoices(scale.definition)
    val degreesById = mutableMapOf<String, ScaleDegree>()
    val rows = mutableListOf<TvChoice>()
    for (tone in scale.tones) {
        if (tone.isRoot) {
            continue
        }
        val id = tone.degree.label
        val canChange = choices.edits(tone.degree).isNotEmpty()
        rows.add(TvChoice(id = id, label = toneTitle(tone), enabled = canChange))
        degreesById[id] = tone.degree
    }
    val canAdd = choices.additions.isNotEmpty()
    rows.add(TvChoice(id = ADD_ID, label = ADD_A_NOTE, enabled = canAdd))
    TvChoiceList(
        title = CHANGE_A_NOTE,
        choices = rows,
        selectedId = null,
        onPick = { id ->
            // The "Add a note" id is not a degree label, so it finds no degree: null is the
            // route's own way of saying "the additions".
            val degree = degreesById[id]
            navigation.push(TvRoute.EditNoteEdits(degree))
        },
        modifier = modifier,
    )
}

/**
 * The changes [degree] allows, spelled in the scale's key ("Lower to F (♭7)", "Remove F♯"),
 * or, when [degree] is null, the notes that could be added ("Add A♭ (♭2)").
 *
 * OK changes the note through the session, which keeps the change as a lesson and puts the
 * result on the board, then drops every level above the board: the change is only worth
 * seeing on the board, where the chips show what it did, so Back from there should leave
 * the panel rather than walk back through lists that describe the old scale.
 *
 * A failed save leaves the board as it was and is told to the player in the list's place
 * until they press OK, which then returns to the board as a success would.
 */
@Composable
fun TvEditNoteEdits(
    degree: ScaleDegree?,
    navigation: TvNavigation,
    modifier: Modifier = Modifier,
) {
    val app = rememberScalesApplication()
    val scale = app.sessionState.value.scale
    var libraryError: ScaleLibraryError? by remember { mutableStateOf(null) }
    val shownError = libraryError
    if (shownError != null) {
        LibraryErrorNotice(
            error = shownError,
            onDismiss = { navigation.popToBoard() },
            modifier = modifier,
        )
        return
    }
    val edits = editsFor(degree, scale)
    val editsById = mutableMapOf<String, ScaleEdit>()
    val rows = mutableListOf<TvChoice>()
    for (edit in edits) {
        val id = editId(edit)
        rows.add(TvChoice(id = id, label = edit.menuTitle(above = scale.root)))
        editsById[id] = edit
    }
    TvChoiceList(
        title = editsTitle(degree, scale),
        choices = rows,
        selectedId = null,
        onPick = { id ->
            val edit = editsById.getValue(id)
            val error = transformOrError(app.session, edit)
            if (error == null) {
                navigation.popToBoard()
            } else {
                libraryError = error
            }
        },
        modifier = modifier,
    )
}

/** The title iOS gives a note's submenu: its name and its degree, "F♯ (7)". */
private fun toneTitle(tone: ScaleTone): String = "${tone.name.displayName} (${tone.degree.label})"

/**
 * The second list's title: the note it changes, as its row in the first list read, or
 * "Add a note". A degree the scale no longer holds (the scale changed underneath the list)
 * falls back to the bare degree, which still says which note was meant.
 */
private fun editsTitle(
    degree: ScaleDegree?,
    scale: Scale,
): String {
    if (degree == null) {
        return ADD_A_NOTE
    }
    val tone = scale.tones.firstOrNull { candidate -> candidate.degree == degree }
    if (tone == null) {
        return degree.label
    }
    return toneTitle(tone)
}

/** The changes one note allows, or the additions when [degree] is null. */
private fun editsFor(
    degree: ScaleDegree?,
    scale: Scale,
): List<ScaleEdit> {
    val choices = EditChoices(scale.definition)
    if (degree == null) {
        return choices.additions
    }
    return choices.edits(degree)
}

/**
 * "lower:7", "add:♭2": the case name Swift's JSON uses and the degree, which together name
 * one edit and stay the same if the row's wording changes.
 */
private fun editId(edit: ScaleEdit): String = "${edit.swiftName}:${edit.degree.label}"

/**
 * Applies [edit] to the board, returning the library error when the change couldn't be
 * saved as a lesson (the board is then left as it was), or null when it went through.
 */
private fun transformOrError(
    session: PracticeSession,
    edit: ScaleEdit,
): ScaleLibraryError? {
    try {
        session.transform(edit)
    } catch (error: ScaleLibraryError) {
        return error
    }
    return null
}

/**
 * The library error drawn in the list's place, the TV's stand-in for the phone's
 * `LibraryErrorAlert`: tv-material has no dialog, and a notice inside the panel keeps focus
 * trapped where the panel already traps it. Drawing it instead of the list, not over it,
 * leaves the OK button the only thing the D-pad can reach.
 */
@Composable
private fun LibraryErrorNotice(
    error: ScaleLibraryError,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val okRequester = remember { FocusRequester() }
    LaunchedEffect(error) {
        requestFocusOnceLaidOut(okRequester)
    }
    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("libraryError"),
        colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Couldn't Update Your Scales",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = error.playerMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .focusRequester(okRequester)
                        .testTag("libraryErrorOk"),
            ) {
                Text(text = "OK")
            }
        }
    }
}
