// Named after the composable, the Compose convention; its colours class is the only class.
@file:Suppress("MatchingDeclarationName")

package com.alnourkhalifa.scales.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.board.ChordTableMetrics
import com.alnourkhalifa.scales.core.board.ChordTableTextStyle
import com.alnourkhalifa.scales.core.music.KeyChordRow
import com.alnourkhalifa.scales.core.music.Triad
import com.alnourkhalifa.scales.core.music.TriadQuality
import com.alnourkhalifa.scales.core.music.keySignature

/**
 * The colours the chord table paints with, passed in because the phone (Material 3) and the
 * TV (TV Material) each have their own theme and this module depends on neither.
 *
 * @property onSurface the key names, as SwiftUI's `.primary`.
 * @property secondary the header numerals and key signatures, as SwiftUI's `.secondary`.
 * @property accent the home chord's background, as SwiftUI's `Color.accentColor`.
 * @property chordTints one colour per chord quality (Swift's `TriadQuality.tint`); a
 *   quality left out is drawn in [onSurface].
 */
@Immutable
data class ChordTableColors(
    val onSurface: Color,
    val secondary: Color,
    val accent: Color,
    val chordTints: Map<TriadQuality, Color>,
) {
    /** The tint for one chord, falling back to the plain text colour. */
    fun tint(quality: TriadQuality): Color = chordTints[quality] ?: onSurface
}

/** The home chord's background is the accent at this opacity, as on iOS. */
private const val HOME_BACKGROUND_ALPHA = 0.15f

/** A long chord name (E♭m, G♯°) shrinks to this share of its size before it is cut. */
private const val MINIMUM_CHORD_SCALE = 0.7f

/** SwiftUI's `Divider` is a hairline in the separator colour; this is that colour's weight. */
private const val DIVIDER_ALPHA = 0.2f

/**
 * Tables of every major or minor key with the chords that belong to each: a header of Roman
 * numerals, then one row per key. Twin of `KeyChordGrid` in `Scales/Views/ChordTablesView.swift`.
 *
 * What differs from Swift: the Major/Minor switch, the footnote and the screen title belong to
 * the phone and TV screens (Material 3 and TV Material), so this painter takes the [rows] to
 * show. SwiftUI's `ViewThatFits` picks between the bare grid and a sideways scroll; here the
 * table always sits in a `horizontalScroll`, which cannot move while the table fits, so it
 * looks and behaves the same. The roomy set caps the width at [ChordTableMetrics.contentMaxWidth]
 * and centres the table, as Swift's two `frame(maxWidth:)` calls do.
 */
@Composable
fun ChordTablesView(
    rows: List<KeyChordRow>,
    metrics: ChordTableMetrics,
    colors: ChordTableColors,
    modifier: Modifier = Modifier,
) {
    val maxWidth = metrics.contentMaxWidth?.dp ?: Dp.Unspecified
    val scrollState = rememberScrollState()
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
        ) {
            KeyChordGrid(rows = rows, metrics = metrics, colors = colors)
        }
    }
}

/**
 * The grid itself. Compose Foundation has no `Grid`, so every column gets the fixed width the
 * metrics name and the dividers are as wide as the whole table, which is what SwiftUI's grid
 * produces from the same fixed frames.
 */
@Composable
private fun KeyChordGrid(
    rows: List<KeyChordRow>,
    metrics: ChordTableMetrics,
    colors: ChordTableColors,
) {
    val firstRow = rows.firstOrNull() ?: return
    val tableWidth = tableWidth(metrics, chordCount = firstRow.chords.size)
    Column(modifier = Modifier.width(tableWidth)) {
        HeaderRow(row = firstRow, metrics = metrics, colors = colors)
        TableDivider(colors)
        for (row in rows) {
            KeyRow(row = row, metrics = metrics, colors = colors)
            TableDivider(colors)
        }
    }
}

/** The key column, one column per chord and the gaps between them. */
private fun tableWidth(
    metrics: ChordTableMetrics,
    chordCount: Int,
): Dp {
    val chordColumns = metrics.chordColumnWidth * chordCount
    val gaps = ChordTableMetrics.columnSpacing * chordCount
    val total = metrics.keyColumnWidth + chordColumns + gaps
    return total.dp
}

/** "Key" over the key column, then the numerals over the chords, all in a semibold serif. */
@Composable
private fun HeaderRow(
    row: KeyChordRow,
    metrics: ChordTableMetrics,
    colors: ChordTableColors,
) {
    val style =
        TextStyle(
            color = colors.secondary,
            fontSize = metrics.headerStyle.fontSize,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        )
    val centered = style.copy(textAlign = TextAlign.Center)
    Row(
        modifier = Modifier.padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(ChordTableMetrics.columnSpacing.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BasicText(
            text = "Key",
            style = style,
            modifier = Modifier.width(metrics.keyColumnWidth.dp),
        )
        for (chord in row.chords) {
            BasicText(
                text = chord.numeral,
                style = centered,
                modifier = Modifier.width(metrics.chordColumnWidth.dp),
            )
        }
    }
}

/** One key: its name and signature, then its chords with the home chord highlighted. */
@Composable
private fun KeyRow(
    row: KeyChordRow,
    metrics: ChordTableMetrics,
    colors: ChordTableColors,
) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ChordTableMetrics.columnSpacing.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeyCell(row = row, metrics = metrics, colors = colors)
        for ((step, chord) in row.chords.withIndex()) {
            ChordCell(
                triad = chord.triad,
                isHome = step == 0,
                metrics = metrics,
                colors = colors,
            )
        }
    }
}

