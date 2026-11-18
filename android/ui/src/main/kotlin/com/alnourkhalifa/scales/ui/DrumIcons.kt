package com.alnourkhalifa.scales.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The play and pause symbols for the drum trainer's button, standing in for the SF Symbols
 * `play.fill` and `pause.fill` that `Scales/Views/DrumTrainerView.swift` uses.
 *
 * Why they are drawn here: the Material icons library is several megabytes for two shapes,
 * and the `ui` module stays on Compose Foundation. Both are 24 × 24 like Material icons, so
 * an `Icon` tints them with the button's content colour; the fill colour below is replaced.
 */
object DrumIcons {
    /** A right-pointing triangle, the usual "start" sign. */
    val Play: ImageVector =
        ImageVector
            .Builder(
                name = "DrumPlay",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 5f)
                lineTo(19f, 12f)
                lineTo(8f, 19f)
                close()
            }.build()

    /** Two upright bars, the usual "pause" sign. */
    val Pause: ImageVector =
        ImageVector
            .Builder(
                name = "DrumPause",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 5f)
                lineTo(10f, 5f)
                lineTo(10f, 19f)
                lineTo(6f, 19f)
                close()
                moveTo(14f, 5f)
                lineTo(18f, 5f)
                lineTo(18f, 19f)
                lineTo(14f, 19f)
                close()
            }.build()
}
