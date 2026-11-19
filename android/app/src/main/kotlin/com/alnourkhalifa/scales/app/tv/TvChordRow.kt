package com.alnourkhalifa.scales.app.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.alnourkhalifa.scales.core.board.BoardChordChipContent
import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.ScaleChord
import com.alnourkhalifa.scales.core.session.BoardChange
import com.alnourkhalifa.scales.core.session.SessionState
import com.alnourkhalifa.scales.ui.ChordChipColors
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.MusicFontFamily
import com.alnourkhalifa.scales.ui.accidentalsInMusicFont
import com.alnourkhalifa.scales.ui.tint

/** At most this many chips share the row's width; a longer scale scrolls the rest in. */
private const val MAXIMUM_CHIPS_ACROSS = 8

/** A focused chip grows by this much, the tv-material cue that it has focus. */
private const val FOCUSED_CHIP_SCALE = 1.06f

/** A long chord name shrinks to this share of its size before it is cut, as on the phone. */
private const val MINIMUM_NAME_SCALE = 0.6f

/** Each line is this many times its font size tall, close to iOS's text styles. */
private const val LINE_HEIGHT_SHARE = 1.2f

/** Font sizes in screen units: the numeral and caption line, the chord name, the notes. */
private const val NUMERAL_UNITS = 2.2f
private const val NAME_UNITS = 4f
private const val NOTES_UNITS = 2.4f

/** The change sentence, the button's words and the empty footnote, in screen units. */
private const val SMALL_TEXT_UNITS = 2.2f

/** The "Back to …" button's height in screen units: its words' line plus a little room. */
private const val BUTTON_HEIGHT_UNITS = 4.4f

/** What the row says when the scale makes no three-note chords (a two-note scale). */
private const val EMPTY_FOOTNOTE = "This scale's notes don't stack into three-note chords."

/**
 * The chords of the scale on the board, as a row of chips under the TV board, with the last
 * note change and its "Back to …" button above them while that change is showing. The TV twin
 * of the phone's `ScaleChordsRow` and `BoardChangeRow`.
 *
 * Why its own composable rather than the phone's grid: the phone's cards are touch targets in
 * a grid that wraps; a remote moves focus one step at a time, so the TV puts every chord on
 * one row (Left and Right walk it, Down from the board enters it) and draws focus with the
 * tv-material grow-and-border cue. OK on a chip rings its notes on the board; OK again clears
 * the ring. Sizes are shares of the screen height, as the rest of the TV layout is, so the row
 * keeps to about a seventh of the screen at 720p, 1080p and 4K.
 *
 * Back from a chip or the button returns to the board before it can leave the app, the order
 * the TV's Back table sets (sublist, list, panel, chips, board, exit); `TvApp` does that
 * through [TvBoardFocusLinks.returnToBoard], which this row keeps informed.
 *
 * @param onTap called with the chord whose chip got OK; the caller toggles its ring.
 * @param onRevert called by "Back to …"; focus moves to the board first, because the button
 *   disappears with the change and focus would otherwise be dropped.
 * @param focus the board's focus links (see [TvBoardFocus]).
 */
@Composable
fun TvChordRow(
    state: SessionState,
    onTap: (ScaleChord) -> Unit,
    onRevert: () -> Unit,
    focus: TvBoardFocus,
    modifier: Modifier = Modifier,
) {
    val unit = screenUnit()
    val isDark = state.settings.tvUsesDarkTheme
    val palette = FretboardPalette.matching(isDark)
    val colors =
        ChordChipColors.matching(
            isDark = isDark,
            primary = palette.title,
            secondary = palette.secondaryText,
            accent = MaterialTheme.colorScheme.primary,
            palette = palette,
        )
    val change = state.currentBoardChange
    val chips = BoardChordChipContent.chips(state.scale, change)
    val rowModifier =
        modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                focus.links.isBelowBoardFocused = focusState.hasFocus
            }.onPreviewKeyEvent { event -> holdFocusOnBack(event) }
            .focusGroup()
    Column(
        modifier = rowModifier,
        verticalArrangement = Arrangement.spacedBy(unit * 0.8f),
    ) {
        if (change != null) {
            ChangeLine(change, colors, unit, focus, onRevert)
        }
        if (chips.isEmpty()) {
            EmptyFootnote(colors, unit, focus, hasChangeLine = change != null)
        } else {
            ChipRow(chips, state.ringedChord, colors, unit, focus, change != null, onTap)
        }
    }
}

