package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reads the `tunings` and `bassTunings` sections Swift wrote: every tuning's names, raw value
 * and open strings, thick to thin, both as spelled notes and as pitch classes.
 */
class AnswersTuningTest {
    // feature: tunings-frets
    @Test
    fun tuningsMatchSwift() {
        val answers = Answers.section("tunings").jsonArray
        assertEquals("tuning count", answers.size, Tuning.entries.size)
        for (index in answers.indices) {
            val expected = answers[index].jsonObject
            val tuning = Tuning.entries[index]
            val case = "tuning ${expected.string("rawValue")}"
            assertSame("$case rawValue", expected.string("rawValue"), tuning.rawValue)
            assertSame("$case displayName", expected.string("displayName"), tuning.displayName)
            assertSame("$case shortName", expected.string("shortName"), tuning.shortName)
            assertSame("$case decoded", tuning, Tuning.fromRawValue(tuning.rawValue))
            checkOpenStrings(case, expected, tuning.openStrings)
        }
    }

    // feature: bass
    @Test
    fun bassTuningsMatchSwift() {
        val answers = Answers.section("bassTunings").jsonArray
        assertEquals("bass tuning count", answers.size, BassTuning.entries.size)
        for (index in answers.indices) {
            val expected = answers[index].jsonObject
            val tuning = BassTuning.entries[index]
            val case = "bass tuning ${expected.string("rawValue")}"
            assertSame("$case rawValue", expected.string("rawValue"), tuning.rawValue)
            assertSame("$case displayName", expected.string("displayName"), tuning.displayName)
            assertSame("$case shortName", expected.string("shortName"), tuning.shortName)
            assertSame("$case decoded", tuning, BassTuning.fromRawValue(tuning.rawValue))
            checkOpenStrings(case, expected, tuning.openStrings)
        }
    }

    /** One tuning's open strings against Swift's, as spelled notes and as pitch classes. */
    private fun checkOpenStrings(
        case: String,
        expected: JsonObject,
        openStrings: List<NoteName>,
    ) {
        val expectedNames = mutableListOf<String>()
        for (element in expected.array("openStrings")) {
            expectedNames.add(element.jsonPrimitive.content)
        }
        val expectedPitchClasses = mutableListOf<Int>()
        for (element in expected.array("openPitchClasses")) {
            expectedPitchClasses.add(element.jsonPrimitive.int)
        }
        val actualNames = mutableListOf<String>()
        val actualPitchClasses = mutableListOf<Int>()
        for (note in openStrings) {
            actualNames.add(note.displayName)
            actualPitchClasses.add(note.pitchClass)
        }
        assertSame("$case openStrings", expectedNames, actualNames)
        assertSame("$case openPitchClasses", expectedPitchClasses, actualPitchClasses)
    }
}
