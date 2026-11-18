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
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.board.BoardChordChipContent
import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.ScaleChord

/**
 * The colours the chord cards and the change row paint with, passed in because the phone
 * (Material 3) and the TV (TV Material) each have their own theme and this module depends on
 * neither. [matching] fills in Apple's system colours so the cards look as they do on iOS.
 *
 * @property fill a card's background, as UIKit's `secondarySystemFill`.
 * @property ringedFill the background of the card whose notes are ringed on the board.
 * @property ring the edge of that card, the board's chord colour (`FretboardPalette.chordRing`)
 *   so card and board match.
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
 * The chords the scale on the board makes, as cards under the board on the Scales screen. Twin
 * of `ScaleChordsRow` in `Scales/Views/ScaleChordsRow.swift`.
 *
 * After a note change, every chord is shown as it is now, and the chords that used the
 * changed note are marked: what each one was, the note it gained in green, and the chords the
 * scale no longer makes crossed out. The marks stay until the player picks another scale.
 *
 * Cards wrap into as many rows as the width needs, so a wide window shows them in one row and
 * a phone in two, each card large enough to read from playing distance.
 *
 * What differs from Swift: the caller builds [chips] with `BoardChordChipContent.chips(scale,
 * change)` (Swift's view takes the scale and the change and builds them itself), and passes
 * the [colors] because this module has no theme. The grid is a `LazyVerticalGrid`, so the
 * caller gives it a bounded height, as the Scales screen does by capping it at a third.
 *
 * @param ringedChord the chord whose notes are ringed on the board, if any.
 * @param onTap called with the chord the player tapped.
 * @param cardMinimumWidth the narrowest a card may be. A phone uses narrow cards so seven
 *   chords make two rows; a wide window has room for roomier ones.
 */
@Composable
fun ScaleChordsRow(
    chips: List<BoardChordChipContent>,
    ringedChord: ScaleChord?,
    onTap: (ScaleChord) -> Unit,
    colors: ChordChipColors,
    modifier: Modifier = Modifier,
    cardMinimumWidth: Dp = 84.dp,
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
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.testTag("boardChords"),
        contentPadding = PaddingValues(vertical = 2.dp),
        horizontalArrangement = across,
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
    ) {
        items(chips, key = { chip -> "${chip.chord.triad.name}-${chip.kind}" }) { chip ->
            val isRinged = chip.chord == ringedChord
            BoardChordChip(
                chip = chip,
                isRinged = isRinged,
                colors = colors,
                onTap = { onTap(chip.chord) },
            )
        }
    }
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
 * A chord card: numeral, name and notes. After a change, a line above says what the chord was,
 * a gained note is green, and a chord the scale lost is crossed out. The chord whose notes are
 * ringed on the board sits on a tinted card edged in the board's chord colour. Twin of the
 * private `BoardChordChip` in `Scales/Views/ScaleChordsRow.swift`.
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
    }
}

/** The numeral (♭III) in a semibold serif, with the change caption at the far end. */
@Composable
private fun NumeralLine(
    chip: BoardChordChipContent,
    colors: ChordChipColors,
) {
    val numeralStyle =
        TextStyle(
            color = colors.secondary,
            fontSize = SUBHEADLINE.fontSize,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        )
    val captionStyle =
        TextStyle(
            color = captionColor(chip.kind, colors),
            fontSize = CAPTION2.fontSize,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.SemiBold,
        )
    // The numeral takes what the caption leaves: a Row measures weighted children last, so
    // a long numeral (IIsus4) is cut short with an ellipsis rather than the "was Am" caption.
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = chip.chord.formulaNumeral,
            style = numeralStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .iosLine(SUBHEADLINE),
        )
        // The caption is a blank line for an unchanged chord, so every card is as tall.
        BasicText(
            text = chip.caption,
            style = captionStyle,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.iosLine(CAPTION2),
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
        TextStyle(
            color = color,
            fontSize = fullSize,
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
        text = chip.chord.triad.name,
        style = style,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = autoSize,
        modifier =
            Modifier
                .fillMaxWidth()
                .iosLine(TITLE2),
    )
}

/** The chord's notes, "D F♯ A", with any note it gained in bold green. */
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
        TextStyle(
            color = color,
            fontSize = SUBHEADLINE.fontSize,
            fontFamily = MusicFontFamily,
        )
    BasicText(
        text = notesText(chip, colors.green),
        style = style,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.iosLine(SUBHEADLINE),
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
 * One iOS text style at the default Dynamic Type setting: its font size and its line height.
 * In `sp`, so the cards grow with the system font size as the iOS cards follow Dynamic Type.
 */
private class IosTextStyle(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
)

private val TITLE2 = IosTextStyle(fontSize = 22.sp, lineHeight = 28.sp)
private val SUBHEADLINE = IosTextStyle(fontSize = 15.sp, lineHeight = 20.sp)
private val CAPTION2 = IosTextStyle(fontSize = 11.sp, lineHeight = 13.sp)

/** iOS fonts sit about a quarter of the font size of descent below the baseline. */
private const val IOS_DESCENT_SHARE = 0.25f

/**
 * Gives one line of text the iOS line height for [style], with its baseline where iOS puts it
 * (a descent's height above the bottom of the line).
 *
 * Why: [MusicFontFamily] comes first in the text's font list, and the bundled font's tall line
 * metrics make every line about 1.8 times its font size, which `TextStyle.lineHeight` cannot
 * shrink. Without this each card is a third taller than on iOS and seven chords no longer fit
 * the third of the screen the Scales screen gives them. The glyphs keep their size and may
 * overhang the line box, which Compose does not clip. Same approach as the chord tables.
 */
private fun Modifier.iosLine(style: IosTextStyle): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val lineHeight = style.lineHeight.roundToPx()
        val descent = (style.fontSize * IOS_DESCENT_SHARE).roundToPx()
        val targetBaseline = lineHeight - descent
        val baseline = placeable[FirstBaseline]
        val top = targetBaseline - baseline
        layout(placeable.width, lineHeight) {
            placeable.place(0, top)
        }
    }