/**
 * Uses up Back's key press while a chip or the button has focus. Left alone, Compose answers
 * that press by moving focus out of the row, so by the time the release reaches `TvApp`'s
 * Back handler the row no longer has focus, and Back would leave the app instead of returning
 * to the board. The release is left to go on to the handler, which moves focus itself.
 */
private fun holdFocusOnBack(event: KeyEvent): Boolean {
    val isBack = event.key == Key.Back
    val isPress = event.type == KeyEventType.KeyDown
    return isBack && isPress
}

/**
 * One hundredth of the screen height: the TV layout's unit, so every size below scales with
 * the picture rather than with the TV's density setting.
 */
@Composable
private fun screenUnit(): Dp {
    val heightPixels = LocalWindowInfo.current.containerSize.height
    val density = LocalDensity.current
    val height = with(density) { heightPixels.toDp() }
    return height / 100f
}

/** A text style of [multiple] screen units, its line a little taller than the glyphs. */
private fun unitStyle(
    unit: Dp,
    multiple: Float,
): TextStyle {
    // TVDisplayView fixes the font scale at 1, so sp and dp agree here and text keeps its
    // share of the screen whatever the TV's font size setting.
    val fontSize = (unit.value * multiple).sp
    val lineHeight = (unit.value * multiple * LINE_HEIGHT_SHARE).sp
    // Each line keeps its full height, the extra space split above and below the glyphs, as
    // iOS's text styles do, rather than trimmed from the first and last line.
    val centered =
        LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        )
    return TextStyle(fontSize = fontSize, lineHeight = lineHeight, lineHeightStyle = centered)
}

/**
 * "Lowered F♯ (7) to F (♭7)." and the button back to the scale before the change.
 * The button is where Down from the board lands while it shows, so the way back is one press
 * away; Up returns to the board and Down goes on to the chips.
 */
@Composable
private fun ChangeLine(
    change: BoardChange,
    colors: ChordChipColors,
    unit: Dp,
    focus: TvBoardFocus,
    onRevert: () -> Unit,
) {
    val transformation = change.transformation
    val smallStyle = unitStyle(unit, SMALL_TEXT_UNITS)
    val sentenceStyle = smallStyle.copy(color = colors.secondary, fontFamily = MusicFontFamily)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(unit * 2f),
    ) {
        BasicText(
            text = transformation.editSentence,
            style = sentenceStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        RevertButton(
            beforeTitle = transformation.beforeScale.title,
            accent = colors.accent,
            unit = unit,
            focus = focus,
            onRevert = onRevert,
        )
    }
}

/**
 * "Back to G Major" in the accent colour, filled with it while focused so the player can see
 * from the sofa that OK will undo the change.
 */
@Composable
private fun RevertButton(
    beforeTitle: String,
    accent: Color,
    unit: Dp,
    focus: TvBoardFocus,
    onRevert: () -> Unit,
) {
    val buttonColors =
        ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = accent,
            focusedContainerColor = accent,
            focusedContentColor = Color.White,
            pressedContainerColor = accent,
            pressedContentColor = Color.White,
        )
    val padding = PaddingValues(horizontal = unit * 1.6f, vertical = 0.dp)
    val buttonModifier =
        Modifier
            .testTag("revertBoardChange")
            // tv-material's 40 dp minimum would make this line a tenth of the screen and
            // squeeze the board; a fixed height in screen units keeps it to one text line.
            .height(unit * BUTTON_HEIGHT_UNITS)
            .focusRequester(focus.below)
            .focusProperties {
                canFocus = focus.canFocus
                up = focus.board
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
    val labelStyle = unitStyle(unit, SMALL_TEXT_UNITS).copy(fontFamily = MusicFontFamily)
    Button(
        onClick = {
            focus.board.requestFocus()
            onRevert()
        },
        modifier = buttonModifier,
        colors = buttonColors,
        contentPadding = padding,
    ) {
        // tv-material's Text takes the button's content colour, which changes with focus.
        Text(
            text = "Back to $beforeTitle",
            style = labelStyle,
            maxLines = 1,
        )
    }
}

