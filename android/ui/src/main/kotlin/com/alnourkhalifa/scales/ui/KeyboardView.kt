package com.alnourkhalifa.scales.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.alnourkhalifa.scales.core.board.KeyboardGeometry
import com.alnourkhalifa.scales.core.board.Rect
import com.alnourkhalifa.scales.core.music.KeyboardLayout
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleTone
import com.alnourkhalifa.scales.core.session.NoteLabelStyle
import androidx.compose.ui.geometry.Size as CanvasSize

/** As on the fretboard: the key names and the few distinct circle labels of one board. */
private const val KEYBOARD_TEXT_CACHE_SIZE = 64

/**
 * Draws the piano octaves with the scale's keys circled and the root picked out. Shared by the
 * phone screen, the TV screen and the full-screen board, like [FretboardView]. Twin of
 * `Scales/Views/KeyboardView.swift`.
 *
 * What differs is what differs on the fretboard: the geometry is built in dp and multiplied
 * back to pixels when drawn, labels ignore the system font scale and are measured once in
 * `drawWithCache`, and the board carries its own spoken label from [scaleTitle]. The keys keep
 * their shape and sit centred in whatever frame they get (`KeyboardGeometry` fits them).
 */
@Composable
fun KeyboardView(
    layout: KeyboardLayout,
    scaleTitle: String,
    labelStyle: NoteLabelStyle,
    palette: FretboardPalette,
    modifier: Modifier = Modifier,
    ringedNotes: Set<NoteName> = emptySet(),
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = KEYBOARD_TEXT_CACHE_SIZE)
    val look = BoardLook(labelStyle, palette, ringedNotes)
    Spacer(
        modifier =
            modifier
                .boardSemantics("Keyboard showing $scaleTitle", labelStyle)
                .drawWithCache {
                    val canvas = BoardCanvas(textMeasurer, density)
                    val geometry =
                        KeyboardGeometry(
                            size = canvas.dpSize(size),
                            whiteKeyCount = layout.whiteKeys.size,
                        )
                    val painter = KeyboardPainter(layout, geometry, look, canvas)
                    onDrawBehind {
                        painter.draw(this)
                    }
                },
    )
}

/** One key ready to draw: its rect in pixels, and the circle and name it carries if any. */
private class PlacedKey(
    val topLeft: Offset,
    val size: CanvasSize,
    val isBlack: Boolean,
    val circle: NoteCircle?,
    val name: PlacedName?,
)

/** A note name written on a key, measured, with its centre in pixels. */
private class PlacedName(
    val text: TextLayoutResult,
    val center: Offset,
)

/**
 * The keyboard's drawing, prepared for one size and one set of settings: every key is placed
 * and every label measured up front, so [draw] only paints. White keys are drawn first
 * because the black keys sit on top of them.
 */
private class KeyboardPainter(
    private val layout: KeyboardLayout,
    private val geometry: KeyboardGeometry,
    private val look: BoardLook,
    private val canvas: BoardCanvas,
) {
    private val keys = placeKeys()
    private val outlineWidth = canvas.px(geometry.outlineWidth)

    fun draw(scope: DrawScope) {
        for (key in keys) {
            if (!key.isBlack) {
                scope.drawWhiteKey(key)
            }
        }
        for (key in keys) {
            if (key.isBlack) {
                scope.drawRect(look.palette.blackKey, key.topLeft, key.size)
                scope.drawToneOnKey(key)
            }
        }
    }

    private fun DrawScope.drawWhiteKey(key: PlacedKey) {
        val outline = Stroke(width = outlineWidth)
        drawRect(look.palette.whiteKey, key.topLeft, key.size)
        drawRect(look.palette.keyOutline, key.topLeft, key.size, style = outline)
        drawToneOnKey(key)
    }

    /** The key's name above its circle, then the circle, as Swift draws them. */
    private fun DrawScope.drawToneOnKey(key: PlacedKey) {
        val name = key.name
        if (name != null) {
            drawTextCentred(name.text, name.center)
        }
        val circle = key.circle ?: return
        drawNoteCircle(circle, look.palette.chordRing)
    }

    private fun placeKeys(): List<PlacedKey> {
        val rects = geometry.keyRects(layout.keys)
        val placed = mutableListOf<PlacedKey>()
        for ((index, key) in layout.keys.withIndex()) {
            placed.add(placeKey(key, rects[index]))
        }
        return placed
    }

    /**
     * A key in the scale gets the same circle as a fret on the guitar, sitting near the
     * bottom of the key where a pianist's eye lands. The key itself keeps its natural colour,
     * so a black key in the scale still reads as a black key.
     */
    private fun placeKey(
        key: KeyboardLayout.Key,
        rect: Rect,
    ): PlacedKey {
        val topLeft = canvas.point(rect.x, rect.y)
        val size = CanvasSize(canvas.px(rect.width), canvas.px(rect.height))
        val tone = key.tone ?: return PlacedKey(topLeft, size, key.isBlack, null, null)
        val radius = KeyboardGeometry.circleRadius(rect.width, key.isBlack)
        val center = KeyboardGeometry.circleCenter(rect, radius)
        val circleCenter = canvas.point(center.x, center.y)
        val circle = look.noteCircle(tone, circleCenter, radius, canvas)
        val circleTop = center.y - radius
        val name = placeKeyName(tone, rect, circleTop, key.isBlack)
        return PlacedKey(topLeft, size, key.isBlack, circle, name)
    }

    /**
     * The note's name written on the key itself, above the circle, so the player can read the
     * scale straight off the keys whatever the circles show. With note names already in the
     * circles the key would say the same thing twice, so it stays blank then.
     */
    private fun placeKeyName(
        tone: ScaleTone,
        keyRect: Rect,
        circleTop: Double,
        isBlack: Boolean,
    ): PlacedName? {
        if (look.labelStyle == NoteLabelStyle.NOTE_NAMES) {
            return null
        }
        val fontSize = KeyboardGeometry.keyNameFontSize(keyRect.width, isBlack)
        var textColor = look.palette.fretNumber
        if (isBlack) {
            textColor = look.palette.whiteKey
        }
        val text = canvas.measure(tone.name.displayName, fontSize, FontWeight.SemiBold, textColor)
        val center = KeyboardGeometry.keyNameCenter(keyRect, circleTop, fontSize)
        return PlacedName(text, canvas.point(center.x, center.y))
    }
}
