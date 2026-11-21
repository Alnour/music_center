// Named after its Swift twin, ScaleChordsRow.swift, which also holds the change row and the
// card; the colours class is shared by all three.
@file:Suppress("MatchingDeclarationName")

package com.alnourkhalifa.scales.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.board.BoardChordChipContent
import com.alnourkhalifa.scales.core.music.CapoChordShape
import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.ChordFingering
import com.alnourkhalifa.scales.core.music.ScaleChord

/**
 * The colours the chord cards and the change row paint with, passed in because the phone
 * (Material 3) and the TV (TV Material) each have their own theme and this module depends on
 * neither. [matching] fills in Apple's system colours so the cards look as they do on iOS.
 *
 * @property fill a card's background, as UIKit's `secondarySystemFill`.
 * @property ringedFill the background of the card whose notes are ringed on the board.
 * @property ring the edge of that card, the board's chord colour (`FretboardPalette.chordRing`)
 *   so card and board match. An open capo shape's line takes it too, so "easy chord" reads in
 *   the same green the board rings chords in.
 * @property primary the notes, and major chord names, as SwiftUI's `.primary`.
 * @property secondary the numerals and the change sentence, as SwiftUI's `.secondary`.
 * @property orange the "was Gm" caption.
 * @property green the "new" caption and the notes a chord gained.
 * @property red the "gone" caption.
 * @property accent the "Back to …" button, as SwiftUI's tint.
 * @property isDark whether the screen is dark, which picks the light or dark chord tints.
 */
@Immutable
data class ChordChipColors(
    val fill: Color,
    val ringedFill: Color,
    val ring: Color,
    val primary: Color,
    val secondary: Color,
    val orange: Color,
    val green: Color,
    val red: Color,
    val accent: Color,
    val isDark: Boolean,
) {
    /**
     * SwiftUI's `.tertiary`, the notes of a chord the scale lost. Apple's tertiary label is
     * the text colour at 30 % where the secondary one is at 60 %, so it is half the secondary.
     */
    val tertiary: Color
        get() = secondary.copy(alpha = secondary.alpha * TERTIARY_SHARE)

    /** Apple's system colours for the cards, and the factory that picks them. */
    companion object {
        /** UIKit's `secondarySystemFill`: grey 120/120/128 at 16 % (light) or 32 % (dark). */
        private val fillLight = Color(red = 120, green = 120, blue = 128, alpha = 41)
        private val fillDark = Color(red = 120, green = 120, blue = 128, alpha = 82)

        /** Apple's `systemGreen` and `systemRed`, light and dark. */
        private val greenLight = Color(0xFF34C759)
        private val greenDark = Color(0xFF30D158)
        private val redLight = Color(0xFFFF3B30)
        private val redDark = Color(0xFFFF453A)

        /**
         * The iOS colours for a light or dark screen. The caller gives the theme's text
         * colours and accent; the ring comes from the board's palette so a ringed card is
         * edged in the colour the board rings the chord's notes in.
         */
        fun matching(
            isDark: Boolean,
            primary: Color,
            secondary: Color,
            accent: Color,
            palette: FretboardPalette,
        ): ChordChipColors {
            val ring = palette.chordRing
            var fill = fillLight
            var green = greenLight
            var red = redLight
            if (isDark) {
                fill = fillDark
                green = greenDark
                red = redDark
            }
            return ChordChipColors(
                fill = fill,
                ringedFill = ring.copy(alpha = RINGED_FILL_ALPHA),
                ring = ring,
                primary = primary,
                secondary = secondary,
                orange = ChordColors.orange(isDark),
                green = green,
                red = red,
                accent = accent,
                isDark = isDark,
            )
        }
    }
}

/** A ringed card is tinted with the ring colour at this opacity, as on iOS. */
private const val RINGED_FILL_ALPHA = 0.12f

/** Apple's tertiary label is half as strong as its secondary one. */
private const val TERTIARY_SHARE = 0.5f

/** A long chord name (G♯°, B♭m) shrinks to this share of its size before it is cut. */
private const val MINIMUM_NAME_SCALE = 0.6f

/** Swift's `GridItem(.adaptive(minimum:maximum: 200))`: cards never grow wider than this. */
private val CARD_MAXIMUM_WIDTH = 200.dp

/** The gap between cards, across and down. */
private val CARD_SPACING = 8.dp

/** What TalkBack says a tap on a card does, as Swift's `accessibilityHint`. */
private const val CARD_TAP_LABEL = "Ring the chord's notes on the board"

