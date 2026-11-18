package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleDefinition
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleEditError
import com.alnourkhalifa.scales.core.music.ScaleFamily
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.TransformationLesson
import com.alnourkhalifa.scales.core.music.definition
import com.alnourkhalifa.scales.core.session.ScaleLibrary
import com.alnourkhalifa.scales.core.session.ScaleLibraryError
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.MusicFontFamily
import com.alnourkhalifa.scales.ui.ScaleNotesRow
import kotlinx.coroutines.launch

/**
 * The Transform screen: pick a scale, change one note, and read what that does to the chords.
 * Twin of `TransformView` in `Scales/Views/TransformView.swift`.
 *
 * It starts on the scale shown on the Scales screen and follows it whenever it changes there
 * ([TransformTabModel.followTheBoard]). Choosing a change opens the lesson in a sheet, where
 * the result is named and kept. Below sit the player's own scales and every lesson they have
 * kept, newest first. The sections are the iOS `Form`'s, in a `LazyColumn`.
 */
@Composable
fun TransformView(
    model: TransformTabModel,
    modifier: Modifier = Modifier,
) {
    val app = rememberScalesApplication()
    // Read so the screen redraws whenever a scale or lesson is saved, renamed or deleted.
    val libraryVersion = app.libraryVersion.value
    val library = app.library
    LaunchedEffect(libraryVersion) {
        model.dropDeletedStart(library)
    }
    var scaleAwaitingDeletion by remember { mutableStateOf<CustomScale?>(null) }
    var libraryError by remember { mutableStateOf<ScaleLibraryError?>(null) }
    val startScale = Scale(model.root, startDefinition(model.start, library))
    val actions =
        TransformListActions(
            model = model,
            startScale = startScale,
            askToDelete = { scale -> scaleAwaitingDeletion = scale },
            deleteLesson = { lesson ->
                libraryError = libraryChange { library.deleteLesson(lesson.id) }
            },
        )
    FormPage(title = "Transform", modifier = modifier, hasLargeTitle = true) {
        transformSections(actions, library)
    }
    val pending = model.pending
    if (pending != null) {
        LessonSheet(transformation = pending, onClose = { model.pending = null })
    }
    val awaiting = scaleAwaitingDeletion
    if (awaiting != null) {
        DeleteScaleDialog(
            scale = awaiting,
            message = deletionMessage(library, awaiting),
            onDelete = { libraryError = libraryChange { library.deleteScale(awaiting.id) } },
            onClose = { scaleAwaitingDeletion = null },
        )
    }
    LibraryErrorAlert(error = libraryError, onDismiss = { libraryError = null })
}

/** What the rows of the screen do, gathered so the sections stay short. */
private class TransformListActions(
    val model: TransformTabModel,
    val startScale: Scale,
    val askToDelete: (CustomScale) -> Unit,
    val deleteLesson: (TransformationLesson) -> Unit,
) {
    /**
     * Opens the lesson sheet for a change. The menus only offer edits the scale allows, so
     * a refusal never happens in practice; it is ignored, as Swift's `try?` does.
     */
    fun begin(edit: ScaleEdit) {
        val transformation =
            try {
                ScaleTransformation(model.root, startScale.definition, edit)
            } catch (ignored: ScaleEditError) {
                return
            }
        model.pending = transformation
    }
}

/** The four sections in iOS order; My Scales and Lessons only once there is something in them. */
private fun LazyListScope.transformSections(
    actions: TransformListActions,
    library: ScaleLibrary,
) {
    item(key = "start") {
        StartSection(actions.model, actions.startScale, library.scales)
    }
    item(key = "change") {
        FormSection(
            header = "Change one note",
            footer =
                "Lower, raise or remove a note, or add one. The lesson shows what happens to " +
                    "every chord, and names the new scale.",
        ) {
            ScaleEditMenus(scale = actions.startScale, onPick = actions::begin)
        }
    }
    if (library.scales.isNotEmpty()) {
        item(key = "myScales") {
            MyScalesSection(library.scales, actions)
        }
    }
    if (library.lessons.isNotEmpty()) {
        item(key = "lessons") {
            LessonsSection(library.lessons.reversed(), actions)
        }
    }
}

