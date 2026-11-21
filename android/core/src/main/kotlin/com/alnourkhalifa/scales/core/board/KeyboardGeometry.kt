package com.alnourkhalifa.scales.core.board

import com.alnourkhalifa.scales.core.music.KeyboardLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Where each key lands inside the canvas: the keyboard keeps its shape and is centred. Twin
 * of `Scales/Views/KeyboardGeometry.swift`, with the same members; lengths are `Double` and
 * the proportions Swift writes inline are named constants.
 *
 * Swift's `keyRects(for keys: [KeyboardLayout.Key])` is the overload taking the layout's
 * keys. A second overload takes only the keys' `isBlack` flags, the form this file was first
 * written in while `core/music/KeyboardLayout.kt` was being ported; the keys overload maps
 * each key to its flag and hands them on.
 *
 * Kept free of drawing code so the numbers are plain arithmetic: the iPhone keyboard and this
 * one share them and `expected/answers.json` pins every key rect.
 */
class KeyboardGeometry(
    size: Size,
    whiteKeyCount: Int,
    whiteKeyHeightToWidth: Double = KeyboardGeometry.whiteKeyHeightToWidth,
) {
    /** Every white key has this width; black keys are a share of it. */
    val whiteKeyWidth: Double

    /** Length of a white key; black keys are a share of it. */
    val keyHeight: Double

    /** Left edge of the keyboard, centred across the canvas. */
    val originX: Double

    /** Top edge of the keyboard, centred down the canvas. */
    val originY: Double

    init {
        val widthToHeight = whiteKeyCount.toDouble() / whiteKeyHeightToWidth
        val fittedWidth = min(size.width, size.height * widthToHeight)
        val fittedHeight = fittedWidth / widthToHeight
        whiteKeyWidth = fittedWidth / whiteKeyCount.toDouble()
        keyHeight = fittedHeight
        originX = (size.width - fittedWidth) / 2
        originY = (size.height - fittedHeight) / 2
    }

    /** The outline drawn around each white key. */
    val outlineWidth: Double
        get() = max(1.0, whiteKeyWidth * OUTLINE_SHARE)

    /** The white key at this position, counting white keys from the left. */
    fun whiteKeyRect(index: Int): Rect =
        Rect(
            x = originX + index.toDouble() * whiteKeyWidth,
            y = originY,
            width = whiteKeyWidth,
            height = keyHeight,
        )

    /** Centred on the line between this white key and the next one. */
    fun blackKeyRect(afterWhiteKey: Int): Rect {
        val width = whiteKeyWidth * blackKeyWidthRatio
        val boundaryX = originX + (afterWhiteKey + 1).toDouble() * whiteKeyWidth
        return Rect(
            x = boundaryX - width / 2,
            y = originY,
            width = width,
            height = keyHeight * blackKeyHeightRatio,
        )
    }

    /**
     * One rect per key, in the layout's left-to-right key order, given each key's `isBlack`.
     * Each black key straddles the boundary after the white key before it, so the white keys
     * are counted up while walking the keys in order.
     */
    fun keyRects(isBlackFlags: List<Boolean>): List<Rect> {
        val rects = mutableListOf<Rect>()
        var whiteKeysPassed = 0
        for (isBlack in isBlackFlags) {
            if (isBlack) {
                rects.add(blackKeyRect(afterWhiteKey = whiteKeysPassed - 1))
            } else {
                rects.add(whiteKeyRect(index = whiteKeysPassed))
                whiteKeysPassed += 1
            }
        }
        return rects
    }

    /**
     * Swift's `keyRects(for:)`: one rect per key of the layout, in its left-to-right order.
     * The JVM name differs from the flags overload because both take a `List` and the JVM
     * erases the element type, so the two would otherwise clash.
     */
    @JvmName("keyRectsForKeys")
    fun keyRects(keys: List<KeyboardLayout.Key>): List<Rect> {
        val isBlackFlags = mutableListOf<Boolean>()
        for (key in keys) {
            isBlackFlags.add(key.isBlack)
        }
        return keyRects(isBlackFlags)
    }

    /**
     * The key's shape, and the circle and name drawn on a key of a given rect. The public
     * shares keep Swift's camelCase names, the keys `expected/answers.json` uses, so they stay
     * plain vals rather than SCREAMING_CASE consts.
     */
    @Suppress("MayBeConstant")
    companion object {
        /**
         * A white key is this many times taller than it is wide, so one octave of eight white
         * keys is 7:3 and two octaves are proportionally wider. The keys keep this shape and
         * are centred in whatever frame they are given, so the TV and the phone match.
         */
        val whiteKeyHeightToWidth: Double = 24.0 / 7.0

        /**
         * The tallest a white key may be stretched when the keyboard fills a short, wide
         * screen (a phone turned sideways): twice its true shape, still a readable key.
         */
        val maxStretchedHeightToWidth: Double = whiteKeyHeightToWidth * 2

        /** A black key's width as a share of a white key's width. */
        val blackKeyWidthRatio: Double = 0.6

        /** A black key's length as a share of a white key's length. */
        val blackKeyHeightRatio: Double = 0.6

        /**
         * Note circle radius as a share of the key's width. Black keys are narrower, so their
         * circles take a larger share to stay readable.
         */
        val whiteKeyCircleShare: Double = 0.36
        val blackKeyCircleShare: Double = 0.4

        private const val OUTLINE_SHARE = 0.03

        /** The circle's centre sits this many radii above the key's bottom edge. */
        private const val CIRCLE_LIFT_RADII = 1.4
        private const val WHITE_KEY_NAME_SHARE = 0.26
        private const val BLACK_KEY_NAME_SHARE = 0.4

        /** The name's centre sits this many font sizes above the circle's top. */
        private const val NAME_LIFT_FONT_SIZES = 0.9

        /** Width over height of a keyboard with this many white keys; lets callers give the
         *  keyboard a frame of its own shape. */
        fun widthToHeight(whiteKeyCount: Int): Double =
            whiteKeyCount.toDouble() / whiteKeyHeightToWidth

        /**
         * The key shape that fills this frame, for the full-screen board on a phone: the keys
         * grow taller than a real piano's so the board is not a strip across a short screen,
         * up to [maxStretchedHeightToWidth], and never shorter than their true shape.
         */
        fun stretchedHeightToWidth(
            size: Size,
            whiteKeyCount: Int,
        ): Double {
            val whiteKeyWidth = size.width / whiteKeyCount.toDouble()
            val fillingHeightToWidth = size.height / whiteKeyWidth
            val cappedHeightToWidth = min(fillingHeightToWidth, maxStretchedHeightToWidth)
            return max(cappedHeightToWidth, whiteKeyHeightToWidth)
        }

        /** Radius of the note circle on a key of this width. */
        fun circleRadius(
            keyWidth: Double,
            isBlack: Boolean,
        ): Double {
            var share = whiteKeyCircleShare
            if (isBlack) {
                share = blackKeyCircleShare
            }
            return keyWidth * share
        }

        /** The circle sits near the bottom of the key, where a pianist's eye lands. */
        fun circleCenter(
            onKey: Rect,
            radius: Double,
        ): Point = Point(x = onKey.midX, y = onKey.maxY - radius * CIRCLE_LIFT_RADII)

        /** Size of the note name written on the key above its circle. */
        fun keyNameFontSize(
            keyWidth: Double,
            isBlack: Boolean,
        ): Double {
            var share = WHITE_KEY_NAME_SHARE
            if (isBlack) {
                share = BLACK_KEY_NAME_SHARE
            }
            return keyWidth * share
        }

        /** Centre of the note name on the key: just above the circle's top edge. */
        fun keyNameCenter(
            onKey: Rect,
            circleTop: Double,
            fontSize: Double,
        ): Point = Point(x = onKey.midX, y = circleTop - fontSize * NAME_LIFT_FONT_SIZES)
    }
}
