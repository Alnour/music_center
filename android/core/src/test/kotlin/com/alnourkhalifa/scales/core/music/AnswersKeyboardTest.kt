package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.bool
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.optionalString
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reads the `keyboards` section Swift wrote (one and two octaves × roots G and F♯ × every
 * scale type) and carries `ScalesTests/KeyboardLayoutTests.swift`.
 *
 * `defaultOctaves` is Swift's `PracticeSettings.defaultKeyboardOctaves`, which belongs to the
 * settings port; here it only pins that the default board draws the 25 keys Swift draws.
 */
class AnswersKeyboardTest {
    private val gMajor = KeyboardLayout(Scale(NoteName(Letter.G), ScaleType.MAJOR))

    // feature: keyboard
    @Test
    fun blackPitchClassesMatchSwift() {
        val section = Answers.section("keyboards").jsonObject
        val expected = mutableListOf<Int>()
        for (element in section.array("blackPitchClasses")) {
            expected.add(element.jsonPrimitive.int)
        }
        assertSame("blackPitchClasses", expected, KeyboardLayout.blackPitchClasses.sorted())

        val defaultOctaves = section.int("defaultOctaves")
        val defaultBoard =
            KeyboardLayout(Scale(NoteName(Letter.G), ScaleType.MAJOR), defaultOctaves)
        val expectedKeyCount = defaultOctaves * KEYS_PER_OCTAVE + 1
        assertEquals("default board key count", expectedKeyCount, defaultBoard.keys.size)
    }

    // feature: keyboard
    @Test
    fun everyOctaveRootAndScaleIsLaidOutAsSwiftLaysItOut() {
        val layouts =
            Answers
                .section("keyboards")
                .jsonObject
                .array("layouts")
        val roots = listOf(NoteName(Letter.G), NoteName(Letter.F, 1))
        val octaveCounts = listOf(1, 2)
        val expectedCount = octaveCounts.size * roots.size * ScaleType.entries.size
        assertEquals("layout count", expectedCount, layouts.size)
        var index = 0
        for (octaves in octaveCounts) {
            for (root in roots) {
                for (type in ScaleType.entries) {
                    val expected = layouts[index].jsonObject
                    checkLayout(expected, KeyboardLayout(Scale(root, type), octaves))
                    index += 1
                }
            }
        }
    }

    /** One layout: its identity first, so a misordered file fails loudly, then every key's
     *  pitch class, colour and tone (`"F♯|7"`, absent off the scale). */
    private fun checkLayout(
        expected: JsonObject,
        layout: KeyboardLayout,
    ) {
        val case =
            "${expected.string("root")} ${expected.string("scaleType")} " +
                "over ${expected.int("octaves")} octaves"
        val expectedKeys = expected.array("keys")
        assertEquals("$case key count", expectedKeys.size, layout.keys.size)
        for (keyIndex in layout.keys.indices) {
            val expectedKey = expectedKeys[keyIndex].jsonObject
            val key = layout.keys[keyIndex]
            val keyCase = "$case key $keyIndex"
            assertSame("$keyCase pitchClass", expectedKey.int("pitchClass"), key.pitchClass)
            assertSame("$keyCase isBlack", expectedKey.bool("isBlack"), key.isBlack)
            assertSame("$keyCase tone", expectedKey.optionalString("tone"), toneLabel(key.tone))
        }
    }

    private fun toneLabel(tone: ScaleTone?): String? {
        if (tone == null) {
            return null
        }
        return tone.name.displayName + "|" + tone.degree.label
    }

    // feature: keyboard
    @Test
    fun octaveRunsFromCToTheNextC() {
        val pitchClasses = mutableListOf<Int>()
        for (key in gMajor.keys) {
            pitchClasses.add(key.pitchClass)
        }
        val expected = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0)
        assertEquals("pitch classes", expected, pitchClasses)
        assertEquals("white keys", 8, gMajor.whiteKeys.size)
        assertEquals("black keys", 5, gMajor.blackKeys.size)
    }

    // feature: keyboard
    @Test
    fun blackKeysAreTheFiveSharpsAndFlats() {
        val blackPitchClasses = mutableListOf<Int>()
        for (key in gMajor.blackKeys) {
            blackPitchClasses.add(key.pitchClass)
        }
        assertEquals("black pitch classes", listOf(1, 3, 6, 8, 10), blackPitchClasses)
    }

    // feature: keyboard
    @Test
    fun gMajorMarksItsSevenNotesWithGAsRoot() {
        val names = mutableListOf<String>()
        val rootNames = mutableListOf<String>()
        for (key in gMajor.keys) {
            val tone = key.tone ?: continue
            names.add(tone.name.displayName)
            if (tone.isRoot) {
                rootNames.add(tone.name.displayName)
            }
        }
        assertEquals("names", listOf("C", "D", "E", "F♯", "G", "A", "B", "C"), names)
        assertEquals("roots", listOf("G"), rootNames)
    }

    // feature: keyboard
    @Test
    fun gMajorUsesOneBlackKeyAndSkipsF() {
        val blackKeysInScale = mutableListOf<String>()
        for (key in gMajor.blackKeys) {
            val tone = key.tone ?: continue
            blackKeysInScale.add(tone.name.displayName)
        }
        assertEquals("black keys in scale", listOf("F♯"), blackKeysInScale)
        val fKey = gMajor.keys[5]
        assertEquals("F pitch class", 5, fKey.pitchClass)
        assertNull("F is outside G major", fKey.tone)
    }

    // feature: keyboard
    @Test
    fun bothCKeysCarryTheSameToneInCMajor() {
        val cMajor = KeyboardLayout(Scale(NoteName(Letter.C), ScaleType.MAJOR))
        val firstKey = cMajor.keys[0]
        val closingKey = cMajor.keys[12]
        assertEquals("first C is the root", true, firstKey.tone?.isRoot)
        assertEquals("closing C", firstKey.tone, closingKey.tone)
    }

    // feature: keyboard
    @Test
    fun twoOctavesRepeatTheKeysAndCloseOnC() {
        val layout = KeyboardLayout(Scale(NoteName(Letter.G), ScaleType.MAJOR), octaveCount = 2)
        assertEquals("keys", 25, layout.keys.size)
        assertEquals("white keys", 15, layout.whiteKeys.size)
        assertEquals("black keys", 10, layout.blackKeys.size)
        assertEquals("key 12", 0, layout.keys[12].pitchClass)
        assertEquals("key 24", 0, layout.keys[24].pitchClass)
        var rootCount = 0
        for (key in layout.keys) {
            if (key.tone?.isRoot == true) {
                rootCount += 1
            }
        }
        assertEquals("G appears once per octave", 2, rootCount)
    }

    // feature: keyboard
    @Test
    fun aPentatonicMarksItsNotesOnBothCKeys() {
        val layout = KeyboardLayout(Scale(NoteName(Letter.A), ScaleType.MINOR_PENTATONIC))
        var markedCount = 0
        for (key in layout.keys) {
            if (key.tone != null) {
                markedCount += 1
            }
        }
        assertEquals("A C D E G, with C marked at both ends of the octave", 6, markedCount)
    }

    private companion object {
        /** Twelve keys per octave; the closing C is added once at the end. */
        const val KEYS_PER_OCTAVE = 12
    }
}