@Composable
private fun StartSection(
    model: TransformTabModel,
    startScale: Scale,
    customScales: List<CustomScale>,
) {
    val palette = FretboardPalette.matching(isSystemInDarkTheme())
    val noteSize = if (hasRegularWidth()) 20.dp else 15.dp
    FormSection(header = "Start from") {
        KeyPickerRow(selected = model.root, onSelect = { root -> model.root = root })
        FormDivider()
        ScalePickerRow(
            selected = model.start,
            customScales = customScales,
            onSelect = { reference -> model.start = reference },
        )
        FormDivider()
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScaleNotesRow(scale = startScale, noteSize = noteSize, palette = palette)
        }
    }
}

/** "Key  G ⌃⌄": the seventeen spellings of `NoteName.rootChoices`. */
@Composable
private fun KeyPickerRow(
    selected: NoteName,
    onSelect: (NoteName) -> Unit,
) {
    PickerRow(label = "Key", value = selected.displayName, tag = "transformKey") { close ->
        for (note in NoteName.rootChoices) {
            PickerChoice(
                title = note.displayName,
                isSelected = note == selected,
                tag = "keyChoice-${note.displayName}",
                onClick = {
                    close()
                    onSelect(note)
                },
            )
        }
    }
}

/**
 * "Starting scale  Major ⌃⌄": the built-in scales grouped as a practice book groups them, then
 * the player's own scales. Twin of `ScalePickerOptions` in `Scales/Views/ScalePickerOptions.swift`.
 */
@Composable
private fun ScalePickerRow(
    selected: ScaleReference,
    customScales: List<CustomScale>,
    onSelect: (ScaleReference) -> Unit,
) {
    val selectedName = referenceName(selected, customScales)
    PickerRow(label = "Starting scale", value = selectedName, tag = "transformScale") { close ->
        val choose = { reference: ScaleReference ->
            close()
            onSelect(reference)
        }
        for ((index, family) in ScaleFamily.entries.withIndex()) {
            PickerGroupTitle(family.title, hasDividerAbove = index > 0)
            for (type in family.scales) {
                val reference = ScaleReference.BuiltIn(type)
                PickerChoice(
                    title = type.displayName,
                    isSelected = reference == selected,
                    tag = "scaleChoice-${type.displayName}",
                    onClick = { choose(reference) },
                )
            }
        }
        if (customScales.isNotEmpty()) {
            PickerGroupTitle("My Scales", hasDividerAbove = true)
        }
        for (customScale in customScales) {
            val reference = ScaleReference.Custom(customScale.id)
            PickerChoice(
                title = customScale.definition.name,
                isSelected = reference == selected,
                tag = "scaleChoice-${customScale.definition.name}",
                onClick = { choose(reference) },
            )
        }
    }
}

/**
 * A form row that shows a choice and opens a menu of the others, the look of a SwiftUI
 * `Picker` in a `Form`: the label leading, the value and the up-and-down chevron trailing.
 */
@Composable
private fun PickerRow(
    label: String,
    value: String,
    tag: String,
    choices: @Composable (close: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(tag)
                    .clickable { isExpanded = true }
                    .heightIn(min = FormMetrics.rowMinHeight)
                    .padding(horizontal = FormMetrics.rowHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                fontFamily = MusicFontFamily,
                style = MaterialTheme.typography.bodyLarge,
                color = secondary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = FormIcons.chevronUpDown,
                contentDescription = null,
                tint = secondary,
                modifier = Modifier.size(FormMetrics.accessoryIconSize),
            )
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                choices { isExpanded = false }
            }
        }
    }
}

/** One choice in a picker's menu, ticked when it is the current one, as iOS ticks it. */
@Composable
private fun PickerChoice(
    title: String,
    isSelected: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = title, fontFamily = MusicFontFamily) },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = FormIcons.checkmark,
                    contentDescription = "Selected",
                    modifier = Modifier.size(FormMetrics.accessoryIconSize),
                )
            } else {
                Spacer(modifier = Modifier.size(FormMetrics.accessoryIconSize))
            }
        },
    )
}