/**
 * The chords under the board on the Scales screen: the chords the scale makes, or, for a
 * pentatonic or blues scale, the chords it is played over (`Scale.boardChords`), with the
 * notes of a chord the scale doesn't hold shown faint and a line saying where the chords come
 * from. Twin of `ScaleChordsRow` in `Scales/Views/ScaleChordsRow.swift`.
 *
 * After a note change, every chord is shown as it is now, and the chords that used the
 * changed note are marked: what each one was, the note it gained in green, and the chords the
 * scale no longer makes crossed out. The marks stay until the player picks another scale.
 *
 * Cards wrap into as many rows as the width needs, so a wide window shows them in one row and
 * a phone in two, each card large enough to read from playing distance.
 *
 * What differs from Swift: the caller builds [chips] with `BoardChordChipContent.chips(scale,
 * change, capo)` (Swift's view takes the scale and the change and builds them itself), and passes
 * the [colors] because this module has no theme, and gives the [harmonyNote] only while no
 * change is showing (Swift checks `change == nil` itself). The grid is a `LazyVerticalGrid`,
 * so the caller gives it a bounded height, as the Scales screen does by capping it at a
 * third; the note under it is measured first, so the cards scroll before the note is cut.
 *
 * @param ringedChord the chord whose notes are ringed on the board, if any.
 * @param onTap called with the chord the player tapped.
 * @param cardMinimumWidth the narrowest a card may be. A phone uses narrow cards so seven
 *   chords make two rows; a wide window has room for roomier ones.
 * @param harmonyNote the line under the cards saying where borrowed chords come from
 *   (`Scale.harmonyNote`), or null for none.
 * @param capoNote the line above the cards saying which key's shapes the capo lines name
 *   (`CapoTransposition.note`), or null without a capo.
 */
@Composable
fun ScaleChordsRow(
    chips: List<BoardChordChipContent>,
    ringedChord: ScaleChord?,
    onTap: (ScaleChord) -> Unit,
    colors: ChordChipColors,
    modifier: Modifier = Modifier,
    cardMinimumWidth: Dp = 84.dp,
    harmonyNote: String? = null,
    capoNote: String? = null,
) {
    if (chips.isEmpty()) {
        val emptyStyle = TextStyle(color = colors.secondary, fontSize = 13.sp)
        BasicText(
            text = "This scale's notes don't stack into three-note chords.",
            style = emptyStyle,
            modifier = modifier,
        )
        return
    }
    val columns = CappedAdaptiveCells(minimum = cardMinimumWidth, maximum = CARD_MAXIMUM_WIDTH)
    // SwiftUI centres grid columns that do not fill the width; spacedBy alone would push
    // them to the start.
    val across = Arrangement.spacedBy(CARD_SPACING, Alignment.CenterHorizontally)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Above the cards, as on iOS, so it is read before the shape lines it explains.
        if (capoNote != null) {
            ChordFootnote(capoNote, "capoNote", colors)
        }
        // Weighted without filling: as tall as its cards, but never pushing the note out.
        val gridModifier =
            Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .testTag("boardChords")
        LazyVerticalGrid(
            columns = columns,
            modifier = gridModifier,
            contentPadding = PaddingValues(vertical = 2.dp),
            horizontalArrangement = across,
            verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
        ) {
            items(chips, key = { chip -> "${chip.chord.name}-${chip.kind}" }) { chip ->
                val isRinged = chip.chord == ringedChord
                BoardChordChip(
                    chip = chip,
                    isRinged = isRinged,
                    colors = colors,
                    onTap = { onTap(chip.chord) },
                )
            }
        }
        if (harmonyNote != null) {
            ChordFootnote(harmonyNote, "harmonyNote", colors)
        }
    }
}

/** One small grey line beside the cards: where borrowed chords come from (under them), or
 *  which key's shapes the capo lines name (above them). */
@Composable
private fun ChordFootnote(
    text: String,
    tag: String,
    colors: ChordChipColors,
) {
    val noteStyle =
        TextStyle(color = colors.secondary, fontSize = 13.sp, fontFamily = MusicFontFamily)
    BasicText(
        text = text,
        style = noteStyle,
        modifier = Modifier.testTag(tag),
    )
}

/**
 * Swift's `GridItem(.adaptive(minimum:maximum:))`: as many columns as fit at [minimum], each
 * sharing the width equally but never wider than [maximum]. Compose's `GridCells.Adaptive`
 * has no maximum, so on a wide tablet its cards would stretch past the iOS ones.
 */
private class CappedAdaptiveCells(
    private val minimum: Dp,
    private val maximum: Dp,
) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(
        availableSize: Int,
        spacing: Int,
    ): List<Int> {
        val minimumPx = minimum.roundToPx()
        val maximumPx = maximum.roundToPx()
        val fitting = (availableSize + spacing) / (minimumPx + spacing)
        val count = maxOf(1, fitting)
        val spaceForCards = availableSize - spacing * (count - 1)
        val sharedWidth = spaceForCards / count
        val cardWidth = minOf(sharedWidth, maximumPx)
        val sizes = mutableListOf<Int>()
        for (index in 0 until count) {
            sizes.add(cardWidth)
        }
        return sizes
    }

    override fun equals(other: Any?): Boolean =
        other is CappedAdaptiveCells && other.minimum == minimum && other.maximum == maximum

    override fun hashCode(): Int = 31 * minimum.hashCode() + maximum.hashCode()
}

