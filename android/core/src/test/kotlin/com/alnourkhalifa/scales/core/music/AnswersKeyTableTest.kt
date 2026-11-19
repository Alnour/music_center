package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.obj
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reads the `keyChordTable` section Swift wrote (every major and minor row with its chords and
 * harmonic-minor stand-ins) and carries the chord-table tests of `DiatonicChordTests` and
 * `LetterTriadTests` in `ScalesTests/MusicTheoryTests.swift` and `TransformationTests.swift`.
 */
class AnswersKeyTableTest {
    // feature: chord-tables
    @Test
    fun keyTablesMatchSwift() {
        val section = Answers.section("keyChordTable").jsonObject
        checkRows("major", section.array("major"), KeyChordTable.majorRows())
        checkRows("minor", section.array("minor"), KeyChordTable.minorRows())
    }

    /** Each row's identity, and its seven chords, in the order the generator
     *  writes them. */
    private fun checkRows(
        table: String,
        expectedRows: JsonArray,
        rows: List<KeyChordRow>,
    ) {
        assertEquals("$table row count", expectedRows.size, rows.size)
        for (rowIndex in rows.indices) {
            val expected = expectedRows[rowIndex].jsonObject
            val row = rows[rowIndex]
            val case = "$table row $rowIndex (${expected.string("keyName")})"
            assertSame("$case id", expected.string("id"), row.id)
            assertSame("$case keyName", expected.string("keyName"), row.keyName)

            val expectedChords = expected.array("chords")
            assertEquals("$case chord count", expectedChords.size, row.chords.size)
            for (chordIndex in row.chords.indices) {
                val chordCase = "$case chord $chordIndex"
                checkChord(chordCase, expectedChords[chordIndex].jsonObject, row.chords[chordIndex])
            }
        }
    }

    private fun checkChord(
        case: String,
        expected: JsonObject,
        chord: DiatonicChord,
    ) {
        assertSame("$case numeral", expected.string("numeral"), chord.numeral)
        assertSame("$case name", expected.string("name"), chord.triad.name)
        assertSame("$case spokenName", expected.string("spokenName"), chord.triad.spokenName)
    }

    // feature: chord-tables
    @Test
    fun cMajorChords() {
        val expected = listOf("C", "Dm", "Em", "F", "G", "Am", "B°")
        assertEquals("C major", expected, chordNames(NoteName(Letter.C), ScaleType.MAJOR))
    }

    // feature: chord-tables
    @Test
    fun majorNumerals() {
        val numerals = numerals(NoteName(Letter.G), ScaleType.MAJOR)
        assertEquals("G major", listOf("I", "ii", "iii", "IV", "V", "vi", "vii°"), numerals)
    }

    // feature: chord-tables
    @Test
    fun sharpAndFlatKeysAreSpelledFromTheirScale() {
        val fSharp = listOf("F♯", "G♯m", "A♯m", "B", "C♯", "D♯m", "E♯°")
        assertEquals("F♯ major", fSharp, chordNames(NoteName(Letter.F, 1), ScaleType.MAJOR))
        val dFlat = listOf("D♭", "E♭m", "Fm", "G♭", "A♭", "B♭m", "C°")
        assertEquals("D♭ major", dFlat, chordNames(NoteName(Letter.D, -1), ScaleType.MAJOR))
    }

    // feature: chord-tables
    @Test
    fun aMinorChordsAndNumerals() {
        val aMinorNames = chordNames(NoteName(Letter.A), ScaleType.NATURAL_MINOR)
        assertEquals("A minor names", listOf("Am", "B°", "C", "Dm", "Em", "F", "G"), aMinorNames)
        val aMinorNumerals = numerals(NoteName(Letter.A), ScaleType.NATURAL_MINOR)
        assertEquals(
            "A minor numerals",
            listOf("i", "ii°", "III", "iv", "v", "VI", "VII"),
            aMinorNumerals,
        )
    }

    // feature: chord-tables
    @Test
    fun minorRowsUseNaturalMinorChordsOnly() {
        val aMinor = KeyChordTable.minorRows()[0]
        assertEquals("keyName", "A minor", aMinor.keyName)
        val names = mutableListOf<String>()
        for (chord in aMinor.chords) {
            names.add(chord.triad.name)
        }
        assertEquals(listOf("Am", "B°", "C", "Dm", "Em", "F", "G"), names)
    }

    // feature: chord-tables
    @Test
    fun tablesCoverTwelveKeysWithMatchingRelatives() {
        val majors = KeyChordTable.majorRows()
        val minors = KeyChordTable.minorRows()
        assertEquals("major rows", 12, majors.size)
        assertEquals("minor rows", 12, minors.size)
        val majorSignatures = mutableSetOf<String>()
        for (major in majors) {
            majorSignatures.add(major.key.keySignature)
            assertEquals("${major.keyName} chords", 7, major.chords.size)
        }
        val minorSignatures = mutableSetOf<String>()
        for (minor in minors) {
            minorSignatures.add(minor.key.keySignature)
            assertEquals("${minor.keyName} chords", 7, minor.chords.size)
        }
        assertEquals("distinct major signatures", 12, majorSignatures.size)
        assertEquals("relative signatures", majorSignatures, minorSignatures)
    }

    // feature: chord-tables
    @Test
    fun tableRowsAreSortedByKeyName() {
        val majorNames = mutableListOf<String>()
        for (row in KeyChordTable.majorRows()) {
            majorNames.add(row.key.root.displayName)
        }
        val expectedMajors = listOf("A♭", "A", "B♭", "B", "C", "D♭", "D", "E♭", "E", "F", "F♯", "G")
        assertEquals("major order", expectedMajors, majorNames)
        val minorNames = mutableListOf<String>()
        for (row in KeyChordTable.minorRows()) {
            minorNames.add(row.key.root.displayName)
        }
        val expectedMinors = listOf("A", "B♭", "B", "C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯")
        assertEquals("minor order", expectedMinors, minorNames)
    }

    // feature: chord-tables
    @Test
    fun nonHeptatonicScalesHaveNoTriadTable() {
        val aMinorPentatonic = Scale(NoteName(Letter.A), ScaleType.MINOR_PENTATONIC)
        assertTrue("A minor pentatonic", aMinorPentatonic.diatonicTriads().isEmpty())
    }

    // feature: chord-tables
    @Test
    fun keyTablesStillOnlyCoverSevenNoteScales() {
        val aMinorPentatonic = Scale(NoteName(Letter.A), ScaleType.MINOR_PENTATONIC)
        assertTrue("no key table", aMinorPentatonic.diatonicTriads().isEmpty())
        assertEquals("letter triads", 4, aMinorPentatonic.letterTriads().size)
    }

    private fun chordNames(
        root: NoteName,
        type: ScaleType,
    ): List<String> {
        val names = mutableListOf<String>()
        for (chord in Scale(root, type).diatonicTriads()) {
            names.add(chord.triad.name)
        }
        return names
    }

    private fun numerals(
        root: NoteName,
        type: ScaleType,
    ): List<String> {
        val numerals = mutableListOf<String>()
        for (chord in Scale(root, type).diatonicTriads()) {
            numerals.add(chord.numeral)
        }
        return numerals
    }
}
