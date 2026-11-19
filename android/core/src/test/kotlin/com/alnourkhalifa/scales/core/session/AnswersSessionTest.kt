package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.board.BoardChordChipContent
import com.alnourkhalifa.scales.core.bool
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleChord
import com.alnourkhalifa.scales.core.music.ScaleDegree
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.boardChords
import com.alnourkhalifa.scales.core.music.letterTriads
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

/**
 * Reads the `session` section Swift wrote: a fresh session with an empty library driven
 * through the steps a player takes on the Scales screen, with what the session shows after
 * each. The same steps are replayed here, in the generator's order
 * (`scripts/ExpectedAnswers/SessionSections.swift`), and every field is compared.
 *
 * Two of Swift's quirks are pinned on purpose: the ringed chord stays picked when the scale
 * changes (step 2, no card marked and nothing ringed, since the chord is no longer one of the
 * cards), and a board change shows again when the player picks its scale in its key again
 * (step 7). The last three steps ring a blues scale's borrowed A7, whose C♯ stays unringed.
 */
class AnswersSessionTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** One player action and the step name Swift gave its snapshot. */
    private class Step(
        val name: String,
        val action: (PracticeSession) -> Unit,
    )

    /** The chord on a degree of the scale on the board, for tapping a card. */
    private fun chord(
        number: Int,
        session: PracticeSession,
    ): ScaleChord =
        session.state.scale.letterTriads().first { chord ->
            chord.degree.number ==
                number
        }

    private val unknownId = UUID.fromString("00000000-0000-0000-0000-0000000000FF")

    /** Every step in the generator's order: the G major walk, then the blues scale's. */
    private fun steps(): List<Step> = noteChangeSteps() + borrowedChordSteps()

    /** G major's chords ringed, a note changed and undone, and the I chord rung and cleared. */
    private fun noteChangeSteps(): List<Step> {
        var tonic: ScaleChord? = null
        return listOf(
            Step("fresh session") { },
            Step(
                "ring the V chord of G Major",
            ) { session -> session.toggleRing(chord(5, session)) },
            Step(
                "choose D Major: G's V chord is not a card here, so nothing is ringed",
            ) { session ->
                session.choose(ScaleReference.BuiltIn(ScaleType.MAJOR), NoteName(Letter.D))
            },
            Step("choose G Major again") { session ->
                session.choose(ScaleReference.BuiltIn(ScaleType.MAJOR), NoteName(Letter.G))
            },
            Step("lower the 7th: G Mixolydian, kept as a lesson") { session ->
                session.transform(ScaleEdit.Lower(ScaleDegree.fromSymbol("7")))
            },
            Step("choose A Minor Pentatonic: the change is no longer shown") { session ->
                session.choose(
                    ScaleReference.BuiltIn(ScaleType.MINOR_PENTATONIC),
                    NoteName(Letter.A),
                )
            },
            Step(
                "revert with no change on the board: nothing happens",
            ) { session -> session.revertBoardChange() },
            Step("choose G Mixolydian: the stored change shows again") { session ->
                session.choose(ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN), NoteName(Letter.G))
            },
            Step("revert: G Major, the lesson stays") { session -> session.revertBoardChange() },
            Step("choose an unknown saved scale: nothing changes") { session ->
                session.choose(ScaleReference.Custom(unknownId))
            },
            Step("ring the I chord") { session ->
                val picked = chord(1, session)
                tonic = picked
                session.toggleRing(picked)
            },
            Step(
                "ring the I chord again: the rings clear",
            ) { session -> session.toggleRing(requireNotNull(tonic)) },
        )
    }

    /** A blues scale's borrowed A7 rung, then a scale whose cards no longer hold it. */
    private fun borrowedChordSteps(): List<Step> =
        listOf(
            Step("choose A Blues: the cards are the twelve-bar seventh chords") { session ->
                session.choose(ScaleReference.BuiltIn(ScaleType.BLUES), NoteName(Letter.A))
            },
            Step("ring A7: only the notes the blues scale holds") { session ->
                val tonicSeventh =
                    session.state.scale
                        .boardChords()
                        .first()
                session.toggleRing(tonicSeventh)
            },
            Step(
                "choose A Minor Pentatonic: A7 is not a card here, so nothing is ringed",
            ) { session ->
                session.choose(ScaleReference.BuiltIn(ScaleType.MINOR_PENTATONIC))
            },
        )

    // feature: chord-chips-rings, note-edit-revert, chord-harmony
    @Test
    fun aScriptedSessionShowsWhatSwiftShows() {
        val answers = Answers.section("session").jsonArray
        val steps = steps()
        assertEquals("session step count", answers.size, steps.size)
        val library = ScaleLibrary(File(folder.root, "ScaleLibrary.json"))
        val session = PracticeSession(SettingsStore(InMemoryKeyValueStore()), library)
        for (index in steps.indices) {
            val step = steps[index]
            val expected = answers[index].jsonObject
            assertSame("step $index name", expected.string("step"), step.name)
            step.action(session)
            checkSnapshot(step.name, expected, session.state, library)
        }
    }

    private fun checkSnapshot(
        case: String,
        expected: JsonObject,
        state: SessionState,
        library: ScaleLibrary,
    ) {
        assertSame("$case scaleTitle", expected.string("scaleTitle"), state.scale.title)
        val settingsJson = Json.parseToJsonElement(ScaleLibraryJson.encodeSettings(state.settings))
        assertSame(
            "$case settingsJSON",
            Json.parseToJsonElement(expected.string("settingsJSON")),
            settingsJson,
        )
        assertSame(
            "$case ringedChord",
            expected.optionalString("ringedChord"),
            state.ringedChord?.spokenTitle,
        )
        assertSame(
            "$case ringedNotes",
            strings(expected.array("ringedNotes")),
            sortedNames(state.ringedNotes),
        )
        assertSame(
            "$case hasStoredBoardChange",
            expected.bool("hasStoredBoardChange"),
            state.boardChange != null,
        )
        checkBoardChange(case, expected, state.currentBoardChange)
        val chips = BoardChordChipContent.chips(state.scale, state.currentBoardChange)
        checkChips(case, expected.array("chips"), chips, state.ringedChord)
        checkLibrary(case, expected, library)
    }

    private fun checkBoardChange(
        case: String,
        expected: JsonObject,
        change: BoardChange?,
    ) {
        if (expected["currentBoardChange"] == null) {
            assertNull("$case currentBoardChange", change)
            return
        }
        val answer = expected.obj("currentBoardChange")
        val actual = requireNotNull(change) { "$case currentBoardChange is missing" }
        assertSame("$case change title", answer.string("title"), actual.transformation.title)
        assertSame(
            "$case change editSentence",
            answer.string("editSentence"),
            actual.transformation.editSentence,
        )
        assertSame("$case change before", answer.string("before"), actual.before.swiftDescription)
        assertSame("$case change after", answer.string("after"), actual.after.swiftDescription)
    }

    private fun checkLibrary(
        case: String,
        expected: JsonObject,
        library: ScaleLibrary,
    ) {
        assertSame("$case savedScaleCount", expected.int("savedScaleCount"), library.scales.size)
        val answers = expected.array("lessons")
        assertSame("$case lesson count", answers.size, library.lessons.size)
        for (index in answers.indices) {
            val answer = answers[index].jsonObject
            val lesson = library.lessons[index]
            assertSame("$case lesson $index title", answer.string("title"), lesson.title)
            assertSame(
                "$case lesson $index afterName",
                answer.string("afterName"),
                lesson.transformation.after.name,
            )
            assertSame(
                "$case lesson $index result",
                answer.string("result"),
                lesson.result.swiftDescription,
            )
        }
    }

    /** Swift's `sortedDisplayNames`: by pitch class, then letter, then accidental, since a
     *  set has no order of its own. */
    private fun sortedNames(notes: Set<NoteName>): List<String> {
        val order =
            compareBy<NoteName>(
                { note -> note.pitchClass },
                { note -> note.letter.rawValue },
                { note -> note.accidental },
            )
        val sorted = notes.sortedWith(order)
        val names = mutableListOf<String>()
        for (note in sorted) {
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

    /** Every card's words, Swift's `ChipAnswer`; shared with [AnswersChipsTest]'s reading. */
    private fun checkChips(
        case: String,
        expected: JsonArray,
        chips: List<BoardChordChipContent>,
        ringedChord: ScaleChord?,
    ) {
        assertSame("$case chip count", expected.size, chips.size)
        for (index in expected.indices) {
            val answer = expected[index].jsonObject
            val chip = chips[index]
            val chipCase = "$case chip $index"
            assertChipWords(chipCase, answer, chip)
            assertSame("$chipCase isRinged", answer.bool("isRinged"), chip.chord == ringedChord)
        }
    }
}

/** The words of one card against Swift's `ChipAnswer`, everything but `isRinged`. */
internal fun assertChipWords(
    case: String,
    answer: JsonObject,
    chip: BoardChordChipContent,
) {
    assertSame("$case formulaNumeral", answer.string("formulaNumeral"), chip.chord.formulaNumeral)
    assertSame("$case name", answer.string("name"), chip.chord.name)
    assertSame("$case notes", jsonStrings(answer.array("notes")), noteNames(chip.chord.notes))
    assertSame(
        "$case outsideNotes",
        jsonStrings(answer.array("outsideNotes")),
        noteNames(chip.outsideNotes),
    )
    assertSame("$case kind", answer.string("kind"), chip.kind.swiftName)
    assertSame("$case caption", answer.string("caption"), chip.caption)
    assertSame(
        "$case accessibilityLabel",
        answer.string("accessibilityLabel"),
        chip.accessibilityLabel,
    )
}

private fun noteNames(notes: List<NoteName>): List<String> {
    val names = mutableListOf<String>()
    for (note in notes) {
        names.add(note.displayName)
    }
    return names
}

private fun jsonStrings(array: JsonArray): List<String> {
    val values = mutableListOf<String>()
    for (element in array) {
        values.add(element.jsonPrimitive.content)
    }
    return values
}
