package com.alnourkhalifa.scales.app.tv

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.alnourkhalifa.scales.app.drums.DrumPlayer
import com.alnourkhalifa.scales.app.drums.rememberDrumPlayer
import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.ui.DrumIcons
import com.alnourkhalifa.scales.ui.DrumStaffView
import kotlin.math.roundToInt

/** The margin on every side, as a share of the screen, clear of what some sets crop. */
private const val OVERSCAN_SHARE = 0.05f

/**
 * The staff's width over its height: the tablet's long, low staff. Across the 90 % of a 16:9
 * screen inside the margins it is about 36 % of the screen height, leaving room below it for
 * the controls and the groove list.
 */
private const val STAFF_ASPECT = 4.5f

/** How far the fast-forward and rewind buttons move the tempo, in beats per minute. */
private const val MEDIA_TEMPO_STEP = 5

/**
 * The Drums screen on the television: the groove on the drum staff with its bouncing ball,
 * and one row of controls under it (Play or Pause, the groove, the tempo and the drum
 * sounds), all worked with the D-pad. The TV counterpart of the phone's `DrumTrainerView`
 * and of `DrumTrainerView` in `Scales/Views/DrumTrainerView.swift`.
 *
 * What differs from the phone:
 * - The tempo is two buttons, "−" and "+", one beat per minute per press and repeating while
 *   OK is held, because a slider with 180 stops is slow to move with a remote. Each change
 *   applies at once.
 * - The groove picker opens as a list under the controls; OK picks a groove, Back closes it.
 * - The remote's media keys work anywhere on the screen: Play/Pause toggles the drums, Play
 *   only starts and Pause only stops them, and Fast-forward and Rewind move the tempo by 5.
 *
 * The player comes from [rememberDrumPlayer], which keeps the screen on while the drums play
 * and stops them when the screen leaves. Back closes the groove list when it is open, and
 * otherwise calls [onBack]. The screen is always dark ([TvDarkTheme]).
 */
@Composable
fun TvDrumsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvDarkTheme {
        TvDrumsContent(onBack = onBack, modifier = modifier)
    }
}

/** The staff, the control row and the groove list when open, with the remote's keys. */
@Composable
private fun TvDrumsContent(
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val player = rememberDrumPlayer()
    var isGrooveListOpen by remember { mutableStateOf(false) }
    val playFocus = remember { FocusRequester() }
    val grooveButtonFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusOnceAttached(playFocus)
    }

    // Focus goes back to the groove button before the list leaves, so it never falls to
    // nothing while the list's focused row is removed.
    val closeGrooveList = {
        grooveButtonFocus.requestFocus()
        isGrooveListOpen = false
    }
    val onBackKey = {
        if (isGrooveListOpen) {
            closeGrooveList()
        } else {
            onBack()
        }
    }

    val background = MaterialTheme.colorScheme.background
    val keyHandler =
        Modifier.onPreviewKeyEvent { event ->
            val isBack = handleBackKey(event.key, event.type, onBackKey)
            isBack || handleMediaKey(event, player)
        }
    OverscanColumn(background = background, modifier = modifier.then(keyHandler)) {
        TvDrumStaff(player = player)
        DrumControlsRow(
            player = player,
            playFocus = playFocus,
            grooveButtonFocus = grooveButtonFocus,
            onOpenGrooves = { isGrooveListOpen = true },
        )
        if (isGrooveListOpen) {
            GrooveList(player = player, onClose = closeGrooveList)
        }
    }
}

/**
 * The screen's background to the edge of the screen, as Google asks, with the content in a
 * column inset by 5 % on every side, clear of the overscan some sets crop.
 */
@Composable
private fun OverscanColumn(
    background: Color,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(background),
    ) {
        val horizontalMargin = maxWidth * OVERSCAN_SHARE
        val verticalMargin = maxHeight * OVERSCAN_SHARE
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalMargin, vertical = verticalMargin),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/**
 * The staff with its moving ball, across the full width. As on the phone, the time lives in
 * a state only the frame loop writes and only the staff's drawing reads, so a playing groove
 * redraws the picture each frame without recomposing the controls; the loop uses
 * `withInfiniteAnimationFrameNanos` so Compose tests still see the screen as idle.
 */