/**
 * The footnote for a scale with no three-note chords. It still carries [TvBoardFocus.below]
 * on an empty focus group when nothing else does, so Down from the board finds the requester
 * attached and simply stays where it is.
 */
@Composable
private fun EmptyFootnote(
    colors: ChordChipColors,
    unit: Dp,
    focus: TvBoardFocus,
    hasChangeLine: Boolean,
) {
    var footnoteModifier: Modifier = Modifier
    if (!hasChangeLine) {
        footnoteModifier =
            footnoteModifier
                .focusRequester(focus.below)
                .focusGroup()
    }
    val style = unitStyle(unit, SMALL_TEXT_UNITS).copy(color = colors.secondary)
    BasicText(text = EMPTY_FOOTNOTE, style = style, modifier = footnoteModifier)
}

/**
 * The chips, sharing the row's width so up to eight fit with no scrolling. Down from the board
 * lands on the ringed chip, or the first one when nothing is ringed; Down from the button
 * above comes back to the chip last left (`focusRestorer`, falling back to that same chip).
 * Left on the first chip and Right on the last do nothing, so focus never jumps off the row
 * sideways.
 */
@Composable
private fun ChipRow(
    chips: List<BoardChordChipContent>,
    ringedChord: ScaleChord?,
    colors: ChordChipColors,
    unit: Dp,
    focus: TvBoardFocus,
    hasChangeLine: Boolean,
    onTap: (ScaleChord) -> Unit,
) {
    val listState = rememberLazyListState()
    val entryIndex by remember(chips, ringedChord) {
        derivedStateOf { entryIndex(chips, ringedChord, listState.layoutInfo) }
    }
    val entryRequester = remember { FocusRequester() }
    val links = ChipFocusLinks(focus, entryRequester)
    val gap = unit * 1.2f
    // Room for a focused end chip to grow without being clipped by the row.
    val edge = unit * 0.8f
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val across = minOf(chips.size, MAXIMUM_CHIPS_ACROSS)
        val gaps = gap * (across - 1).toFloat()
        val chipWidth = (maxWidth - edge * 2f - gaps) / across.toFloat()
        val rowModifier =
            Modifier
                .fillMaxWidth()
                .testTag("tvChips")
                .focusRestorer(entryRequester)
                .focusGroup()
        LazyRow(
            state = listState,
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(horizontal = edge, vertical = unit * 0.6f),
        ) {
            itemsIndexed(
                chips,
                key = { index, chip -> "$index-${chip.chord.triad.name}-${chip.kind}" },
            ) { index, chip ->
                val chipModifier =
                    Modifier
                        .width(chipWidth)
                        .testTag("tvChip-$index")
                        .then(chipFocus(index, chips.lastIndex, entryIndex, links, hasChangeLine))
                TvChordChip(
                    chip = chip,
                    isRinged = chip.chord == ringedChord,
                    colors = colors,
                    unit = unit,
                    onClick = { onTap(chip.chord) },
                    modifier = chipModifier,
                )
            }
        }
    }
}

/** The requesters a chip's focus rules point at, gathered so they travel as one value. */
private class ChipFocusLinks(
    val board: TvBoardFocus,
    /** The chip the row's `focusRestorer` falls back to: ringed, or first. */
    val entry: FocusRequester,
)

