package com.alnourkhalifa.scales.core.board

/**
 * A width and height on the drawing canvas, the stand-in for CoreGraphics' `CGSize` that the
 * Swift geometry files take. `core` has no Android imports, so the board, keyboard and drum
 * staff geometry use these three small types and the `ui` module converts them to Compose's
 * own where it draws.
 */
data class Size(
    val width: Double,
    val height: Double,
)

/** A position on the canvas, the stand-in for `CGPoint`. */
data class Point(
    val x: Double,
    val y: Double,
)

/**
 * A rectangle on the canvas by its top-left corner and size, the stand-in for `CGRect`.
 * Only the edges the geometry reads (`midX`, `maxY`) are mirrored.
 */
data class Rect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    /** The horizontal centre, where a key's circle and name are drawn. */
    val midX: Double
        get() = x + width / 2

    /** The bottom edge; y grows downward on both platforms. */
    val maxY: Double
        get() = y + height
}
