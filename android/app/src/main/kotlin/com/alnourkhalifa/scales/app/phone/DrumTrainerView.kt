package com.alnourkhalifa.scales.app.phone

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.app.drums.DrumPlayer
import com.alnourkhalifa.scales.app.drums.rememberDrumPlayer
import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.ui.DrumIcons
import com.alnourkhalifa.scales.ui.DrumStaffView
import kotlin.math.roundToInt

/** Windows at least this wide get the iPad's wider staff, as a regular size class does on iOS. */
private const val WIDE_WINDOW_DP = 600

/** The staff's width over its height in a wide window, where there is room to spread out. */
private const val WIDE_STAFF_ASPECT = 4.5f

/** A phone is narrow, so the staff gets a taller shape there to keep the notes legible. */
private const val NARROW_STAFF_ASPECT = 3f

/** One slider step per BPM from 40 to 220: 179 stops between the two ends. */
private const val TEMPO_STEPS = 179

/**
 * The Drums tab: a groove written on the drum staff with a ball that bounces from count to
 * count in time, so the player can read, count and play along. Twin of `DrumTrainerView` in
 * `Scales/Views/DrumTrainerView.swift`; the staff itself is [DrumStaffView].
 *
 * The player comes from [rememberDrumPlayer], which stops the drums and frees the audio
 * output when the screen leaves or the app goes to the background, as Swift's `onDisappear`
 * does.
 *
 * What differs from Swift:
 * - The ball's clock is a frame loop that runs only while playing, the counterpart of
 *   SwiftUI's `TimelineView(.animation(paused:))`. It uses `withInfiniteAnimationFrameNanos`
 *   so Compose UI tests still see the screen as idle while the drums play.
 * - The tempo slider's label follows the finger, but the tempo reaches the player only when
 *   the finger lifts. Every tempo change restarts the loop from beat one with a freshly
 *   rendered bar; applying each of the 179 stops along a drag would restart the sound that
 *   many times. iOS applies the tempo continuously.
 * - The groove picker is a Material read-only dropdown field labelled "Groove", standing in
 *   for SwiftUI's menu picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumTrainerView(modifier: Modifier = Modifier) {
    val player = rememberDrumPlayer()
    Scaffold(
        modifier = modifier,
        topBar = {
            DrumsTitleBar()
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DrumStaff(player = player)
            DrumControls(player = player)
        }
    }
}

/**
 * The screen's title, standing in for the iPhone's large navigation title: a medium Material
 * bar whose title is set large and bold, so the title's size and the staff's place below it
 * match the iPhone (a large Material bar sits about 40dp taller than iOS's). The screen never
 * scrolls far enough to collapse it, so no scroll behaviour is attached.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrumsTitleBar() {
    MediumTopAppBar(
        title = {
            Text(
                text = "Drums",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}

/**
 * The staff with its moving ball. The current time lives in a state that only the frame loop
 * writes and only the staff's drawing reads, so a playing groove redraws the picture each
 * frame without recomposing the controls below it.
 */
@Composable
private fun DrumStaff(player: DrumPlayer) {
    val isPlaying = player.isPlaying
    var nowNanos by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            withInfiniteAnimationFrameNanos {
                nowNanos = System.nanoTime()
            }
        }
    }

    val aspect = staffAspect()
    val corners = RoundedCornerShape(16.dp)
    DrumStaffView(
        groove = player.groove,
        isPlaying = isPlaying,
        ball = { player.ballPosition(nowNanos) },
        isDark = isSystemInDarkTheme(),
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(corners),
    )
}

/**
 * The staff's shape for this window: wide windows (a tablet, an unfolded phone) get the
 * long, low staff; a phone gets a taller one. The window's width decides, as the horizontal
 * size class does on iOS, so a tablet app sharing the screen falls back to the phone shape.
 */
@Composable
private fun staffAspect(): Float {
    val windowWidthPixels = LocalWindowInfo.current.containerSize.width
    val windowWidth = with(LocalDensity.current) { windowWidthPixels.toDp() }
    if (windowWidth >= WIDE_WINDOW_DP.dp) {
        return WIDE_STAFF_ASPECT
    }
    return NARROW_STAFF_ASPECT
}

/** Play or pause, the groove, the tempo and the sound switch, top to bottom. */
@Composable
private fun DrumControls(player: DrumPlayer) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PlayPauseButton(player = player)
        GroovePicker(player = player)
        TempoSlider(player = player)
        SoundSwitch(player = player)
    }
}

@Composable
private fun PlayPauseButton(player: DrumPlayer) {
    val isPlaying = player.isPlaying
    val title = if (isPlaying) "Pause" else "Play"
    val icon = if (isPlaying) DrumIcons.Pause else DrumIcons.Play
    Button(
        onClick = { player.togglePlayback() },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The built-in grooves in a read-only dropdown; picking one while playing restarts it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroovePicker(player: DrumPlayer) {
    var isExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { expanded -> isExpanded = expanded },
    ) {
        OutlinedTextField(
            value = player.groove.name,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = "Groove") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            for (groove in DrumGroove.builtIn) {
                DropdownMenuItem(
                    text = { Text(text = groove.name) },
                    onClick = {
                        player.groove = groove
                        isExpanded = false
                    },
                )
            }
        }
    }
}

/**
 * The tempo label and slider. While the finger is down, [draggedTempo] holds the slider's
 * position so the label and thumb follow it; lifting the finger hands that tempo to the
 * player once (see [DrumTrainerView] for why) and the slider goes back to reading the player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TempoSlider(player: DrumPlayer) {
    var draggedTempo by remember { mutableFloatStateOf(Float.NaN) }
    val isDragging = !draggedTempo.isNaN()
    val shownTempo = if (isDragging) draggedTempo else player.tempo.toFloat()
    val shownBpm = shownTempo.roundToInt()
    val range = DrumPlayer.tempoRange
    val sliderRange = range.start.toFloat()..range.endInclusive.toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Tempo: $shownBpm BPM",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("tempoLabel"),
        )
        Slider(
            value = shownTempo,
            onValueChange = { tempo -> draggedTempo = tempo },
            onValueChangeFinished = {
                if (!draggedTempo.isNaN()) {
                    player.tempo = draggedTempo.roundToInt().toDouble()
                }
                draggedTempo = Float.NaN
            },
            valueRange = sliderRange,
            steps = TEMPO_STEPS,
            track = { sliderState ->
                // 179 tick marks would crowd the track into a dotted line; iOS shows none.
                SliderDefaults.Track(sliderState = sliderState, drawTick = { _, _ -> })
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Sound on or off; with it off the ball still bounces, for counting in silence. */
@Composable
private fun SoundSwitch(player: DrumPlayer) {
    val isSoundOn = player.isSoundOn
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isSoundOn,
                    role = Role.Switch,
                    onValueChange = { isOn -> player.isSoundOn = isOn },
                ),
    ) {
        Text(
            text = "Drum sounds",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = isSoundOn, onCheckedChange = null)
    }
}
