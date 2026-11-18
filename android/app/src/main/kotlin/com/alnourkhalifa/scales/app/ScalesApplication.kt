package com.alnourkhalifa.scales.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.alnourkhalifa.scales.core.session.PracticeSession
import com.alnourkhalifa.scales.core.session.ScaleLibrary
import com.alnourkhalifa.scales.core.session.SessionState
import com.alnourkhalifa.scales.core.session.SettingsStore

/**
 * The process-wide home of the practice session, so the phone and TV screens read one board.
 * Twin of what `ScalesApp.swift` hands to its views through the environment: the session,
 * the saved scales and lessons, and the saved settings behind them.
 *
 * Screens read [sessionState] and [libraryVersion], never the session's own fields, because
 * Compose only redraws when a `State` it read is assigned. `PracticeSession` replaces its
 * immutable `SessionState` snapshot on every change and hands it to `onChange`, which assigns
 * it here; a mutable object kept in `mutableStateOf` would change underneath Compose and
 * never trigger a redraw. Actions go through [session] (`choose`, `transform`,
 * `updateSettings`…) on the main thread.
 */
class ScalesApplication : Application() {
    /** The saved settings, one `SharedPreferences` key per setting. */
    lateinit var settingsStore: SettingsStore
        private set

    /** Saved scales and lessons, `files/Scales/ScaleLibrary.json`. */
    lateinit var library: ScaleLibrary
        private set

    /** The board's single source of truth; every change arrives in [sessionState]. */
    lateinit var session: PracticeSession
        private set

    private val mutableSessionState: MutableState<SessionState> = mutableStateOf(SessionState())
    private val mutableLibraryVersion: MutableIntState = mutableIntStateOf(0)

    /** The latest session snapshot; reading it in a composable redraws it on every change. */
    val sessionState: State<SessionState>
        get() = mutableSessionState

    /**
     * Goes up by one whenever the library file changes. `ScaleLibrary` is a plain object, so
     * screens that list saved scales or lessons read this number to be redrawn, then read
     * the lists from [library].
     */
    val libraryVersion: State<Int>
        get() = mutableLibraryVersion

    override fun onCreate() {
        super.onCreate()
        reloadFromStorage()
    }

    /**
     * Builds the store, library and session from what is on disk, as a new process does.
     * Tests call it after wiping the saved settings and library, so each one starts from a
     * fresh install, or after saving, to prove the settings come back from disk. The `State`
     * objects stay the same, so screens already showing keep reading them.
     */
    fun reloadFromStorage() {
        val keyValueStore = SharedPreferencesKeyValueStore(this)
        val libraryFile = ScaleLibrary.standardFile(filesDir)
        settingsStore = SettingsStore(keyValueStore)
        library = ScaleLibrary(libraryFile)
        session = PracticeSession(settingsStore, library)
        session.onChange = { newState ->
            mutableSessionState.value = newState
        }
        // What iOS RootView's `onChange(of: library.scales)` does: a renamed saved scale on
        // the board takes its new name, a deleted one gives way to the built-in scale.
        library.onChange = {
            session.refreshCustomScale()
            mutableLibraryVersion.intValue += 1
        }
        mutableSessionState.value = session.state
        mutableLibraryVersion.intValue += 1
    }

    /** Finds the one app object from wherever a screen or test stands. */
    companion object {
        /** The app object from any context (activity, view, test). */
        fun from(context: Context): ScalesApplication =
            context.applicationContext as ScalesApplication
    }
}

/**
 * The app object for the composable's context, so a screen can write
 * `rememberScalesApplication().sessionState.value` and act through `.session`.
 */
@Composable
fun rememberScalesApplication(): ScalesApplication {
    val context = LocalContext.current
    return remember(context) { ScalesApplication.from(context) }
}
