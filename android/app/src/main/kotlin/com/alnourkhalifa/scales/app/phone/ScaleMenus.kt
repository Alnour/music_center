package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleFamily
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.ui.MusicFontFamily

/** One scale the picker offers: what choosing it selects, and the name it is listed under. */
data class ScalePickerChoice(
    val reference: ScaleReference,
    val name: String,
)

/** A heading in the scale picker ("Major & Minor", "My Scales") and the scales under it. */
data class ScalePickerGroup(
    val title: String,
    val choices: List<ScalePickerChoice>,
)

/**
 * The scale picker's contents, as plain data so every screen with a scale menu (the Scales
 * screen, the Transform screen) lists the same groups in the same order. Twin of
 * `ScalePickerOptions` in `Scales/Views/ScalePickerOptions.swift`.
 *
 * The built-in scales come grouped as a practice book groups them; the player's own scales
 * follow under "My Scales", and that heading only appears once there is a saved scale, so a
 * new player never sees an empty group.
 */
fun scalePickerGroups(customScales: List<CustomScale>): List<ScalePickerGroup> {
    val groups = mutableListOf<ScalePickerGroup>()
    for (family in ScaleFamily.entries) {
        val choices = mutableListOf<ScalePickerChoice>()
        for (scaleType in family.scales) {
            val reference = ScaleReference.BuiltIn(scaleType)
            choices.add(ScalePickerChoice(reference, scaleType.displayName))
        }
        groups.add(ScalePickerGroup(family.title, choices))
    }
    if (customScales.isNotEmpty()) {
        val choices = mutableListOf<ScalePickerChoice>()
        for (customScale in customScales) {
            val reference = ScaleReference.Custom(customScale.id)
            choices.add(ScalePickerChoice(reference, customScale.definition.name))
        }
        groups.add(ScalePickerGroup(MY_SCALES_TITLE, choices))
    }
    return groups
}

/** The heading over the player's saved scales, as on iOS. */
const val MY_SCALES_TITLE: String = "My Scales"

/**
 * The key picker in the Scales toolbar: the root in the accent colour with an up-down
 * chevron, opening a menu of the 17 roots with a tick beside the current one. Twin of the
 * "Root" `Picker` (menu style) in `BoardToolbar`, `Scales/Views/ContentView.swift`.
 *
 * Items carry the tag `root-<name>` ("root-F♯") so tests pick one without matching the same
 * letter elsewhere on screen.
 */
@Composable
fun RootMenu(
    root: NoteName,
    onChoose: (NoteName) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        MenuPickerButton(
            text = root.displayName,
            onClick = { isOpen = true },
            modifier = Modifier.testTag("rootMenu"),
        )
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            for (note in NoteName.rootChoices) {
                CheckedMenuItem(
                    text = note.displayName,
                    isChecked = note == root,
                    onClick = {
                        isOpen = false
                        onChoose(note)
                    },
                    modifier = Modifier.testTag("root-${note.displayName}"),
                )
            }
        }
    }
}

/**
 * The scale picker in the Scales toolbar: the scale's name in the accent colour, opening a
 * menu of [scalePickerGroups] with a heading per group. Twin of the "Scale" `Picker` in
 * `BoardToolbar`, `Scales/Views/ContentView.swift`.
 *
 * The menu scrolls when it is taller than the screen; items carry the tag `scale-<name>`.
 */
@Composable
fun ScaleMenu(
    selection: ScaleReference,
    selectionName: String,
    customScales: List<CustomScale>,
    onChoose: (ScaleReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOpen by remember { mutableStateOf(false) }
    val groups = scalePickerGroups(customScales)
    Box(modifier = modifier) {
        MenuPickerButton(
            text = selectionName,
            onClick = { isOpen = true },
            modifier = Modifier.testTag("scaleMenu"),
        )
        DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
            for ((index, group) in groups.withIndex()) {
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                val pick = { reference: ScaleReference ->
                    isOpen = false
                    onChoose(reference)
                }
                ScaleMenuGroup(group, selection, pick)
            }
        }
    }
}

/** One group of the scale menu: its heading, then a ticked or plain item per scale. */
@Composable
private fun ScaleMenuGroup(
    group: ScalePickerGroup,
    selection: ScaleReference,
    onPick: (ScaleReference) -> Unit,
) {
    MenuGroupHeading(group.title)
    for (choice in group.choices) {
        CheckedMenuItem(
            text = choice.name,
            isChecked = choice.reference == selection,
            onClick = { onPick(choice.reference) },
            modifier = Modifier.testTag("scale-${choice.name}"),
        )
    }
}

/**
 * A menu picker's button: the current choice in the accent colour followed by SF Symbols'
 * `chevron.up.chevron.down`, the way iOS draws a menu-style `Picker` outside a Form.
 */
@Composable
internal fun MenuPickerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontFamily = MusicFontFamily,
            fontSize = 18.sp,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = MenuIcons.upDownChevron,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** A menu item with a tick in front of the current choice, as iOS marks a picker's value. */
@Composable
internal fun CheckedMenuItem(
    text: String,
    isChecked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        text = { Text(text = text, fontFamily = MusicFontFamily) },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = {
            // An empty box keeps every name in line with the ticked one.
            Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                if (isChecked) {
                    Icon(imageVector = MenuIcons.checkmark, contentDescription = "Selected")
                }
            }
        },
    )
}

/** A group's title inside a menu, small and grey like a `Section` header in a SwiftUI menu. */
@Composable
private fun MenuGroupHeading(title: String) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The SF Symbols the menus use, drawn as 24 × 24 strokes so `Icon` tints them. Drawn by hand
 * because the Material icons library is several megabytes for a handful of shapes.
 */
internal object MenuIcons {
    private val ink = SolidColor(Color.Black)

    /** `chevron.up.chevron.down`: a menu picker's marker. */
    val upDownChevron: ImageVector =
        strokedIcon("UpDownChevron", strokeWidth = 2.4f) {
            moveTo(7f, 9.5f)
            lineTo(12f, 4.5f)
            lineTo(17f, 9.5f)
            moveTo(7f, 14.5f)
            lineTo(12f, 19.5f)
            lineTo(17f, 14.5f)
        }

    /** `checkmark`: the current choice in a menu. */
    val checkmark: ImageVector =
        strokedIcon("Checkmark", strokeWidth = 2.4f) {
            moveTo(4.5f, 12.5f)
            lineTo(9.5f, 17.5f)
            lineTo(19.5f, 6.5f)
        }

    /** `chevron.right`: an item that opens a submenu. */
    val chevronRight: ImageVector =
        strokedIcon("ChevronRight", strokeWidth = 2.2f) {
            moveTo(9f, 5f)
            lineTo(16f, 12f)
            lineTo(9f, 19f)
        }

    /** `chevron.left`: the way back out of a submenu. */
    val chevronLeft: ImageVector =
        strokedIcon("ChevronLeft", strokeWidth = 2.2f) {
            moveTo(15f, 5f)
            lineTo(8f, 12f)
            lineTo(15f, 19f)
        }

    /** One open path drawn with rounded ends and joins, like SF Symbols' regular weight. */
    fun strokedIcon(
        name: String,
        strokeWidth: Float,
        pathBuilder: PathBuilder.() -> Unit,
    ): ImageVector {
        val builder =
            ImageVector.Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
        builder.path(
            stroke = ink,
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathBuilder,
        )
        return builder.build()
    }
}