/**
 * The key's root in bold with its key signature small beneath. TalkBack reads the pair as
 * one label, "B♭ major", rather than "B♭" and "2♭".
 */
@Composable
private fun KeyCell(
    row: KeyChordRow,
    metrics: ChordTableMetrics,
    colors: ChordTableColors,
) {
    val rootStyle =
        TextStyle(
            color = colors.onSurface,
            fontSize = metrics.keyStyle.fontSize,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.Bold,
        )
    val signatureStyle =
        TextStyle(
            color = colors.secondary,
            fontSize = metrics.smallStyle.fontSize,
            fontFamily = MusicFontFamily,
        )
    Column(
        modifier =
            Modifier
                .width(metrics.keyColumnWidth.dp)
                .clearAndSetSemantics { contentDescription = row.keyName },
    ) {
        BasicText(
            text = row.key.root.displayName,
            style = rootStyle,
            modifier = Modifier.iosLine(metrics.keyStyle),
        )
        BasicText(
            text = row.key.keySignature,
            style = signatureStyle,
            modifier = Modifier.iosLine(metrics.smallStyle),
        )
    }
}

/**
 * One chord name, tinted by quality so major, minor and diminished read at a glance. The home
 * chord (the key's own I or i) is heavier and sits on a soft accent tile.
 *
 * Long names shrink to 70 % of the size on one line rather than wrap, matching SwiftUI's
 * `lineLimit(1)` with `minimumScaleFactor(0.7)`; `TextAutoSize` steps down until it fits.
 */
@Composable
private fun ChordCell(
    triad: Triad,
    isHome: Boolean,
    metrics: ChordTableMetrics,
    colors: ChordTableColors,
) {
    val weight = if (isHome) FontWeight.ExtraBold else FontWeight.SemiBold
    val fullSize = metrics.chordStyle.fontSize
    val smallestSize = fullSize * MINIMUM_CHORD_SCALE
    val style =
        TextStyle(
            color = colors.tint(triad.quality),
            fontSize = fullSize,
            fontFamily = MusicFontFamily,
            fontWeight = weight,
            textAlign = TextAlign.Center,
        )
    val autoSize =
        TextAutoSize.StepBased(
            minFontSize = smallestSize,
            maxFontSize = fullSize,
            stepSize = 0.5.sp,
        )
    var cellModifier =
        Modifier
            .size(width = metrics.chordColumnWidth.dp, height = metrics.cellHeight.dp)
            .clearAndSetSemantics { contentDescription = triad.spokenName }
    if (isHome) {
        val tile = colors.accent.copy(alpha = HOME_BACKGROUND_ALPHA)
        cellModifier = cellModifier.background(tile, RoundedCornerShape(8.dp))
    }
    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        BasicText(
            text = triad.name,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            autoSize = autoSize,
        )
    }
}

/**
 * The hairline under the header and under every key, as wide as the table. One physical pixel
 * thick, as SwiftUI's `Divider` is on every screen density.
 */
@Composable
private fun TableDivider(colors: ChordTableColors) {
    val line = colors.onSurface.copy(alpha = DIVIDER_ALPHA)
    val onePixel = with(LocalDensity.current) { 1.toDp() }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(onePixel)
                .background(line),
    )
}

/** iOS fonts sit about a quarter of the font size of descent below the baseline. */
private const val IOS_DESCENT_SHARE = 0.25f

/**
 * Gives one line of text the iOS line height for [style], with its baseline where iOS puts
 * it (a descent's height above the bottom of the line).
 *
 * Why: [MusicFontFamily] comes first in the text's font list, and the bundled font's tall
 * line metrics make every line about 1.8 times its font size; `TextStyle.lineHeight` cannot
 * shrink a line below its font's own extent. Without this the key column stacks the root and
 * the key signature far apart and every row is a third taller than on iOS. The glyphs keep
 * their size and may overhang the line box, which Compose does not clip.
 */
private fun Modifier.iosLine(style: ChordTableTextStyle): Modifier =
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

/** Each iOS text style's line height at the default Dynamic Type setting. */
private val ChordTableTextStyle.lineHeight: TextUnit
    get() =
        when (this) {
            ChordTableTextStyle.TITLE -> 34.sp
            ChordTableTextStyle.TITLE2 -> 28.sp
            ChordTableTextStyle.TITLE3 -> 25.sp
            ChordTableTextStyle.CALLOUT -> 21.sp
            ChordTableTextStyle.SUBHEADLINE -> 20.sp
            ChordTableTextStyle.FOOTNOTE -> 18.sp
            ChordTableTextStyle.CAPTION2 -> 13.sp
        }

/**
 * The size of each iOS text style at the default Dynamic Type setting. These are `sp`, so
 * the table grows with the system font size the way the iOS table follows Dynamic Type.
 */
private val ChordTableTextStyle.fontSize: TextUnit
    get() =
        when (this) {
            ChordTableTextStyle.TITLE -> 28.sp
            ChordTableTextStyle.TITLE2 -> 22.sp
            ChordTableTextStyle.TITLE3 -> 20.sp
            ChordTableTextStyle.CALLOUT -> 16.sp
            ChordTableTextStyle.SUBHEADLINE -> 15.sp
            ChordTableTextStyle.FOOTNOTE -> 13.sp
            ChordTableTextStyle.CAPTION2 -> 11.sp
        }