/**
 * The last note change spelled out, with the way back to the scale it started from. Twin of
 * `BoardChangeRow` in `Scales/Views/ScaleChordsRow.swift`.
 *
 * What differs from Swift: it takes the words rather than the `BoardChange`
 * ([sentence] is `transformation.editSentence`, [beforeTitle] is
 * `transformation.beforeScale.title`), and the SF Symbol `arrow.uturn.backward` is drawn as a
 * path because Android has no symbol font.
 */
@Composable
fun BoardChangeRow(
    sentence: String,
    beforeTitle: String,
    onRevert: () -> Unit,
    colors: ChordChipColors,
    modifier: Modifier = Modifier,
) {
    val sentenceStyle =
        TextStyle(color = colors.secondary, fontSize = 13.sp, fontFamily = MusicFontFamily)
    val buttonStyle = sentenceStyle.copy(color = colors.accent)
    val arrowSize = with(LocalDensity.current) { 13.sp.toDp() }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text = sentence, style = sentenceStyle, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier =
                Modifier
                    .testTag("revertBoardChange")
                    .clickable(role = Role.Button, onClick = onRevert)
                    .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            UndoArrow(color = colors.accent, modifier = Modifier.size(arrowSize))
            BasicText(text = "Back to $beforeTitle", style = buttonStyle, maxLines = 1)
        }
    }
}

/**
 * SF Symbols' `arrow.uturn.backward`: a stroke that leaves to the left with an arrowhead,
 * bends round on the right and comes back underneath. Purely decorative; the button's words
 * say what it does.
 */
@Composable
private fun UndoArrow(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val lineWidth = width * 0.13f
        val top = height * 0.3f
        val bottom = height * 0.88f
        val turnRadius = (bottom - top) / 2f
        val tipX = width * 0.08f
        val turnStartX = width * 0.92f - turnRadius

        val path = Path()
        // The lower stroke, left to right, then the half-circle up the right side.
        path.moveTo(width * 0.35f, bottom)
        path.lineTo(turnStartX, bottom)
        val turn =
            Rect(
                left = turnStartX - turnRadius,
                top = top,
                right = turnStartX + turnRadius,
                bottom = bottom,
            )
        path.arcTo(turn, startAngleDegrees = 90f, sweepAngleDegrees = -180f, forceMoveTo = false)
        // The upper stroke back to the arrow's tip.
        path.lineTo(tipX, top)
        // The arrowhead.
        val headReach = height * 0.24f
        path.moveTo(tipX + headReach, top - headReach)
        path.lineTo(tipX, top)
        path.lineTo(tipX + headReach, top + headReach)

        val stroke = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path = path, color = color, style = stroke)
    }
}

/**
 * A chord card: numeral, name and notes. A note the scale doesn't hold is faint. After a
 * change, a line above says what the chord was, a gained note is green, and a chord the scale
 * lost is crossed out. The chord whose notes are ringed on the board sits on a tinted card
 * edged in the board's chord colour. Twin of the private `BoardChordChip` in
 * `Scales/Views/ScaleChordsRow.swift`.
 *
 * TalkBack reads the card as one element with the chip's spoken label and "selected" when it
 * is ringed. `clickable` (rather than `selectable`) is used so the card can carry Swift's
 * accessibility hint as its click label; the `selected` property is set alongside it.
 */
@Composable
private fun BoardChordChip(
    chip: BoardChordChipContent,
    isRinged: Boolean,
    colors: ChordChipColors,
    onTap: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    var background = colors.fill
    if (isRinged) {
        background = colors.ringedFill
    }
    var cardModifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = CARD_TAP_LABEL, onClick = onTap)
            .semantics {
                contentDescription = chip.accessibilityLabel
                selected = isRinged
            }.background(background, shape)
    if (isRinged) {
        // Compose borders are drawn inside the shape, like SwiftUI's `strokeBorder`.
        cardModifier = cardModifier.border(2.dp, colors.ring, shape)
    }
    Column(
        modifier =
            cardModifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        NumeralLine(chip, colors)
        ChordName(chip, colors)
        NotesLine(chip, colors)
        val capoShape = chip.capoShape
        if (capoShape != null) {
            CapoShapeLine(capoShape, colors)
        }
    }
}

/**
 * The shape to finger with the capo on, "G shape" or "Bm barre", in small type under the
 * notes. An open shape is green so the easy chords stand out across the row; a barre or
 * movable shape stays in the secondary colour.
 */
