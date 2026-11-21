package com.alnourkhalifa.scales.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.math.abs

/**
 * The one copy of `expected/answers.json`, written by the Swift generator
 * (`swift run --quiet ExpectedAnswers expected/answers.json`) and read by every Kotlin
 * answers test. Loading it once keeps the suite fast; the file is about 3 MB.
 *
 * Swift's synthesized Encodable leaves a nil optional OUT of the object rather than writing
 * `null`, so readers use the `optional…` helpers for fields that may be absent.
 */
object Answers {
    /** Every top-level section and the Kotlin test file that reads it. A section listed here
     *  with no reader yet is a wave 1 task; a section in the file but not here fails
     *  [AnswersSectionsTest], which is how a new Swift section reaches Kotlin. */
    val knownSections: Map<String, String> =
        mapOf(
            "meta" to "AnswersSectionsTest",
            "roots" to "AnswersSpellingTest",
            "scaleTypes" to "AnswersSpellingTest",
            "families" to "AnswersSpellingTest",
            "tunings" to "AnswersTuningTest",
            "bassTunings" to "AnswersTuningTest",
            "spellings" to "AnswersSpellingTest",
            "fretboards" to "AnswersFretboardTest",
            "bassFretboards" to "AnswersFretboardTest",
            "capo" to "AnswersCapoTest",
            "keyboards" to "AnswersKeyboardTest",
            "edits" to "AnswersEditTest",
            "identifier" to "AnswersIdentifierTest",
            "settings" to "AnswersSettingsTest",
            "session" to "AnswersSessionTest",
            "library" to "AnswersLibraryTest",
            "keyChordTable" to "AnswersKeyTableTest",
            "drums" to "AnswersDrumTest",
            "drumRender" to "AnswersDrumTest",
            "geometry" to "AnswersGeometryTest",
        )

    val root: JsonObject by lazy { load() }

    /** Doubles in the file are rounded to this many decimals by the generator. */
    const val DECIMALS = 6

    /** Half a unit in the last rounded decimal, plus slack for a 1-ulp difference. */
    const val TOLERANCE = 1.0e-6

    fun section(name: String): JsonElement {
        val element = root[name]
        requireNotNull(element) { "answers.json has no section '$name'" }
        return element
    }

    private fun load(): JsonObject {
        val stream = Answers::class.java.getResourceAsStream("/answers.json")
        requireNotNull(stream) {
            "answers.json is not on the test classpath; run `make answers` at the repository root"
        }
        val text = stream.bufferedReader().use { reader -> reader.readText() }
        return Json.parseToJsonElement(text).jsonObject
    }
}

// Small readers that name the failing field, so a mismatch reports the exact case.

fun JsonObject.obj(key: String): JsonObject = requiredField(key).jsonObject

fun JsonObject.array(key: String): JsonArray = requiredField(key).jsonArray

fun JsonObject.string(key: String): String = requiredField(key).jsonPrimitive.content

fun JsonObject.int(key: String): Int = requiredField(key).jsonPrimitive.int

fun JsonObject.double(key: String): Double = requiredField(key).jsonPrimitive.double

fun JsonObject.bool(key: String): Boolean =
    requiredField(key)
        .jsonPrimitive.content
        .toBoolean()

/** A field Swift leaves out when its optional is nil. */
fun JsonObject.optionalString(key: String): String? {
    val element = this[key] ?: return null
    if (element is JsonPrimitive && element.content == "null") return null
    return element.jsonPrimitive.content
}

fun JsonObject.optionalInt(key: String): Int? {
    val element = this[key] ?: return null
    return element.jsonPrimitive.int
}

private fun JsonObject.requiredField(key: String): JsonElement {
    val element = this[key]
    requireNotNull(element) { "field '$key' is missing from ${keys.sorted()}" }
    return element
}

/** Compares a computed double with a rounded answer, naming the case on failure. */
fun assertClose(
    case: String,
    expected: Double,
    actual: Double,
    tolerance: Double = Answers.TOLERANCE,
) {
    val difference = abs(expected - actual)
    assertTrue(
        "$case: expected $expected, got $actual (difference $difference)",
        difference <= tolerance,
    )
}

fun assertSame(
    case: String,
    expected: Any?,
    actual: Any?,
) {
    assertEquals(case, expected, actual)
}
