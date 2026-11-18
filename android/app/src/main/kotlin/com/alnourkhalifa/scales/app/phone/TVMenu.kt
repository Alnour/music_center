package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** What the TV sheet's footer tells an Android player: casting is the system's job here. */
private const val TV_FOOTER =
    "Use Cast screen from Quick Settings to show the full-screen board on a TV."

/** The TV button in the Scales title bar that opens [TVMenuSheet]. Tag `tvMenu`. */
@Composable
fun TVMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .testTag("tvMenu")
                .semantics { contentDescription = "TV" },
    ) {
        Icon(imageVector = TVIcons.television, contentDescription = null)
    }
}

/**
 * The TV controls behind the TV button: the dark TV theme and the full-screen board. Twin of
 * `TVMenu` in `Scales/Views/ContentView.swift`.
 *
 * What differs: the iPhone's menu also holds the AirPlay picker and the casting status.
 * Android has no AirPlay; a player casts the whole screen from Quick Settings, so the sheet
 * says so in its footer and offers the full-screen board, which is what such a cast should
 * show. The full-screen board opens only once the sheet has slid away ([onShowFullScreenBoard]
 * runs after the hide animation), as iOS waits for its popover to close: a full-screen window
 * opened over a closing sheet would be covered by it for a moment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TVMenuSheet(
    tvUsesDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onShowFullScreenBoard: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val showBoardAfterClosing = {
        scope
            .launch { sheetState.hide() }
            .invokeOnCompletion { onShowFullScreenBoard() }
        Unit
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("tvMenuSheet"),
    ) {
        FormColumn {
            FormCard {
                FormSwitchRow(
                    title = "Dark TV theme",
                    isOn = tvUsesDarkTheme,
                    onToggle = onDarkThemeChange,
                )
                HorizontalDivider()
                FullScreenBoardRow(onClick = showBoardAfterClosing)
            }
            Text(
                text = TV_FOOTER,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** "Full-screen board" in the accent colour with its symbol, as a Form button on iOS. */
@Composable
private fun FullScreenBoardRow(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    FormRow(
        modifier =
            Modifier
                .testTag("fullScreenBoard")
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(imageVector = TVIcons.expand, contentDescription = null, tint = accent)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "Full-screen board", color = accent)
    }
}

/**
 * SF Symbols' `tv` and `arrow.up.left.and.arrow.down.right`, drawn as 24 × 24 strokes so
 * `Icon` tints them; the Material icons library is several megabytes for two shapes.
 */
private object TVIcons {
    private val ink = SolidColor(Color.Black)

    /** A screen with a stand bar under it. */
    val television: ImageVector =
        ImageVector
            .Builder(
                name = "TVMenuTelevision",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).path(
                stroke = ink,
                strokeLineWidth = 1.8f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 4f)
                lineTo(19.5f, 4f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, 1.5f, 1.5f)
                lineTo(21f, 15.5f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, 1.5f)
                lineTo(4.5f, 17f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, -1.5f)
                lineTo(3f, 5.5f)
                arcToRelative(1.5f, 1.5f, 0f, false, true, 1.5f, -1.5f)
                close()
            }.path(
                stroke = ink,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(8f, 20f)
                lineTo(16f, 20f)
            }.build()

    /** Two arrows pointing out to opposite corners. */
    val expand: ImageVector =
        MenuIcons.strokedIcon("TVMenuExpand", strokeWidth = 2f) {
            moveTo(10f, 10f)
            lineTo(4f, 4f)
            moveTo(4f, 9.5f)
            lineTo(4f, 4f)
            lineTo(9.5f, 4f)
            moveTo(14f, 14f)
            lineTo(20f, 20f)
            moveTo(14.5f, 20f)
            lineTo(20f, 20f)
            lineTo(20f, 14.5f)
        }
}
