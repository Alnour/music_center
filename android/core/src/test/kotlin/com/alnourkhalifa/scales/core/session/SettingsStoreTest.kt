package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.music.BassTuning
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SettingsStore] over [InMemoryKeyValueStore]: settings come back as they were saved, each
 * value stands alone, and optional settings take no key until they are set. No Swift twin:
 * iOS saves one JSON value in `UserDefaults`.
 */
class SettingsStoreTest {
    private fun settingsAnswer(key: String): PracticeSettings {
        val section = Answers.section("settings").jsonObject
        return ScaleLibraryJson.decodeSettings(section.string(key))
    }

    // feature: settings-persist
    @Test
    fun bothAnswerSettingsComeBackAsTheyWereSaved() {
        for (key in listOf("defaultJSON", "everyOptionalSetJSON")) {
            val saved = settingsAnswer(key)
            val values = InMemoryKeyValueStore()

            SettingsStore(values).save(saved)
            val loaded = SettingsStore(values).load()

            assertEquals(key, saved, loaded)
            val reencoded = Json.parseToJsonElement(ScaleLibraryJson.encodeSettings(loaded))
            val expected =
                Json.parseToJsonElement(
                    Answers
                        .section("settings")
                        .jsonObject
                        .string(key),
                )
            assertEquals("$key as Swift's JSON", expected, reencoded)
        }
    }

    // feature: settings-persist
    @Test
    fun oneCorruptValueFallsBackAloneToItsDefault() {
        val saved = settingsAnswer("everyOptionalSetJSON")
        val values = InMemoryKeyValueStore()
        SettingsStore(values).save(saved)

        values.putString(SettingsStore.FRET_COUNT, "twenty")
        values.putString(SettingsStore.SCALE_TYPE, "noSuchScale")
        values.putString(SettingsStore.CUSTOM_SCALE, "{not json")
        val loaded = SettingsStore(values).load()

        val expected =
            saved.copy(
                fretCount = PracticeSettings.DEFAULT_FRET_COUNT,
                scaleType = ScaleType.MAJOR,
                customScale = null,
            )
        assertEquals(expected, loaded)
    }

    // feature: settings-persist
    @Test
    fun anUnknownLetterFallsBackWithoutTheAccidental() {
        val values = InMemoryKeyValueStore()
        values.putString(SettingsStore.ROOT_LETTER, "9")
        values.putString(SettingsStore.ROOT_ACCIDENTAL, "-1")

        val loaded = SettingsStore(values).load()

        assertEquals(NoteName(Letter.G, -1), loaded.root)
    }

    // feature: settings-persist
    @Test
    fun anEmptyStoreLoadsTheDefaults() {
        val loaded = SettingsStore(InMemoryKeyValueStore()).load()
        assertEquals(PracticeSettings(), loaded)
    }

    // feature: settings-persist
    @Test
    fun defaultSettingsStoreNoOptionalKey() {
        val values = InMemoryKeyValueStore()
        val everything = settingsAnswer("everyOptionalSetJSON")
        SettingsStore(values).save(everything)

        SettingsStore(values).save(PracticeSettings())

        val optionalKeys =
            setOf(
                SettingsStore.CUSTOM_SCALE,
                SettingsStore.INSTRUMENT,
                SettingsStore.KEYBOARD_OCTAVES,
                SettingsStore.CAPO_FRET,
                SettingsStore.BASS_TUNING,
            )
        val left = values.keys().intersect(optionalKeys)
        assertTrue("optional keys left behind: $left", left.isEmpty())
        assertEquals(PracticeSettings(), SettingsStore(values).load())
    }

    // feature: settings-persist, capo-shapes
    @Test
    fun aCapoFretIsSavedAndNoCapoRemovesTheKey() {
        val values = InMemoryKeyValueStore()
        val withCapo = PracticeSettings().withSelectedCapoFret(5)

        SettingsStore(values).save(withCapo)

        assertEquals("5", values.getString(SettingsStore.CAPO_FRET))
        assertEquals(5, SettingsStore(values).load().selectedCapoFret)

        val noCapo = withCapo.withSelectedCapoFret(0)
        SettingsStore(values).save(noCapo)

        assertEquals(null, values.getString(SettingsStore.CAPO_FRET))
        assertEquals(PracticeSettings(), SettingsStore(values).load())
    }

    // feature: settings-persist, bass
    @Test
    fun aBassTuningIsSavedAndStandardRemovesTheKey() {
        val values = InMemoryKeyValueStore()
        val bass = PracticeSettings().withSelectedInstrument(Instrument.BASS)
        val dropD = bass.withSelectedBassTuning(BassTuning.DROP_D)

        SettingsStore(values).save(dropD)

        assertEquals("bass", values.getString(SettingsStore.INSTRUMENT))
        assertEquals("dropD", values.getString(SettingsStore.BASS_TUNING))
        val loaded = SettingsStore(values).load()
        assertEquals(BassTuning.DROP_D, loaded.selectedBassTuning)
        assertEquals("Bass · Drop D", loaded.boardCaption)
        assertEquals(4, loaded.boardOpenStrings.size)

        val standard = dropD.withSelectedBassTuning(BassTuning.STANDARD)
        SettingsStore(values).save(standard.withSelectedInstrument(Instrument.GUITAR))

        assertEquals(null, values.getString(SettingsStore.BASS_TUNING))
        assertEquals(PracticeSettings(), SettingsStore(values).load())
    }

    // feature: settings-persist, capo-shapes
    @Test
    fun settingsSavedBeforeTheCapoLoadWithNoCapo() {
        val values = InMemoryKeyValueStore()
        values.putString(SettingsStore.TUNING, "dropD")
        values.putString(SettingsStore.FRET_COUNT, "12")

        val loaded = SettingsStore(values).load()

        assertEquals(null, loaded.capoFret)
        assertEquals(0, loaded.selectedCapoFret)
        assertEquals(0, loaded.boardCapoFret)
        assertEquals("Drop D", loaded.boardCaption)
    }
}
