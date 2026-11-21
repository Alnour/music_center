package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertClose
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.board.FretboardGeometry
import com.alnourkhalifa.scales.core.board.Size
import com.alnourkhalifa.scales.core.bool
import com.alnourkhalifa.scales.core.double
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.obj
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reads the `capo` section Swift wrote (`scripts/ExpectedAnswers/CapoSections.swift`): the
 * fret choices, each tuning's shape offset, the picker's rows and advice for every root and
 * scale type, the shapes a few frets give, the neck with the frets behind the capo emptied,
 * and the capo bar's rectangle. Every entry is checked, in the generator's order, so a
 * misordered or missing case fails by name.
 */
class AnswersCapoTest {
    private val section: JsonObject by lazy { Answers.section("capo").jsonObject }

    /** The frets the generator writes shapes for: both spellings of the black keys and the
     *  far end of the choices. */
    private val shapeFrets = listOf(1, 4, 9)

    // feature: capo-shapes
    @Test
    fun theFretChoicesAndAdviceMatch() {
        val expectedFrets = mutableListOf<Int>()
        for (element in section.array("fretChoices")) {
            expectedFrets.add(element.jsonPrimitive.int)
        }
        assertEquals("fretChoices", expectedFrets, Capo.fretChoices)
        assertSame(
            "unsupportedTuningAdvice",
            section.string("unsupportedTuningAdvice"),
            Capo.unsupportedTuningAdvice,
        )
    }

    // feature: capo-shapes
    @Test
    fun everyTuningsShapeOffsetMatches() {
        val offsets = section.array("standardShapeOffsets")
        assertEquals("offset count", offsets.size, Tuning.entries.size)
        for (index in offsets.indices) {
            val element = offsets[index]
            val tuning = Tuning.entries[index]
            val expected = if (element is JsonNull) null else element.jsonPrimitive.int
            assertSame(
                "${tuning.rawValue} standardShapeOffset",
                expected,
                tuning.standardShapeOffset,
            )
        }
    }

    // feature: capo-shapes
    @Test
    fun everyPickerMatches() {
        val answers = section.array("pickers")
        val cases = pickerCases()
        assertEquals("picker count", answers.size, cases.size)
        for (index in answers.indices) {
            checkPicker(answers[index].jsonObject, cases[index])
        }
    }

    // feature: capo-shapes
    @Test
    fun everyShapeListMatches() {
        val answers = section.array("shapes")
        val cases = shapeCases()
        assertEquals("shape list count", answers.size, cases.size)
        for (index in answers.indices) {
            checkShapes(answers[index].jsonObject, cases[index])
        }
    }

    // feature: capo-shapes
    @Test
    fun everyBoardEmptiesTheFretsBehindTheCapo() {
        val answers = section.array("boards")
        val requestedFrets = listOf(0, 3, 9, 30)
        assertEquals("board count", answers.size, requestedFrets.size)
        for (index in answers.indices) {
            val answer = answers[index].jsonObject
            checkBoard(answer, requestedFrets[index])
        }
    }

    // feature: capo-shapes
    @Test
    fun everyCapoBarMatches() {
        val answers = section.array("bars")
        var index = 0
        for (isLeftHanded in listOf(false, true)) {
            val geometry =
                FretboardGeometry(
                    size = Size(BAR_BOARD_WIDTH, BAR_BOARD_HEIGHT),
                    fretCount = BAR_BOARD_FRETS,
                    stringCount = BAR_BOARD_STRINGS,
                    isLeftHanded = isLeftHanded,
                )
            for (fret in listOf(1, 3, 9)) {
                val answer = answers[index].jsonObject
                val case = "bar fret $fret left-handed $isLeftHanded"
                assertSame("$case fret", answer.int("fret"), fret)
                assertSame("$case isLeftHanded", answer.bool("isLeftHanded"), isLeftHanded)
                val rect = geometry.capoBarRect(fret, BAR_BOARD_STRINGS)
                val expected = answer.obj("rect")
                assertClose("$case x", expected.double("x"), rect.x)
                assertClose("$case y", expected.double("y"), rect.y)
                assertClose("$case width", expected.double("width"), rect.width)
                assertClose("$case height", expected.double("height"), rect.height)
                index += 1
            }
        }
        assertEquals("bar count", answers.size, index)
    }

