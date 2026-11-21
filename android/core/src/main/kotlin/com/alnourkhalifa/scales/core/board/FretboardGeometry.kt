package com.alnourkhalifa.scales.core.board

import kotlin.math.max
import kotlin.math.min

/**
 * Turns string and fret numbers into points on the canvas. Twin of
 * `Scales/Views/FretboardGeometry.swift`, with the same members; lengths are `Double`, the
 * proportions Swift writes inline are named constants (the core keeps detekt's magic-number
 * rule), and [labelLength] is added so every caller counts a label the way Swift does.
 *
 * Frets are evenly spaced on purpose, as in the reference chart: real guitars narrow toward
 * the body, but equal cells give every note circle the same size, which keeps the high frets
 * readable from across the room.
 *
 * Columns from left to right (right-handed): a narrow column naming each open string, one
 * column for open-string notes, then one column per fret. Rows from top to bottom: a band of
 * fret numbers, one row per string, then a band for the inlay dots.
 */
class FretboardGeometry(
    val size: Size,
    fretCount: Int,
    stringCount: Int,
    val isLeftHanded: Boolean,
) {
    /** Width of one fret's column; the open-string column has the same width. */
    val cellWidth: Double

    /** Height of one string's row; the number and inlay bands are shares of it. */
    val rowHeight: Double

    init {
        val rowUnits = stringCount.toDouble() + numberBandShare + inlayBandShare
        cellWidth = size.width / columnUnits(fretCount)
        rowHeight = size.height / rowUnits
    }

    /** Every note circle shares this radius, so it fits both the cell and the row. */
    val noteRadius: Double
        get() = min(cellWidth, rowHeight) * NOTE_RADIUS_SHARE

    /** Centre of the column that names each open string. */
    val tuningLabelX: Double
        get() = placed(cellWidth * tuningColumnShare / 2)

    /** The nut's line, between the open-string column and fret 1. */
    val nutX: Double
        get() = placed(unplacedNutX)

    /** Centre of the cell where a note on this fret is drawn; fret 0 sits left of the nut. */
    fun noteX(fret: Int): Double {
        if (fret == 0) {
            return placed(unplacedNutX - cellWidth / 2)
        }
        val cellsFromNut = fret.toDouble() - HALF_CELL
        return placed(unplacedNutX + cellsFromNut * cellWidth)
    }

    /** The wire that closes this fret's cell; fret 0 is the nut's own line. */
    fun fretWireX(fret: Int): Double = placed(unplacedNutX + fret.toDouble() * cellWidth)

    /** Vertical centre of a string, counting rows from the top of the board. */
    fun stringY(row: Int): Double {
        val rowsFromTop = row.toDouble() + HALF_CELL
        return numberBandHeight + rowsFromTop * rowHeight
    }

    /** Centre of the band of fret numbers above the strings. */
    val fretNumberY: Double
        get() = numberBandHeight / 2

    /** Centre of the band of inlay dots below the last string. */
    fun inlayY(stringCount: Int): Double {
        val stringsHeight = stringCount.toDouble() * rowHeight
        return numberBandHeight + stringsHeight + inlayBandHeight / 2
    }

    private val numberBandHeight: Double
        get() = rowHeight * numberBandShare

    private val inlayBandHeight: Double
        get() = rowHeight * inlayBandShare

    private val unplacedNutX: Double
        get() = cellWidth * (tuningColumnShare + 1)

    /** Mirrors a right-handed x position for left-handed players. */
    private fun placed(x: Double): Double {
        if (isLeftHanded) {
            return size.width - x
        }
        return x
    }

    // Drawing sizes, kept beside the positions rather than inside the view so they are plain
    // numbers the Android board shares with the iPhone and `expected/answers.json` pins.

    /**
     * Line width of one string. Strings get slightly thicker toward the bass side, so the
     * board reads like a real neck at a glance; [stringIndex] is the layout's index, where 0
     * is the thickest string.
     */
    fun stringStrokeWidth(
        stringIndex: Int,
        stringCount: Int,
    ): Double {
        val baseWidth = max(1.0, rowHeight * STRING_WIDTH_SHARE)
        val bassRank = (stringCount - 1 - stringIndex).toDouble()
        return baseWidth * (1 + STRING_WIDTH_GROWTH_PER_STRING * bassRank)
    }

    /** Radius of an inlay dot: a small fraction of a note circle, never vanishing on a small
     *  board. */
    val inlayRadius: Double
        get() = max(MIN_INLAY_RADIUS, noteRadius * INLAY_RADIUS_SHARE)

    /** Gap between the two dots of a double inlay and the fret's centre. */
    val doubleInlayOffset: Double
        get() = inlayRadius * DOUBLE_INLAY_OFFSET_RADII

    /** A fret wire, never thinner than a hairline. */
    val fretWireWidth: Double
        get() = max(1.0, cellWidth * FRET_WIRE_SHARE)

    /** The nut is drawn thicker than a fret wire, as on a guitar. */
    val nutWidth: Double
        get() = max(MIN_NUT_WIDTH, cellWidth * NUT_SHARE)

    /**
     * The capo's bar: a rounded bar across every string, centred in the capo fret's cell and
     * reaching a little past the outer strings, as a real capo does. It is narrower than a
     * note circle, so the notes on the capo's fret (the new open strings) are drawn over it
     * and stay readable.
     */
    fun capoBarRect(
        fret: Int,
        stringCount: Int,
    ): Rect {
        val width = cellWidth * CAPO_BAR_WIDTH_SHARE
        val overhang = rowHeight * CAPO_BAR_OVERHANG_SHARE
        val top = stringY(row = 0) - overhang
        val bottom = stringY(row = stringCount - 1) + overhang
        val left = noteX(fret) - width / 2
        return Rect(x = left, y = top, width = width, height = bottom - top)
    }

    /** The fret numbers above the board. */
    val fretNumberFontSize: Double
        get() = min(rowHeight, cellWidth) * FRET_NUMBER_FONT_SHARE

    /** The open-string names left of the nut. */
    val openStringLabelFontSize: Double
        get() = min(rowHeight, cellWidth) * OPEN_STRING_FONT_SHARE

    /**
     * Shares of the board, the inlay frets, and the sizes shared with the keyboard. The public
     * shares keep Swift's camelCase names, the keys `expected/answers.json` uses, so they stay
     * plain vals rather than SCREAMING_CASE consts.
     */
    @Suppress("MayBeConstant")
    companion object {
        /** Width of the open-string name column, in cell widths. */
        val tuningColumnShare: Double = 0.6

        /** Height of the fret-number band above the strings, in row heights. */
        val numberBandShare: Double = 0.8

        /** Height of the inlay band below the strings, in row heights. */
        val inlayBandShare: Double = 0.6

        /** Frets marked with one inlay dot below the strings, as on most guitars. */
        val singleInlayFrets: Set<Int> = setOf(3, 5, 7, 9, 15, 17, 19, 21)

        /** Octave frets, marked with two dots side by side. */
        val doubleInlayFrets: Set<Int> = setOf(12, 24)

        private const val NOTE_RADIUS_SHARE = 0.42

        /** A fret's note sits half a cell before its wire; a string half a row below its
         *  row's top. */
        private const val HALF_CELL = 0.5
        private const val STRING_WIDTH_SHARE = 0.025

        /** Each step toward the bass adds a fifth of the thinnest string's width. */
        private const val STRING_WIDTH_GROWTH_PER_STRING = 0.2
        private const val MIN_INLAY_RADIUS = 2.0
        private const val INLAY_RADIUS_SHARE = 0.16
        private const val DOUBLE_INLAY_OFFSET_RADII = 1.8
        private const val FRET_WIRE_SHARE = 0.02
        private const val MIN_NUT_WIDTH = 3.0
        private const val NUT_SHARE = 0.06
        private const val CAPO_BAR_WIDTH_SHARE = 0.3
        private const val CAPO_BAR_OVERHANG_SHARE = 0.46
        private const val FRET_NUMBER_FONT_SHARE = 0.38
        private const val OPEN_STRING_FONT_SHARE = 0.32
        private const val LONG_LABEL_FONT_SHARE = 0.78
        private const val SHORT_LABEL_FONT_SHARE = 0.95
        private const val MIN_CHORD_RING_WIDTH = 1.5
        private const val CHORD_RING_WIDTH_SHARE = 0.1
        private const val CHORD_RING_OUTSET_WIDTHS = 0.9

        /** How many cell widths the whole board needs; lets callers size a scrolling board. */
        fun columnUnits(fretCount: Int): Double = tuningColumnShare + 1 + fretCount.toDouble()

        /**
         * Which string sits on a given row. `FretboardLayout` lists the thickest string
         * first, so the order flips when the thin string should be on top, as in tab.
         */
        fun stringIndex(
            forRow: Int,
            stringCount: Int,
            thinStringOnTop: Boolean,
        ): Int {
            val lastIndex = stringCount - 1
            if (thinStringOnTop) {
                return lastIndex - forRow
            }
            return forRow
        }

        /**
         * The label inside a note circle. Two-character labels ("F♯", "♭3") get a smaller
         * size than one-character ones so both fit inside the same circle; shared with the
         * keyboard. Callers measure the label with the companion's `labelLength(label)`, which
         * counts it as Swift's `label.count` does.
         */
        fun noteLabelFontSize(
            noteRadius: Double,
            labelLength: Int,
        ): Double {
            var fontScale = SHORT_LABEL_FONT_SHARE
            if (labelLength > 1) {
                fontScale = LONG_LABEL_FONT_SHARE
            }
            return noteRadius * fontScale
        }

        /**
         * How many characters a note label has, as Swift's `String.count` gives it. Swift
         * counts grapheme clusters; Kotlin's `length` counts UTF-16 units, which would make
         * "𝄪" (a surrogate pair) two characters and shrink its label. The board's labels are
         * letters, digits and the accidentals ♭ ♯ 𝄫 𝄪, each a single code point with no
         * combining marks, so counting code points gives Swift's count without a
         * `BreakIterator`.
         */
        fun labelLength(label: String): Int = label.codePointCount(0, label.length)

        /**
         * Line width of the ring around a note of the picked chord, on the fretboard and on
         * the keyboard alike: a tenth of the circle's diameter, never thinner than a hairline.
         */
        fun chordRingLineWidth(circleDiameter: Double): Double =
            max(MIN_CHORD_RING_WIDTH, circleDiameter * CHORD_RING_WIDTH_SHARE)

        /** How far outside the note's circle the ring is drawn, so the label stays clear. */
        fun chordRingOutset(circleDiameter: Double): Double =
            chordRingLineWidth(circleDiameter) * CHORD_RING_OUTSET_WIDTHS
    }
}
