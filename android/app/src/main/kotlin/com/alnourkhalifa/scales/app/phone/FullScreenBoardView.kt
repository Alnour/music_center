package com.alnourkhalifa.scales.app.phone

import android.content.pm.ActivityInfo
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.alnourkhalifa.scales.core.session.SessionState
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.TVDisplayView

/** Below this smallest width the device is a phone, which the app may turn to landscape. */
private const val TABLET_SMALLEST_WIDTH_DP = 600

/** The hint under the board on an upright tablet. */
const val TURN_TABLET_HINT: String = "Turn your tablet sideways so the board fills the TV"

/** The TV picture's shape, which the board keeps on an upright tablet. */
private const val TV_ASPECT_RATIO = 16f / 9f

/**
 * The TV layout filling the phone in landscape. Twin of `FullScreenBoardView` in
 * `Scales/Views/FullScreenBoardView.swift`.
 *
 * It is what a player shows on a TV that only copies the screen (Cast screen from Quick
 * Settings): mirrored, it fills the TV much like the TV app's own screen. The phone is turned
 * to landscape for it and turned back on close. A tablet is left as the player holds it:
 * while it is upright the board keeps the TV's 16:9 shape and asks to be turned sideways,
 * because stretched to an upright screen the strings would spread far apart and a TV
 * mirroring it would show a narrow strip.
 *
 * What differs: SwiftUI's `fullScreenCover` becomes a full-screen dialog window, so the tab
 * bar underneath stays put; the window hides the system bars (swipe to peek) and keeps the
 * screen on, since a cast stops when the phone locks. Back closes it, as the close button
 * does. The background runs under the camera cutout while the board keeps clear of it on
 * both sides, as the iPhone's landscape safe area keeps the board clear of the notch.
 */
@Composable
fun FullScreenBoardView(
    state: SessionState,
    onClose: () -> Unit,
) {
    LandscapeOnPhones()
    val properties =
        DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    Dialog(onDismissRequest = onClose, properties = properties) {
        ImmersiveDialogWindow()
        BackHandler(onBack = onClose)
        val palette = tvPalette(state)
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .padding(horizontal = symmetricCutoutMargin())
                    .testTag("fullScreenBoardView"),
        ) {
            val isWideWindow = maxWidth >= TABLET_SMALLEST_WIDTH_DP.dp
            val isUpright = maxHeight > maxWidth
            if (isWideWindow && isUpright) {
                UprightBoard(state, palette, onClose)
            } else {
                FullScreenTVBoard(state, palette, onClose, Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * The camera cutout's width, given to both sides: the iPhone's landscape safe area is the
 * same on both edges, so the board sits centred whichever way the phone was turned.
 */
@Composable
private fun symmetricCutoutMargin(): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val cutout = WindowInsets.displayCutout
    val left = cutout.getLeft(density, layoutDirection)
    val right = cutout.getRight(density, layoutDirection)
    val widest = maxOf(left, right)
    return with(density) { widest.toDp() }
}

/** The TV's own theme, which the player picks in the TV menu, whatever the phone's is. */
private fun tvPalette(state: SessionState): FretboardPalette {
    if (state.settings.tvUsesDarkTheme) {
        return FretboardPalette.dark
    }
    return FretboardPalette.light
}

/** The board at the TV's 16:9 shape, centred, with the hint to turn the tablet beneath. */
@Composable
private fun UprightBoard(
    state: SessionState,
    palette: FretboardPalette,
    onClose: () -> Unit,
) {
    val hintStyle =
        TextStyle(
            color = palette.secondaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val boardModifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(TV_ASPECT_RATIO)
        FullScreenTVBoard(state, palette, onClose, boardModifier)
        BasicText(
            text = TURN_TABLET_HINT,
            style = hintStyle,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/** The TV layout with every board setting the player saved, and its close button. */
@Composable
private fun FullScreenTVBoard(
    state: SessionState,
    palette: FretboardPalette,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    val settings = state.settings
    TVDisplayView(
        scale = state.scale,
        palette = palette,
        modifier = modifier,
        instrument = settings.selectedInstrument,
        tuning = settings.tuning,
        bassTuning = settings.selectedBassTuning,
        fretCount = settings.fretCount,
        keyboardOctaves = settings.selectedKeyboardOctaves,
        labelStyle = settings.labelStyle,
        thinStringOnTop = settings.thinStringOnTop,
        isLeftHanded = settings.isLeftHanded,
        capoFret = settings.boardCapoFret,
        ringedNotes = state.ringedNotes,
        onClose = onClose,
    )
}

/**
 * Turns a phone to landscape while the board is shown and gives back whatever orientation
 * the activity asked for before. Tablets are not turned: Swift's `OrientationLock` acts only
 * where the screen can be rotated by the app, and the upright layout covers the rest.
 */
@Composable
private fun LandscapeOnPhones() {
    val activity = LocalActivity.current ?: return
    val smallestWidth = LocalConfiguration.current.smallestScreenWidthDp
    val isPhone = smallestWidth < TABLET_SMALLEST_WIDTH_DP
    DisposableEffect(activity, isPhone) {
        val previous = activity.requestedOrientation
        if (isPhone) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            activity.requestedOrientation = previous
        }
    }
}

/**
 * Makes the dialog's window cover the whole screen, the camera cutout included, with the
 * status and navigation bars hidden until the player swipes from an edge, no dimming behind,
 * and the screen kept on.
 */
@Composable
private fun ImmersiveDialogWindow() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: return
    DisposableEffect(window, view) {
        coverWholeScreen(window, view)
        onDispose {}
    }
}

private fun coverWholeScreen(
    window: Window,
    view: View,
) {
    window.setLayout(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
    )
    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val attributes = window.attributes
    attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    window.attributes = attributes
    val controller = WindowCompat.getInsetsController(window, view)
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
}
