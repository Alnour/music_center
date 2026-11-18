package com.alnourkhalifa.scales.core.board

import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.core.music.DrumVoice

/**
 * Where everything sits on the drum staff for a view of this size. Twin of
 * `Scales/Views/DrumStaffLayout.swift`, with the same members; lengths are `Double` and the
 * proportions Swift writes inline are named constants.
 *
 * Every size is a share of the view's height (or width, along the bar), so the staff reads
 * the same on a phone and a TV. Kept free of drawing code so the iPhone staff and this one
 * share the numbers and `expected/answers.json` can pin them.
 */
class DrumStaffLayout(
    val groove: DrumGroove,
    val size: Size,
) {
    /** The gap between two staff lines; most other sizes are a share of it. */
    val lineSpacing: Double
        get() = size.height * LINE_SPACING_SHARE

    /** The top staff line. The staff sits low, leaving room above for counts and the ball. */
    val staffTop: Double
        get() = size.height * STAFF_TOP_SHARE

    /** The bottom staff line, four spaces below the top one. */
    val staffBottom: Double
        get() = staffTop + lineSpacing * STAFF_SPACES

    val staffLeft: Double
        get() = size.width * STAFF_LEFT_SHARE

    val staffRight: Double
        get() = size.width * STAFF_RIGHT_SHARE

    /** The beam that joins the stems above the staff. */
    val beamY: Double
        get() = staffTop - lineSpacing * BEAM_SPACES_ABOVE_STAFF

    /** The baseline row of the counts ("1 & 2 &"). */
    val countY: Double
        get() = staffTop - lineSpacing * COUNT_SPACES_ABOVE_STAFF

    /** The ball lands on the count being played, so it rests on the count row. */
    val ballRestY: Double
        get() = countY

    /** How high the ball rises between two steps. */
    val bounceHeight: Double
        get() = size.height * BOUNCE_HEIGHT_SHARE

    /** The five staff lines. */
    val staffLineWidth: Double
        get() = lineSpacing * STAFF_LINE_WIDTH_SHARE

    /** The closing bar line, heavier than the staff lines. */
    val barLineWidth: Double
        get() = lineSpacing * BAR_LINE_WIDTH_SHARE

    val stemWidth: Double
        get() = lineSpacing * STEM_WIDTH_SHARE

    val beamWidth: Double
        get() = lineSpacing * BEAM_WIDTH_SHARE

    /** Sixteenth notes get a second beam this far below the first. */
    val secondBeamOffset: Double
        get() = lineSpacing * SECOND_BEAM_OFFSET_SHARE

    /** Half the width of a hi-hat cross; snare and kick heads are ellipses built from it. */
    val headRadius: Double
        get() = lineSpacing * HEAD_RADIUS_SHARE

    val ballRadius: Double
        get() = lineSpacing * BALL_RADIUS_SHARE

    /** The counts ("1 & 2 &") written above the staff. */
    val countFontSize: Double
        get() = lineSpacing * COUNT_FONT_SHARE

    /** The bar behind the count being played, as wide as nearly one staff space. */
    val countMarkerWidth: Double
        get() = lineSpacing * COUNT_MARKER_WIDTH_SHARE

    /** One column per step plus one spare, split as margins before the first and after the
     *  last note. */
    val columnWidth: Double
        get() = (staffRight - staffLeft) / (groove.stepsPerBar + 1).toDouble()

    /** Centre of a step's note heads. */
    fun columnX(step: Int): Double {
        val columnsFromLeft = step.toDouble() + FIRST_COLUMN_OFFSET
        return staffLeft + columnWidth * columnsFromLeft
    }

    /** Stems hang off the right side of the head, as engraved music does. */
    fun stemX(step: Int): Double = columnX(step) + lineSpacing * STEM_OFFSET_SHARE

    /** Each voice has its own place: hi-hat above the staff, snare and kick in spaces. */
    fun headY(voice: DrumVoice): Double =
        when (voice) {
            DrumVoice.HI_HAT -> staffTop - lineSpacing * HI_HAT_SPACES_ABOVE_TOP
            DrumVoice.SNARE -> staffTop + lineSpacing * SNARE_SPACES_BELOW_TOP
            DrumVoice.KICK -> staffTop + lineSpacing * KICK_SPACES_BELOW_TOP
        }

    /** The shares of the view and of a staff space that place everything. */
    private companion object {
        const val LINE_SPACING_SHARE = 0.075
        const val STAFF_TOP_SHARE = 0.6

        /** Five lines enclose four spaces. */
        const val STAFF_SPACES = 4.0

        /** The staff runs from 2 % to 98 % of the width. */
        const val STAFF_LEFT_SHARE = 0.02
        const val STAFF_RIGHT_SHARE = 0.98
        const val BEAM_SPACES_ABOVE_STAFF = 2.6
        const val COUNT_SPACES_ABOVE_STAFF = 4.6
        const val BOUNCE_HEIGHT_SHARE = 0.16
        const val STAFF_LINE_WIDTH_SHARE = 0.08
        const val BAR_LINE_WIDTH_SHARE = 0.2
        const val STEM_WIDTH_SHARE = 0.1
        const val BEAM_WIDTH_SHARE = 0.35
        const val SECOND_BEAM_OFFSET_SHARE = 0.55
        const val HEAD_RADIUS_SHARE = 0.45
        const val BALL_RADIUS_SHARE = 0.75
        const val COUNT_FONT_SHARE = 1.7
        const val COUNT_MARKER_WIDTH_SHARE = 0.9

        /** The first note sits a little past the middle of the first column. */
        const val FIRST_COLUMN_OFFSET = 0.6
        const val STEM_OFFSET_SHARE = 0.5
        const val HI_HAT_SPACES_ABOVE_TOP = 0.5
        const val SNARE_SPACES_BELOW_TOP = 1.5
        const val KICK_SPACES_BELOW_TOP = 3.5
    }
}
