package com.alnourkhalifa.scales.core.board

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleDegree
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.definition
import com.alnourkhalifa.scales.core.session.BoardChange
import com.alnourkhalifa.scales.core.session.assertChipWords
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The chord cards under the board, built straight from a scale and a change rather than
 * through a session, against the cards Swift wrote in the `session` section: step 0 (G Major)
 * and step 4 (G Major with the 7th lowered, now G Mixolydian).
 */
class AnswersChipsTest {
    private val gMajor = Scale(NoteName(Letter.G), ScaleType.MAJOR)

    private val loweredSeventh: BoardChange
        get() {
            val transformation =
                ScaleTransformation(
                    NoteName(Letter.G),
                    ScaleType.MAJOR.definition,
                    ScaleEdit.Lower(ScaleDegree.fromSymbol("7")),
                )
            return BoardChange(
                transformation = transformation.naming(ScaleType.MIXOLYDIAN.displayName),
                before = ScaleReference.BuiltIn(ScaleType.MAJOR),
                after = ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN),
            )
        }

    private fun sessionChips(step: Int): JsonArray {
        val answer =
            Answers
                .section("session")
                .jsonArray[step]
                .jsonObject
        return answer.array("chips")
    }

    private fun checkAll(
        case: String,
        expected: JsonArray,
        chips: List<BoardChordChipContent>,
    ) {
        assertEquals("$case chip count", expected.size, chips.size)
        for (index in expected.indices) {
            assertChipWords("$case chip $index", expected[index].jsonObject, chips[index])
        }
    }

    // feature: chord-chips-rings
    @Test
    fun gMajorCardsMatchSwift() {
        val chips = BoardChordChipContent.chips(gMajor, change = null)
        checkAll("G Major", sessionChips(0), chips)
    }

    // feature: chord-chips-rings, note-edit-revert
    @Test
    fun cardsAfterLoweringTheSeventhMatchSwift() {
        val mixolydian = Scale(NoteName(Letter.G), ScaleType.MIXOLYDIAN)
        val chips = BoardChordChipContent.chips(mixolydian, loweredSeventh)
        checkAll("G Major lowered 7th", sessionChips(4), chips)
    }

    // feature: chord-chips-rings
    @Test
    fun theWordsOnTheCards() {
        val plain = BoardChordChipContent.chips(gMajor, change = null)
        val dominant = plain[4]
        assertEquals(" ", dominant.caption)
        assertEquals("V, D major: D, F♯, A", dominant.accessibilityLabel)

        val changed = BoardChordChipContent.chips(gMajor, loweredSeventh)
        val mediant = changed[2]
        assertEquals(ChordChange.Kind.CHANGED, mediant.kind)
        assertEquals("was Bm", mediant.caption)
        assertEquals("iii°, B diminished: B, D, F. Was B minor.", mediant.accessibilityLabel)
        assertEquals(listOf(NoteName(Letter.F)), mediant.newNotes)
        assertEquals("was F♯°", changed[6].caption)
    }

    // feature: chord-chips-rings
    @Test
    fun addedAndRemovedChordsSayNewAndGone() {
        val transformation =
            ScaleTransformation(
                NoteName(Letter.C),
                ScaleType.MAJOR.definition,
                ScaleEdit.Remove(ScaleDegree.fromSymbol("4")),
            )
        val change =
            BoardChange(
                transformation,
                ScaleReference.BuiltIn(ScaleType.MAJOR),
                ScaleReference.BuiltIn(ScaleType.MAJOR),
            )

        val chips = BoardChordChipContent.chips(transformation.afterScale, change)

        val kinds = mutableSetOf<ChordChange.Kind>()
        for (chip in chips) {
            kinds.add(chip.kind)
            if (chip.kind == ChordChange.Kind.REMOVED) {
                assertEquals("gone", chip.caption)
                assertEquals(true, chip.accessibilityLabel.endsWith(". Gone."))
            }
            if (chip.kind == ChordChange.Kind.ADDED) {
                assertEquals("new", chip.caption)
                assertEquals(true, chip.accessibilityLabel.endsWith(". New."))
            }
        }
        assertEquals("removing a note loses a chord", true, ChordChange.Kind.REMOVED in kinds)
    }
}
