package com.alnourkhalifa.scales.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.board.DrumStaffLayout
import com.alnourkhalifa.scales.core.music.BallPosition
import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.core.music.DrumVoice
import com.alnourkhalifa.scales.core.board.Size as LayoutSize

/**
 * The drum notation drawn as one picture: counts along the top, beamed hi-hats above the
 * staff, snare and kick in their spaces, the current count marked by a bar while playing, and
 * the bouncing ball above it all. Twin of `DrumStaffView` in `Scales/Views/DrumTrainerView.swift`;
 * every position comes from [DrumStaffLayout], the twin of `Scales/Views/DrumStaffLayout.swift`.
 *
 * What differs from Swift:
 * - The ball arrives as a function, [ball], read only while drawing. The ball moves every
 *   frame, so reading it during composition would recompose the whole view sixty times a
 *   second; read in the draw phase, a frame-clock state behind it redraws the picture alone.
 * - Everything that does not move (staff, notes, measured counts) is built once per groove,
 *   size and theme in `drawWithCache`; only the marker and the ball are drawn fresh.
 * - The layout works in pixels directly. Its sizes are all shares of the view's height or
 *   width with no minimums, so pixels and points give the same picture.
 * - SwiftUI's rounded system font has no Android counterpart; the counts use the system font
 *   at the same extra-bold weight, measured with a font scale of 1 so a large-text setting
 *   cannot burst the counts out of their columns (SwiftUI's fixed size ignores it too).
 * - The theme arrives as [isDark] because this module does not read the app's theme.
 */
@Composable
fun DrumStaffView(
    groove: DrumGroove,
    isPlaying: Boolean,
    ball: () -> BallPosition,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val currentBall = rememberUpdatedState(ball)
    val currentlyPlaying = rememberUpdatedState(isPlaying)
    val description = "${groove.name} on the drum staff"
    val drawing =
        remember(groove, isDark, textMeasurer) {
            Modifier.staffDrawing(groove, isDark, textMeasurer, currentlyPlaying, currentBall)
        }
    Spacer(
        modifier =
            modifier
                .semantics { contentDescription = description }
                .then(drawing),
    )
}

/** The colours of the staff in one theme, Apple's light and dark system colours. */
private class StaffColors(
    isDark: Boolean,
) {
    val background = if (isDark) Color(0.13f, 0.13f, 0.13f) else Color(0.89f, 0.94f, 0.97f)
    val ink = if (isDark) Color.White else Color.Black

    /** `Color.accentColor`, the system blue, at the marker's 55 % opacity. */
    val marker = (if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)).copy(alpha = 0.55f)

    /** SwiftUI's `.cyan` at the ball's highlight and `.blue` at its rim. */
    val ballHighlight = if (isDark) Color(0xFF64D2FF) else Color(0xFF32ADE6)
    val ballRim = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
}

/**
 * The parts of the picture that stay put for a groove at one size: built once, then drawn
 * every frame under the moving ball.
 */
private class StaffPicture(
    val layout: DrumStaffLayout,
    val staffLines: Path,
    val barLine: Path,
    val counts: List<Pair<Offset, TextLayoutResult>>,
    val crosses: Path,
    val heads: Path,
    val stems: Path,
    val beams: Path,
)

/**
 * The drawing modifier. The cache block runs again when the view's size changes; the caller
 * remembers this modifier per groove and theme, so those rebuild it too. The two states are
 * read inside `onDrawBehind` only, so a new ball position redraws without rebuilding.
 */
private fun Modifier.staffDrawing(
    groove: DrumGroove,
    isDark: Boolean,
    textMeasurer: TextMeasurer,
    isPlaying: State<Boolean>,
    ball: State<() -> BallPosition>,
): Modifier {
    val colors = StaffColors(isDark)
    return this.drawWithCache {
        val layoutSize = LayoutSize(size.width.toDouble(), size.height.toDouble())
        val layout = DrumStaffLayout(groove, layoutSize)
        val fixedDensity = Density(density, fontScale = 1f)
        val picture = buildPicture(layout, textMeasurer, fixedDensity, colors.ink)
        onDrawBehind {
            val position = ball.value()
            drawRect(colors.background)
            drawStaff(picture, colors.ink)
            if (isPlaying.value) {
                drawCountMarker(layout, position.step, colors.marker)
            }
            drawCounts(picture)
            drawHits(picture, colors.ink)
            drawBall(layout, position, colors)
        }
    }
}

