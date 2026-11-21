package com.alnourkhalifa.scales.core.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the keys are shaped: true piano proportions everywhere but the full-screen board on a
 * phone, where they stretch taller so the keyboard is not a strip across the screen. Twin of
 * `ScalesTests/KeyboardGeometryTests.swift`, with the same three tests.
 */
class KeyboardGeometryTest {
    // Two octaves on a phone turned sideways would be a strip at their true shape, so the
    // keys grow to fill the frame, but never past twice their true shape.
    // feature: keyboard, tv-board
    @Test
    fun keysStretchToFillAShortWideFrame() {
        val phoneFrame = Size(735.0, 300.0)
        val stretched = KeyboardGeometry.stretchedHeightToWidth(phoneFrame, whiteKeyCount = 15)
        assertTrue(stretched > KeyboardGeometry.whiteKeyHeightToWidth)
        assertTrue(stretched <= KeyboardGeometry.maxStretchedHeightToWidth)

        val geometry =
            KeyboardGeometry(
                size = phoneFrame,
                whiteKeyCount = 15,
                whiteKeyHeightToWidth = stretched,
            )
        assertEquals(300.0, geometry.keyHeight, TOLERANCE)
        assertEquals(0.0, geometry.originY, TOLERANCE)
    }

    // A frame already taller than the keys' true shape (one octave on the same phone) keeps
    // the true shape: stretching only ever makes keys taller, never squatter.
    // feature: keyboard, tv-board
    @Test
    fun keysKeepTheirTrueShapeInATallFrame() {
        val frame = Size(735.0, 300.0)
        val shape = KeyboardGeometry.stretchedHeightToWidth(frame, whiteKeyCount = 8)
        assertEquals(KeyboardGeometry.whiteKeyHeightToWidth, shape, TOLERANCE)
    }

    // Without a shape of its own, the geometry is the one the TV and the answers file pin.
    // feature: keyboard
    @Test
    fun defaultShapeIsTheTruePianoShape() {
        val frame = Size(1920.0, 1080.0)
        val geometry = KeyboardGeometry(size = frame, whiteKeyCount = 15)
        val expectedHeight = 1920.0 / 15 * KeyboardGeometry.whiteKeyHeightToWidth
        assertEquals(expectedHeight, geometry.keyHeight, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.000_001
    }
}
