package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reads the chord fields of the `spellings` section Swift wrote (`keySignature`,
 * `diatonicTriadCount`, `letterTriads`) for every root and scale type, and carries the
 * `LetterTriadTests` of `ScalesTests/TransformationTests.swift` plus the key-signature check
 * of `DiatonicChordTests` in `ScalesTests/MusicTheoryTests.swift`.
 */
class AnswersTriadTest {
    // feature: roots-scales, chord-chips-rings
    @Test
    fun everyRootAndScaleStacksTheChordsSwiftStacks() {
        val answers = Answers.section("spellings").jsonArray
        val expectedCount = NoteName.rootChoices.size * ScaleType.entries.size
        assertEquals("spelling count", expectedCount, answers.size)
        var index = 0
        for (root in NoteName.rootChoices) {
            for (type in ScaleType.entries) {
                val expected = answers[index].jsonObject
                checkChords(expected, Scale(root, type))
                index += 1
            }
        }
    }

    /** One root and scale: its identity first, so a misordered file fails loudly, then the
     *  key signature, the key-table size and every chord field by field. */
    private fun checkChords(
        expected: JsonObject,
        scale: Scale,
    ) {
        val case = "${expected.string("root")} ${expected.string("scaleType")}"
        assertSame("$case title", expected.string("title"), scale.title)
        assertSame("$case keySignature", expected.string("keySignature"), scale.keySignature)
        assertSame(
            "$case diatonicTriadCount",
            expected.int("diatonicTriadCount"),
            scale.diatonicTriads().size,
        )

        val expectedChords = expected.array("letterTriads")
        val chords = scale.letterTriads()
        assertEquals("$case letterTriads count", expectedChords.size, chords.size)
        for (chordIndex in chords.indices) {
            val expectedChord = expectedChords[chordIndex].jsonObject
            val chordCase = "$case chord $chordIndex (${expectedChord.string("name")})"
            checkChord(chordCase, expectedChord, chords[chordIndex])
        }
    }

    private fun checkChord(
        case: String,
        expected: JsonObject,
        chord: ScaleChord,
    ) {
        assertSame("$case degree", expected.string("degree"), chord.degree.label)
        assertSame("$case numeral", expected.string("numeral"), chord.numeral)
        assertSame("$case formulaNumeral", expected.string("formulaNumeral"), chord.formulaNumeral)
        assertSame("$case quality", expected.string("quality"), chord.triad.quality.swiftName)
        assertSame("$case name", expected.string("name"), chord.triad.name)
        assertSame("$case spokenName", expected.string("spokenName"), chord.triad.spokenName)
        assertSame("$case spokenTitle", expected.string("spokenTitle"), chord.spokenTitle)
        assertSame("$case notes", strings(expected.array("notes")), displayNames(chord.notes))
    }

    // feature: roots-scales, chord-chips-rings
    @Test
    fun byLetterMatchesEveryOtherNoteForSevenNoteScales() {
        for (type in ScaleType.entries) {
            if (type.degrees.size != SEVEN_NOTES) {
                continue
            }
            for (root in NoteName.rootChoices) {
                val scale = Scale(root, type)
                val expected = everyOtherNoteChordNames(scale)
                val case = "${root.displayName} ${type.rawValue}"
                assertEquals(case, expected, chordNames(scale.letterTriads()))
            }
        }
    }

    // feature: roots-scales, chord-chips-rings
    @Test
    fun pentatonicScalesMakeTwoTriadsAndTwoSuspendedChords() {
        val cMajorPentatonic = Scale(NoteName(Letter.C), ScaleType.MAJOR_PENTATONIC)
        assertEquals(
            "C major pentatonic",
            listOf("C", "Dsus4", "Gsus4", "Am"),
            chordNames(cMajorPentatonic.letterTriads()),
        )
        val aMinorPentatonic = Scale(NoteName(Letter.A), ScaleType.MINOR_PENTATONIC)
        assertEquals(
            "A minor pentatonic",
            listOf("Am", "C", "Dsus4", "Gsus4"),
            chordNames(aMinorPentatonic.letterTriads()),
        )
    }

    // feature: roots-scales, chord-chips-rings
    @Test
    fun suspendedChordsCarryTheStandInNote() {
        val chords = Scale(NoteName(Letter.C), ScaleType.MAJOR_PENTATONIC).letterTriads()
        val onD = chords.firstOrNull { chord -> chord.degree.number == 2 }
        assertNotNull("C major pentatonic has a chord on D", onD)
        val expectedNotes = listOf(NoteName(Letter.D), NoteName(Letter.G), NoteName(Letter.A))
        assertEquals("notes", expectedNotes, onD!!.notes)
        assertEquals("spokenName", "D suspended fourth", onD.triad.spokenName)
        assertEquals("formulaNumeral", "IIsus4", onD.formulaNumeral)
    }

    // feature: roots-scales, chord-chips-rings
    @Test
    fun theSecondStandsInWhenThereIsNoFourthEither() {
        // C D G: no E, no F, so the chord on C is Csus2.
        val degrees =
            listOf(
                ScaleDegree.fromSymbol("1"),
                ScaleDegree.fromSymbol("2"),
                ScaleDegree.fromSymbol("5"),
            )
        val definition = ScaleDefinition("Test", degrees)
        val chords = Scale(NoteName(Letter.C), definition).letterTriads()
        assertEquals("C D G", listOf("Csus2", "Gsus4"), chordNames(chords))
    }

