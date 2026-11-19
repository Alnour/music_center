package com.alnourkhalifa.scales.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.board.FretboardGeometry
import com.alnourkhalifa.scales.core.music.FretboardLayout
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleTone
import com.alnourkhalifa.scales.core.session.NoteLabelStyle
import androidx.compose.ui.geometry.Size as CanvasSize
import com.alnourkhalifa.scales.core.board.Size as BoardSize

/** The test tag every board carries, fretboard or keyboard, phone or TV. */
const val BOARD_TEST_TAG: String = "board"

/**
 * Enough room for one board's labels: up to 24 fret numbers, six string names and the few
 * distinct note labels, so a redraw never measures text again.
 */
private const val BOARD_TEXT_CACHE_SIZE = 64

/**
 * Draws the neck with every scale tone circled and the roots highlighted. Shared by the phone
 * screen, the TV screen and the full-screen board. Twin of `Scales/Views/FretboardView.swift`,
 * which also holds `PhoneFretboard` from `ContentView.swift`, `ChordRing` and `toneColors`.
 *
 * What differs: the canvas hands over pixels, so the geometry is built in dp (pixels divided
 * by the density) and every length is multiplied back when drawn; otherwise the geometry's
 * minimum widths, which are points on iOS, would shrink to hairlines on a dense screen. Text
 * is measured with a font scale of 1, as SwiftUI's fixed sizes ignore Dynamic Type, so a
 * large system text setting never bursts the circles. Labels are measured once per size and
 * settings in `drawWithCache`. The board also carries its own spoken label ([scaleTitle]),
 * which iOS sets on the screen around it.
 */
@Composable
fun FretboardView(
    layout: FretboardLayout,
    scaleTitle: String,
    labelStyle: NoteLabelStyle,
    palette: FretboardPalette,
    modifier: Modifier = Modifier,
    ringedNotes: Set<NoteName> = emptySet(),
    thinStringOnTop: Boolean = true,
    isLeftHanded: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = BOARD_TEXT_CACHE_SIZE)
    val look = BoardLook(labelStyle, palette, ringedNotes)
    Spacer(
        modifier =
            modifier
                .boardSemantics("Fretboard showing $scaleTitle", labelStyle)
                .drawWithCache {
                    val canvas = BoardCanvas(textMeasurer, density)
                    val geometry =
                        FretboardGeometry(
                            size = canvas.dpSize(size),
                            fretCount = layout.fretCount,
                            stringCount = layout.strings.size,
                            isLeftHanded = isLeftHanded,
                        )
                    val painter = FretboardPainter(layout, geometry, look, thinStringOnTop, canvas)
                    onDrawBehind {
                        painter.draw(this)
                    }
                },
    )
}

/**
 * The fretboard on the phone's Scales screen, scrolling sideways when the frets do not fit.
 * Twin of the private `PhoneFretboard` in `Scales/Views/ContentView.swift`; the phone screen
 * itself is ported by another file, so this one is public.
 *
 * Cells never get narrower than 46 dp, so a note circle stays big enough to read and tap
 * on a narrow phone; on a wide screen they grow to fill the width. Left-handed players start
 * scrolled to the end, where their nut is.
 */
@Composable
fun PhoneFretboard(
    layout: FretboardLayout,
    scaleTitle: String,
    labelStyle: NoteLabelStyle,
    palette: FretboardPalette,
    modifier: Modifier = Modifier,
    ringedNotes: Set<NoteName> = emptySet(),
    thinStringOnTop: Boolean = true,
    isLeftHanded: Boolean = false,
) {
    BoxWithConstraints(modifier = modifier.background(palette.background)) {
        val columnUnits = FretboardGeometry.columnUnits(layout.fretCount)
        val cellWidth = phoneCellWidth(maxWidth, maxHeight, layout)
        val boardWidth = cellWidth * columnUnits.toFloat()
        val scrollState = rememberScrollState()
        LaunchedEffect(isLeftHanded, scrollState.maxValue) {
            var anchor = 0
            if (isLeftHanded) {
                anchor = scrollState.maxValue
            }
            scrollState.scrollTo(anchor)
        }
        FretboardView(
            layout = layout,
            scaleTitle = scaleTitle,
            labelStyle = labelStyle,
            palette = palette,
            modifier =
                Modifier
                    .horizontalScroll(scrollState)
                    .size(width = boardWidth, height = maxHeight),
            ringedNotes = ringedNotes,
            thinStringOnTop = thinStringOnTop,
            isLeftHanded = isLeftHanded,
        )
    }
}

