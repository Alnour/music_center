package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

/**
 * Reads the `library` section Swift wrote: one library file with a saved scale and two lessons
 * (a lowered note that lands on a built-in scale, an added note filed under the saved scale)
 * exactly as `ScaleLibrary.encoded` writes it. Compared as parsed JSON trees.
 */
class AnswersLibraryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val section: JsonObject by lazy { Answers.section("library").jsonObject }

    private fun expectedTitles(): List<String> {
        val titles = mutableListOf<String>()
        for (element in section.array("lessonTitles")) {
            titles.add(element.jsonPrimitive.content)
        }
        return titles
    }

    // feature: transform-lessons
    @Test
    fun theSwiftFileReadsAndIsWrittenBackTheSame() {
        val text = section.string("documentJSON")

        val document = ScaleLibraryJson.decodeDocument(text)
        val written = ScaleLibraryJson.encodeDocument(document)

        assertEquals(
            "documentJSON",
            Json.parseToJsonElement(text),
            Json.parseToJsonElement(written),
        )
        assertEquals("scaleCount", section.int("scaleCount"), document.scales.size)
        val titles = mutableListOf<String>()
        for (lesson in document.lessons) {
            titles.add(lesson.title)
        }
        assertEquals("lessonTitles", expectedTitles(), titles)
    }

    // feature: transform-lessons
    @Test
    fun theDecodedLessonsPointWhereSwiftsDo() {
        val document = ScaleLibraryJson.decodeDocument(section.string("documentJSON"))
        val savedId = UUID.fromString("00000000-0000-0000-0000-000000000001")

        assertEquals(
            savedId,
            document.scales
                .first()
                .id,
        )
        assertEquals(ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN), document.lessons[0].result)
        assertEquals(ScaleReference.Custom(savedId), document.lessons[1].result)
        assertEquals(
            "the saved name, not the working name",
            "Bebop Major",
            document.lessons[1]
                .transformation.after.name,
        )
        assertEquals(
            800_000_000.0,
            document.lessons[0]
                .date.secondsSinceReferenceDate,
            0.0,
        )
    }

    // feature: transform-lessons
    @Test
    fun theLibraryOpensTheSwiftFile() {
        val file = File(folder.root, "ScaleLibrary.json")
        file.writeText(section.string("documentJSON"))

        val library = ScaleLibrary(file)

        assertEquals("scaleCount", section.int("scaleCount"), library.scales.size)
        val titles = mutableListOf<String>()
        for (lesson in library.lessons) {
            titles.add(lesson.title)
        }
        assertEquals("lessonTitles", expectedTitles(), titles)
    }
}
