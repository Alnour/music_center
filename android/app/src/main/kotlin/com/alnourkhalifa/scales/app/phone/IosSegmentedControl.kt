package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The height of the control: iOS's segmented control, a little taller to tap. */
private val CONTROL_HEIGHT: Dp = 36.dp

/** The gap between the grey track and the raised segment inside it, as iOS draws it. */
private val THUMB_INSET: Dp = 2.dp

/** The raised segment's soft shadow in light mode; iOS lifts the thumb off the track. */
private val THUMB_SHADOW: Dp = 1.dp

/**
 * The space either side of a label, so neighbouring labels never touch. It is kept small
 * because the folded toolbar gives "Keyboard" only a fifth of a phone's width.
 */
private val LABEL_PADDING = PaddingValues(horizontal = 4.dp)

/** iOS 26 draws the track and the thumb as capsules, fully round at both ends. */
private val CapsuleShape = RoundedCornerShape(percent = 50)

/** The track, iOS's `tertiarySystemFill` laid on white and on black. */
private val TrackLight = Color(0xFFEEEEEF)
private val TrackDark = Color(0xFF1C1C1F)

/** The picked segment: white on the light track, the raised grey iOS draws on the dark one. */
private val ThumbLight = Color.White
private val ThumbDark = Color(0xFF636366)

/**
 * One choice out of a few, drawn as iOS draws `.pickerStyle(.segmented)`: a grey capsule
 * track with the picked segment as a raised white (dark mode: grey) thumb and no tick. Used
 * by the Scales toolbar (instrument, labels), the board settings (frets, octaves) and the
 * Chords tab (major or minor keys), so the same control looks the same on every screen.
 *
 * Material's `SegmentedButton` is not used because its outlined segments and tick read as a
 * different control beside the iPhone screenshots, and the tick widens the segment it lands
 * on, shifting the labels each time the choice changes. Every segment takes the width of the
 * widest label, as iOS sizes them, so a long label never squeezes its neighbours.
 *
 * Each segment is a radio button in a selectable group, so TalkBack reads "selected" on the
 * picked one and tests can `assertIsSelected` it. [modifier] sizes the whole control (pass
 * `fillMaxWidth()` to stretch it); [optionTag] gives each segment its own test tag when a
 * test taps segments by tag rather than by text.
 */
@Composable
fun <T> IosSegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    optionTag: ((T) -> String)? = null,
) {
    val isDark = isSystemInDarkTheme()
    val trackColor = if (isDark) TrackDark else TrackLight
    // The intrinsic width makes the row as wide as its widest segment times the count, the
    // way iOS sizes segments, unless the caller's modifier fixes the width first.
    Row(
        modifier =
            modifier
                .width(IntrinsicSize.Min)
                .height(CONTROL_HEIGHT)
                .clip(CapsuleShape)
                .background(trackColor)
                .padding(THUMB_INSET)
                .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (option in options) {
            var segmentModifier = Modifier.weight(1f)
            if (optionTag != null) {
                segmentModifier = segmentModifier.testTag(optionTag(option))
            }
            Segment(
                text = label(option),
                isSelected = option == selected,
                isDark = isDark,
                onClick = { onSelect(option) },
                modifier = segmentModifier,
            )
        }
    }
}

/**
 * One segment: the label centred on the track, or on the raised thumb when it is picked. The
 * light thumb carries a faint shadow; the dark one stands out by its lighter grey alone, as
 * on iOS, where a shadow on black would not show.
 */
@Composable
private fun Segment(
    text: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    var surface = modifier.fillMaxHeight()
    if (isSelected) {
        val thumbColor = if (isDark) ThumbDark else ThumbLight
        if (!isDark) {
            surface = surface.shadow(elevation = THUMB_SHADOW, shape = CapsuleShape)
        }
        surface = surface.background(color = thumbColor, shape = CapsuleShape)
    }
    Box(
        modifier =
            surface
                .clip(CapsuleShape)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .padding(LABEL_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
    }
}
