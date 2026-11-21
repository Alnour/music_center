package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.music.BassTuning
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reads the `settings` section Swift wrote: `PracticeSettings` as Swift's JSON with and
 * without every optional, the first release's saved settings, the picker choices and the
 * label styles. JSON is compared as parsed trees, so only key order and spacing may differ.
 */
class AnswersSettingsTest {
    private val section: JsonObject by lazy { Answers.section("settings").jsonObject }

    private fun ints(array: JsonArray): List<Int> {
        val values = mutableListOf<Int>()
        for (element in array) {
            values.add(element.jsonPrimitive.int)
        }
        return values
    }

    private fun strings(array: JsonArray): List<String> {
        val values = mutableListOf<String>()
        for (element in array) {
            values.add(element.jsonPrimitive.content)
        }
        return values
    }

    // feature: settings-persist
    @Test
    fun defaultSettingsAreWrittenAsSwiftWritesThem() {
        val written = ScaleLibraryJson.encodeSettings(PracticeSettings())
        val expected = Json.parseToJsonElement(section.string("defaultJSON"))
        assertEquals("defaultJSON", expected, Json.parseToJsonElement(written))
        val decoded = ScaleLibraryJson.decodeSettings(section.string("defaultJSON"))
        assertEquals("defaultJSON decoded", PracticeSettings(), decoded)
    }

    // feature: settings-persist
    @Test
    fun everyOptionalSetRoundTrips() {
        val text = section.string("everyOptionalSetJSON")
        val decoded = ScaleLibraryJson.decodeSettings(text)
        val written = ScaleLibraryJson.encodeSettings(decoded)
        assertEquals(
            "everyOptionalSetJSON",
            Json.parseToJsonElement(text),
            Json.parseToJsonElement(written),
        )
        assertEquals("every optional is set", Instrument.KEYBOARD, decoded.instrument)
        assertEquals("keyboardOctaves", 1, decoded.keyboardOctaves)
        assertEquals("capoFret", 3, decoded.capoFret)
        assertEquals("bassTuning", BassTuning.DROP_D, decoded.bassTuning)
        assertEquals(
            "customScale",
            "Augmented Major",
            decoded.customScale
                ?.definition
                ?.name,
        )
    }

    // feature: settings-persist
    @Test
    fun theKeysAreSwiftsKeys() {
        val decoded = ScaleLibraryJson.decodeSettings(section.string("everyOptionalSetJSON"))
        val written = Json.parseToJsonElement(ScaleLibraryJson.encodeSettings(decoded)).jsonObject
        assertEquals("allKeys", strings(section.array("allKeys")), written.keys.sorted())
    }

    // feature: settings-persist
    @Test
    fun theFirstReleaseSettingsStillLoad() {
        val decoded = ScaleLibraryJson.decodeSettings(section.string("firstReleaseJSON"))
        val written = ScaleLibraryJson.encodeSettings(decoded)
        val expected = Json.parseToJsonElement(section.string("firstReleaseDecodedJSON"))
        assertEquals("firstReleaseDecodedJSON", expected, Json.parseToJsonElement(written))
    }

    // feature: settings-persist
    @Test
    fun theChoicesMatch() {
        assertEquals(
            "fretCountChoices",
            ints(section.array("fretCountChoices")),
            PracticeSettings.fretCountChoices,
        )
        assertEquals(
            "keyboardOctaveChoices",
            ints(section.array("keyboardOctaveChoices")),
            PracticeSettings.keyboardOctaveChoices,
        )
        assertEquals(
            "defaultKeyboardOctaves",
            section.int("defaultKeyboardOctaves"),
            PracticeSettings.DEFAULT_KEYBOARD_OCTAVES,
        )
        assertEquals("defaultKeyboardOctaves read", 2, PracticeSettings().selectedKeyboardOctaves)
    }

    // feature: settings-persist, keyboard, bass
    @Test
    fun theInstrumentsMatch() {
        val rawValues = mutableListOf<String>()
        for (instrument in Instrument.entries) {
            rawValues.add(instrument.rawValue)
        }
        assertEquals("instruments", strings(section.array("instruments")), rawValues)
    }

    // feature: settings-persist, labels
    @Test
    fun theLabelStylesMatch() {
        val expected = section.array("labelStyles")
        assertEquals("labelStyles count", expected.size, NoteLabelStyle.entries.size)
        for (index in expected.indices) {
            val answer = expected[index].jsonObject
            val style = NoteLabelStyle.entries[index]
            val case = "labelStyles[$index]"
            assertEquals("$case rawValue", answer.string("rawValue"), style.rawValue)
            assertEquals("$case title", answer.string("title"), style.title)
            assertEquals(
                "$case accessibilityDescription",
                answer.string("accessibilityDescription"),
                style.accessibilityDescription,
            )
            assertEquals("$case fromRawValue", style, NoteLabelStyle.fromRawValue(style.rawValue))
        }
    }
}
