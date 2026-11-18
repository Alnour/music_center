package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.core.music.EditChoices
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleTone
import com.alnourkhalifa.scales.core.music.menuTitle
import com.alnourkhalifa.scales.ui.MusicFontFamily

/**
 * The rows that change one note of a scale: a menu per note but the root (lower, raise,
 * remove) and a menu of notes that could be added. Twin of `ScaleEditMenus` in
 * `Scales/Views/ScaleEditMenuViews.swift`, the Transform screen's row-per-note version; the
 * Scales screen folds the same choices into one compact menu.
 *
 * Each row is tagged `editNote-<degree>` ("editNote-7") and the add row `addNote`, Swift's
 * accessibility identifiers, so the tests on both platforms find the same rows. A note with
 * nothing to offer, or a scale with no room for another note, shows its row greyed out.
 * Rows are separated by [FormDivider], since they sit in a [FormSection] card.
 */
@Composable
fun ScaleEditMenus(
    scale: Scale,
    onPick: (ScaleEdit) -> Unit,
) {
    val choices = remember(scale.definition) { EditChoices(scale.definition) }
    for (tone in scale.tones) {
        if (tone.isRoot) {
            continue
        }
        val edits = choices.edits(tone.degree)
        EditMenuRow(
            edits = edits,
            root = scale.root,
            onPick = onPick,
            modifier =
                Modifier
                    .testTag("editNote-${tone.degree.label}")
                    .semantics { contentDescription = spokenNote(tone) },
        ) { isEnabled ->
            NoteLabel(tone, isEnabled)
        }
        FormDivider()
    }
    EditMenuRow(
        edits = choices.additions,
        root = scale.root,
        onPick = onPick,
        modifier = Modifier.testTag("addNote"),
    ) { isEnabled ->
        AddNoteLabel(isEnabled)
    }
}

/** "Change F♯, the 7": what TalkBack says for a note's row, as VoiceOver does on iOS. */
private fun spokenNote(tone: ScaleTone): String =
    "Change ${tone.name.displayName}, the ${tone.degree.label}"

/**
 * One row that opens a menu of edits under its trailing edge, where iOS opens a `Menu`.
 * Picking an item closes the menu before handing the edit on, so the lesson sheet opens over
 * a closed menu.
 */
@Composable
private fun EditMenuRow(
    edits: List<ScaleEdit>,
    root: NoteName,
    onPick: (ScaleEdit) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.(isEnabled: Boolean) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isEnabled = edits.isNotEmpty()
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) { isExpanded = true }
                    .heightIn(min = FormMetrics.rowMinHeight)
                    .padding(horizontal = FormMetrics.rowHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            label(isEnabled)
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                for (edit in edits) {
                    DropdownMenuItem(
                        text = { Text(edit.menuTitle(root), fontFamily = MusicFontFamily) },
                        onClick = {
                            isExpanded = false
                            onPick(edit)
                        },
                    )
                }
            }
        }
    }
}

/**
 * The note in bold, its degree lighter, and the up-and-down chevron of an iOS menu, all in the
 * accent colour: iOS tints a menu's label as a button, which tells the player the row opens
 * something. A row with nothing to offer is grey.
 */
@Composable
private fun RowScope.NoteLabel(
    tone: ScaleTone,
    isEnabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (isEnabled) colors.primary else colors.onSurfaceVariant
    val lighterTint = tint.copy(alpha = 0.6f)
    Text(
        text = tone.name.displayName,
        fontFamily = MusicFontFamily,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyLarge,
        color = tint,
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = tone.degree.label,
        fontFamily = MusicFontFamily,
        style = MaterialTheme.typography.bodyLarge,
        color = lighterTint,
    )
    Spacer(modifier = Modifier.weight(1f))
    Icon(
        imageVector = FormIcons.chevronUpDown,
        contentDescription = null,
        tint = lighterTint,
        modifier = Modifier.size(FormMetrics.accessoryIconSize),
    )
}

/** "Add a note" with a plus in a circle, in the accent colour like an iOS menu label. */
@Composable
private fun RowScope.AddNoteLabel(isEnabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val tint = if (isEnabled) colors.primary else colors.onSurfaceVariant
    Icon(
        imageVector = FormIcons.plusCircle,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(FormMetrics.leadingIconSize),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
        text = "Add a note",
        style = MaterialTheme.typography.bodyLarge,
        color = tint,
    )
}
