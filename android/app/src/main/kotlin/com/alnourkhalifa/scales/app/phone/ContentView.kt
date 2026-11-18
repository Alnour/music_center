package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.app.ScalesApplication
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.session.ScaleLibraryError
import com.alnourkhalifa.scales.core.session.SessionState
import com.alnourkhalifa.scales.ui.BoardChangeRow
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.MusicFontFamily
import com.alnourkhalifa.scales.ui.ScaleNotesRow

/**
 * The width from which a window counts as wide (iOS's regular horizontal size class): a
 * tablet, or a large phone held sideways. Wide windows get roomier spacing, notes and cards.
 */
private val WIDE_WINDOW_WIDTH = 600.dp

/**
 * The phone or tablet's Scales screen: the scale's name in the title bar with the TV and
 * gear buttons, one toolbar of choices, the notes row, the board filling what is left, and
 * the chord cards under it. Twin of `ContentView` in `Scales/Views/ContentView.swift`.
 *
 * Everything a player changes while practising sits in the toolbar above the board; the
 * one-time setup (frets, tuning, octaves) and the TV controls sit behind the two title-bar
 * buttons, so the board keeps most of the screen. The screen stays on while it is showing,
 * because a mirrored cast stops when the phone locks.
 *
 * What differs: iOS also casts the board through AirPlay and watches for screen mirroring
 * to open the full-screen board on its own. Android has neither hook (a cast from Quick
 * Settings copies the screen without telling the app), so the TV menu offers the
 * full-screen board for the player to open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentView(modifier: Modifier = Modifier) {
    val app = rememberScalesApplication()
    val state = app.sessionState.value
    val sheets = remember { PresentedSheets() }
    var libraryError: ScaleLibraryError? by remember { mutableStateOf(null) }
    KeepScreenOn()
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            ScalesTitleBar(
                title = state.scale.title,
                onTVMenu = { sheets.isShowingTVMenu = true },
                onBoardSettings = { sheets.isShowingBoardSettings = true },
            )
        },
    ) { innerPadding ->
        ScalesScreenBody(
            app = app,
            state = state,
            onLibraryError = { error -> libraryError = error },
            modifier = Modifier.padding(innerPadding),
        )
    }
    ScalesScreenSheets(app, state, sheets)
    LibraryErrorAlert(error = libraryError, onDismiss = { libraryError = null })
}

/**
 * Which of the screen's sheets is up. Kept for the life of the screen only: the activity
 * handles rotation itself, so a turn of the phone keeps them, and a relaunch starts on the
 * plain board as the iPhone does.
 */
@Stable
private class PresentedSheets {
    var isShowingBoardSettings by mutableStateOf(false)
    var isShowingTVMenu by mutableStateOf(false)
    var isShowingFullScreenBoard by mutableStateOf(false)
}

/**
 * The gear's settings, the TV menu and the full-screen board, whichever is up. The TV menu
 * hands over to the full-screen board only once it has closed itself.
 */
@Composable
private fun ScalesScreenSheets(
    app: ScalesApplication,
    state: SessionState,
    sheets: PresentedSheets,
) {
    if (sheets.isShowingBoardSettings) {
        BoardSettingsSheet(
            settings = state.settings,
            onChange = { change -> app.session.updateSettings(change) },
            onDismiss = { sheets.isShowingBoardSettings = false },
        )
    }
    if (sheets.isShowingTVMenu) {
        TVMenuSheet(
            tvUsesDarkTheme = state.settings.tvUsesDarkTheme,
            onDarkThemeChange = { isDark ->
                app.session.updateSettings { it.copy(tvUsesDarkTheme = isDark) }
            },
            onShowFullScreenBoard = {
                sheets.isShowingTVMenu = false
                sheets.isShowingFullScreenBoard = true
            },
            onDismiss = { sheets.isShowingTVMenu = false },
        )
    }
    if (sheets.isShowingFullScreenBoard) {
        val close = { sheets.isShowingFullScreenBoard = false }
        FullScreenBoardView(state = state, onClose = close)
    }
}

