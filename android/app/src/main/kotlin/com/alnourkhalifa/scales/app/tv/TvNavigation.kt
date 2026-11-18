package com.alnourkhalifa.scales.app.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.alnourkhalifa.scales.core.music.ScaleDegree

/**
 * One place the TV screen can be. The TV has no tabs and no touch, so every screen is a level
 * the remote's OK button climbs into and its Back button climbs out of; a stack of these is
 * the whole of the TV's navigation.
 */
sealed interface TvRoute {
    /** True while the options panel is drawn over the board (the panel and its lists). */
    val showsPanel: Boolean
        get() = false

    /** The full-screen board, where the app opens. */
    data object Board : TvRoute

    /** The options panel on the right, over the board. */
    data object Panel : TvRoute {
        override val showsPanel: Boolean
            get() = true
    }

    /**
     * The list of choices behind one panel row (Root, Scale, Tuning…), drawn in the panel's
     * place so the board stays visible and redraws as the player picks.
     */
    data class ChoiceList(
        val row: TvPanelRow,
    ) : TvRoute {
        override val showsPanel: Boolean
            get() = true
    }

    /** "Change a note": the scale's notes, one row each, plus "Add a note". */
    data object EditNoteNotes : TvRoute {
        override val showsPanel: Boolean
            get() = true
    }

    /**
     * The changes one note allows (lower, raise, remove). [degree] is null for the list of
     * notes that could be added, so one route serves both kinds of second-level list.
     */
    data class EditNoteEdits(
        val degree: ScaleDegree?,
    ) : TvRoute {
        override val showsPanel: Boolean
            get() = true
    }

    /** The major and minor key chord tables, full screen. */
    data object Chords : TvRoute

    /** The drum trainer, full screen. */
    data object Drums : TvRoute
}

/**
 * The TV screen's route stack. It starts at the board; [push] climbs one level and [pop]
 * climbs back. `TvApp` routes Back to [pop] and finishes the activity when [pop] reports
 * that the board was already showing.
 *
 * Why a hand-made stack and not Navigation Compose: there are seven routes, none takes
 * arguments worth serialising, and nothing outlives the activity, so a list in one
 * `mutableStateOf` is the whole job and keeps the Back order easy to read and to test.
 *
 * Focus and Back, the table every TV screen follows (plan 18 Nov §7 wave 4, Nov 5 §5):
 *
 * - Board. Focus: the board box (`tvBoard`), at launch and whenever the panel closes.
 *   OK opens Panel. Back leaves the app.
 * - Panel. Focus: Root on a fresh opening, the row just left on returning; left and right
 *   are trapped. OK opens the row's list or screen, or flips its switch. Back: Board.
 * - ChoiceList(row). Focus: the current value. OK applies it and returns to Panel.
 *   Back: Panel, unchanged.
 * - EditNoteNotes. Focus: the first note. OK opens EditNoteEdits(degree). Back: Panel.
 * - EditNoteEdits(degree). Focus: the first change. OK changes the note and returns to
 *   Board ([popToBoard]), where the chips show what it did. Back: EditNoteNotes.
 * - Chords, Drums. Focus: the screen's first control. Back: Panel, on the row that
 *   opened them.
 *
 * There is no exit confirmation and no menu loop (Google's TV-DB), and nothing depends on a
 * Menu button (TV-DM). Later chips under the board slot in between: Back from a chip goes to
 * the board box before leaving the app.
 */
@Stable
class TvNavigation {
    private var stack: List<TvRoute> by mutableStateOf(listOf(TvRoute.Board))

    /** The route on top, the one drawn now. */
    val current: TvRoute
        get() = stack.last()

    /** Every route from the board up, for tests and for screens that ask where they came from. */
    val routes: List<TvRoute>
        get() = stack

    /** Climbs one level, to a route opened from the current one. */
    fun push(route: TvRoute) {
        stack = stack + route
    }

    /**
     * Climbs back one level. Returns false, and changes nothing, when the board is already
     * showing, so the caller knows Back should leave the app.
     */
    fun pop(): Boolean {
        if (stack.size <= 1) {
            return false
        }
        stack = stack.dropLast(1)
        return true
    }

    /**
     * Drops every level above the board, for actions that finish with the board in view
     * (a note change shows its result on the board, not in the panel it came from).
     */
    fun popToBoard() {
        stack = listOf(TvRoute.Board)
    }
}

/** One [TvNavigation] for the TV screen, kept while the activity's composition lives. */
@Composable
fun rememberTvNavigation(): TvNavigation = remember { TvNavigation() }
