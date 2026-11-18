package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleChord
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleEditError
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation

/**
 * Twin of `BoardChange` in `Scales/Session/PracticeSession.swift`: one note change made to the
 * scale on the board, with where it started and ended.
 */
data class BoardChange(
    val transformation: ScaleTransformation,
    /** The scale on the board before the change, to go back to. */
    val before: ScaleReference,
    /** The scale the change produced, which the board shows now. */
    val after: ScaleReference,
)

/**
 * Twin of `PracticeSession` in `Scales/Session/PracticeSession.swift`: the single source of
 * truth shared by the phone and TV screens, which both read [state].
 *
 * Differs from Swift:
 * - the stored values live in an immutable [SessionState]; every action replaces it once and
 *   then calls [onChange] with the new snapshot, so the app can put it in a Compose
 *   `mutableStateOf` and every screen recomposes. Swift's `transform` sets the settings and
 *   then the board change, two observable changes; here they arrive together.
 * - the library is passed once to the constructor rather than to each action.
 * - settings are saved through [SettingsStore] whenever they differ from the last snapshot,
 *   Swift's `didSet { save() }`.
 * - Swift's TV status, mirroring flags and AirPlay caster are iOS-only and not here.
 */
class PracticeSession(
    private val store: SettingsStore,
    private val library: ScaleLibrary,
) {
    var state: SessionState = SessionState(settings = store.load())
        private set

    /** Called with the new snapshot after every action that changed something. */
    var onChange: ((SessionState) -> Unit)? = null

    /**
     * Puts a scale on the board, in a new key when one is given. A built-in scale clears any
     * saved scale; a saved scale is copied in from the library, and an id the library doesn't
     * know changes nothing.
     */
    fun choose(
        reference: ScaleReference,
        root: NoteName? = null,
    ) {
        val settings = chosenSettings(reference, root) ?: return
        publish(state.copy(settings = settings))
    }

    /**
     * Changes one note of the scale on the board and puts the result on the board.
     *
     * The change is kept as a lesson first, so the Transform screen can explain it later and a
     * failed save leaves the board as it was. A result the app knows takes that scale's name;
     * a new one is saved to My Scales under its working name ("Major ♭7"), which the player
     * can rename there. An edit the scale does not allow (the menus only offer legal ones)
     * changes nothing.
     *
     * @throws ScaleLibraryError.CouldNotSave when the library file can't be written.
     */
    fun transform(edit: ScaleEdit) {
        val settings = state.settings
        val transformation =
            try {
                ScaleTransformation(settings.root, settings.scaleDefinition, edit)
            } catch (ignored: ScaleEditError) {
                return
            }
        val lesson = library.saveLesson(transformation, transformation.after.name)
        val before = settings.scaleReference
        val chosen = chosenSettings(lesson.result, root = null) ?: settings
        val change = BoardChange(lesson.transformation, before, lesson.result)
        publish(state.copy(settings = chosen, boardChange = change))
    }

    /** Puts back the scale the last board change started from. The lesson it made stays. */
    fun revertBoardChange() {
        val change = state.currentBoardChange ?: return
        val restored = chosenSettings(change.before, root = null) ?: state.settings
        publish(state.copy(settings = restored, boardChange = null))
    }

    /** Rings a chord's notes on the board, or clears the rings when the same chord is picked
     *  again. */
    fun toggleRing(chord: ScaleChord) {
        val picked = if (state.ringedChord == chord) null else chord
        publish(state.copy(ringedChord = picked))
    }

    /**
     * Keeps the board's copy of a saved scale in step with the library: a renamed scale takes
     * its new name, and a deleted one gives way to the built-in scale chosen before. The app
     * calls it from the library's `onChange`.
     */
    fun refreshCustomScale() {
        val current = state.settings.customScale ?: return
        val stored = library.scale(current.id)
        if (stored == current) {
            return
        }
        val refreshed = state.settings.copy(customScale = stored)
        publish(state.copy(settings = refreshed))
    }

    /** Any other settings change (frets, tuning, labels, instrument, a new key), Swift's
     *  `session.settings.x = …`. */
    fun updateSettings(transform: (PracticeSettings) -> PracticeSettings) {
        val updated = transform(state.settings)
        publish(state.copy(settings = updated))
    }

    /** The settings with a scale chosen, or null for a saved scale the library doesn't
     *  hold. Settings change once, so the TV redraws once. */
    private fun chosenSettings(
        reference: ScaleReference,
        root: NoteName?,
    ): PracticeSettings? {
        val current = state.settings
        val key = root ?: current.root
        return when (reference) {
            is ScaleReference.BuiltIn -> {
                current.copy(root = key, scaleType = reference.type, customScale = null)
            }

            is ScaleReference.Custom -> {
                val customScale = library.scale(reference.id) ?: return null
                current.copy(root = key, customScale = customScale)
            }
        }
    }

    /** Replaces the snapshot, saves settings that changed and tells the listener. */
    private fun publish(newState: SessionState) {
        val previous = state
        if (newState == previous) {
            return
        }
        state = newState
        if (newState.settings != previous.settings) {
            store.save(newState.settings)
        }
        onChange?.invoke(newState)
    }
}