/** Builds the still parts of the staff in the layout's pixels. */
private fun buildPicture(
    layout: DrumStaffLayout,
    textMeasurer: TextMeasurer,
    fixedDensity: Density,
    ink: Color,
): StaffPicture {
    val staffLines = Path()
    for (index in 0 until STAFF_LINE_COUNT) {
        val y = (layout.staffTop + index * layout.lineSpacing).toFloat()
        staffLines.moveTo(layout.staffLeft.toFloat(), y)
        staffLines.lineTo(layout.staffRight.toFloat(), y)
    }
    val barLine = Path()
    barLine.moveTo(layout.staffRight.toFloat(), layout.staffTop.toFloat())
    barLine.lineTo(layout.staffRight.toFloat(), layout.staffBottom.toFloat())

    val counts = measureCounts(layout, textMeasurer, fixedDensity, ink)
    val crosses = Path()
    val heads = Path()
    val stems = Path()
    addNotes(layout, crosses, heads, stems)
    val beams = buildBeams(layout)
    return StaffPicture(layout, staffLines, barLine, counts, crosses, heads, stems, beams)
}

/**
 * Measures each count ("1 + 2 +") once and works out where its box goes. SwiftUI draws text
 * centred on the point it is given, so each box is centred on its column at `countY`.
 */