/**
 * One chip's focus rules. Up goes to the button above when a change shows, otherwise straight
 * to the board. Left on the first chip and Right on the last are cancelled, so focus stays on
 * the row. The entry chip carries the row's fallback requester and, with no button above it,
 * the board's Down target. Nothing can take focus while the panel is open.
 */
private fun chipFocus(
    index: Int,
    lastIndex: Int,
    entryIndex: Int,
    links: ChipFocusLinks,
    hasChangeLine: Boolean,
): Modifier {
    val focus = links.board
    val isEntry = index == entryIndex
    var modifier: Modifier = Modifier
    if (isEntry) {
        modifier = modifier.focusRequester(links.entry)
    }
    if (isEntry && !hasChangeLine) {
        modifier = modifier.focusRequester(focus.below)
    }
    val upTarget = if (hasChangeLine) focus.below else focus.board
    val isFirst = index == 0
    val isLast = index == lastIndex
    return modifier.focusProperties {
        canFocus = focus.canFocus
        up = upTarget
        if (isFirst) {
            left = FocusRequester.Cancel
        }
        if (isLast) {
            right = FocusRequester.Cancel
        }
    }
}

/**
 * The chip focus enters on: the ringed one when it is on screen, else the first chip, else
 * (in a long row scrolled along) the first chip on screen. A chip off screen isn't composed,
 * so its requester couldn't take focus.
 */
private fun entryIndex(
    chips: List<BoardChordChipContent>,
    ringedChord: ScaleChord?,
    layoutInfo: LazyListLayoutInfo,
): Int {
    var preferred = 0
    for ((index, chip) in chips.withIndex()) {
        if (chip.chord == ringedChord) {
            preferred = index
        }
    }
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) {
        return preferred
    }
    for (item in visible) {
        if (item.index == preferred) {
            return preferred
        }
    }
    return visible.first().index
}

/**
 * One chord: numeral and change caption, the name tinted by quality, and its notes. The ringed
 * chip is edged and tinted in the board's chord colour, as on the phone, so it reads as the
 * chord whose notes are ringed above; focus adds a light edge and grows the chip.
 *
 * TalkBack reads one sentence per chip ("V, D major: D, F♯, A. Was D major.") and whether it
 * is ringed, the same label the phone's cards carry. Every line sets its own text colour, so
 * only the surface's container colours are given.
 */
@Composable
private fun TvChordChip(
    chip: BoardChordChipContent,
    isRinged: Boolean,
    colors: ChordChipColors,
    unit: Dp,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(unit * 1.2f)
    val focusedFill = colors.fill.copy(alpha = minOf(1f, colors.fill.alpha * 1.8f))
    val surfaceColors =
        SelectableSurfaceDefaults.colors(
            containerColor = colors.fill,
            focusedContainerColor = focusedFill,
            pressedContainerColor = focusedFill,
            selectedContainerColor = colors.ringedFill,
            focusedSelectedContainerColor = colors.ringedFill,
            pressedSelectedContainerColor = colors.ringedFill,
        )
    val ringBorder = Border(BorderStroke(2.dp, colors.ring), shape = shape)
    val focusBorder = Border(BorderStroke(2.dp, colors.primary), shape = shape)
    val focusedRingBorder = Border(BorderStroke(3.dp, colors.ring), shape = shape)
    val borders =
        SelectableSurfaceDefaults.border(
            focusedBorder = focusBorder,
            pressedBorder = focusBorder,
            selectedBorder = ringBorder,
            focusedSelectedBorder = focusedRingBorder,
            pressedSelectedBorder = focusedRingBorder,
        )
    val semanticsModifier =
        modifier.semantics {
            contentDescription = chip.accessibilityLabel
            selected = isRinged
        }
    Surface(
        selected = isRinged,
        onClick = onClick,
        modifier = semanticsModifier,
        shape = SelectableSurfaceDefaults.shape(shape = shape),
        colors = surfaceColors,
        scale = SelectableSurfaceDefaults.scale(focusedScale = FOCUSED_CHIP_SCALE),
        border = borders,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(horizontal = unit * 1.2f, vertical = unit * 0.8f)
                    .clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(unit * 0.2f),
        ) {
            NumeralLine(chip, colors, unit)
            ChordName(chip, colors, unit)
            NotesLine(chip, colors, unit)
        }
    }
}

