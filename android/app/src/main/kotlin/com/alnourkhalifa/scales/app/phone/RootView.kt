package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * The phone's four screens behind a bottom navigation bar: the fretboard, the key chord
 * tables, the Transform screen and the drum trainer. Twin of `RootView` in
 * `Scales/Views/RootView.swift`.
 *
 * The screens arrive as parameters so each can be built and tested on its own and swapped
 * in when it is ready. The selected tab survives rotation and process death
 * (`rememberSaveable`), as the iPhone keeps its tab. Keeping a saved scale on the board in
 * step with the library, which Swift's `RootView` does, lives in `ScalesApplication`, so the
 * TV screen gets it too.
 *
 * Each screen gets only the tab bar's height as padding and keeps the status bar and side
 * insets for itself (`safeDrawingPadding`), so its own background reaches the top edge, as
 * the board's colour does on the iPhone.
 */
@Composable
fun RootView(
    scales: @Composable () -> Unit,
    chords: @Composable () -> Unit,
    transform: @Composable () -> Unit,
    drums: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.SCALES) }
    val navigation = remember { AppNavigation(select = { tab -> selectedTab = tab }) }
    val screenInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = 0)
    CompositionLocalProvider(LocalAppNavigation provides navigation) {
        RootScaffold(
            selectedTab = selectedTab,
            onSelect = { tab -> selectedTab = tab },
            screenInsets = screenInsets,
            modifier = modifier,
            scales = scales,
            chords = chords,
            transform = transform,
            drums = drums,
        )
    }
}

@Composable
private fun RootScaffold(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
    screenInsets: WindowInsets,
    modifier: Modifier,
    scales: @Composable () -> Unit,
    chords: @Composable () -> Unit,
    transform: @Composable () -> Unit,
    drums: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = screenInsets,
        bottomBar = {
            TabBar(selectedTab = selectedTab, onSelect = onSelect)
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        ) {
            when (selectedTab) {
                AppTab.SCALES -> scales()
                AppTab.CHORDS -> chords()
                AppTab.TRANSFORM -> transform()
                AppTab.DRUMS -> drums()
            }
        }
    }
}

@Composable
private fun TabBar(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit,
) {
    val colors = iPhoneTabColors()
    NavigationBar {
        for (tab in AppTab.entries) {
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                icon = { Icon(imageVector = TabIcons.forTab(tab), contentDescription = null) },
                label = { Text(tab.label) },
                colors = colors,
                modifier = Modifier.testTag(tab.testTag),
            )
        }
    }
}

/**
 * The selected tab in the accent colour on a quiet pill, as the iPhone's tab bar draws it,
 * instead of Material's tinted indicator with a dark icon.
 */
@Composable
private fun iPhoneTabColors(): NavigationBarItemColors {
    val scheme = MaterialTheme.colorScheme
    return NavigationBarItemDefaults.colors(
        selectedIconColor = scheme.primary,
        selectedTextColor = scheme.primary,
        indicatorColor = scheme.surfaceVariant,
    )
}

/**
 * Tab symbols standing in for the SF Symbols the iPhone uses (`guitars`, `tablecells`,
 * `wand.and.stars`, `metronome`). Drawn here because the Material icons library is several
 * megabytes for four shapes and has no guitar or metronome. All are 24 × 24 so `Icon` tints
 * them like Material icons; the black fill is replaced by the tab's colour.
 */
private object TabIcons {
    private val ink = SolidColor(Color.Black)

    fun forTab(tab: AppTab): ImageVector =
        when (tab) {
            AppTab.SCALES -> guitar
            AppTab.CHORDS -> table
            AppTab.TRANSFORM -> wand
            AppTab.DRUMS -> metronome
        }

    /** An upright guitar: head, neck, and a body of two overlapping bouts. */
    private val guitar: ImageVector =
        iconBuilder("TabGuitar")
            .path(fill = ink) {
                rectangle(left = 10f, top = 1f, right = 14f, bottom = 4.5f)
                // The neck stops where the body starts: shapes drawn in opposite directions
                // would cancel out where they overlap and leave a hole.
                rectangle(left = 11.2f, top = 4.5f, right = 12.8f, bottom = 9.4f)
                circle(centerX = 12f, centerY = 12.8f, radius = 3.6f)
                circle(centerX = 12f, centerY = 18f, radius = 4.8f)
            }.build()

    /** Six cells in two rows of three, like a chord table. */
    private val table: ImageVector =
        iconBuilder("TabTable")
            .path(fill = ink) {
                val columns = listOf(3f to 8.4f, 9.3f to 14.7f, 15.6f to 21f)
                val rows = listOf(5f to 11.5f, 12.5f to 19f)
                for (row in rows) {
                    for (column in columns) {
                        rectangle(
                            left = column.first,
                            top = row.first,
                            right = column.second,
                            bottom = row.second,
                        )
                    }
                }
            }.build()

    /** A slanted wand with two sparkles above it. */
    private val wand: ImageVector =
        iconBuilder("TabWand")
            .path(fill = ink) {
                moveTo(3.5f, 19f)
                lineTo(14.5f, 8f)
                lineTo(16.5f, 10f)
                lineTo(5.5f, 21f)
                close()
                sparkle(centerX = 18f, centerY = 4.5f, radius = 3.5f)
                sparkle(centerX = 8f, centerY = 6f, radius = 2.2f)
                sparkle(centerX = 20f, centerY = 13f, radius = 1.8f)
            }.build()

    /** A metronome: a hollow tapered case with its pendulum leaning to the right. */
    private val metronome: ImageVector =
        iconBuilder("TabMetronome")
            .path(fill = ink, pathFillType = PathFillType.EvenOdd) {
                moveTo(9f, 2.5f)
                lineTo(15f, 2.5f)
                lineTo(20f, 21.5f)
                lineTo(4f, 21.5f)
                close()
                moveTo(10.4f, 4.5f)
                lineTo(13.6f, 4.5f)
                lineTo(17.4f, 19.5f)
                lineTo(6.6f, 19.5f)
                close()
            }.path(fill = ink) {
                moveTo(11.2f, 17.6f)
                lineTo(17.6f, 6.4f)
                lineTo(19f, 7.2f)
                lineTo(12.6f, 18.4f)
                close()
            }.build()

    private fun iconBuilder(name: String): ImageVector.Builder =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )

    private fun PathBuilder.rectangle(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
    ) {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right, bottom)
        lineTo(left, bottom)
        close()
    }

    /** A full circle as two half-circle arcs, the way SVG paths draw one. */
    private fun PathBuilder.circle(
        centerX: Float,
        centerY: Float,
        radius: Float,
    ) {
        val diameter = radius * 2
        moveTo(centerX - radius, centerY)
        arcToRelative(radius, radius, 0f, isMoreThanHalf = true, isPositiveArc = true, diameter, 0f)
        arcToRelative(
            radius,
            radius,
            0f,
            isMoreThanHalf = true,
            isPositiveArc = true,
            -diameter,
            0f,
        )
        close()
    }

    /** A four-pointed star with pinched sides. */
    private fun PathBuilder.sparkle(
        centerX: Float,
        centerY: Float,
        radius: Float,
    ) {
        val waist = radius * 0.28f
        moveTo(centerX, centerY - radius)
        lineTo(centerX + waist, centerY - waist)
        lineTo(centerX + radius, centerY)
        lineTo(centerX + waist, centerY + waist)
        lineTo(centerX, centerY + radius)
        lineTo(centerX - waist, centerY + waist)
        lineTo(centerX - radius, centerY)
        lineTo(centerX - waist, centerY - waist)
        close()
    }
}
