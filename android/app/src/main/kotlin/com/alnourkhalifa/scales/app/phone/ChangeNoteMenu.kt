package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.core.music.EditChoices
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleTone
import com.alnourkhalifa.scales.core.music.menuTitle
import com.alnourkhalifa.scales.ui.MusicFontFamily

/**
 * Which page of the note-change menu is showing. iOS nests a submenu per note inside one
 * `Menu`; a Compose `DropdownMenu` has no submenus, so one menu swaps its items between the
 * list of notes and one note's changes, with a back item at the top of the second page.
 */
private sealed interface ChangeNotePage {
    /** The notes of the scale, one item each, then "Add a note". */
    data object Notes : ChangeNotePage

    /** One note's changes: lower, raise, remove. */
    data class Note(
        val tone: ScaleTone,
    ) : ChangeNotePage

    /** The notes that could be added. */
    data object Additions : ChangeNotePage
}

/**
 * The wand button in the Scales toolbar that changes one note of the scale on the board.
 * Twin of `CompactScaleEditMenu` in `Scales/Views/ScaleEditMenuViews.swift`.
 *
 * The first page lists every note but the root as "F♯ (7)" (the root cannot change, or the
 * scale would be in another key) and "Add a note"; a note with nothing to offer is greyed
 * out rather than hidden, so the list always spells the whole scale. Picking a change hands
 * it to [onPick] and closes the menu; reopening starts on the first page again.
 *
 * Tags: the button `changeBoardNote`, the first page's items `editNote-<degree>` and
 * `addNote`, the back item `editNoteBack`.
 */
@Composable
fun ChangeNoteMenu(
    scale: Scale,
    onPick: (ScaleEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    var page: ChangeNotePage by remember { mutableStateOf(ChangeNotePage.Notes) }
    val choices = EditChoices(scale.definition)
    val close = {
        isOpen = false
        page = ChangeNotePage.Notes
    }
    Box(modifier = modifier) {
        IconButton(
            onClick = { isOpen = true },
            modifier =
                Modifier
                    .testTag("changeBoardNote")
                    .semantics { contentDescription = "Change a note" },
        ) {
            Icon(
                imageVector = WandIcon.image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = close) {
            val pick = { edit: ScaleEdit ->
                close()
                onPick(edit)
            }
            when (val shown = page) {
                ChangeNotePage.Notes -> {
                    NotesPage(scale, choices, onOpen = { page = it })
                }

                is ChangeNotePage.Note -> {
                    val title = toneTitle(shown.tone)
                    val edits = choices.edits(shown.tone.degree)
                    EditsPage(title, edits, scale, onBack = { page = ChangeNotePage.Notes }, pick)
                }

                ChangeNotePage.Additions -> {
                    val back = { page = ChangeNotePage.Notes }
                    EditsPage(ADD_A_NOTE, choices.additions, scale, onBack = back, pick)
                }
            }
        }
    }
}

/** The title iOS gives a note's submenu: its name and its degree, "F♯ (7)". */
private fun toneTitle(tone: ScaleTone): String = "${tone.name.displayName} (${tone.degree.label})"

private const val ADD_A_NOTE = "Add a note"

/** The first page: a submenu item per note but the root, then the additions. */
@Composable
private fun NotesPage(
    scale: Scale,
    choices: EditChoices,
    onOpen: (ChangeNotePage) -> Unit,
) {
    for (tone in scale.tones) {
        if (tone.isRoot) {
            continue
        }
        val hasEdits = choices.edits(tone.degree).isNotEmpty()
        SubmenuItem(
            title = toneTitle(tone),
            isEnabled = hasEdits,
            onClick = { onOpen(ChangeNotePage.Note(tone)) },
            modifier = Modifier.testTag("editNote-${tone.degree.label}"),
        )
    }
    HorizontalDivider()
    SubmenuItem(
        title = ADD_A_NOTE,
        isEnabled = choices.additions.isNotEmpty(),
        onClick = { onOpen(ChangeNotePage.Additions) },
        modifier = Modifier.testTag("addNote"),
    )
}

/** An item that opens a second page, marked with a chevron as iOS marks a submenu. */
@Composable
private fun SubmenuItem(
    title: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    DropdownMenuItem(
        text = { Text(text = title, fontFamily = MusicFontFamily) },
        onClick = onClick,
        modifier = modifier,
        enabled = isEnabled,
        trailingIcon = { Icon(imageVector = MenuIcons.chevronRight, contentDescription = null) },
    )
}

/**
 * The second page: the way back, titled with the page it came from, then each change spelled
 * in the scale's key ("Lower to F (♭7)").
 */
@Composable
private fun EditsPage(
    title: String,
    edits: List<ScaleEdit>,
    scale: Scale,
    onBack: () -> Unit,
    onPick: (ScaleEdit) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = title, fontFamily = MusicFontFamily) },
        onClick = onBack,
        modifier = Modifier.testTag("editNoteBack"),
        leadingIcon = { Icon(imageVector = MenuIcons.chevronLeft, contentDescription = "Back") },
    )
    HorizontalDivider()
    for (edit in edits) {
        val editTitle = edit.menuTitle(above = scale.root)
        DropdownMenuItem(
            text = { Text(text = editTitle, fontFamily = MusicFontFamily) },
            onClick = { onPick(edit) },
        )
    }
}

/**
 * SF Symbols' `wand.and.stars`: a slanted wand with sparkles, filled so `Icon` tints it in
 * the accent colour as the iPhone's toolbar does.
 */
private object WandIcon {
    private val ink = SolidColor(Color.Black)

    val image: ImageVector =
        ImageVector
            .Builder(
                name = "ChangeNoteWand",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = ink) {
                moveTo(4.6f, 20.8f)
                lineTo(15.4f, 10f)
                lineTo(16.8f, 11.4f)
                lineTo(6f, 22.2f)
                close()
                moveTo(16.2f, 9.2f)
                lineTo(17.4f, 8f)
                lineTo(18.8f, 9.4f)
                lineTo(17.6f, 10.6f)
                close()
                sparkle(centerX = 18.5f, centerY = 4f, radius = 3.2f)
                sparkle(centerX = 9f, centerY = 6f, radius = 2.2f)
                sparkle(centerX = 21f, centerY = 14.5f, radius = 1.8f)
                sparkle(centerX = 4f, centerY = 13f, radius = 1.5f)
            }.build()

    /** A four-pointed star with pinched sides. */
    private fun PathBuilder.sparkle(
        centerX: Float,
        centerY: Float,
        radius: Float,
    ) {
        val waist = radius * 0.28f
        moveTo(centerX, centerY - radius)
        lineTo(centerX + waist, centerY - waist)
        lineTo(centerX + radius, centerY)
        lineTo(centerX + waist, centerY + waist)
        lineTo(centerX, centerY + radius)
        lineTo(centerX - waist, centerY + waist)
        lineTo(centerX - radius, centerY)
        lineTo(centerX - waist, centerY - waist)
        close()
    }
}