/** The scale's name centred in the title bar, with the TV and gear buttons at its end. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScalesTitleBar(
    title: String,
    onTVMenu: () -> Unit,
    onBoardSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = scheme.surface,
            scrolledContainerColor = scheme.surface,
        )
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontFamily = MusicFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("scaleTitle"),
            )
        },
        actions = {
            TVMenuButton(onClick = onTVMenu)
            BoardSettingsButton(onClick = onBoardSettings)
        },
        colors = colors,
    )
}

/**
 * Everything under the title bar. The toolbar, notes row and chords take their natural
 * height and the board fills the rest; the chords are capped at a third of this area so the
 * board never gets squeezed out, which is why the area is measured here.
 */
@Composable
private fun ScalesScreenBody(
    app: ScalesApplication,
    state: SessionState,
    onLibraryError: (ScaleLibraryError) -> Unit,
    modifier: Modifier,
) {
    val session = app.session
    val palette = FretboardPalette.matching(isSystemInDarkTheme())
    // Read so a saved scale's rename or deletion redraws the scale menu.
    val libraryVersion = app.libraryVersion.value
    val customScales = remember(libraryVersion) { app.library.scales }
    val transformBoard = { edit: ScaleEdit -> transformOrReport(app, edit, onLibraryError) }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= WIDE_WINDOW_WIDTH
        val screenHeight = maxHeight
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = sideMargin(isWide))
                    .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (isWide) 12.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoardToolbar(
                state = state,
                customScales = customScales,
                session = session,
                onEdit = transformBoard,
            )
            ScaleNotesRow(
                scale = state.scale,
                noteSize = if (isWide) 18.dp else 14.dp,
                palette = palette,
                highlightedNotes = state.ringedNotes,
            )
            PhoneBoard(
                state = state,
                palette = palette,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
            ChangeRowIfChanged(state, palette, onRevert = { session.revertBoardChange() })
            BoardChordGrid(
                state = state,
                screenHeight = screenHeight,
                isWide = isWide,
                onTap = { chord -> session.toggleRing(chord) },
            )
        }
    }
}

/**
 * Changes one note of the scale on the board; the chords under it show what the change did.
 * Saving the change as a lesson can fail, and the player is told why.
 */
private fun transformOrReport(
    app: ScalesApplication,
    edit: ScaleEdit,
    onLibraryError: (ScaleLibraryError) -> Unit,
) {
    try {
        app.session.transform(edit)
    } catch (error: ScaleLibraryError) {
        onLibraryError(error)
    }
}

private fun sideMargin(isWide: Boolean): Dp {
    if (isWide) {
        return 20.dp
    }
    return 12.dp
}

/** The last note change and the way back, while the board still shows its result. */
@Composable
private fun ChangeRowIfChanged(
    state: SessionState,
    palette: FretboardPalette,
    onRevert: () -> Unit,
) {
    val change = state.currentBoardChange ?: return
    val transformation = change.transformation
    BoardChangeRow(
        sentence = transformation.editSentence,
        beforeTitle = transformation.beforeScale.title,
        onRevert = onRevert,
        colors = phoneChordChipColors(palette),
    )
}

/**
 * Keeps the screen awake while the Scales screen is showing, as iOS's
 * `isIdleTimerDisabled`: a player practising with both hands on the guitar never touches the
 * phone, and a cast to the TV ends when the phone locks.
 */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }
}

/**
 * A failed save of a note change, as an alert. Twin of `LibraryErrorAlert` in
 * `Scales/Views/LessonViews.swift`, for the one error the Scales screen can meet.
 */
@Composable
private fun LibraryErrorAlert(
    error: ScaleLibraryError?,
    onDismiss: () -> Unit,
) {
    if (error == null) {
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = { Text("Couldn't Update Your Scales") },
        text = { Text(libraryErrorMessage(error)) },
    )
}

/** Swift's `ScaleLibraryError.message`, the words the player reads. */
private fun libraryErrorMessage(error: ScaleLibraryError): String =
    when (error) {
        ScaleLibraryError.EmptyName -> {
            "Give the new scale a name first."
        }

        ScaleLibraryError.ScaleNotFound -> {
            "That scale is no longer in your library."
        }

        ScaleLibraryError.LessonNotFound -> {
            "That lesson was already deleted."
        }

        is ScaleLibraryError.CouldNotSave -> {
            "Your scales couldn't be saved on this device. ${error.reason}"
        }
    }