private fun measureCounts(
    layout: DrumStaffLayout,
    textMeasurer: TextMeasurer,
    fixedDensity: Density,
    ink: Color,
): List<Pair<Offset, TextLayoutResult>> {
    val fontSizeInSp = layout.countFontSize.toFloat() / fixedDensity.density
    val style =
        TextStyle(
            color = ink,
            fontSize = fontSizeInSp.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    val groove = layout.groove
    val counts = mutableListOf<Pair<Offset, TextLayoutResult>>()
    for (step in 0 until groove.stepsPerBar) {
        val label = groove.countLabel(forStep = step)
        val measured =
            textMeasurer.measure(
                text = label,
                style = style,
                softWrap = false,
                maxLines = 1,
                density = fixedDensity,
            )
        val halfWidth = measured.size.width / 2f
        val halfHeight = measured.size.height / 2f
        val left = layout.columnX(step).toFloat() - halfWidth
        val top = layout.countY.toFloat() - halfHeight
        counts.add(Offset(left, top) to measured)
    }
    return counts
}

/**
 * Adds every step's heads and its stem. Each stem hangs from the beam down to the lowest head
 * on its step; a step with no hit gets a stem of no length, which draws nothing, as in Swift.
 */
private fun addNotes(
    layout: DrumStaffLayout,
    crosses: Path,
    heads: Path,
    stems: Path,
) {
    val groove = layout.groove
    val radius = layout.headRadius.toFloat()
    for (step in 0 until groove.stepsPerBar) {
        val x = layout.columnX(step).toFloat()
        var lowestY = layout.beamY.toFloat()
        for (voice in groove.voices(atStep = step)) {
            val y = layout.headY(voice).toFloat()
            lowestY = maxOf(lowestY, y)
            if (voice == DrumVoice.HI_HAT) {
                addCross(crosses, Offset(x, y), radius)
            } else {
                addFilledHead(heads, Offset(x, y), radius)
            }
        }
        val stemX = layout.stemX(step).toFloat()
        stems.moveTo(stemX, layout.beamY.toFloat())
        stems.lineTo(stemX, lowestY)
    }
}

/** A hi-hat is written as an X the width of two head radii. */
private fun addCross(
    crosses: Path,
    center: Offset,
    radius: Float,
) {
    crosses.moveTo(center.x - radius, center.y - radius)
    crosses.lineTo(center.x + radius, center.y + radius)
    crosses.moveTo(center.x + radius, center.y - radius)
    crosses.lineTo(center.x - radius, center.y + radius)
}

/** Snare and kick are filled ovals, wider than tall like an engraved note head. */
private fun addFilledHead(
    heads: Path,
    center: Offset,
    radius: Float,
) {
    val halfWidth = radius * 1.2f
    val halfHeight = radius * 0.85f
    val oval =
        Rect(
            left = center.x - halfWidth,
            top = center.y - halfHeight,
            right = center.x + halfWidth,
            bottom = center.y + halfHeight,
        )
    heads.addOval(oval)
}

/** One beam per beat joining its stems; sixteenth grooves get a second beam below it. */
private fun buildBeams(layout: DrumStaffLayout): Path {
    val groove = layout.groove
    val beams = Path()
    for (beat in 0 until groove.beatsPerBar) {
        val firstStep = beat * groove.subdivision
        val lastStep = firstStep + groove.subdivision - 1
        val startX = layout.stemX(firstStep).toFloat()
        val endX = layout.stemX(lastStep).toFloat()
        val beamY = layout.beamY.toFloat()
        beams.moveTo(startX, beamY)
        beams.lineTo(endX, beamY)
        if (groove.subdivision == SIXTEENTHS) {
            val secondY = beamY + layout.secondBeamOffset.toFloat()
            beams.moveTo(startX, secondY)
            beams.lineTo(endX, secondY)
        }
    }
    return beams
}

/** The five staff lines, then the heavier closing bar line. */
private fun DrawScope.drawStaff(
    picture: StaffPicture,
    ink: Color,
) {
    val layout = picture.layout
    drawPath(picture.staffLines, ink, style = Stroke(width = layout.staffLineWidth.toFloat()))
    drawPath(picture.barLine, ink, style = Stroke(width = layout.barLineWidth.toFloat()))
}

/** A rounded bar behind the count being played, from above the beam to below the staff. */
private fun DrawScope.drawCountMarker(
    layout: DrumStaffLayout,
    step: Int,
    color: Color,
) {
    val width = layout.countMarkerWidth
    val left = layout.columnX(step) - width / 2
    val top = layout.beamY - layout.lineSpacing * 0.6
    val bottom = layout.staffBottom + layout.lineSpacing * 0.6
    val corner = (width / 4).toFloat()
    drawRoundRect(
        color = color,
        topLeft = Offset(left.toFloat(), top.toFloat()),
        size = Size(width.toFloat(), (bottom - top).toFloat()),
        cornerRadius = CornerRadius(corner, corner),
    )
}

private fun DrawScope.drawCounts(picture: StaffPicture) {
    for ((topLeft, measured) in picture.counts) {
        drawText(measured, topLeft = topLeft)
    }
}

/** Heads first, then the stems and beams over them, in Swift's order. */
private fun DrawScope.drawHits(
    picture: StaffPicture,
    ink: Color,
) {
    val layout = picture.layout
    val crossWidth = layout.headRadius.toFloat() * 0.3f
    drawPath(picture.crosses, ink, style = Stroke(width = crossWidth))
    drawPath(picture.heads, ink)
    drawPath(picture.stems, ink, style = Stroke(width = layout.stemWidth.toFloat()))
    drawPath(picture.beams, ink, style = Stroke(width = layout.beamWidth.toFloat()))
}

/**
 * The ball: it travels from the count it left towards the next column and rises by its
 * bounce, lit from the upper left by a cyan-to-blue radial gradient.
 */
private fun DrawScope.drawBall(
    layout: DrumStaffLayout,
    position: BallPosition,
    colors: StaffColors,
) {
    val fromX = layout.columnX(position.step)
    val x = (fromX + layout.columnWidth * position.travel).toFloat()
    val y = (layout.ballRestY - layout.bounceHeight * position.bounce).toFloat()
    val radius = layout.ballRadius.toFloat()
    val highlight = Offset(x - radius * 0.35f, y - radius * 0.35f)
    val shading =
        Brush.radialGradient(
            colors = listOf(colors.ballHighlight, colors.ballRim),
            center = highlight,
            radius = radius * 1.6f,
        )
    drawCircle(brush = shading, radius = radius, center = Offset(x, y))
}

/** Five lines make a staff. */
private const val STAFF_LINE_COUNT = 5

/** Notes per beat in a sixteenth-note groove, the one grid that gets a second beam. */
private const val SIXTEENTHS = 4
