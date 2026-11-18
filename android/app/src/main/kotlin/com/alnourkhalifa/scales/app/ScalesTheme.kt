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
 * The phone's Material 3 theme: light or dark with the system, like the iPhone app.
 *
 * Dynamic colour (Android 12's wallpaper colours) stays off because the iOS app uses fixed
 * colours, and the board's palette (`FretboardPalette`) is fixed too; a wallpaper-tinted
 * toolbar beside it would look like a different app. The accent is iOS's system blue so the
 * selected tab and menus match the iPhone. Typography keeps Material's defaults on the system
 * font; text that shows accidentals opts in to `MusicFontFamily` itself.
 */
@Composable
fun ScalesTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colorScheme = colorSchemeFor(isDark)
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun colorSchemeFor(isDark: Boolean): ColorScheme {
    if (isDark) {
        return darkColorScheme(primary = SystemBlueDark, onPrimary = Color.White)
    }
    return lightColorScheme(primary = SystemBlueLight, onPrimary = Color.White)
}