    // feature: capo-shapes
    @Test
    fun noCapoAndOtherTuningsNameNoShapes() {
        val gMajor = Scale(NoteName(Letter.G), ScaleType.MAJOR)
        assertNull("no capo", CapoTransposition.create(gMajor, 0, Tuning.STANDARD))
        assertNull("drop D", CapoTransposition.create(gMajor, 3, Tuning.DROP_D))
        val halfStepDown = CapoTransposition.create(gMajor, 3, Tuning.HALF_STEP_DOWN)
        assertEquals("half step down", 2, halfStepDown?.semitonesDown)
    }

    /** One picker case: a scale in a tuning, in the generator's order. */
    private class PickerCase(
        val root: NoteName,
        val type: ScaleType,
        val tuning: Tuning,
    )

    /** Every root × every scale type in standard tuning, then G major in every other tuning. */
    private fun pickerCases(): List<PickerCase> {
        val cases = mutableListOf<PickerCase>()
        for (root in NoteName.rootChoices) {
            for (type in ScaleType.entries) {
                cases.add(PickerCase(root, type, Tuning.STANDARD))
            }
        }
        for (tuning in Tuning.entries) {
            if (tuning != Tuning.STANDARD) {
                cases.add(PickerCase(NoteName(Letter.G), ScaleType.MAJOR, tuning))
            }
        }
        return cases
    }

