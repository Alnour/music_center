package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.bool
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.obj
import com.alnourkhalifa.scales.core.optionalString
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * Reads the `edits` section Swift wrote: for every scale type in C, each edit
 * `legalEdits()` offers in full (labels, menu title, sentence, result, lesson title, the name
 * it is recognised by and every chord change) and each refused candidate with Swift's reason.
 */
class AnswersEditTest {
    // feature: note-edit-revert, transform-lessons
    @Test
    fun everyScaleOffersAndRefusesTheEditsSwiftDoes() {
        val answers = Answers.section("edits").jsonArray
        assertEquals("edit section count", ScaleType.entries.size, answers.size)
        val root = NoteName(Letter.C)
        for (index in ScaleType.entries.indices) {
            val type = ScaleType.entries[index]
            val expected = answers[index].jsonObject
            assertSame("scale type $index", expected.string("scaleType"), type.rawValue)
            assertSame("${type.rawValue} root", expected.string("root"), root.displayName)
            assertSame(
                "${type.rawValue} formula",
                expected.string("formula"),
                type.definition.formula,
            )
            checkLegalEdits(type, root, expected.array("legal"))
            checkRejectedEdits(type, expected.array("rejected"))
        }
    }

    /** The offered edits in Swift's order, then each one's every field. */
    private fun checkLegalEdits(
        type: ScaleType,
        root: NoteName,
        expectedEdits: JsonArray,
    ) {
        val parsedEdits = mutableListOf<ScaleEdit>()
        for (element in expectedEdits) {
            val editAnswer = element.jsonObject.obj("edit")
            parsedEdits.add(parseEdit(editAnswer.string("json")))
        }
        assertEquals("${type.rawValue} legalEdits", parsedEdits, type.definition.legalEdits())

        for (editIndex in parsedEdits.indices) {
            val edit = parsedEdits[editIndex]
            val expected = expectedEdits[editIndex].jsonObject
            val case = "${type.rawValue} ${edit.swiftName} ${edit.degree.label}"
            checkLegalEdit(case, expected, ScaleTransformation(root, type.definition, edit))
        }
    }

    private fun checkLegalEdit(
        case: String,
        expected: JsonObject,
        transformation: ScaleTransformation,
    ) {
        val edit = transformation.edit
        val root = transformation.root
        val editAnswer = expected.obj("edit")
        assertSame("$case kind", editAnswer.string("kind"), edit.swiftName)
        assertSame("$case degree", editAnswer.string("degree"), edit.degree.label)
        assertSame("$case shortLabel", expected.string("shortLabel"), edit.shortLabel)
        assertSame("$case menuTitle", expected.string("menuTitle"), edit.menuTitle(root))
        assertSame("$case sentence", expected.string("sentence"), edit.sentence(root))
        assertSame("$case editSentence", expected.string("sentence"), transformation.editSentence)
        assertSame("$case workingName", expected.string("workingName"), transformation.after.name)
        assertSame("$case formula", expected.string("formula"), transformation.after.formula)
        assertSame("$case title", expected.string("title"), transformation.title)
        val match = ScaleIdentifier.match(transformation.after, emptyList())
        assertSame("$case identifiedAs", expected.optionalString("identifiedAs"), match?.name)

        val report = transformation.chordChanges
        assertSame(
            "$case changesAnyChord",
            expected.bool("changesAnyChord"),
            report.changesAnyChord,
        )
        val expectedChanges = expected.array("chordChanges")
        assertEquals("$case chordChanges count", expectedChanges.size, report.changes.size)
        for (changeIndex in report.changes.indices) {
            val changeCase = "$case chord change $changeIndex"
            checkChordChange(
                changeCase,
                expectedChanges[changeIndex].jsonObject,
                report.changes[changeIndex],
            )
        }
    }

    private fun checkChordChange(
        case: String,
        expected: JsonObject,
        change: ChordChange,
    ) {
        assertSame("$case kind", expected.string("kind"), change.kind.swiftName)
        checkChordSummary("$case before", expected["before"]?.jsonObject, change.before)
        checkChordSummary("$case after", expected["after"]?.jsonObject, change.after)
        assertSame(
            "$case notesGone",
            strings(expected.array("notesGone")),
            displayNames(change.notesGone),
        )
        assertSame(
            "$case notesNew",
            strings(expected.array("notesNew")),
            displayNames(change.notesNew),
        )
        assertSame("$case summary", expected.string("summary"), change.summary)
    }

    /** Swift leaves out a missing chord, so an absent object must meet a null chord. */
    private fun checkChordSummary(
        case: String,
        expected: JsonObject?,
        chord: ScaleChord?,
    ) {
        if (expected == null) {
            assertNull("$case should be absent", chord)
            return
        }
        if (chord == null) {
            fail("$case is missing; Swift has ${expected.string("name")}")
            return
        }
        assertSame("$case numeral", expected.string("numeral"), chord.numeral)
        assertSame("$case formulaNumeral", expected.string("formulaNumeral"), chord.formulaNumeral)
        assertSame("$case name", expected.string("name"), chord.triad.name)
    }

    /** Each refused candidate is refused for Swift's reason, naming the same clashing degree. */
    private fun checkRejectedEdits(
        type: ScaleType,
        expectedEdits: JsonArray,
    ) {
        for (element in expectedEdits) {
            val expected = element.jsonObject
            val edit = parseEdit(expected.obj("edit").string("json"))
            val case = "${type.rawValue} ${edit.swiftName} ${edit.degree.label}"
            val error = refusal(type.definition, edit)
            if (error == null) {
                fail("$case was accepted; Swift refuses it with ${expected.string("error")}")
                return
            }
            assertSame("$case error", expected.string("error"), error.swiftName)
            val clashesWith = (error as? ScaleEditError.SamePitchAsAnotherNote)?.clashesWith?.label
            assertSame("$case clashesWith", expected.optionalString("clashesWith"), clashesWith)
        }
    }

    private fun refusal(
        definition: ScaleDefinition,
        edit: ScaleEdit,
    ): ScaleEditError? =
        try {
            definition.applying(edit)
            null
        } catch (error: ScaleEditError) {
            error
        }

    private fun strings(array: JsonArray): List<String> {
        val values = mutableListOf<String>()
        for (element in array) {
            values.add(element.jsonPrimitive.content)
        }
        return values
    }

    private fun displayNames(notes: List<NoteName>): List<String> {
        val names = mutableListOf<String>()
        for (note in notes) {
            names.add(note.displayName)
        }
        return names
    }

    companion object {
        /** Reads an edit as Swift's synthesized Codable writes it:
         *  `{"lower":{"_0":{"alteration":0,"number":7}}}`. */
        fun parseEdit(json: String): ScaleEdit {
            val wrapper = Json.parseToJsonElement(json).jsonObject
            val kind = wrapper.keys.single()
            val degreeObject = wrapper.obj(kind).obj("_0")
            val degree = ScaleDegree(degreeObject.int("number"), degreeObject.int("alteration"))
            return when (kind) {
                "lower" -> ScaleEdit.Lower(degree)
                "raise" -> ScaleEdit.Raise(degree)
                "remove" -> ScaleEdit.Remove(degree)
                "add" -> ScaleEdit.Add(degree)
                else -> error("Unknown edit kind $kind in $json")
            }
        }
    }
}