    // feature: roots-scales, chord-chips-rings
    @Test
    fun everyChordQualityHasAChartName() {
        val names = mutableListOf<String>()
        for (quality in TriadQuality.entries) {
            names.add(Triad(NoteName(Letter.C), quality).name)
        }
        val expected =
            listOf(
                "C",
                "Cm",
                "C°",
                "C+",
                "C(♭5)",
                "Cm(♯5)",
                "Csus4",
                "Csus2",
                "Csus4(♭5)",
                "Csus2(♭5)",
                "Csus4(♯5)",
                "Csus2(♯5)",
            )
        assertEquals("names", expected, names)
        assertEquals("I(♭5)", "I(♭5)", TriadQuality.MAJOR_FLAT_FIVE.numeral("I"))
        assertEquals("iii(♯5)", "iii(♯5)", TriadQuality.MINOR_SHARP_FIVE.numeral("III"))
        assertEquals("IVsus2", "IVsus2", TriadQuality.SUSPENDED_SECOND.numeral("IV"))
        assertNull("fourth over a major sixth", TriadQuality.fromSemitones(5, 9))
    }

    // feature: roots-scales, chord-chips-rings
    @Test
    fun bluesPrefersThePairingWithFewestAccidentals() {
        val chords = Scale(NoteName(Letter.C), ScaleType.BLUES).letterTriads()
        assertTrue("C blues has chords", chords.isNotEmpty())
        val tonic = chords.first()
        assertEquals("name", "Cm", tonic.triad.name)
        val expectedNotes =
            listOf(NoteName(Letter.C), NoteName(Letter.E, -1), NoteName(Letter.G))
        assertEquals("notes", expectedNotes, tonic.notes)
    }

    // feature: roots-scales, chord-chips-rings
    @Test
    fun chordsCarryTheirSpelledNotes() {
        val dominant = Scale(NoteName(Letter.G), ScaleType.MAJOR).letterTriads()[4]
        val expectedNotes =
            listOf(NoteName(Letter.D), NoteName(Letter.F, 1), NoteName(Letter.A))
        assertEquals("notes", expectedNotes, dominant.notes)
        assertEquals("spokenTitle", "V, D major", dominant.spokenTitle)
    }

    // feature: roots-scales, chord-tables
    @Test
    fun formulaNumeralsCarryAccidentalsAndKeyNumeralsStayPlain() {
        val harmonicMinor = Scale(NoteName(Letter.C), ScaleType.HARMONIC_MINOR).letterTriads()
        val formulaNumerals = mutableListOf<String>()
        val numerals = mutableListOf<String>()
        for (chord in harmonicMinor) {
            formulaNumerals.add(chord.formulaNumeral)
            numerals.add(chord.numeral)
        }
        assertEquals(
            "formula numerals",
            listOf("i", "ii°", "♭III+", "iv", "V", "♭VI", "vii°"),
            formulaNumerals,
        )
        assertEquals("numerals", listOf("i", "ii°", "III+", "iv", "V", "VI", "vii°"), numerals)
        val phrygianSecond = Scale(NoteName(Letter.C), ScaleType.PHRYGIAN).letterTriads()[1]
        assertEquals("Phrygian ♭II", "♭II", phrygianSecond.formulaNumeral)
    }

    // feature: roots-scales
    @Test
    fun keySignatures() {
        assertEquals("C major", "—", Scale(NoteName(Letter.C), ScaleType.MAJOR).keySignature)
        assertEquals("E major", "4♯", Scale(NoteName(Letter.E), ScaleType.MAJOR).keySignature)
        assertEquals(
            "B♭ major",
            "2♭",
            Scale(NoteName(Letter.B, -1), ScaleType.MAJOR).keySignature,
        )
    }

    /** The textbook way for a seven-note scale: stack every other note by position. */
    private fun everyOtherNoteChordNames(scale: Scale): List<String> {
        val names = mutableListOf<String>()
        val count = scale.tones.size
        for (step in 0 until count) {
            val root = scale.tones[step].name
            val third = scale.tones[(step + 2) % count].name
            val fifth = scale.tones[(step + 4) % count].name
            val thirdSemitones = PitchMath.wrap(third.pitchClass - root.pitchClass)
            val fifthSemitones = PitchMath.wrap(fifth.pitchClass - root.pitchClass)
            val quality = TriadQuality.fromSemitones(thirdSemitones, fifthSemitones)
            if (quality != null) {
                names.add(Triad(root, quality).name)
            }
        }
        return names
    }

    private fun chordNames(chords: List<ScaleChord>): List<String> {
        val names = mutableListOf<String>()
        for (chord in chords) {
            names.add(chord.triad.name)
        }
        return names
    }

    private fun displayNames(notes: List<NoteName>): List<String> {
        val names = mutableListOf<String>()
        for (note in notes) {
            names.add(note.displayName)
        }
        return names
    }

    private fun strings(array: JsonArray): List<String> {
        val values = mutableListOf<String>()
        for (element in array) {
            values.add(element.jsonPrimitive.content)
        }
        return values
    }

    private companion object {
        const val SEVEN_NOTES = 7
    }
}
