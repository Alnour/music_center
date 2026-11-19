package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleDefinition
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.Tuning
import com.alnourkhalifa.scales.core.music.definition

/**
 * Twin of `PracticeSettings` in `Scales/Session/PracticeSettings.swift`: everything the player
 * chooses, saved between launches so the app reopens on the scale they were last practising.
 *
 * Differs from Swift: an immutable data class, so Swift's `selectedInstrument` and
 * `selectedKeyboardOctaves` setters are [withSelectedInstrument] and
 * [withSelectedKeyboardOctaves]. Swift's `Codable` shape is written and read by
 * [ScaleLibraryJson.encodeSettings] and [ScaleLibraryJson.decodeSettings].
 */
data class PracticeSettings(
    val root: NoteName = NoteName(Letter.G),
    /** The built-in scale, and the one the board falls back to if a saved scale is deleted. */
    val scaleType: ScaleType = ScaleType.MAJOR,
    val tuning: Tuning = Tuning.STANDARD,
    val fretCount: Int = DEFAULT_FRET_COUNT,
    val labelStyle: NoteLabelStyle = NoteLabelStyle.NOTE_NAMES,
    val isLeftHanded: Boolean = false,
    /** Matches the reference chart and tab notation: the thin high E string at the top. */
    val thinStringOnTop: Boolean = true,
    /** A dark board is easier on the eyes on a big TV in a dim room. */
    val tvUsesDarkTheme: Boolean = true,
    /**
     * One of the player's own scales, shown instead of [scaleType] while it is set.
     *
     * A full copy is kept rather than an id: settings saved before custom scales existed still
     * load (the field is optional), and the TV screen can draw the board without the library.
     */
    val customScale: CustomScale? = null,
    /** Which board is drawn. Null for the guitar, so settings saved before the keyboard
     *  existed still load and a guitar player's saved JSON is unchanged. */
    val instrument: Instrument? = null,
    /** How many octaves the keyboard shows. Null for the default of two, the same way
     *  [instrument] is null for the guitar, so a default player's saved JSON never changes. */
    val keyboardOctaves: Int? = null,
) {
    /** The instrument as the pickers see it; the guitar is stored as null (see [instrument]). */
    val selectedInstrument: Instrument
        get() = instrument ?: Instrument.GUITAR

    /** The octave count as the pickers see it; the default of two is stored as null. */
    val selectedKeyboardOctaves: Int
        get() = keyboardOctaves ?: DEFAULT_KEYBOARD_OCTAVES

    /** The scale on the board, for pickers to select. */
    val scaleReference: ScaleReference
        get() {
            val custom = customScale ?: return ScaleReference.BuiltIn(scaleType)
            return ScaleReference.Custom(custom.id)
        }

    /** The name and formula of the scale on the board. */
    val scaleDefinition: ScaleDefinition
        get() = customScale?.definition ?: scaleType.definition

    /** Swift's `selectedInstrument` setter: choosing the guitar stores null, so the saved
     *  JSON goes back to exactly what it was before the keyboard was picked. */
    fun withSelectedInstrument(choice: Instrument): PracticeSettings {
        val stored = if (choice == Instrument.GUITAR) null else choice
        return copy(instrument = stored)
    }

    /** Swift's `selectedKeyboardOctaves` setter: the default count is stored as null. */
    fun withSelectedKeyboardOctaves(octaves: Int): PracticeSettings {
        val stored = if (octaves == DEFAULT_KEYBOARD_OCTAVES) null else octaves
        return copy(keyboardOctaves = stored)
    }

    /** Swift's static members. */
    companion object {
        const val DEFAULT_FRET_COUNT = 15

        /** Two octaves by default: a scale then shows its root twice and every chord shape
         *  fits without wrapping back to the start of the keyboard. */
        const val DEFAULT_KEYBOARD_OCTAVES = 2

        val fretCountChoices: List<Int> = listOf(12, 15, 17, 22, 24)
        val keyboardOctaveChoices: List<Int> = listOf(1, 2)
    }
}