/** The narrowest cell a phone fretboard gets. */
private val minimumPhoneCellWidth = 46.dp

/**
 * How wide a cell is made for each dp of string row height. Note circles are sized by the
 * smaller of the two, so cells narrower than this would waste the board's height.
 */
private const val CELL_WIDTH_PER_ROW_HEIGHT = 0.9f

/** The widest of: the minimum, the width shared out, and what the row height can use. */
private fun phoneCellWidth(
    width: Dp,
    height: Dp,
    layout: FretboardLayout,
): Dp {
    val columnUnits = FretboardGeometry.columnUnits(layout.fretCount).toFloat()
    val bandShares = FretboardGeometry.numberBandShare + FretboardGeometry.inlayBandShare
    val rowUnits =
        layout.strings.size
            .toFloat() + bandShares.toFloat()
    val rowHeight = height / rowUnits
    val fittedCellWidth = width / columnUnits
    val readableCellWidth = rowHeight * CELL_WIDTH_PER_ROW_HEIGHT
    val widerOfTwo = maxOf(fittedCellWidth, readableCellWidth)
    return maxOf(minimumPhoneCellWidth, widerOfTwo)
}

/**
 * The fretboard's drawing, prepared for one size and one set of settings: every label is
 * measured up front, and [draw] only places them, so a redraw costs no text layout.
 * The draw order is Swift's: fret numbers, inlays, wires, strings, then the note circles on
 * top of everything.
 */
