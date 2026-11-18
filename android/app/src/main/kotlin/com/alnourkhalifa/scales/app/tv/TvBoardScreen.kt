package com.alnourkhalifa.scales.app.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * The TV's start screen: the saved board full screen, 5% margins all round (Google's 48 dp
 * and 27 dp, Amazon's 90% fill), the background running to the screen's edge.
 *
 * The whole board is one focusable box that takes focus at launch, so a remote's OK always
 * has something to act on (Amazon tests "Select with nothing highlighted") and opens the
 * options panel. [canFocus] is false while the panel is over the board, so focus can't fall
 * back onto it; when it turns true again the board takes focus back.
 *
 * [bottomContent] sits under the board inside the margins; the chord chips go there. Its
 * focusable controls are children of the board box, and the focus rules between the box and
 * them belong to whoever fills the slot (extend the box's `focusProperties`).
 */
@Composable
fun TvBoardScreen(
    canFocus: Boolean,
    onOpenPanel: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val app = rememberScalesApplication()
    val state = app.sessionState.value
    val settings = state.settings
    val requester = remember { FocusRequester() }
    LaunchedEffect(canFocus) {
        if (canFocus) {
            requestFocusOnceLaidOut(requester)
        }
    }
    val boxModifier =
        modifier
            .fillMaxSize()
            .testTag("tvBoard")
            .focusRequester(requester)
            .focusProperties { this.canFocus = canFocus }
            .onKeyEvent { event -> handleOk(event, onOpenPanel) }
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
            bottomContent = bottomContent,
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
