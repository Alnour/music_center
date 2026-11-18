package com.alnourkhalifa.scales.core.music

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.optionalString
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/**
 * Reads the `identifier` section Swift wrote: the name every built-in scale is known by, the
 * name every edit result is recognised as, and the saved-scale cases.
 */
class AnswersIdentifierTest {
    private val section: JsonObject by lazy { Answers.section("identifier").jsonObject }

    // feature: transform-lessons
    @Test
    fun everyBuiltInIsCalledWhatSwiftCallsIt() {
        val answers = section.array("builtIns")
        assertEquals("built-in count", ScaleType.entries.size, answers.size)
        for (index in ScaleType.entries.indices) {
            val type = ScaleType.entries[index]
            val expected = answers[index].jsonObject
            assertSame("built-in $index", expected.string("scaleType"), type.rawValue)
            val match = ScaleIdentifier.match(type.definition, emptyList())
            val matchedType = (match as? ScaleMatch.BuiltIn)?.type?.rawValue
            assertSame(
                "${type.rawValue} matchedName",
                expected.optionalString("matchedName"),
                match?.name,
            )
            assertSame(
                "${type.rawValue} matchedType",
                expected.optionalString("matchedType"),
                matchedType,
            )
        }
    }

    // feature: transform-lessons
    @Test
    fun everyEditResultIsRecognisedAsSwiftRecognisesIt() {
        val answers = section.array("editResults")
        var index = 0
        for (type in ScaleType.entries) {
            for (edit in type.definition.legalEdits()) {
                val expected = answers[index].jsonObject
                index += 1
                val case = "${type.rawValue} ${edit.shortLabel}"
                assertSame("$case scaleType", expected.string("scaleType"), type.rawValue)
                assertSame("$case edit", expected.string("edit"), edit.shortLabel)
                val result = type.definition.applying(edit)
                assertSame("$case resultFormula", expected.string("resultFormula"), result.formula)
                val match = ScaleIdentifier.match(result, emptyList())
                assertSame("$case matchedName", expected.optionalString("matchedName"), match?.name)
            }
        }
        assertEquals("edit result count", answers.size, index)
    }

    // feature: transform-lessons
    @Test
    fun savedScalesAreRecognisedAfterTheBuiltIns() {
        val answers = section.array("custom")
        for (element in answers) {
            val expected = element.jsonObject
            val case = expected.string("description")
            val definition = ScaleDefinition("Checked", degrees(expected.string("formula")))
            val saved = mutableListOf<CustomScale>()
            for (name in expected.array("savedScales")) {
                saved.add(savedScale(name.jsonPrimitive.content))
            }
            val match = ScaleIdentifier.match(definition, saved)
            assertSame("$case matchedName", expected.optionalString("matchedName"), match?.name)
            assertSame("$case matchedKind", expected.optionalString("matchedKind"), kind(match))
        }
    }

    private fun kind(match: ScaleMatch?): String? =
        when (match) {
            is ScaleMatch.BuiltIn -> "builtIn"
            is ScaleMatch.Custom -> "custom"
            null -> null
        }

    /** The saved scales of the generator's fixtures (`Fixtures.swift`, `customMatches()`):
     *  the file names them but does not carry their formulas. */
    private fun savedScale(name: String): CustomScale {
        val formula =
            when (name) {
                "Augmented Major" -> "1 2 3 4 ♯5 6 7"
                "My Major" -> ScaleType.MAJOR.formula
                else -> error("No fixture for saved scale $name")
            }
        return CustomScale(FIXTURE_ID, ScaleDefinition(name, degrees(formula)), FIXTURE_DATE)
    }

    private fun degrees(formula: String): List<ScaleDegree> {
        val degrees = mutableListOf<ScaleDegree>()
        for (symbol in formula.split(" ")) {
            degrees.add(ScaleDegree.fromSymbol(symbol))
        }
        return degrees
    }

    private companion object {
        val FIXTURE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val FIXTURE_DATE = ReferenceDate(800_000_000.0)
    }
}
