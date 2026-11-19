package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reads the `roots`, `scaleTypes`, `families` and `spellings` sections Swift wrote and checks
 * the Kotlin theory spells every root and scale the same way. The chord fields of
 * `spellings` (`keySignature`, `letterTriads`, `diatonicTriadCount`) belong to the Triad port
 * and are read by its own test.
 */
class AnswersSpellingTest {
    // feature: roots-scales
    @Test
    fun rootChoicesMatchSwift() {
        val answers = Answers.section("roots").jsonArray
        assertEquals("root count", answers.size, NoteName.rootChoices.size)
        for (index in answers.indices) {
            val expected = answers[index].jsonObject
            val root = NoteName.rootChoices[index]
            val case = "root $index (${expected.string("displayName")})"
            assertSame("$case letter", expected.int("letter"), root.letter.rawValue)
            assertSame("$case accidental", expected.int("accidental"), root.accidental)
            assertSame("$case displayName", expected.string("displayName"), root.displayName)
            assertSame("$case pitchClass", expected.int("pitchClass"), root.pitchClass)
        }
    }

    // feature: roots-scales
    @Test
    fun scaleTypesMatchSwift() {
        val answers = Answers.section("scaleTypes").jsonArray
        assertEquals("scale type count", answers.size, ScaleType.entries.size)
        for (index in answers.indices) {
            val expected = answers[index].jsonObject
            val type = ScaleType.entries[index]
            val case = "scale type ${expected.string("rawValue")}"
            assertSame("$case rawValue", expected.string("rawValue"), type.rawValue)
            assertSame("$case displayName", expected.string("displayName"), type.displayName)
            assertSame("$case formula", expected.string("formula"), type.formula)
            assertSame("$case degreeLabels", strings(expected.array("degreeLabels")), labels(type))
            assertSame(
                "$case semitonesAboveRoot",
                ints(expected.array("semitonesAboveRoot")),
                semitones(type),
            )
            assertSame("$case decoded", type, ScaleType.fromRawValue(type.rawValue))
        }
    }

    // feature: roots-scales
    @Test
    fun familiesMatchSwift() {
        val answers = Answers.section("families").jsonArray
        assertEquals("family count", answers.size, ScaleFamily.entries.size)
        for (index in answers.indices) {
            val expected = answers[index].jsonObject
            val family = ScaleFamily.entries[index]
            val case = "family ${expected.string("rawValue")}"
            assertSame("$case rawValue", expected.string("rawValue"), family.rawValue)
            assertSame("$case title", expected.string("title"), family.title)
            val memberRawValues = mutableListOf<String>()
            for (type in family.scales) {
                memberRawValues.add(type.rawValue)
            }
            assertSame("$case members", strings(expected.array("members")), memberRawValues)
        }
    }

    // feature: roots-scales
    @Test
    fun everyRootAndScaleIsSpelledAsSwiftSpellsIt() {
        val answers = Answers.section("spellings").jsonArray
        val expectedCount = NoteName.rootChoices.size * ScaleType.entries.size
        assertEquals("spelling count", expectedCount, answers.size)
        var index = 0
        for (root in NoteName.rootChoices) {
            for (type in ScaleType.entries) {
                val expected = answers[index].jsonObject
                checkSpelling(expected, Scale(root, type))
                index += 1
            }
        }
    }

    /** One root and scale: the case's identity first, so a misordered file fails loudly,
     *  then the title and every tone as `"F♯|7"` with its pitch class. */
    private fun checkSpelling(
        expected: JsonObject,
        scale: Scale,
    ) {
        val case = "${expected.string("root")} ${expected.string("scaleType")}"
        assertSame("$case root", expected.string("root"), scale.root.displayName)
        assertSame("$case title", expected.string("title"), scale.title)

        val expectedTones = mutableListOf<String>()
        val expectedPitchClasses = mutableListOf<Int>()
        for (element in expected.array("tones")) {
            val tone = element.jsonObject
            expectedTones.add(tone.string("name") + "|" + tone.string("degree"))
            expectedPitchClasses.add(tone.int("pitchClass"))
        }
        val actualTones = mutableListOf<String>()
        val actualPitchClasses = mutableListOf<Int>()
        for (tone in scale.tones) {
            actualTones.add(tone.name.displayName + "|" + tone.degree.label)
            actualPitchClasses.add(tone.name.pitchClass)
        }
        assertSame("$case tones", expectedTones, actualTones)
        assertSame("$case pitch classes", expectedPitchClasses, actualPitchClasses)

        for (tone in scale.tones) {
            val found = scale.tone(tone.name.pitchClass)
            assertSame("$case lookup ${tone.name.displayName}", tone, found)
        }
    }

    private fun labels(type: ScaleType): List<String> {
        val labels = mutableListOf<String>()
        for (degree in type.degrees) {
            labels.add(degree.label)
        }
        return labels
    }

    private fun semitones(type: ScaleType): List<Int> {
        val semitones = mutableListOf<Int>()
        for (degree in type.degrees) {
            semitones.add(degree.semitonesAboveRoot)
        }
        return semitones
    }

    private fun strings(array: JsonArray): List<String> {
        val values = mutableListOf<String>()
        for (element in array) {
            values.add(element.jsonPrimitive.content)
        }
        return values
    }

    private fun ints(array: JsonArray): List<Int> {
        val values = mutableListOf<Int>()
        for (element in array) {
            values.add(element.jsonPrimitive.int)
        }
        return values
    }
}