@Composable
private fun TvDrumStaff(player: DrumPlayer) {
    val isPlaying = player.isPlaying
    var nowNanos by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withInfiniteAnimationFrameNanos {
                nowNanos = System.nanoTime()
            }
        }
    }
    DrumStaffView(
        groove = player.groove,
        isPlaying = isPlaying,
        ball = { player.ballPosition(nowNanos) },
        isDark = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(STAFF_ASPECT)
                .clip(RoundedCornerShape(16.dp)),
    )
}

/**
 * Play or Pause, the groove, "−", the tempo, "+" and the drum sounds, left to right. The row
 * is one focus group that remembers its last focused control, so coming back up from the
 * groove list lands where the player left.
 */
@Composable
private fun DrumControlsRow(
    player: DrumPlayer,
    playFocus: FocusRequester,
    grooveButtonFocus: FocusRequester,
    onOpenGrooves: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .focusRestorer()
                .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayPauseButton(player = player, modifier = Modifier.focusRequester(playFocus))
        Button(
            onClick = onOpenGrooves,
            modifier = Modifier.focusRequester(grooveButtonFocus),
        ) {
            Text(text = "Groove: ${player.groove.name}")
        }
        TempoStepButton(label = "−", step = -1, player = player, tag = "tempoDown")
        Text(
            text = "Tempo: ${player.tempo.roundToInt()} BPM",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("tempoLabel"),
        )
        TempoStepButton(label = "+", step = 1, player = player, tag = "tempoUp")
        SoundSwitch(player = player)
    }
}

/** Starts or pauses the drums; its icon and title say what a press will do. */
@Composable
private fun PlayPauseButton(
    player: DrumPlayer,
    modifier: Modifier,
) {
    val isPlaying = player.isPlaying
    val title = if (isPlaying) "Pause" else "Play"
    val icon = if (isPlaying) DrumIcons.Pause else DrumIcons.Play
    Button(
        onClick = { player.togglePlayback() },
        modifier = modifier.testTag("playPause"),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
        Text(text = title)
    }
}

/**
 * Moves the tempo by [step] beats per minute per press, within the player's 40 to 220.
 *
 * Holding OK repeats the step: the remote sends the held key again and again, and every
 * repeat moves the tempo here, before the button sees it. The button itself still clicks
 * once when OK is released, so a hold moves the tempo once per key press the remote sent.
 */
@Composable
private fun TempoStepButton(
    label: String,
    step: Int,
    player: DrumPlayer,
    tag: String,
) {
    val applyStep = {
        player.tempo = player.tempo + step
    }
    Button(
        onClick = applyStep,
        modifier =
            Modifier
                .testTag(tag)
                .onPreviewKeyEvent { event ->
                    val isHeldSelect = isSelectKey(event.key) && isRepeatedPress(event)
                    if (isHeldSelect) {
                        applyStep()
                    }
                    isHeldSelect
                },
    ) {
        Text(text = label)
    }
}

/**
 * Drum sounds on or off; with them off the ball still bounces, for counting in silence. The
 * whole row is one control that OK flips, read out as a switch.
 */
@Composable
private fun SoundSwitch(player: DrumPlayer) {
    val isSoundOn = player.isSoundOn
    Surface(
        onClick = { player.isSoundOn = !isSoundOn },
        modifier =
            Modifier
                .testTag("drumSounds")
                .semantics {
                    role = Role.Switch
                    toggleableState = ToggleableState(isSoundOn)
                },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Drum sounds")
            Switch(checked = isSoundOn, onCheckedChange = null)
        }
    }
}

/**
 * The five built-in grooves, opened from the groove button. Focus starts on the groove that
 * is playing; OK picks the focused one (a playing groove restarts with it) and closes the
 * list, and Back closes it unchanged (handled by the screen).
 */
