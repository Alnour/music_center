package com.alnourkhalifa.scales.core.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Where strings and frets land: the string order and left-handed mirroring, which the player
 * sets once and expects every board (phone, TV, full screen) to honour. Twin of
 * `ScalesTests/FretboardGeometryTests.swift`, with the same three tests.
 */
class FretboardGeometryTest {
    // The layout lists the thick low E string first. With the thin string on top (tab order,
    // the default) row 0 shows the high E; turned off, row 0 shows the low E.
    // feature: left-handed-thin-top
    @Test
    fun stringOrderFollowsTheThinStringOnTopSetting() {
        val thinOnTop = mutableListOf<Int>()
        val thickOnTop = mutableListOf<Int>()
        for (row in 0 until 6) {
            val thinIndex =
                FretboardGeometry.stringIndex(
                    forRow = row,
                    stringCount = 6,
                    thinStringOnTop = true,
                )
            val thickIndex =
                FretboardGeometry.stringIndex(
                    forRow = row,
                    stringCount = 6,
                    thinStringOnTop = false,
                )
            thinOnTop.add(thinIndex)
            thickOnTop.add(thickIndex)
        }
        assertEquals(listOf(5, 4, 3, 2, 1, 0), thinOnTop)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), thickOnTop)
    }

    // A left-handed board is the right-handed one seen in a mirror: every fret, the nut and
    // the tuning labels sit at the same distance from the other edge, and nothing moves up or
    // down.
    // feature: left-handed-thin-top
    @Test
    fun leftHandedBoardMirrorsEveryColumn() {
        val size = Size(width = 1920.0, height = 1080.0)
        val right =
            FretboardGeometry(size = size, fretCount = 15, stringCount = 6, isLeftHanded = false)
        val left =
            FretboardGeometry(size = size, fretCount = 15, stringCount = 6, isLeftHanded = true)

        assertTrue(isClose(left.nutX, size.width - right.nutX))
        assertTrue(isClose(left.tuningLabelX, size.width - right.tuningLabelX))
        for (fret in 0..15) {
            assertTrue(isClose(left.noteX(fret), size.width - right.noteX(fret)))
            assertTrue(isClose(left.fretWireX(fret), size.width - right.fretWireX(fret)))
        }
        for (row in 0 until 6) {
            assertEquals(right.stringY(row), left.stringY(row), 0.0)
        }
        assertTrue("open notes sit left of the nut", right.noteX(0) < right.nutX)
        assertTrue("and right of it when left-handed", left.noteX(0) > left.nutX)
    }

    // The bass side is drawn thicker: the low E string is the widest line.
    // feature: board
    @Test
    fun bassStringsAreDrawnThicker() {
        val geometry =
            FretboardGeometry(
                size = Size(width = 1920.0, height = 1080.0),
                fretCount = 15,
                stringCount = 6,
                isLeftHanded = false,
            )
        val lowE = geometry.stringStrokeWidth(stringIndex = 0, stringCount = 6)
        val highE = geometry.stringStrokeWidth(stringIndex = 5, stringCount = 6)
        assertTrue(lowE > highE)
        assertTrue(isClose(lowE, highE * 2))
    }

    private fun isClose(
        first: Double,
        second: Double,
    ): Boolean = abs(first - second) < 0.000_001
}