private class FretboardPainter(
    private val layout: FretboardLayout,
    private val geometry: FretboardGeometry,
    private val look: BoardLook,
    private val thinStringOnTop: Boolean,
    private val canvas: BoardCanvas,
) {
    private val stringCount = layout.strings.size
    private val fretNumberLabels = measureFretNumbers()
    private val openStringLabels = measureOpenStringNames()
    private val noteCircles = placeNoteCircles()

    fun draw(scope: DrawScope) {
        scope.drawFretNumbers()
        scope.drawInlays()
        scope.drawFretWires()
        scope.drawStrings()
        for (circle in noteCircles) {
            scope.drawNoteCircle(circle, look.palette.chordRing)
        }
    }

    /** Which string sits on a given row; see `FretboardGeometry.stringIndex`. */
    private fun stringIndex(row: Int): Int =
        FretboardGeometry.stringIndex(
            forRow = row,
            stringCount = stringCount,
            thinStringOnTop = thinStringOnTop,
        )

    private fun measureFretNumbers(): List<TextLayoutResult> {
        val labels = mutableListOf<TextLayoutResult>()
        for (fret in 1..layout.fretCount) {
            val label =
                canvas.measure(
                    text = fret.toString(),
                    fontSizeDp = geometry.fretNumberFontSize,
                    weight = FontWeight.Medium,
                    color = look.palette.fretNumber,
                )
            labels.add(label)
        }
        return labels
    }

    /** Each string's open note, printed beside the nut, in row order. */
    private fun measureOpenStringNames(): List<TextLayoutResult> {
        val labels = mutableListOf<TextLayoutResult>()
        for (row in 0 until stringCount) {
            val openNote = layout.strings[stringIndex(row)].openNote
            val label =
                canvas.measure(
                    text = openNote.displayName,
                    fontSizeDp = geometry.openStringLabelFontSize,
                    weight = FontWeight.SemiBold,
                    color = look.palette.fretNumber,
                )
            labels.add(label)
        }
        return labels
    }

    private fun placeNoteCircles(): List<NoteCircle> {
        val circles = mutableListOf<NoteCircle>()
        for (row in 0 until stringCount) {
            val guitarString = layout.strings[stringIndex(row)]
            val y = geometry.stringY(row)
            for ((fret, tone) in guitarString.tonesByFret.withIndex()) {
                if (tone == null) {
                    continue
                }
                val center = canvas.point(geometry.noteX(fret), y)
                circles.add(look.noteCircle(tone, center, geometry.noteRadius, canvas))
            }
        }
        return circles
    }

    private fun DrawScope.drawFretNumbers() {
        for (fret in 1..layout.fretCount) {
            val center = canvas.point(geometry.noteX(fret), geometry.fretNumberY)
            drawTextCentred(fretNumberLabels[fret - 1], center)
        }
    }

    private fun DrawScope.drawInlays() {
        val radius = canvas.px(geometry.inlayRadius)
        val y = geometry.inlayY(stringCount)
        val offset = geometry.doubleInlayOffset
        for (fret in 1..layout.fretCount) {
            val centerX = geometry.noteX(fret)
            if (fret in FretboardGeometry.singleInlayFrets) {
                drawCircle(look.palette.inlay, radius, canvas.point(centerX, y))
            } else if (fret in FretboardGeometry.doubleInlayFrets) {
                drawCircle(look.palette.inlay, radius, canvas.point(centerX - offset, y))
                drawCircle(look.palette.inlay, radius, canvas.point(centerX + offset, y))
            }
        }
    }

    /** The wires run from the top string to the bottom one; the nut is drawn last, on top. */
    private fun DrawScope.drawFretWires() {
        val top = geometry.stringY(0)
        val bottom = geometry.stringY(stringCount - 1)
        val wireWidth = canvas.px(geometry.fretWireWidth)
        for (fret in 1..layout.fretCount) {
            val x = geometry.fretWireX(fret)
            val start = canvas.point(x, top)
            val end = canvas.point(x, bottom)
            drawLine(look.palette.fretWire, start, end, strokeWidth = wireWidth)
        }
        val nutTop = canvas.point(geometry.nutX, top)
        val nutBottom = canvas.point(geometry.nutX, bottom)
        val nutWidth = canvas.px(geometry.nutWidth)
        drawLine(look.palette.nut, nutTop, nutBottom, strokeWidth = nutWidth)
    }

    /**
     * Strings get slightly thicker toward the bass side, so the board reads like a real neck
     * at a glance. Each string's open note is printed beside the nut.
     */
    private fun DrawScope.drawStrings() {
        val startX = geometry.nutX
        val endX = geometry.fretWireX(layout.fretCount)
        for (row in 0 until stringCount) {
            val y = geometry.stringY(row)
            val lineWidth = geometry.stringStrokeWidth(stringIndex(row), stringCount)
            val start = canvas.point(startX, y)
            val end = canvas.point(endX, y)
            drawLine(look.palette.string, start, end, strokeWidth = canvas.px(lineWidth))
            val labelCenter = canvas.point(geometry.tuningLabelX, y)
            drawTextCentred(openStringLabels[row], labelCenter)
        }
    }
}

/**
 * The ring drawn around a note that belongs to the chord the player picked, in the palette's
 * chord colour, just outside the note's circle so the label stays clear. Twin of Swift's
 * `ChordRing`; it takes the circle's centre in pixels and its radius in dp, because the
 * ring's minimum width is in dp.
 */
object ChordRing {
    fun draw(
        scope: DrawScope,
        center: Offset,
        circleRadiusDp: Double,
        color: Color,
    ) {
        val diameter = circleRadiusDp * 2
        val lineWidth = FretboardGeometry.chordRingLineWidth(circleDiameter = diameter)
        val outset = FretboardGeometry.chordRingOutset(circleDiameter = diameter)
        val ringRadius = (circleRadiusDp + outset) * scope.density
        val stroke = Stroke(width = (lineWidth * scope.density).toFloat())
        scope.drawCircle(color, ringRadius.toFloat(), center, style = stroke)
    }
}

/** The fill and text colours of one note circle. */
data class ToneColors(
    val fill: Color,
    val text: Color,
)

/**
 * Fill and text for one note circle. A chord note takes the green chord colours; the scale's
 * root keeps its own colour even inside a chord, because losing the root marker while a chord
 * rings would confuse more than it helps. The ring says "chord". Swift returns a tuple.
 */
fun FretboardPalette.toneColors(
    isRoot: Boolean,
    isInChord: Boolean,
): ToneColors {
    if (isRoot) {
        return ToneColors(rootFill, rootText)
    }
    if (isInChord) {
        return ToneColors(chordFill, chordText)
    }
    return ToneColors(toneFill, toneText)
}

