package com.alnourkhalifa.scales.app.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.TVDisplayView
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/** Margins on every side, as a share of the screen: clear of the overscan some TVs crop. */
private const val TV_MARGIN_SHARE = 0.05

/** How long the "Press OK for options" hint stays before fading. */
private val HINT_DURATION = 5.seconds

/**
 * The focus requesters joining the TV board to the controls under it, kept by `TvApp` for
 * the screen's life.
 *
 * Why it is hoisted that far: the board box and the chips are separate composables, yet each
 * needs the other's [FocusRequester] (the board sends Down to [below], the chips send Up to
 * [board]), and Back from a chip must return to the board before it can leave the app. Back
 * reaches the app through the activity's back dispatcher on the key's release, and only
 * `TvApp` has a `BackHandler`; it asks [returnToBoard] first.
 *
 * @property board the board box, where Up and Back from the chips land.
 * @property below where Down from the board lands. The row under the board attaches it to the
 *   "Back to …" button when a note change is shown, otherwise to the ringed or first chip.
 */
@Stable
class TvBoardFocusLinks {
    val board: FocusRequester = FocusRequester()
    val below: FocusRequester = FocusRequester()

    /** True while a chip or the "Back to …" button holds focus; set by the row. */
    var isBelowBoardFocused: Boolean by mutableStateOf(false)

    /**
     * Back's first stop: when focus is under the board it moves to the board and this returns
     * true, so the caller does nothing more; otherwise false and Back carries on as usual.
     */
    fun returnToBoard(): Boolean {
        if (!isBelowBoardFocused) {
            return false
        }
        board.requestFocus()
        return true
    }
}

/** One [TvBoardFocusLinks] for the TV screen, kept while the activity's composition lives. */
@Composable
fun rememberTvBoardFocusLinks(): TvBoardFocusLinks = remember { TvBoardFocusLinks() }

/**
 * What `TvBoardScreen` hands to the slot under the board: the [links] and whether anything
 * there may take focus now. [canFocus] is false while the options panel covers the board, so
 * focus can't slip from the panel onto a chip the player can't see is focused.
 */
@Immutable
data class TvBoardFocus(
    val links: TvBoardFocusLinks,
    val canFocus: Boolean,
) {
    val board: FocusRequester
        get() = links.board

    val below: FocusRequester
        get() = links.below
}

/**
 * The TV's start screen: the saved board full screen, 5% margins all round (Google's 48 dp
 * and 27 dp, Amazon's 90% fill), the background running to the screen's edge.
 *
 * The whole board is one focusable box that takes focus at launch, so a remote's OK always
 * has something to act on (Amazon tests "Select with nothing highlighted") and opens the
 * options panel. [canFocus] is false while the panel is over the board, so focus can't fall
 * back onto it; when it turns true again the board takes focus back.
 *
 * [bottomContent] sits under the board inside the margins; the chord chips go there. Its
 * focusable controls are children of the board box, so focus can't reach them by searching:
 * the board sends Down to [TvBoardFocusLinks.below], which the slot attaches to its first
 * control, and the slot sends Up to [TvBoardFocusLinks.board]. [links] is passed in by
 * `TvApp`, whose Back handler uses it to return from the chips to the board. The same
 * [canFocus] flag reaches the slot, so nothing under the board takes focus while the panel is
 * open.
 */
@Composable
fun TvBoardScreen(
    canFocus: Boolean,
    onOpenPanel: () -> Unit,
    modifier: Modifier = Modifier,
    links: TvBoardFocusLinks = rememberTvBoardFocusLinks(),
    bottomContent: (@Composable (TvBoardFocus) -> Unit)? = null,
) {
    val app = rememberScalesApplication()
    val state = app.sessionState.value
    val settings = state.settings
    val requester = links.board
    val belowRequester = links.below
    val focus = TvBoardFocus(links = links, canFocus = canFocus)
    LaunchedEffect(canFocus) {
        if (canFocus) {
            requestFocusOnceLaidOut(requester)
        }
    }
    val hasBottomContent = bottomContent != null
    val boxModifier =
        modifier
            .fillMaxSize()
            .testTag("tvBoard")
            .focusRequester(requester)
            .focusProperties {
                this.canFocus = canFocus
                if (hasBottomContent) {
                    down = belowRequester
                }
            }.onKeyEvent { event -> handleOk(event, onOpenPanel) }
            .focusable()
    Box(modifier = boxModifier) {
        val palette = FretboardPalette.matching(settings.tvUsesDarkTheme)
        TVDisplayView(
            scale = state.scale,
            palette = palette,
            modifier = Modifier.fillMaxSize(),
            instrument = settings.selectedInstrument,
            tuning = settings.tuning,
            fretCount = settings.fretCount,
            keyboardOctaves = settings.selectedKeyboardOctaves,
            labelStyle = settings.labelStyle,
            thinStringOnTop = settings.thinStringOnTop,
            isLeftHanded = settings.isLeftHanded,
            ringedNotes = state.ringedNotes,
            horizontalMarginShare = TV_MARGIN_SHARE,
            verticalMarginShare = TV_MARGIN_SHARE,
            bottomContent = bottomContent?.let { content -> { content(focus) } },
        )
        OptionsHint(palette)
    }
}

/**
 * OK (the remote's centre button, or Enter on a keyboard) opens the panel. It acts on the key
 * coming up, not going down: the panel takes focus as it opens, and a release arriving after
 * that would land on the panel's first row and open its list as well. The press is consumed
 * too, so nothing else sees half of the click.
 */
private fun handleOk(
    event: KeyEvent,
    onOpenPanel: () -> Unit,
): Boolean {
    val isOk =
        event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
    if (!isOk) {
        return false
    }
    if (event.type == KeyEventType.KeyUp) {
        onOpenPanel()
    }
    return true
}

/**
 * "Press OK for options" in the bottom-right margin, fading after five seconds. The board has
 * no visible button, so a first-time player needs telling once; after that the words would
 * only sit on the practice picture.
 */
@Composable
private fun OptionsHint(palette: FretboardPalette) {
    var isShowing by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(HINT_DURATION)
        isShowing = false
    }
    val opacity by animateFloatAsState(
        targetValue = if (isShowing) 1f else 0f,
        label = "hint opacity",
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val endMargin = maxWidth * TV_MARGIN_SHARE.toFloat()
        Text(
            text = "Press OK for options",
            color = palette.secondaryText,
            fontSize = 16.sp,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = endMargin, bottom = 4.dp)
                    .alpha(opacity)
                    .testTag("tvHint"),
        )
    }
}
