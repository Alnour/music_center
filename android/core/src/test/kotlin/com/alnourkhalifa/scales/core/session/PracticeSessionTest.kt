package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleDegree
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.letterTriads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Twin of `BoardTransformTests` in `ScalesTests/BoardTransformTests.swift`, test for test under
 * the same names: changing one note from the Scales screen, the board follows, the chords that
 * used the note change with it, and the change can be undone. Kotlin's own tests come last.
 */
class PracticeSessionTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val library: ScaleLibrary by lazy {
        ScaleLibrary(
            File(folder.root, "ScaleLibrary.json"),
        )
    }

    /** Settings that never touch the player's own, in a store of their own. */
    private fun freshSession(): PracticeSession =
        PracticeSession(SettingsStore(InMemoryKeyValueStore()), library)

    private val lowerTheSeventh = ScaleEdit.Lower(ScaleDegree.fromSymbol("7"))

    // feature: note-edit-revert
    @Test
    fun loweringTheSeventhPutsMixolydianOnTheBoard() {
        val session = freshSession()

        session.transform(lowerTheSeventh)

        assertEquals(
            ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN),
            session.state.settings.scaleReference,
        )
        assertEquals("G Mixolydian", session.state.scale.title)
        assertTrue(library.scales.isEmpty())
        assertEquals(1, library.lessons.size)
        val change = session.state.currentBoardChange
        assertNotNull(change)
        assertEquals(ScaleReference.BuiltIn(ScaleType.MAJOR), change?.before)
        assertEquals(ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN), change?.after)
    }

    // Every chord that had F♯ now has F instead: iii Bm becomes iii° B°, V D becomes v Dm,
    // vii° F♯° becomes ♭VII F. The others keep their notes.
    // feature: note-edit-revert, chord-chips-rings
    @Test
    fun chordsThatUsedTheOldNoteTakeTheNewOne() {
        val session = freshSession()
        val fSharp = NoteName(Letter.F, 1)
        val f = NoteName(Letter.F)

        session.transform(lowerTheSeventh)

        val change = requireNotNull(session.state.currentBoardChange)
        val changedNames = mutableListOf<String>()
        for (chordChange in change.transformation.chordChanges.changes) {
            val before = requireNotNull(chordChange.before)
            val after = requireNotNull(chordChange.after)
            if (fSharp !in before.notes) {
                assertEquals(ChordChange.Kind.UNCHANGED, chordChange.kind)
                assertEquals(before.notes, after.notes)
                continue
            }
            assertEquals(ChordChange.Kind.CHANGED, chordChange.kind)
            assertTrue(f in after.notes)
            assertFalse(fSharp in after.notes)
            assertEquals(listOf(fSharp), chordChange.notesGone)
            assertEquals(listOf(f), chordChange.notesNew)
            changedNames.add("${before.triad.name} → ${after.triad.name}")
        }
        assertEquals(listOf("Bm → B°", "D → Dm", "F♯° → F"), changedNames)
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun anUnknownResultIsSavedUnderItsWorkingName() {
        val session = freshSession()

        session.transform(ScaleEdit.Raise(ScaleDegree.fromSymbol("5")))

        val saved = library.scales.first()
        assertEquals("Major ♯5", saved.definition.name)
        assertEquals(ScaleReference.Custom(saved.id), session.state.settings.scaleReference)
        assertEquals("G Major ♯5", session.state.scale.title)
    }

    // feature: note-edit-revert
    @Test
    fun revertingPutsTheStartingScaleBack() {
        val session = freshSession()
        session.transform(lowerTheSeventh)

        session.revertBoardChange()

        assertEquals(ScaleReference.BuiltIn(ScaleType.MAJOR), session.state.settings.scaleReference)
        assertNull(session.state.currentBoardChange)
        assertEquals(1, library.lessons.size)
    }

    // feature: note-edit-revert
    @Test
    fun pickingAnotherScaleOrKeyDropsTheChange() {
        val session = freshSession()
        session.transform(lowerTheSeventh)
        assertNotNull(session.state.currentBoardChange)

        session.updateSettings { settings -> settings.copy(root = NoteName(Letter.A)) }
        assertNull(session.state.currentBoardChange)

        session.updateSettings { settings -> settings.copy(root = NoteName(Letter.G)) }
        assertNotNull(session.state.currentBoardChange)
        session.choose(ScaleReference.BuiltIn(ScaleType.MAJOR))
        assertNull(session.state.currentBoardChange)
    }

    // feature: note-edit-revert
    @Test
    fun anIllegalEditChangesNothing() {
        val session = freshSession()

        session.transform(ScaleEdit.Remove(ScaleDegree.fromSymbol("♭3")))

        assertEquals(ScaleReference.BuiltIn(ScaleType.MAJOR), session.state.settings.scaleReference)
        assertNull(session.state.currentBoardChange)
        assertTrue(library.lessons.isEmpty())
    }

    // feature: chord-chips-rings
    @Test
    fun pickingAChordRingsItsNotesUntilTheScaleLosesOne() {
        val session = freshSession()
        val dominant =
            session.state.scale
                .letterTriads()
                .first { chord -> chord.triad.name == "D" }

        session.toggleRing(dominant)
        val expected = setOf(NoteName(Letter.D), NoteName(Letter.F, 1), NoteName(Letter.A))
        assertEquals(expected, session.state.ringedNotes)

        // F♯ leaves the scale with the lowered seventh, so the rings go too.
        session.transform(lowerTheSeventh)
        assertTrue(
            session.state.ringedNotes
                .isEmpty(),
        )
        assertEquals(
            "the pick is kept while the scale changes",
            dominant,
            session.state.ringedChord,
        )

        session.toggleRing(dominant)
        assertNull(session.state.ringedChord)
    }

    // Kotlin's own: the listener and the saving that replace Swift's observation and didSet.

    // feature: note-edit-revert
    @Test
    fun oneNoteChangeReachesTheListenerOnceWithEverythingInIt() {
        val session = freshSession()
        val snapshots = mutableListOf<SessionState>()
        session.onChange = { state -> snapshots.add(state) }

        session.transform(lowerTheSeventh)
        session.choose(ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN))

        assertEquals("choosing the scale already shown changes nothing", 1, snapshots.size)
        assertEquals(session.state, snapshots.first())
        assertNotNull(snapshots.first().currentBoardChange)
    }

    // feature: settings-persist
    @Test
    fun settingsChangesAreSavedAndRingsAreNot() {
        val values = InMemoryKeyValueStore()
        val session = PracticeSession(SettingsStore(values), library)
        val tonic =
            session.state.scale
                .letterTriads()
                .first()

        session.toggleRing(tonic)
        assertTrue("a ring alone writes nothing", values.keys().isEmpty())

        session.updateSettings { settings -> settings.copy(fretCount = 24) }
        val relaunched = PracticeSession(SettingsStore(values), library)
        assertEquals(24, relaunched.state.settings.fretCount)
        assertNull("rings are kept for this run only", relaunched.state.ringedChord)
    }
}
