package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.BassTuning
import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.Tuning

/**
 * Reads and writes [PracticeSettings] through a [KeyValueStore], one key per setting.
 *
 * Swift twin: `PracticeSession.save()` and `loadSettings(from:)` in
 * `Scales/Session/PracticeSession.swift`. Differs from Swift: iOS keeps the whole settings as
 * one JSON value, so one outdated field resets everything; here each setting has its own key,
 * so a missing or unreadable value falls back to its own default and the rest still load.
 * The saved scale is the one value stored as JSON, in Swift's shape. Optional settings
 * (`customScale`, `instrument`, `keyboardOctaves`, `capoFret`, `bassTuning`) are removed when
 * null, the way Swift's JSON leaves them out, so a default player stores none of them.
 *
 * Adding a setting means one read line in [load] and one write line in [save].
 */
class SettingsStore(
    private val store: KeyValueStore,
) {
    fun load(): PracticeSettings {
        val defaults = PracticeSettings()
        val root =
            NoteName(
                letter = read(ROOT_LETTER, ::parseLetter) ?: defaults.root.letter,
                accidental = read(ROOT_ACCIDENTAL, String::toIntOrNull) ?: defaults.root.accidental,
            )
        return PracticeSettings(
            root = root,
            scaleType = read(SCALE_TYPE, ScaleType::fromRawValue) ?: defaults.scaleType,
            tuning = read(TUNING, Tuning::fromRawValue) ?: defaults.tuning,
            fretCount = read(FRET_COUNT, String::toIntOrNull) ?: defaults.fretCount,
            labelStyle = read(LABEL_STYLE, NoteLabelStyle::fromRawValue) ?: defaults.labelStyle,
            isLeftHanded =
                read(IS_LEFT_HANDED, String::toBooleanStrictOrNull) ?: defaults.isLeftHanded,
            thinStringOnTop =
                read(THIN_STRING_ON_TOP, String::toBooleanStrictOrNull) ?: defaults.thinStringOnTop,
            tvUsesDarkTheme =
                read(TV_USES_DARK_THEME, String::toBooleanStrictOrNull) ?: defaults.tvUsesDarkTheme,
            customScale = read(CUSTOM_SCALE, ::parseCustomScale),
            instrument = read(INSTRUMENT, Instrument::fromRawValue),
            keyboardOctaves = read(KEYBOARD_OCTAVES, String::toIntOrNull),
            capoFret = read(CAPO_FRET, String::toIntOrNull),
            bassTuning = read(BASS_TUNING, BassTuning::fromRawValue),
        )
    }

    fun save(settings: PracticeSettings) {
        write(ROOT_LETTER, settings.root.letter.rawValue)
        write(ROOT_ACCIDENTAL, settings.root.accidental)
        write(SCALE_TYPE, settings.scaleType.rawValue)
        write(TUNING, settings.tuning.rawValue)
        write(FRET_COUNT, settings.fretCount)
        write(LABEL_STYLE, settings.labelStyle.rawValue)
        write(IS_LEFT_HANDED, settings.isLeftHanded)
        write(THIN_STRING_ON_TOP, settings.thinStringOnTop)
        write(TV_USES_DARK_THEME, settings.tvUsesDarkTheme)
        write(CUSTOM_SCALE, settings.customScale?.let(ScaleLibraryJson::encodeCustomScale))
        write(INSTRUMENT, settings.instrument?.rawValue)
        write(KEYBOARD_OCTAVES, settings.keyboardOctaves)
        write(CAPO_FRET, settings.capoFret)
        write(BASS_TUNING, settings.bassTuning?.rawValue)
    }

    /** A number as its decimal text, or no key for null. */
    private fun write(
        key: String,
        value: Int?,
    ) {
        write(key, value?.toString())
    }

    /** A switch as "true" or "false". */
    private fun write(
        key: String,
        value: Boolean,
    ) {
        write(key, value.toString())
    }

    /** The stored value parsed, or null when it is missing or does not parse. */
    private fun <T> read(
        key: String,
        parse: (String) -> T?,
    ): T? {
        val text = store.getString(key) ?: return null
        return parse(text)
    }

    /** Stores the value, or removes the key for a null one. */
    private fun write(
        key: String,
        value: String?,
    ) {
        if (value == null) {
            store.remove(key)
        } else {
            store.putString(key, value)
        }
    }

    /** The keys, as the plan and `SharedPreferences` know them. */
    companion object {
        const val ROOT_LETTER = "root.letter"
        const val ROOT_ACCIDENTAL = "root.accidental"
        const val SCALE_TYPE = "scaleType"
        const val TUNING = "tuning"
        const val FRET_COUNT = "fretCount"
        const val LABEL_STYLE = "labelStyle"
        const val IS_LEFT_HANDED = "isLeftHanded"
        const val THIN_STRING_ON_TOP = "thinStringOnTop"
        const val TV_USES_DARK_THEME = "tvUsesDarkTheme"
        const val CUSTOM_SCALE = "customScale"
        const val INSTRUMENT = "instrument"
        const val KEYBOARD_OCTAVES = "keyboardOctaves"
        const val CAPO_FRET = "capoFret"
        const val BASS_TUNING = "bassTuning"

        private fun parseLetter(text: String): Letter? {
            val rawValue = text.toIntOrNull() ?: return null
            return Letter.fromRawValue(rawValue)
        }

        /** A saved scale that no longer reads is dropped, so the board shows the built-in
         *  scale chosen before it, as when the scale is deleted. */
        private fun parseCustomScale(text: String): CustomScale? =
            try {
                ScaleLibraryJson.decodeCustomScale(text)
            } catch (ignored: IllegalArgumentException) {
                null
            }
    }
}