/** A family's name over its scales in the picker menu, like a menu section title on iOS. */
@Composable
private fun PickerGroupTitle(
    title: String,
    hasDividerAbove: Boolean,
) {
    if (hasDividerAbove) {
        HorizontalDivider()
    }
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun MyScalesSection(
    scales: List<CustomScale>,
    actions: TransformListActions,
) {
    FormSection(header = "My Scales") {
        for ((index, customScale) in scales.withIndex()) {
            if (index > 0) {
                FormDivider()
            }
            val name = customScale.definition.name
            SwipeToDeleteRow(onDelete = { actions.askToDelete(customScale) }) {
                NavigationRow(
                    title = name,
                    subtitle = customScale.definition.formula,
                    tag = "myScale-$name",
                    onClick = { actions.model.open(TransformPage.MyScale(customScale.id)) },
                )
            }
        }
    }
}

@Composable
private fun LessonsSection(
    newestFirst: List<TransformationLesson>,
    actions: TransformListActions,
) {
    FormSection(header = "Lessons") {
        for ((index, lesson) in newestFirst.withIndex()) {
            if (index > 0) {
                FormDivider()
            }
            SwipeToDeleteRow(onDelete = { actions.deleteLesson(lesson) }) {
                NavigationRow(
                    title = lesson.title,
                    subtitle = lesson.transformation.editSentence,
                    tag = "lesson-${lesson.id}",
                    onClick = { actions.model.open(TransformPage.Lesson(lesson.id)) },
                )
            }
        }
    }
}

/**
 * A row swiped from right to left reveals a red Delete, as iOS's `swipeActions` do, and a full
 * swipe deletes. The row then springs back: a deleted lesson disappears from the list anyway,
 * and a scale waits for its confirmation dialog.
 */
@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val state = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { direction ->
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            scope.launch { state.reset() }
        },
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(FormColors.destructive)
                        .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(text = "Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        Box(modifier = Modifier.background(FormColors.current().card)) {
            content()
        }
    }
}

/**
 * The confirmation before a scale and its lessons are deleted, Swift's `confirmationDialog`.
 */
@Composable
internal fun DeleteScaleDialog(
    scale: CustomScale,
    message: String,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Delete ${scale.definition.name}?", fontFamily = MusicFontFamily) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = {
                    onClose()
                    onDelete()
                },
            ) {
                Text(text = "Delete Scale", color = FormColors.destructive)
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        },
    )
}

/**
 * The definition behind a picker choice. A saved scale deleted elsewhere falls back to Major
 * until [TransformTabModel.dropDeletedStart] catches up.
 */
private fun startDefinition(
    reference: ScaleReference,
    library: ScaleLibrary,
): ScaleDefinition =
    when (reference) {
        is ScaleReference.BuiltIn -> {
            reference.type.definition
        }

        is ScaleReference.Custom -> {
            library.scale(reference.id)?.definition
                ?: ScaleType.MAJOR.definition
        }
    }

/** The name the scale picker shows for its current choice. */
private fun referenceName(
    reference: ScaleReference,
    customScales: List<CustomScale>,
): String {
    if (reference is ScaleReference.BuiltIn) {
        return reference.type.displayName
    }
    for (customScale in customScales) {
        if (ScaleReference.Custom(customScale.id) == reference) {
            return customScale.definition.name
        }
    }
    return ScaleType.MAJOR.displayName
}

/** What deleting a scale takes with it, counted from the lessons filed under it. */
private fun deletionMessage(
    library: ScaleLibrary,
    scale: CustomScale,
): String {
    val lessonCount = library.lessons(ScaleReference.Custom(scale.id)).size
    return when (lessonCount) {
        0 -> "The scale will be removed from this device."
        1 -> "The scale and the lesson that made it will be removed from this device."
        else -> "The scale and its $lessonCount lessons will be removed from this device."
    }
}