@Composable
private fun CapoShapeLine(
    shape: CapoChordShape,
    colors: ChordChipColors,
) {
    val isOpen = shape.fingering == ChordFingering.OPEN
    val color = if (isOpen) colors.ring else colors.secondary
    val style =
        CAPTION2.copy(
            color = color,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.SemiBold,
        )
    BasicText(
        text = shape.caption,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The numeral (♭III) in a semibold serif, with the change caption at the far end. */
@Composable
private fun NumeralLine(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
) {
    val numeralStyle =
        SUBHEADLINE.copy(
            color = colors.secondary,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        )
    val captionStyle =
        CAPTION2.copy(
            color = captionColor(chip.kind, colors),
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.SemiBold,
        )
    // The numeral takes what the caption leaves: a Row measures weighted children last, so
    // a long numeral (IIsus4) is cut short with an ellipsis rather than the "was Am" caption.
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = accidentalsInMusicFont(chip.chord.formulaNumeral),
            style = numeralStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // The caption is a blank line for an unchanged chord, so every card is as tall.
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
 * a glance; a chord the scale lost is crossed out in grey. A long name shrinks to 60 % on one
 * line rather than wrap, matching SwiftUI's `lineLimit(1)` with `minimumScaleFactor(0.6)`.
 */
@Composable
private fun ChordName(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
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
    val fullSize = TITLE2.fontSize
    val style =
        TITLE2.copy(
            color = color,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.Bold,
            textDecoration = decoration,
        )
    val autoSize =
        TextAutoSize.StepBased(
            minFontSize = fullSize * MINIMUM_NAME_SCALE,
            maxFontSize = fullSize,
            stepSize = 0.5.sp,
        )
    BasicText(
        text = chip.chord.name,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = autoSize,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The chord's notes, "D F♯ A", with any note it gained in bold green and any note the scale
 *  lacks faint. */
@Composable
private fun NotesLine(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
) {
    var color = colors.primary
    if (chip.kind == ChordChange.Kind.REMOVED) {
        color = colors.tertiary
    }
    val style =
        SUBHEADLINE.copy(
            color = color,
            fontFamily = MusicFontFamily,
        )
    BasicText(
        text = notesText(chip, colors),
        style = style,
        maxLines = 1,
        softWrap = false,
    )
}

/** The notes separated by spaces; a note the change added is bold and green, and a note of
 *  a borrowed chord that the scale lacks is in the secondary colour. */
private fun notesText(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
): AnnotatedString {
    val newNoteStyle = SpanStyle(color = colors.green, fontWeight = FontWeight.Bold)
    val outsideNoteStyle = SpanStyle(color = colors.secondary)
    return buildAnnotatedString {
        for ((index, note) in chip.chord.notes
            .withIndex()) {
            if (index > 0) {
                append(" ")
            }
            val isNew = note in chip.newNotes
            val isOutside = note in chip.outsideNotes
            if (isNew) {
                withStyle(newNoteStyle) { append(note.displayName) }
            } else if (isOutside) {
                withStyle(outsideNoteStyle) { append(note.displayName) }
            } else {
                append(note.displayName)
            }
        }
    }
}

/** "was Gm" in orange, "new" in green, "gone" in red; an unchanged chord's blank line. */
private fun captionColor(
    kind: ChordChange.Kind,
    colors: ChordChipColors,
): Color =
    when (kind) {
        ChordChange.Kind.UNCHANGED -> Color.Transparent
        ChordChange.Kind.CHANGED -> colors.orange
        ChordChange.Kind.ADDED -> colors.green
        ChordChange.Kind.REMOVED -> colors.red
    }

/**
 * One iOS text style at the default Dynamic Type setting: its font size and its line height,
 * with the glyphs centred in the line. In `sp`, so the cards grow with the system font size as
 * the iOS cards follow Dynamic Type.
 *
 * Why the line height is set: each card stacks three lines (numeral, chord name, notes), and
 * iOS gives each line its text style's full height. Roboto's own line is shorter, so without
 * it the stack would sit closer together than on iOS. `Trim.None` keeps the extra space above
 * and below every line, as iOS does, rather than trimming it from the first and last line.
 */
private fun iosTextStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
): TextStyle {
    val centered =
        LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        )
    return TextStyle(
        fontSize = fontSize,
        lineHeight = lineHeight,
        lineHeightStyle = centered,
    )
}

private val TITLE2 = iosTextStyle(fontSize = 22.sp, lineHeight = 28.sp)
private val SUBHEADLINE = iosTextStyle(fontSize = 15.sp, lineHeight = 20.sp)
private val CAPTION2 = iosTextStyle(fontSize = 11.sp, lineHeight = 13.sp)
