package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.ChordChangeReport
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ReferenceDate
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleChord
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleIdentifier
import com.alnourkhalifa.scales.core.music.ScaleMatch
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation
import com.alnourkhalifa.scales.core.music.TransformationLesson
import com.alnourkhalifa.scales.core.session.ScaleLibrary
import com.alnourkhalifa.scales.core.session.ScaleLibraryError
import com.alnourkhalifa.scales.ui.ChordColors
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.MusicFontFamily
import com.alnourkhalifa.scales.ui.ScaleNotesRow
import com.alnourkhalifa.scales.ui.tint
import java.text.DateFormat
import java.util.Date
import java.util.UUID

// Twin of `Scales/Views/LessonViews.swift`: the lesson sheet, a kept lesson, one of the
// player's own scales, and the lesson pieces they share.

/**
 * Opens when the player picks a change: the lesson itself, then naming and saving it. Twin of
 * `LessonSheet` in `Scales/Views/LessonViews.swift`.
 *
 * A result the app recognises is named for the player ("This is G Mixolydian"); a new one asks
 * for a name before it can be saved, and is then added to My Scales. The sheet opens fully,
 * like an iOS sheet, since the chord list below is the point of the lesson.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonSheet(
    transformation: ScaleTransformation,
    onClose: () -> Unit,
) {
    val app = rememberScalesApplication()
    // Read so the page redraws whenever the library changes; the lists come from the library.
    app.libraryVersion.value
    val match = ScaleIdentifier.match(transformation.after, app.library.scales)
    var newName by remember(transformation) { mutableStateOf("") }
    var libraryError by remember { mutableStateOf<ScaleLibraryError?>(null) }
    val trimmedName = newName.trim()
    // The name shown while the player types, or the working name ("Major ♯5") before.
    val shownName = match?.name ?: trimmedName.ifEmpty { transformation.after.name }
    val save = {
        libraryError = libraryChange { app.library.saveLesson(transformation, newName) }
        if (libraryError == null) {
            onClose()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FormColors.current().page,
        dragHandle = null,
        modifier =
            Modifier
                .statusBarsPadding()
                .padding(top = 12.dp),
    ) {
        LessonSheetBar(
            canSave = match != null || trimmedName.isNotEmpty(),
            onCancel = onClose,
            onSave = save,
        )
        LazyColumn(
            modifier = Modifier.testTag("lessonSheetList"),
            verticalArrangement = Arrangement.spacedBy(FormMetrics.sectionSpacing),
        ) {
            lessonSummary(transformation.naming(shownName))
            item(key = "result") {
                LessonResultSection(transformation, match, newName) { typed -> newName = typed }
            }
            chordChanges(transformation.chordChanges)
        }
    }
    LibraryErrorAlert(error = libraryError, onDismiss = { libraryError = null })
}

/** "Cancel   Lesson   Save", the sheet's navigation bar on iOS. */
@Composable
private fun LessonSheetBar(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
            Text("Cancel")
        }
        Text(
            text = "Lesson",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Center),
        )
        TextButton(
            onClick = onSave,
            enabled = canSave,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .testTag("saveLessonButton"),
        ) {
            Text(text = "Save", fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * "The result": the scale's name when the app knows it, or a field to name a new one, and a
 * footnote saying where saving files the lesson.
 */
@Composable
private fun LessonResultSection(
    transformation: ScaleTransformation,
    match: ScaleMatch?,
    newName: String,
    onNameChange: (String) -> Unit,
) {
    FormSection(header = "The result", footer = match.saveFootnote) {
        if (match != null) {
            Row(
                modifier = Modifier.padding(FormMetrics.rowHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FormIcons.checkmarkSeal,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(FormMetrics.leadingIconSize),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "This is ${transformation.root.displayName} ${match.name}",
                    fontFamily = MusicFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            return@FormSection
        }
        Text(
            text = "The app doesn't know this scale yet. Name it to keep it with this lesson.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(FormMetrics.rowHorizontalPadding),
        )
        FormDivider()
        FormTextField(
            value = newName,
            onValueChange = onNameChange,
            placeholder = transformation.after.name,
            modifier = Modifier.testTag("scaleNameField"),
        )
    }
}

/**
 * A lesson the player kept, read again from the Lessons list. Twin of `LessonDetailView` in
 * `Scales/Views/LessonViews.swift`. "Show … on the Fretboard" puts the lesson's result on the
 * board in the lesson's key and opens the Scales tab.
 */
@Composable
fun LessonDetailView(
    lessonId: UUID,
    model: TransformTabModel,
    modifier: Modifier = Modifier,
) {
    val app = rememberScalesApplication()
    // Read so the page redraws whenever the library changes; the lists come from the library.
    app.libraryVersion.value
    val navigation = LocalAppNavigation.current
    val lesson =
        app.library.lessons
            .firstOrNull { saved -> saved.id == lessonId }
    if (lesson == null) {
        DeletedPage(title = "Lesson Deleted", onBack = model::goBack, modifier = modifier)
        return
    }
    val transformation = lesson.transformation
    FormPage(title = "Lesson", onBack = model::goBack, modifier = modifier) {
        lessonSummary(transformation)
        item(key = "show") {
            FormSection(footer = "Saved ${lesson.date.described}.") {
                FormButtonRow(title = "Show ${transformation.afterScale.title} on the Fretboard") {
                    app.session.choose(lesson.result, transformation.root)
                    navigation.open(AppTab.SCALES)
                }
            }
        }
        chordChanges(transformation.chordChanges)
    }
}

/**
 * One of the player's own scales: rename it, play it, change it again, or delete it, and read
 * the lessons that led to it. Twin of `CustomScaleDetailView` in
 * `Scales/Views/LessonViews.swift`.
 *
 * A changed name is saved when the player presses Done or leaves the page; a blank name puts
 * the old one back.
 */
@Composable
fun CustomScaleDetailView(
    scaleId: UUID,
    model: TransformTabModel,
    modifier: Modifier = Modifier,
) {
    val app = rememberScalesApplication()
    // Read so the page redraws whenever the library changes; the lists come from the library.
    app.libraryVersion.value
    val customScale = app.library.scale(scaleId)
    val savedName = customScale?.definition?.name ?: ""
    var name by remember(scaleId) { mutableStateOf(savedName) }
    var isConfirmingDeletion by remember { mutableStateOf(false) }
    var libraryError by remember { mutableStateOf<ScaleLibraryError?>(null) }
    val rename = {
        val outcome = renameScale(app.library, scaleId, name)
        name = outcome.shownName
        libraryError = outcome.error
    }
    val renameOnLeave by rememberUpdatedState(rename)
    DisposableEffect(scaleId) {
        onDispose { renameOnLeave() }
    }
    if (customScale == null) {
        DeletedPage(title = "Scale Deleted", onBack = model::goBack, modifier = modifier)
        return
    }
    FormPage(
        title = customScale.definition.name,
        modifier = modifier,
        hasLargeTitle = true,
        onBack = model::goBack,
    ) {
        item(key = "name") {
            FormSection(header = "Name") {
                FormTextField(name, { typed -> name = typed }, "Name", onDone = rename)
            }
        }
        customScaleSections(scaleId, model) { isConfirmingDeletion = true }
    }
    if (isConfirmingDeletion) {
        DeleteScaleDialog(
            scale = customScale,
            message = "The scale and the lessons filed under it will be removed from this device.",
            onDelete = {
                libraryError = libraryChange { app.library.deleteScale(scaleId) }
                if (libraryError == null) {
                    model.goBack()
                }
            },
            onClose = { isConfirmingDeletion = false },
        )
    }
    LibraryErrorAlert(error = libraryError, onDismiss = { libraryError = null })
}

/** The scale in the board's key, what to do with it, the lessons behind it, and Delete. */
private fun LazyListScope.customScaleSections(
    scaleId: UUID,
    model: TransformTabModel,
    askToDelete: () -> Unit,
) {
    item(key = "notes") {
        CustomScaleNotesSection(scaleId)
    }
    item(key = "actions") {
        val app = rememberScalesApplication()
        val navigation = LocalAppNavigation.current
        FormSection {
            FormButtonRow(title = "Show on the Fretboard") {
                app.session.choose(ScaleReference.Custom(scaleId))
                navigation.open(AppTab.SCALES)
            }
            FormDivider()
            FormButtonRow(title = "Change a Note in This Scale") {
                model.startFrom(ScaleReference.Custom(scaleId))
            }
        }
    }
    item(key = "lessons") {
        MadeFromLessonsSection(scaleId, model)
    }
    item(key = "delete") {
        FormSection {
            FormButtonRow(title = "Delete Scale", isDestructive = true, onClick = askToDelete)
        }
    }
}

/** "How you made it": the lessons filed under the scale, oldest first; hidden when none. */
@Composable
private fun MadeFromLessonsSection(
    scaleId: UUID,
    model: TransformTabModel,
) {
    val app = rememberScalesApplication()
    val lessons = app.library.lessons(ScaleReference.Custom(scaleId))
    if (lessons.isEmpty()) {
        return
    }
    FormSection(header = "How you made it") {
        for ((index, lesson) in lessons.withIndex()) {
            if (index > 0) {
                FormDivider()
            }
            NavigationRow(
                title = lesson.title,
                subtitle = lesson.transformation.editSentence,
                tag = "lesson-${lesson.id}",
                onClick = { model.open(TransformPage.Lesson(lesson.id)) },
            )
        }
    }
}

/** "In G": the saved scale spelled in the board's current key, and its formula. */
@Composable
private fun CustomScaleNotesSection(scaleId: UUID) {
    val app = rememberScalesApplication()
    val customScale = app.library.scale(scaleId) ?: return
    val root = app.sessionState.value.settings.root
    val shown = Scale(root, customScale.definition)
    val palette = FretboardPalette.matching(isSystemInDarkTheme())
    FormSection(header = "In ${root.displayName}") {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScaleNotesRow(scale = shown, noteSize = 15.dp, palette = palette)
        }
        FormDivider()
        Row(
            modifier = Modifier.padding(FormMetrics.rowHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Formula", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = customScale.definition.formula,
                fontFamily = MusicFontFamily,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** What a rename did: the name to show in the field now, and any error to report. */
private class RenameOutcome(
    val shownName: String,
    val error: ScaleLibraryError?,
)

/**
 * Saves a changed name, Swift's `rename()`. An unchanged name does nothing; a blank one, or
 * one the library refuses, puts the saved name back, and only a real failure is reported
 * (a blank name is the player clearing the field, not a mistake to scold).
 */
private fun renameScale(
    library: ScaleLibrary,
    scaleId: UUID,
    typedName: String,
): RenameOutcome {
    val current = library.scale(scaleId)
    if (current == null || typedName.trim() == current.definition.name) {
        return RenameOutcome(typedName, null)
    }
    val error =
        libraryChange { library.rename(scaleId, typedName) }
            ?: return RenameOutcome(typedName, null)
    val reported = if (error == ScaleLibraryError.EmptyName) null else error
    return RenameOutcome(current.definition.name, reported)
}

/**
 * The top of every lesson: what was changed, and the scale before and after with the changed
 * note ringed in both. Twin of `LessonSummarySection`.
 */
private fun LazyListScope.lessonSummary(transformation: ScaleTransformation) {
    item(key = "summary") {
        FormSection {
            Column(
                modifier = Modifier.padding(FormMetrics.rowHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = transformation.title,
                    fontFamily = MusicFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = transformation.editSentence,
                    fontFamily = MusicFontFamily,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FormDivider()
            LabelledScale("Before", transformation.beforeScale, transformation.beforeRing)
            FormDivider()
            LabelledScale("After", transformation.afterScale, transformation.afterRing)
        }
    }
}

@Composable
private fun LabelledScale(
    label: String,
    scale: Scale,
    ringed: Set<NoteName>,
) {
    val palette = FretboardPalette.matching(isSystemInDarkTheme())
    val noteSize = if (hasRegularWidth()) 20.dp else 14.dp
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = FormMetrics.rowHorizontalPadding, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ScaleNotesRow(
            scale = scale,
            noteSize = noteSize,
            palette = palette,
            highlightedNotes = ringed,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** The note that was lowered, raised or removed; nothing when a note was added. */
private val ScaleTransformation.beforeRing: Set<NoteName>
    get() {
        if (edit is ScaleEdit.Add) {
            return emptySet()
        }
        return setOf(Scale.spell(edit.degree, root))
    }

/** Where the edited note ended up; nothing when it was removed. */
private val ScaleTransformation.afterRing: Set<NoteName>
    get() {
        val result = edit.resultingDegree ?: return emptySet()
        return setOf(Scale.spell(result, root))
    }

/** Where saving files the lesson, under the result section. */
private val ScaleMatch?.saveFootnote: String
    get() =
        when (this) {
            is ScaleMatch.BuiltIn -> {
                "Saving files this lesson under ${type.displayName}."
            }

            is ScaleMatch.Custom -> {
                "Saving files this lesson under your scale ${scale.definition.name}."
            }

            null -> {
                "Saving adds the scale to My Scales, so you can play it on the fretboard and the TV."
            }
        }

/** "Nov 18, 2026, 2:40 PM", the iPhone's abbreviated date with a short time, in the
 *  phone's own language and time zone. */
private val ReferenceDate.described: String
    get() {
        val unixSeconds = secondsSinceReferenceDate + ReferenceDate.UNIX_SECONDS_AT_REFERENCE_DATE
        val milliseconds = (unixSeconds * 1000).toLong()
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        return format.format(Date(milliseconds))
    }

/**
 * Every chord of the scale, before and after, each with its sentence, and a tally under them.
 * Twin of `ChordChangesSection`. The card is one list item, so the sheet scrolls it as a
 * whole, as an iOS `Form` section scrolls.
 */
private fun LazyListScope.chordChanges(report: ChordChangeReport) {
    item(key = "chords") {
        FormSection(header = "Chords", footer = ChordChangeStyle.tally(report)) {
            for ((index, change) in report.changes.withIndex()) {
                if (index > 0) {
                    FormDivider()
                }
                ChordChangeRow(change)
            }
        }
    }
}

/**
 * One chord's story: the chord before and after, with lost notes in red and new ones in
 * green, a badge saying what happened, and the sentence that explains it.
 */
@Composable
private fun ChordChangeRow(change: ChordChange) {
    val colors = MaterialTheme.colorScheme
    val isUnchanged = change.kind == ChordChange.Kind.UNCHANGED
    val summaryColor = if (isUnchanged) colors.onSurfaceVariant else colors.onSurface
    Column(
        modifier =
            Modifier.padding(
                horizontal = FormMetrics.rowHorizontalPadding,
                vertical = 12.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChordChangeChips(change)
        Text(
            text = change.summary,
            fontFamily = MusicFontFamily,
            style = MaterialTheme.typography.bodyMedium,
            color = summaryColor,
        )
    }
}

/**
 * The chips of one change and its badge ("same", "changed", "gone", "new" on a tinted
 * capsule). The sentence under them says everything they show, so TalkBack skips them and
 * reads only the sentence, as VoiceOver does on iOS.
 */
@Composable
private fun ChordChangeChips(change: ChordChange) {
    val colors = MaterialTheme.colorScheme
    val badgeColor = ChordChangeStyle.color(change.kind, isSystemInDarkTheme())
    Row(
        modifier = Modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (change.kind == ChordChange.Kind.UNCHANGED) {
            ChordChip(change.before, highlighted = emptyList(), highlightColor = colors.onSurface)
        } else {
            ChordChip(change.before, change.notesGone, highlightColor = ChordChangeStyle.red)
            Icon(
                imageVector = FormIcons.arrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier =
                    Modifier
                        .padding(top = 20.dp)
                        .size(18.dp),
            )
            ChordChip(change.after, change.notesNew, highlightColor = ChordChangeStyle.green)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = ChordChangeStyle.title(change.kind),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = badgeColor,
            modifier =
                Modifier
                    .background(badgeColor.copy(alpha = 0.15f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A chord's numeral, name and notes, or a quiet "none" where a chord is missing. */
@Composable
private fun ChordChip(
    chord: ScaleChord?,
    highlighted: List<NoteName>,
    highlightColor: Color,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val numeralStyle = MaterialTheme.typography.labelMedium
    val nameStyle = MaterialTheme.typography.titleLarge
    Column(
        modifier = Modifier.widthIn(min = 56.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (chord == null) {
            Text(text = " ", style = numeralStyle)
            Text(text = "none", style = nameStyle, color = colors.outline)
            return@Column
        }
        Text(
            text = chord.formulaNumeral,
            style = numeralStyle,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = chord.triad.name,
            style = nameStyle,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.Bold,
            color =
                chord.triad.quality
                    .tint(colors.onSurface, isDark),
        )
        Text(
            text = ChordChangeStyle.notesText(chord.notes, highlighted, highlightColor),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = MusicFontFamily,
        )
    }
}

/** The words and colours a chord change is shown with. */
private object ChordChangeStyle {
    /** iOS's `systemRed` and `systemGreen`, for notes lost and gained. */
    val red = FormColors.destructive
    val green = Color(0xFF34C759)
    private val gray = Color(0xFF8E8E93)

    fun title(kind: ChordChange.Kind): String =
        when (kind) {
            ChordChange.Kind.UNCHANGED -> "same"
            ChordChange.Kind.CHANGED -> "changed"
            ChordChange.Kind.REMOVED -> "gone"
            ChordChange.Kind.ADDED -> "new"
        }

    fun color(
        kind: ChordChange.Kind,
        isDark: Boolean,
    ): Color =
        when (kind) {
            ChordChange.Kind.UNCHANGED -> gray
            ChordChange.Kind.CHANGED -> ChordColors.orange(isDark)
            ChordChange.Kind.REMOVED -> red
            ChordChange.Kind.ADDED -> green
        }

    /** "B D F♯" with the lost or gained notes in bold and in their colour. */
    fun notesText(
        notes: List<NoteName>,
        highlighted: List<NoteName>,
        highlightColor: Color,
    ): AnnotatedString {
        val highlightStyle = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)
        val builder = AnnotatedString.Builder()
        for ((index, note) in notes.withIndex()) {
            if (index > 0) {
                builder.append(" ")
            }
            if (note in highlighted) {
                builder.withStyle(highlightStyle) { append(note.displayName) }
            } else {
                builder.append(note.displayName)
            }
        }
        return builder.toAnnotatedString()
    }

    /** "2 chords change, 1 chord is gone, 4 stay the same." */
    fun tally(report: ChordChangeReport): String {
        if (report.changes.isEmpty()) {
            return "Neither scale stacks into three-note chords."
        }
        val counts = mutableMapOf<ChordChange.Kind, Int>()
        for (change in report.changes) {
            val counted = counts[change.kind] ?: 0
            counts[change.kind] = counted + 1
        }
        val parts = mutableListOf<String>()
        addPart(parts, counts[ChordChange.Kind.CHANGED], "1 chord changes", "chords change")
        addPart(parts, counts[ChordChange.Kind.REMOVED], "1 chord is gone", "chords are gone")
        addPart(parts, counts[ChordChange.Kind.ADDED], "1 new chord appears", "new chords appear")
        addPart(parts, counts[ChordChange.Kind.UNCHANGED], "1 stays the same", "stay the same")
        return parts.joinToString(separator = ", ") + "."
    }

    /** Adds "1 chord changes" or "3 chords change" when the count is above zero. */
    private fun addPart(
        parts: MutableList<String>,
        count: Int?,
        single: String,
        plural: String,
    ) {
        if (count == null || count == 0) {
            return
        }
        val part = if (count == 1) single else "$count $plural"
        parts.add(part)
    }
}
