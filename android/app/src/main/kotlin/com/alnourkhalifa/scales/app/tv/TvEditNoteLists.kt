package com.alnourkhalifa.scales.app.tv

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alnourkhalifa.scales.core.music.ScaleDegree

/*
 * "Change a note" on the TV: two lists in the panel's place, the scale's notes and then the
 * changes one note allows. The panel calls these two by name; the real lists replace the
 * bodies here (the core's `EditChoices` and `ScaleEdit.menuTitle` give their rows, and
 * `session.transform` applies a change).
 */

/** Id of the stand-in row the placeholder lists show. */
private const val PLACEHOLDER_ID = "placeholder"

/**
 * The scale's notes, one row each, plus "Add a note". OK on a note pushes
 * `TvRoute.EditNoteEdits(degree)`; on "Add a note", `TvRoute.EditNoteEdits(null)`.
 */
@Composable
fun TvEditNoteNotes(
    navigation: TvNavigation,
    modifier: Modifier = Modifier,
) {
    TvChoiceList(
        title = "Change a note",
        choices = listOf(TvChoice(id = PLACEHOLDER_ID, label = "Coming soon")),
        selectedId = null,
        onPick = { navigation.pop() },
        modifier = modifier,
    )
}

/**
 * The changes [degree] allows (lower, raise, remove), or the notes that could be added when
 * [degree] is null. OK applies the change and returns to the board, where the chips show
 * what it did.
 */
@Composable
fun TvEditNoteEdits(
    degree: ScaleDegree?,
    navigation: TvNavigation,
    modifier: Modifier = Modifier,
) {
    val title = if (degree == null) "Add a note" else "Change ${degree.label}"
    TvChoiceList(
        title = title,
        choices = listOf(TvChoice(id = PLACEHOLDER_ID, label = "Coming soon")),
        selectedId = null,
        onPick = { navigation.pop() },
        modifier = modifier,
    )
}
