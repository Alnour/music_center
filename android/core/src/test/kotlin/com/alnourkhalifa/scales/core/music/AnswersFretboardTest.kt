package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reads the `fretboards` section Swift wrote (G at 24 frets, every tuning × every scale type)
 * and carries the `FretboardLayoutTests` of `ScalesTests/MusicTheoryTests.swift`.
 */
class AnswersFretboardTest {
    private val gMajor =
        FretboardLayout(Scale(NoteName(Letter.G), ScaleType.MAJOR), Tuning.STANDARD, 15)

    // feature: tunings-frets
    @Test
    fun everyTuningAndScaleIsLaidOutAsSwiftLaysItOut() {
        val answers = Answers.section("fretboards").jsonArray
        val expectedCount = Tuning.entries.size * ScaleType.entries.size
        assertEquals("fretboard count", expectedCount, answers.size)
        var index = 0
        for (tuning in Tuning.entries) {
            for (type in ScaleType.entries) {
                val expected = answers[index].jsonObject
                checkBoard(expected, tuning, type)
                index += 1
            }
        }
    }

    /** One board: its identity first, so a misordered file fails loudly, then every string's
     *  open note and the tone at every fret as `"F♯|7"` or null. */
    private fun checkBoard(
        expected: JsonObject,
        tuning: Tuning,
        type: ScaleType,
    ) {
        val case =
            "${expected.string("root")} ${expected.string("scaleType")} " +
                "in ${expected.string("tuning")}"
        assertSame("$case tuning", expected.string("tuning"), tuning.rawValue)
        assertSame("$case scaleType", expected.string("scaleType"), type.rawValue)
        val root = NoteName(Letter.G)
        assertSame("$case root", expected.string("root"), root.displayName)

        val fretCount = expected.int("fretCount")
        val layout = FretboardLayout(Scale(root, type), tuning, fretCount)
        assertSame("$case fretCount", fretCount, layout.fretCount)

        val expectedStrings = expected.array("strings")
        assertEquals("$case string count", expectedStrings.size, layout.strings.size)
        for (stringIndex in layout.strings.indices) {
            val expectedString = expectedStrings[stringIndex].jsonObject
            val guitarString = layout.strings[stringIndex]
            val stringCase = "$case string $stringIndex"
            assertSame(
                "$stringCase openNote",
                expectedString.string("openNote"),
                guitarString.openNote.displayName,
            )
            assertSame(
                "$stringCase tonesByFret",
                expectedTones(expectedString),
                toneLabels(guitarString.tonesByFret),
            )
        }
    }

    private fun expectedTones(expectedString: JsonObject): List<String?> {
        val labels = mutableListOf<String?>()
        for (element in expectedString.array("tonesByFret")) {
            if (element is JsonNull) {
                labels.add(null)
            } else {
                labels.add(element.jsonPrimitive.content)
            }
        }
        return labels
    }

    private fun toneLabels(tones: List<ScaleTone?>): List<String?> {
        val labels = mutableListOf<String?>()
        for (tone in tones) {
            if (tone == null) {
                labels.add(null)
            } else {
                labels.add(tone.name.displayName + "|" + tone.degree.label)
            }
        }
        return labels
    }

    // feature: board
    @Test
    fun gMajorMatchesReferenceChart() {
        // Strings are listed thickest first: 0 is low E, 5 is high E.
        assertEquals("string 5", listOf(0, 2, 3, 5, 7, 8, 10, 12, 14, 15), gMajor.frets(5))
        assertEquals("string 4", listOf(0, 1, 3, 5, 7, 8, 10, 12, 13, 15), gMajor.frets(4))
        assertEquals("string 3", listOf(0, 2, 4, 5, 7, 9, 11, 12, 14), gMajor.frets(3))
        assertEquals("string 2", listOf(0, 2, 4, 5, 7, 9, 10, 12, 14), gMajor.frets(2))
        assertEquals("string 1", listOf(0, 2, 3, 5, 7, 9, 10, 12, 14, 15), gMajor.frets(1))
        assertEquals("string 0", listOf(0, 2, 3, 5, 7, 8, 10, 12, 14, 15), gMajor.frets(0))
    }

    // feature: board
    @Test
    fun gMajorRootsMatchReferenceChart() {
        val rootFretsByString = mutableListOf<List<Int>>()
        for (guitarString in gMajor.strings) {
            val rootFrets = mutableListOf<Int>()
            for (fret in guitarString.tonesByFret.indices) {
                if (guitarString.tonesByFret[fret]?.isRoot == true) {
                    rootFrets.add(fret)
                }
            }
            rootFretsByString.add(rootFrets)
        }
        val expected =
            listOf(listOf(3, 15), listOf(10), listOf(5), listOf(0, 12), listOf(8), listOf(3, 15))
        assertEquals("root frets", expected, rootFretsByString)
    }

    // feature: tunings-frets
    @Test
    fun dropDLowStringIsTheFifthOfG() {
        val layout = FretboardLayout(Scale(NoteName(Letter.G), ScaleType.MAJOR), Tuning.DROP_D, 12)
        val openTone = layout.strings[0].tonesByFret[0]
        assertEquals("open low D", "5", openTone?.degree?.label)
    }

    // feature: tunings-frets
    @Test
    fun layoutCoversOpenStringPlusEveryFret() {
        for (stringIndex in gMajor.strings.indices) {
            val tonesByFret = gMajor.strings[stringIndex].tonesByFret
            assertEquals("string $stringIndex", 16, tonesByFret.size)
        }
    }
}