    private fun checkPicker(
        answer: JsonObject,
        case: PickerCase,
    ) {
        val name = "${answer.string(
            "root",
        )} ${answer.string("scaleType")} ${answer.string("tuning")}"
        assertSame("$name root", answer.string("root"), case.root.displayName)
        assertSame("$name scaleType", answer.string("scaleType"), case.type.rawValue)
        assertSame("$name tuning", answer.string("tuning"), case.tuning.rawValue)
        val scale = Scale(case.root, case.type)
        val suggestions = Capo.suggestions(scale, case.tuning)
        val expected = answer.array("suggestions")
        assertEquals("$name suggestion count", expected.size, suggestions.size)
        for (index in expected.indices) {
            checkSuggestion(
                "$name suggestion $index",
                expected[index].jsonObject,
                suggestions[index],
            )
        }
        assertSame("$name advice", answer.string("advice"), Capo.advice(scale, case.tuning))
    }

    private fun checkSuggestion(
        case: String,
        answer: JsonObject,
        suggestion: CapoSuggestion,
    ) {
        assertSame("$case capoFret", answer.int("capoFret"), suggestion.capoFret)
        assertSame("$case title", answer.string("title"), suggestion.title)
        assertSame("$case detail", answer.string("detail"), suggestion.detail)
        assertSame("$case label", answer.string("label"), suggestion.label)
        assertSame("$case openChordCount", answer.int("openChordCount"), suggestion.openChordCount)
        assertSame("$case chordCount", answer.int("chordCount"), suggestion.chordCount)
    }

    /** One shapes case: the transposition the generator wrote, with what it was built from. */
    private class ShapesCase(
        val root: NoteName,
        val type: ScaleType,
        val tuning: Tuning,
        val capoFret: Int,
        val transposition: CapoTransposition,
    )

    /** One scale, tuning and fret the generator asked for shapes of. */
    private class ShapeRequest(
        val root: NoteName,
        val type: ScaleType,
        val tuning: Tuning,
        val capoFret: Int,
    )

    /** The requests that name shapes, each with its transposition; a tuning without the
     *  standard shapes gives none, so the generator wrote nothing for it. */
    private fun shapeCases(): List<ShapesCase> {
        val cases = mutableListOf<ShapesCase>()
        for (request in shapeRequests()) {
            val scale = Scale(request.root, request.type)
            val transposition =
                CapoTransposition.create(scale, request.capoFret, request.tuning) ?: continue
            val case =
                ShapesCase(
                    request.root,
                    request.type,
                    request.tuning,
                    request.capoFret,
                    transposition,
                )
            cases.add(case)
        }
        return cases
    }

    /** Frets 1, 4 and 9 of every root and scale type in standard tuning, then G major with a
     *  capo on 3 in every other tuning, in the generator's order. */
    private fun shapeRequests(): List<ShapeRequest> {
        val requests = mutableListOf<ShapeRequest>()
        for (root in NoteName.rootChoices) {
            for (type in ScaleType.entries) {
                for (fret in shapeFrets) {
                    requests.add(ShapeRequest(root, type, Tuning.STANDARD, fret))
                }
            }
        }
        val gMajorRoot = NoteName(Letter.G)
        for (tuning in Tuning.entries) {
            if (tuning != Tuning.STANDARD) {
                requests.add(ShapeRequest(gMajorRoot, ScaleType.MAJOR, tuning, OTHER_TUNING_FRET))
            }
        }
        return requests
    }

    private fun checkShapes(
        answer: JsonObject,
        case: ShapesCase,
    ) {
        val name =
            "${answer.string("root")} ${answer.string("scaleType")} " +
                "${answer.string("tuning")} capo ${answer.int("capoFret")}"
        assertSame("$name root", answer.string("root"), case.root.displayName)
        assertSame("$name scaleType", answer.string("scaleType"), case.type.rawValue)
        assertSame("$name tuning", answer.string("tuning"), case.tuning.rawValue)
        assertSame("$name capoFret", answer.int("capoFret"), case.capoFret)
        val transposition = case.transposition
        assertSame("$name transposition capoFret", answer.int("capoFret"), transposition.capoFret)
        assertSame("$name semitonesDown", answer.int("semitonesDown"), transposition.semitonesDown)
        assertSame(
            "$name shapeRoot",
            answer.string("shapeRoot"),
            transposition.shapeRoot.displayName,
        )
        assertSame(
            "$name shapeScaleTitle",
            answer.string("shapeScaleTitle"),
            transposition.shapeScaleTitle,
        )
        assertSame("$name note", answer.string("note"), transposition.note)

        val chords = Scale(case.root, case.type).boardChords()
        val expected = answer.array("shapes")
        assertEquals("$name shape count", expected.size, chords.size)
        for (index in expected.indices) {
            val shapeAnswer = expected[index].jsonObject
            val chord = chords[index]
            checkShape("$name shape $index", shapeAnswer, chord, transposition.shape(chord))
        }
    }

    private fun checkShape(
        case: String,
        answer: JsonObject,
        chord: ScaleChord,
        shape: CapoChordShape,
    ) {
        assertSame("$case chord", answer.string("chord"), chord.name)
        assertSame("$case name", answer.string("name"), shape.name)
        assertSame("$case spokenName", answer.string("spokenName"), shape.spokenName)
        assertSame("$case fingering", answer.string("fingering"), shape.fingering.rawValue)
        assertSame("$case caption", answer.string("caption"), shape.caption)
        assertSame("$case spokenSentence", answer.string("spokenSentence"), shape.spokenSentence)
    }

    /** G major in standard tuning with the capo asked for on one fret, every string's tones
     *  against Swift's, written `"F♯|7"` or null. */
    private fun checkBoard(
        answer: JsonObject,
        requestedCapoFret: Int,
    ) {
        val case = "board capo $requestedCapoFret"
        assertSame("$case requestedCapoFret", answer.int("requestedCapoFret"), requestedCapoFret)
        val fretCount = answer.int("fretCount")
        val scale = Scale(NoteName(Letter.G), ScaleType.MAJOR)
        val layout =
            FretboardLayout(scale, Tuning.STANDARD, fretCount, capoFret = requestedCapoFret)
        assertSame("$case fretCount", fretCount, layout.fretCount)
        assertSame("$case capoFret", answer.int("capoFret"), layout.capoFret)
        val expectedStrings = answer.array("strings")
        assertEquals("$case string count", expectedStrings.size, layout.strings.size)
        for (index in expectedStrings.indices) {
            val expectedString = expectedStrings[index].jsonObject
            val guitarString = layout.strings[index]
            val stringCase = "$case string $index"
            assertSame(
                "$stringCase openNote",
                expectedString.string("openNote"),
                guitarString.openNote.displayName,
            )
            assertSame(
                "$stringCase tonesByFret",
                expectedTones(expectedString.array("tonesByFret")),
                toneLabels(guitarString.tonesByFret),
            )
        }
    }

    private fun expectedTones(array: JsonArray): List<String?> {
        val labels = mutableListOf<String?>()
        for (element in array) {
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

    private companion object {
        /** The generator's other-tuning shapes put the capo on fret 3. */
        const val OTHER_TUNING_FRET = 3

        /** The capo bars are measured on a 1500 × 500 board of 15 frets and 6 strings. */
        const val BAR_BOARD_WIDTH = 1500.0
        const val BAR_BOARD_HEIGHT = 500.0
        const val BAR_BOARD_FRETS = 15
        const val BAR_BOARD_STRINGS = 6
    }
}