@Composable
private fun GrooveList(
    player: DrumPlayer,
    onClose: () -> Unit,
) {
    val currentFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusOnceAttached(currentFocus)
    }
    LazyColumn(
        modifier =
            Modifier
                .width(GrooveListWidth)
                .focusRestorer()
                .testTag("grooveList"),
    ) {
        for (groove in DrumGroove.builtIn) {
            item(key = groove.name) {
                val isCurrent = groove == player.groove
                var itemModifier: Modifier = Modifier
                if (isCurrent) {
                    itemModifier = itemModifier.focusRequester(currentFocus)
                }
                DenseListItem(
                    selected = isCurrent,
                    onClick = {
                        player.groove = groove
                        onClose()
                    },
                    headlineContent = { Text(text = groove.name) },
                    modifier = itemModifier,
                )
            }
        }
    }
}

/** Wide enough for "Four on the Floor" with the list item's padding. */
private val GrooveListWidth = 320.dp

/**
 * Handles the remote's media keys and reports whether the key was used. Acting on the press
 * and swallowing the release keeps a release from reaching another control.
 */
private fun handleMediaKey(
    event: KeyEvent,
    player: DrumPlayer,
): Boolean {
    val action = mediaActionFor(event.key) ?: return false
    if (event.type == KeyEventType.KeyDown) {
        applyMediaAction(action, player)
    }
    return true
}

/** What each media key on the remote does to the drums. */
private enum class MediaAction {
    TOGGLE,
    PLAY,
    PAUSE,
    FASTER,
    SLOWER,
}

/** The action for a media key, or null for any other key. */
private fun mediaActionFor(key: Key): MediaAction? =
    when (key) {
        Key.MediaPlayPause -> MediaAction.TOGGLE
        Key.MediaPlay -> MediaAction.PLAY
        Key.MediaPause -> MediaAction.PAUSE
        Key.MediaFastForward -> MediaAction.FASTER
        Key.MediaRewind -> MediaAction.SLOWER
        else -> null
    }

/**
 * Play only starts and Pause only stops, so a remote with separate buttons never does the
 * opposite of its label; the combined Play/Pause button toggles.
 */
private fun applyMediaAction(
    action: MediaAction,
    player: DrumPlayer,
) {
    when (action) {
        MediaAction.TOGGLE -> {
            player.togglePlayback()
        }

        MediaAction.PLAY -> {
            if (!player.isPlaying) {
                player.togglePlayback()
            }
        }

        MediaAction.PAUSE -> {
            if (player.isPlaying) {
                player.togglePlayback()
            }
        }

        MediaAction.FASTER -> {
            player.tempo = player.tempo + MEDIA_TEMPO_STEP
        }

        MediaAction.SLOWER -> {
            player.tempo = player.tempo - MEDIA_TEMPO_STEP
        }
    }
}

/** OK on a remote, or Enter on a keyboard plugged into the television. */
private fun isSelectKey(key: Key): Boolean =
    key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

/** A press the remote sends again because the button is still held down. */
private fun isRepeatedPress(event: KeyEvent): Boolean {
    val isPress = event.type == KeyEventType.KeyDown
    val isRepeat = event.nativeKeyEvent.repeatCount > 0
    return isPress && isRepeat
}

/** Frames to keep asking for focus before giving up, about a sixth of a second. */
private const val FOCUS_ATTEMPT_FRAMES = 10

/**
 * Moves focus to [requester] once its control can take it, trying again each frame.
 *
 * Why not one `requestFocus()`: the effect that asks runs as soon as the screen is composed,
 * before the control has been placed, and the first request can find nothing to focus.
 * Waiting a frame between attempts lets it arrive. The screens use this for the control that
 * should hold focus when they open, so the remote's first press always has a target.
 */
internal suspend fun focusOnceAttached(requester: FocusRequester) {
    repeat(FOCUS_ATTEMPT_FRAMES) {
        if (requester.requestFocus()) {
            return
        }
        withFrameNanos { }
    }
}
