package com.alnourkhalifa.scales.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the contract between the Swift generator and the Kotlin readers: a section the
 * generator starts writing must get a reader here before the build is green again.
 */
class AnswersSectionsTest {
    // feature: roots-scales
    @Test
    fun everySectionInTheFileHasAReader() {
        val unknown =
            Answers.root.keys
                .filter { key -> key !in Answers.knownSections }
        assertTrue("sections with no Kotlin reader: $unknown", unknown.isEmpty())
    }

    // feature: roots-scales
    @Test
    fun everyKnownSectionIsInTheFile() {
        val missing =
            Answers.knownSections.keys
                .filter { key -> key !in Answers.root }
        assertTrue("sections the readers expect but the file lacks: $missing", missing.isEmpty())
    }

    // feature: roots-scales
    @Test
    fun theFormatIsTheOneTheReadersUnderstand() {
        val meta =
            Answers.section("meta").let { element ->
                element as kotlinx.serialization.json.JsonObject
            }
        assertEquals(1, meta.int("formatVersion"))
        assertEquals(Answers.DECIMALS, meta.int("decimals"))
    }
}
