package com.alnourkhalifa.scales.app.phone

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The app's four screens, in the iPhone's tab order. Twin of `AppTab` in
 * `Scales/Views/RootView.swift`. [testTag] ("tab-scales"…) is how UI tests open a tab.
 */
enum class AppTab(
    val label: String,
    val testTag: String,
) {
    SCALES("Scales", "tab-scales"),
    CHORDS("Chords", "tab-chords"),
    TRANSFORM("Transform", "tab-transform"),
    DRUMS("Drums", "tab-drums"),
}

/**
 * Lets a screen switch tabs, the way `AppNavigation` in `Scales/Views/RootView.swift` lets a
 * lesson put its scale on the fretboard: the Transform screen's "Show on the Fretboard" opens
 * the Scales tab after choosing the scale. Only [RootView] owns the selected tab; screens get
 * this handle through [LocalAppNavigation].
 */
class AppNavigation(
    private val select: (AppTab) -> Unit,
) {
    fun open(tab: AppTab) {
        select(tab)
    }
}

val LocalAppNavigation =
    staticCompositionLocalOf<AppNavigation> {
        error("LocalAppNavigation is provided by RootView")
    }