/** The numeral (♭III) in a semibold serif, the change caption at the far end. */
@Composable
private fun NumeralLine(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
    unit: Dp,
) {
    val numeralStyle =
        unitStyle(unit, NUMERAL_UNITS).copy(
            color = colors.secondary,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        )
    // "was Gm" in orange, "new" in green, "gone" in red; an unchanged chord's blank line.
    val captionColor =
        when (chip.kind) {
            ChordChange.Kind.UNCHANGED -> Color.Transparent
            ChordChange.Kind.CHANGED -> colors.orange
            ChordChange.Kind.ADDED -> colors.green
            ChordChange.Kind.REMOVED -> colors.red
        }
    val captionStyle =
        unitStyle(unit, NUMERAL_UNITS).copy(
            color = captionColor,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.SemiBold,
        )
    // A fixed height keeps every chip's lines level whatever glyphs a line holds: the flat
    // in a numeral (♭VII) is drawn from the music font, whose metrics differ from the
    // serif letters' beside it.
    val lineHeight = unit * (NUMERAL_UNITS * LINE_HEIGHT_SHARE)
    // The numeral takes what the caption leaves, so a long numeral is cut, not "was Am".
    Row(
        modifier = Modifier.height(lineHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = accidentalsInMusicFont(chip.chord.formulaNumeral),
            style = numeralStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // The caption is a blank line for an unchanged chord, so every chip is as tall.
        BasicText(
            text = chip.caption,
            style = captionStyle,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * The chord's name, large and bold, tinted by quality so major, minor and diminished read at
 * a glance; a chord the scale lost is crossed out in grey. A long name shrinks rather than
 * wraps, so every chip keeps one line for it.
 */
@Composable
private fun ChordName(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
    unit: Dp,
) {
    val isRemoved = chip.kind == ChordChange.Kind.REMOVED
    var color =
        chip.chord.triad.quality
            .tint(colors.primary, colors.isDark)
    var decoration = TextDecoration.None
    if (isRemoved) {
        color = colors.secondary
        decoration = TextDecoration.LineThrough
    }
    val style =
        unitStyle(unit, NAME_UNITS).copy(
            color = color,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.Bold,
            textDecoration = decoration,
        )
    val fullSize = style.fontSize
    val autoSize =
        TextAutoSize.StepBased(
            minFontSize = fullSize * MINIMUM_NAME_SCALE,
            maxFontSize = fullSize,
            stepSize = 0.5.sp,
        )
    BasicText(
        text = chip.chord.triad.name,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = autoSize,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The chord's notes, "D F♯ A", any note it gained in bold green. */
@Composable
private fun NotesLine(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
    unit: Dp,
) {
    var color = colors.primary
    if (chip.kind == ChordChange.Kind.REMOVED) {
        color = colors.tertiary
    }
    val style = unitStyle(unit, NOTES_UNITS).copy(color = color, fontFamily = MusicFontFamily)
    BasicText(
        text = notesText(chip, colors.green),
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

/** The notes separated by spaces; a note the change added is bold and green. */
private fun notesText(
    chip: BoardChordChipContent,
    green: Color,
): AnnotatedString {
    val newNoteStyle = SpanStyle(color = green, fontWeight = FontWeight.Bold)
    return buildAnnotatedString {
        for ((index, note) in chip.chord.notes
            .withIndex()) {
            if (index > 0) {
                append(" ")
            }
            val isNew = note in chip.newNotes
            if (isNew) {
                withStyle(newNoteStyle) { append(note.displayName) }
            } else {
                append(note.displayName)
            }
        }
    }
}
