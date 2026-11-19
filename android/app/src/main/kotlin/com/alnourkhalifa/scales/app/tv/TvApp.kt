package com.alnourkhalifa.scales.app.tv

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.ui.ChordColors

/**
 * The whole TV screen: the board, the options panel over it, and the full-screen Chord tables
 * and Drums, one level each on a [TvNavigation] stack.
 *
 * The theme follows the board's Dark board switch rather than the system, because the panel
 * sits beside the board and a light panel on a dark board would glare in a dim room.
 *
 * Back from a chip under the board returns to the board; otherwise it pops one level, and at
 * the board it finishes the activity, with no confirmation and no menu loop (Google's TV-DB).
 * It is handled here, once, so every screen gets the same order and none needs its own
 * `BackHandler`.
 */
@Composable
fun TvApp() {
    val app = rememberScalesApplication()
    val isDark = app.sessionState.value.settings.tvUsesDarkTheme
    val colorScheme = tvColorScheme(isDark)
    val navigation = rememberTvNavigation()
    val memory = rememberTvPanelMemory()
    val timer = rememberTvPracticeTimer()
    val activity = LocalActivity.current
    val boardFocusLinks = rememberTvBoardFocusLinks()
    BackHandler {
        // A chip or "Back to …" under the board: Back goes to the board first.
        if (boardFocusLinks.returnToBoard()) {
            return@BackHandler
        }
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
                TvChordsTab(onBack = { navigation.pop() })
            }

            TvRoute.Drums -> {
                TvDrumsTab(onBack = { navigation.pop() })
            }

            else -> {
                BoardWithPanel(route, navigation, memory, timer, boardFocusLinks)
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
    boardFocusLinks: TvBoardFocusLinks,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        TvBoardScreen(
            canFocus = route == TvRoute.Board,
            onOpenPanel = { navigation.push(TvRoute.Panel) },
            links = boardFocusLinks,
            bottomContent = { focus -> BoardChords(focus) },
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

/**
 * The chord chips under the board. The session is read through the app each time rather than
 * captured, because tests rebuild it (`reloadFromStorage`) while the screen is showing.
 */
@Composable
private fun BoardChords(focus: TvBoardFocus) {
    val app = rememberScalesApplication()
    val state = app.sessionState.value
    TvChordRow(
        state = state,
        onTap = { chord -> app.session.toggleRing(chord) },
        onRevert = { app.session.revertBoardChange() },
        focus = focus,
    )
}

/**
 * The TV theme for a dark or light board, with Apple's system blue as the accent (the
 * "Back to …" button and anything else drawn in `primary`), so the TV's controls take the
 * colour the iPhone's do.
 */
private fun tvColorScheme(isDark: Boolean): ColorScheme {
    if (isDark) {
        return darkColorScheme(primary = ChordColors.blueDark, onPrimary = Color.White)
    }
    return lightColorScheme(primary = ChordColors.blueLight, onPrimary = Color.White)
}
