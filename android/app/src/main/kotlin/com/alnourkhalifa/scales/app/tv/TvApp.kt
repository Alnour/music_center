package com.alnourkhalifa.scales.app.tv

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.alnourkhalifa.scales.app.rememberScalesApplication

/**
 * The whole TV screen: the board, the options panel over it, and the full-screen Chord tables
 * and Drums, one level each on a [TvNavigation] stack.
 *
 * The theme follows the board's Dark board switch rather than the system, because the panel
 * sits beside the board and a light panel on a dark board would glare in a dim room.
 *
 * Back pops one level; at the board it finishes the activity, with no confirmation and no
 * menu loop (Google's TV-DB). It is handled here, once, so every screen gets the same order
 * and none needs its own `BackHandler`.
 */
@Composable
fun TvApp() {
    val app = rememberScalesApplication()
    val isDark = app.sessionState.value.settings.tvUsesDarkTheme
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()
    val navigation = rememberTvNavigation()
    val memory = rememberTvPanelMemory()
    val timer = rememberTvPracticeTimer()
    val activity = LocalActivity.current
    BackHandler {
        val climbed = navigation.pop()
        if (!climbed) {
            activity?.finish()
        }
    }
    val route = navigation.current
    LaunchedEffect(route) {
        if (route == TvRoute.Board) {
            memory.reset()
        }
    }
    MaterialTheme(colorScheme = colorScheme) {
        when (route) {
            TvRoute.Chords -> {
                TvChordsTab()
            }

            TvRoute.Drums -> {
                TvDrumsTab()
            }

            else -> {
                BoardWithPanel(route, navigation, memory, timer)
            }
        }
    }
}

/** The board, with the panel on its right while the route is one of the panel's levels. */
@Composable
private fun BoardWithPanel(
    route: TvRoute,
    navigation: TvNavigation,
    memory: TvPanelMemory,
    timer: TvPracticeTimer,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TvBoardScreen(
            canFocus = route == TvRoute.Board,
            onOpenPanel = { navigation.push(TvRoute.Panel) },
        )
        if (route.showsPanel) {
            TvOptionsPanel(
                route = route,
                navigation = navigation,
                memory = memory,
                timer = timer,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}
