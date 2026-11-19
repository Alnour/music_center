package com.alnourkhalifa.scales.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** iOS's system blue in light mode, the tint of the iPhone's menus and selected tab. */
private val SystemBlueLight = Color(0xFF007AFF)

/** iOS's system blue in dark mode, slightly lighter to read on black. */
private val SystemBlueDark = Color(0xFF0A84FF)

/**
 * UIKit's light system colours. `systemBackground` is the plain white of the Scales screen;
 * `systemGroupedBackground` is the grey behind a Form's white cards (the gear and TV menus).
 * `secondaryLabel` is #3C3C43 at 60 %, given here already blended on white, because Material
 * components draw `onSurfaceVariant` as an opaque colour.
 */
private object LightSystemColors {
    val background = Color(0xFFFFFFFF)
    val groupedBackground = Color(0xFFF2F2F7)
    val label = Color(0xFF000000)
    val secondaryLabel = Color(0xFF8A8A8E)
    val separator = Color(0xFFC6C6C8)
    val fill = Color(0xFFE5E5EA)
}

/**
 * UIKit's dark system colours: a black screen, Form cards in `secondarySystemGroupedBackground`
 * (#1C1C1E), and `secondaryLabel` (#EBEBF5 at 60 %) blended on black.
 */
private object DarkSystemColors {
    val background = Color(0xFF000000)
    val groupedCard = Color(0xFF1C1C1E)
    val raisedCard = Color(0xFF2C2C2E)
    val label = Color(0xFFFFFFFF)
    val secondaryLabel = Color(0xFF98989F)
    val separator = Color(0xFF38383A)
    val fill = Color(0xFF3A3A3C)
}

/**
 * The phone's Material 3 theme: light or dark with the system, like the iPhone app.
 *
 * Dynamic colour (Android 12's wallpaper colours) stays off because the iOS app uses fixed
 * colours, and the board's palette (`FretboardPalette`) is fixed too; a wallpaper-tinted
 * toolbar beside it would look like a different app. The accent is iOS's system blue so the
 * selected tab and menus match the iPhone. Typography keeps Material's defaults on the system
 * font; text that shows accidentals opts in to `MusicFontFamily` itself.
 *
 * Every neutral role is set to Apple's greys, because Material's defaults are tinted lavender
 * and would put purple behind the selected pill and in the menus. The roles map as:
 * `surface` = the screen (`systemBackground`), `background` and the light sheet = the Form
 * grey, `surfaceContainer*` = menus and Form cards, `onSurfaceVariant` = `secondaryLabel`,
 * `secondaryContainer` = a selected segment's fill.
 */
@Composable
fun ScalesTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = colorSchemeFor(isDark)
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun colorSchemeFor(isDark: Boolean): ColorScheme {
    if (isDark) {
        return darkScheme()
    }
    return lightScheme()
}

private fun lightScheme(): ColorScheme {
    val colors = LightSystemColors
    return lightColorScheme(
        primary = SystemBlueLight,
        onPrimary = Color.White,
        secondary = colors.secondaryLabel,
        onSecondary = Color.White,
        secondaryContainer = colors.fill,
        onSecondaryContainer = colors.label,
        background = colors.groupedBackground,
        onBackground = colors.label,
        surface = colors.background,
        onSurface = colors.label,
        surfaceVariant = colors.fill,
        onSurfaceVariant = colors.secondaryLabel,
        surfaceTint = Color.Transparent,
        surfaceBright = colors.background,
        surfaceDim = colors.groupedBackground,
        surfaceContainerLowest = colors.background,
        surfaceContainerLow = colors.groupedBackground,
        surfaceContainer = colors.background,
        surfaceContainerHigh = colors.background,
        surfaceContainerHighest = colors.fill,
        outline = colors.separator,
        outlineVariant = colors.fill,
    )
}

private fun darkScheme(): ColorScheme {
    val colors = DarkSystemColors
    return darkColorScheme(
        primary = SystemBlueDark,
        onPrimary = Color.White,
        secondary = colors.secondaryLabel,
        onSecondary = Color.Black,
        secondaryContainer = colors.fill,
        onSecondaryContainer = colors.label,
        background = colors.background,
        onBackground = colors.label,
        surface = colors.background,
        onSurface = colors.label,
        surfaceVariant = colors.raisedCard,
        onSurfaceVariant = colors.secondaryLabel,
        surfaceTint = Color.Transparent,
        surfaceBright = colors.raisedCard,
        surfaceDim = colors.background,
        surfaceContainerLowest = colors.background,
        surfaceContainerLow = colors.groupedCard,
        surfaceContainer = colors.groupedCard,
        surfaceContainerHigh = colors.raisedCard,
        surfaceContainerHighest = colors.fill,
        outline = colors.separator,
        outlineVariant = colors.raisedCard,
    )
}