/**
 * What a board's note circles show and in which colours: shared by the fretboard and the
 * keyboard, which Swift each pass the same three values.
 */
internal class BoardLook(
    val labelStyle: NoteLabelStyle,
    val palette: FretboardPalette,
    val ringedNotes: Set<NoteName>,
) {
    /**
     * The circle for one scale tone, its label measured. The label shrinks for two characters
     * ("F♯", "♭3") so both kinds fit the same circle.
     */
    fun noteCircle(
        tone: ScaleTone,
        center: Offset,
        radiusDp: Double,
        canvas: BoardCanvas,
    ): NoteCircle {
        val isRinged = tone.name in ringedNotes
        val colors = palette.toneColors(isRoot = tone.isRoot, isInChord = isRinged)
        val labelText = labelStyle.label(tone)
        var label: TextLayoutResult? = null
        if (labelText != null) {
            val labelLength = FretboardGeometry.labelLength(labelText)
            val fontSize = FretboardGeometry.noteLabelFontSize(radiusDp, labelLength)
            label = canvas.measure(labelText, fontSize, FontWeight.Bold, colors.text)
        }
        return NoteCircle(center, radiusDp, colors.fill, isRinged, label)
    }
}

/** One note circle ready to draw: centre in pixels, radius in dp, label already measured. */
internal class NoteCircle(
    val center: Offset,
    val radiusDp: Double,
    val fill: Color,
    val isRinged: Boolean,
    val label: TextLayoutResult?,
)

/** Fill, then the chord ring when the note is in the picked chord, then the label. */
internal fun DrawScope.drawNoteCircle(
    circle: NoteCircle,
    ringColor: Color,
) {
    val radius = (circle.radiusDp * density).toFloat()
    drawCircle(circle.fill, radius, circle.center)
    if (circle.isRinged) {
        ChordRing.draw(this, circle.center, circle.radiusDp, ringColor)
    }
    val label = circle.label ?: return
    drawTextCentred(label, circle.center)
}

/**
 * Turns the dp the geometry works in into the pixels the canvas draws in, and measures the
 * board's labels in the music font at a fixed font scale.
 */
internal class BoardCanvas(
    private val textMeasurer: TextMeasurer,
    private val density: Float,
) {
    /** The system text size is ignored on the board, as SwiftUI's fixed sizes do. */
    private val fixedFontDensity = Density(density, fontScale = 1f)

    /** The canvas size in dp, the unit the geometry's minimums are written in. */
    fun dpSize(pixels: CanvasSize): BoardSize {
        val width = pixels.width / density
        val height = pixels.height / density
        return BoardSize(width.toDouble(), height.toDouble())
    }

    fun px(dp: Double): Float = (dp * density).toFloat()

    fun point(
        xDp: Double,
        yDp: Double,
    ): Offset = Offset(px(xDp), px(yDp))

    /** With a font scale of 1, a size in sp is the same number of dp. */
    fun measure(
        text: String,
        fontSizeDp: Double,
        weight: FontWeight,
        color: Color,
    ): TextLayoutResult {
        val style =
            TextStyle(
                color = color,
                fontSize = fontSizeDp.toFloat().sp,
                fontWeight = weight,
                fontFamily = MusicFontFamily,
            )
        return textMeasurer.measure(
            text = text,
            style = style,
            softWrap = false,
            maxLines = 1,
            density = fixedFontDensity,
        )
    }
}

/** Draws measured text centred on a point, as SwiftUI's `.center` anchor does. */
internal fun DrawScope.drawTextCentred(
    text: TextLayoutResult,
    center: Offset,
) {
    val halfWidth = text.size.width / 2f
    val halfHeight = text.size.height / 2f
    val topLeft = Offset(center.x - halfWidth, center.y - halfHeight)
    drawText(text, topLeft = topLeft)
}

/**
 * What TalkBack reads for a board: what it shows ("Fretboard showing G Major") and, as its
 * state, what the circles hold. Tests find every board by [BOARD_TEST_TAG].
 */
internal fun Modifier.boardSemantics(
    description: String,
    labelStyle: NoteLabelStyle,
): Modifier =
    this
        .semantics {
            contentDescription = description
            stateDescription = labelStyle.accessibilityDescription
        }.testTag(BOARD_TEST_TAG)
