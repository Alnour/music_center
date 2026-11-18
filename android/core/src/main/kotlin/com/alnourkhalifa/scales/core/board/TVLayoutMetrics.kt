package com.alnourkhalifa.scales.core.board

import kotlin.math.min

/**
 * The sizes of the TV layout (`TVDisplayView`) for a screen of a given size. Twin of
 * `Scales/Views/TVLayoutMetrics.swift`, with the same members; lengths are `Double` and the
 * multiples Swift writes inline are named constants.
 *
 * Everything is a multiple of one unit, a hundredth of the screen height, capped so a screen
 * wider than 16:9 does not grow the text past what the height allows. That keeps a 720p,
 * 1080p and 4K picture identical in shape. The margins keep everything clear of TV overscan,
 * where some sets crop the outer edge of the picture.
 */
class TVLayoutMetrics(
    val size: Size,
) {
    /** A hundredth of the height, or a 178th of the width on a screen wider than 16:9. */
    val unit: Double
        get() = min(size.height / UNITS_PER_HEIGHT, size.width / UNITS_PER_WIDTH)

    /** Space between the title row, the notes row and the board. */
    val spacing: Double
        get() = unit * SPACING_UNITS

    val titleFontSize: Double
        get() = unit * TITLE_FONT_UNITS

    /** The tuning (or "Keyboard") written beside the title. */
    val captionFontSize: Double
        get() = unit * CAPTION_FONT_UNITS

    val notesFontSize: Double
        get() = unit * NOTES_FONT_UNITS

    /** The close button, shown only when the layout fills the phone. */
    val closeIconSize: Double
        get() = unit * CLOSE_ICON_UNITS

    val closeLeadingPadding: Double
        get() = unit * CLOSE_LEADING_PADDING_UNITS

    /** Clear of the overscan that some sets crop from the sides. */
    val horizontalMargin: Double
        get() = size.width * HORIZONTAL_MARGIN_SHARE

    /** Clear of the overscan that some sets crop from the top and bottom. */
    val verticalMargin: Double
        get() = size.height * VERTICAL_MARGIN_SHARE

    /** The unit's divisors and every size in units. */
    private companion object {
        const val UNITS_PER_HEIGHT = 100.0

        /** 16:9 of a hundred units is about 178 units across. */
        const val UNITS_PER_WIDTH = 178.0
        const val SPACING_UNITS = 3.0
        const val TITLE_FONT_UNITS = 8.0
        const val CAPTION_FONT_UNITS = 3.5
        const val NOTES_FONT_UNITS = 3.6
        const val CLOSE_ICON_UNITS = 6.0
        const val CLOSE_LEADING_PADDING_UNITS = 3.0
        const val HORIZONTAL_MARGIN_SHARE = 0.04
        const val VERTICAL_MARGIN_SHARE = 0.05
    }
}
