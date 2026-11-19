package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.Tuning
import com.alnourkhalifa.scales.core.session.PracticeSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The gear in the Scales title bar that opens [BoardSettingsSheet]. Tag `boardSettings`. */
@Composable
fun BoardSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .testTag("boardSettings")
                .semantics { contentDescription = "Board settings" },
    ) {
        Icon(imageVector = GearIcon.image, contentDescription = null)
    }
}

/**
 * One-time setup behind the gear button: frets, tuning and string order for the guitar, the
 * octave count for the keyboard; each instrument shows only its own rows. Twin of
 * `BoardSettingsMenu` in `Scales/Views/ContentView.swift`.
 *
 * What differs: the iPhone shows a popover; a bottom sheet is Android's way to show a few
 * settings over the screen without leaving it. The rows sit on cards over the grouped grey,
 * the way an iOS `Form` draws them. Frets are a segmented row because five short choices
 * read at a glance there; iOS uses a menu.
 *
 * @param onChange receives a change to apply to the settings, as `session.updateSettings`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardSettingsSheet(
    settings: PracticeSettings,
    onChange: ((PracticeSettings) -> PracticeSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("boardSettingsSheet"),
    ) {
        FormColumn {
            when (settings.selectedInstrument) {
                Instrument.GUITAR -> GuitarOptions(settings, onChange)
                Instrument.KEYBOARD -> KeyboardOptions(settings, onChange)
            }
        }
    }
}

@Composable
private fun GuitarOptions(
    settings: PracticeSettings,
    onChange: ((PracticeSettings) -> PracticeSettings) -> Unit,
) {
    FormCard {
        FormLabeledRow(title = "Frets") {
            IosSegmentedControl(
                options = PracticeSettings.fretCountChoices,
                selected = settings.fretCount,
                label = { count -> "$count frets" },
                onSelect = { count -> onChange { it.copy(fretCount = count) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider()
        TuningRow(settings.tuning) { tuning -> onChange { it.copy(tuning = tuning) } }
        HorizontalDivider()
        FormSwitchRow(
            title = "Thin string on top",
            isOn = settings.thinStringOnTop,
            onToggle = { isOn -> onChange { it.copy(thinStringOnTop = isOn) } },
        )
        HorizontalDivider()
        FormSwitchRow(
            title = "Left-handed",
            isOn = settings.isLeftHanded,
            onToggle = { isOn -> onChange { it.copy(isLeftHanded = isOn) } },
        )
    }
}

@Composable
private fun KeyboardOptions(
    settings: PracticeSettings,
    onChange: ((PracticeSettings) -> PracticeSettings) -> Unit,
) {
    FormCard {
        FormLabeledRow(title = "Octaves") {
            IosSegmentedControl(
                options = PracticeSettings.keyboardOctaveChoices,
                selected = settings.selectedKeyboardOctaves,
                label = { count -> octavesTitle(count) },
                onSelect = { count -> onChange { it.withSelectedKeyboardOctaves(count) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun octavesTitle(count: Int): String {
    if (count == 1) {
        return "1 octave"
    }
    return "$count octaves"
}

/** "Tuning" with the current tuning as a menu picker at the row's end, as in an iOS Form. */
@Composable
private fun TuningRow(
    tuning: Tuning,
    onChoose: (Tuning) -> Unit,
) {
    var isOpen by remember { mutableStateOf(false) }
    FormRow {
        Text(text = "Tuning", modifier = Modifier.padding(end = 12.dp))
        Spacer(modifier = Modifier.weight(1f))
        Box {
            MenuPickerButton(
                text = tuning.displayName,
                onClick = { isOpen = true },
                modifier = Modifier.testTag("tuningMenu"),
            )
            DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
                for (choice in Tuning.entries) {
                    CheckedMenuItem(
                        text = choice.displayName,
                        isChecked = choice == tuning,
                        onClick = {
                            isOpen = false
                            onChoose(choice)
                        },
                    )
                }
            }
        }
    }
}

/** A sheet's contents: Form cards stacked with iOS's spacing, scrolling when too tall. */
@Composable
internal fun FormColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** A Form section: rows on a rounded card over the sheet's grey, as iOS's inset grouping. */
@Composable
internal fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

/** One row of a Form card, at least as tall as iOS's 44-point rows. */
@Composable
internal fun FormRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A row with its title above a control that wants the card's full width. */
@Composable
private fun FormLabeledRow(
    title: String,
    control: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title)
        control()
    }
}

/**
 * A switch row; the whole row toggles, and TalkBack reads the title with the switch's state.
 */
@Composable
internal fun FormSwitchRow(
    title: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    FormRow {
        Text(text = title, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isOn,
            onCheckedChange = onToggle,
            colors = iosSwitchColors(),
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

/** UIKit's `systemGreen`, the colour of a switch that is on. */
private val SystemGreen = Color(0xFF34C759)

/** A switch that is green when on, as every iOS toggle is, with a white thumb. */
@Composable
private fun iosSwitchColors(): SwitchColors =
    SwitchDefaults.colors(
        checkedTrackColor = SystemGreen,
        checkedThumbColor = Color.White,
        checkedBorderColor = SystemGreen,
    )

/**
 * SF Symbols' `gearshape`: a toothed wheel with a hole in the middle. The outline is worked
 * out from polar points (eight teeth); the rings inside are cut with the even-odd rule.
 */
private object GearIcon {
    private const val TEETH = 8
    private const val OUTER_RADIUS = 10.5f
    private const val INNER_RADIUS = 8f

    /**
     * The rings inside the teeth, from the outside in. With the even-odd rule they alternate
     * cut and filled, leaving a toothed band and a small ring at the hub, as the outline
     * symbol draws them.
     */
    private val ringRadii = listOf(6.6f, 3.9f, 2.2f)
    private const val CENTER = 12f

    /** Half a tooth's angular width at its tip and at its root, in degrees. */
    private const val TIP_HALF_WIDTH = 9.0
    private const val ROOT_HALF_WIDTH = 15.0

    val image: ImageVector =
        ImageVector
            .Builder(
                name = "BoardSettingsGear",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                teeth()
                for (radius in ringRadii) {
                    circle(radius)
                }
            }.build()

    private fun PathBuilder.teeth() {
        val toothAngle = 360.0 / TEETH
        for (tooth in 0 until TEETH) {
            val middle = tooth * toothAngle
            val points =
                listOf(
                    INNER_RADIUS to middle - ROOT_HALF_WIDTH,
                    OUTER_RADIUS to middle - TIP_HALF_WIDTH,
                    OUTER_RADIUS to middle + TIP_HALF_WIDTH,
                    INNER_RADIUS to middle + ROOT_HALF_WIDTH,
                )
            for ((index, point) in points.withIndex()) {
                val x = CENTER + point.first * cos(point.second * PI / 180).toFloat()
                val y = CENTER + point.first * sin(point.second * PI / 180).toFloat()
                if (tooth == 0 && index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }
        close()
    }

    /** A full circle as two half-circle arcs, the way SVG paths draw one. */
    private fun PathBuilder.circle(radius: Float) {
        val diameter = radius * 2
        moveTo(CENTER - radius, CENTER)
        arcToRelative(radius, radius, 0f, true, true, diameter, 0f)
        arcToRelative(radius, radius, 0f, true, true, -diameter, 0f)
        close()
    }
}
