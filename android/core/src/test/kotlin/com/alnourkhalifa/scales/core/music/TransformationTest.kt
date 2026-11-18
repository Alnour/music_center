package com.alnourkhalifa.scales.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

// The suites of `ScalesTests/TransformationTests.swift` that cover edits, chord changes, the
// identifier and transformations, one Kotlin class per Swift suite and test for test under the
// same names. The JSON round trip belongs with the library's serializers and is tested there.
// Each suite ends with any test that is Kotlin's own.

/** Shorthand for degrees in the tests: `degree("♭7")`. */
private fun degree(symbol: String): ScaleDegree = ScaleDegree.fromSymbol(symbol)

/** Twin of `ScaleEditTests`: what one change does to a formula, and which changes are refused,
 *  plus the menu lists built from the accepted ones. */
class ScaleEditTest {
    private val major = ScaleType.MAJOR.definition

    // feature: note-edit-revert, transform-lessons
    @Test
    fun loweringTheSeventhOfMajorGivesMixolydian() {
        val result = major.applying(ScaleEdit.Lower(degree("7")))
        assertEquals("degrees", ScaleType.MIXOLYDIAN.degrees, result.degrees)
        assertEquals("name", "Major ♭7", result.name)
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun raisingTheFourthOfMajorGivesLydian() {
        val result = major.applying(ScaleEdit.Raise(degree("4")))
        assertEquals("degrees", ScaleType.LYDIAN.degrees, result.degrees)
        assertEquals("name", "Major ♯4", result.name)
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun takingAnAccidentalAwayIsWrittenAsANatural() {
        val edit = ScaleEdit.Lower(degree("♯4"))
        val result =
            ScaleType.LYDIAN.definition
                .applying(edit)
        assertEquals("degrees", ScaleType.MAJOR.degrees, result.degrees)
        assertEquals("shortLabel", "♮4", edit.shortLabel)
        assertEquals("name", "Lydian ♮4", result.name)
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun removingTheFourthOfMajorLeavesSixNotes() {
        val result = major.applying(ScaleEdit.Remove(degree("4")))
        assertEquals("formula", "1 2 3 5 6 7", result.formula)
        assertEquals("name", "Major no 4", result.name)
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun addingTheFourthToMajorPentatonicSortsItIntoPlace() {
        val result =
            ScaleType.MAJOR_PENTATONIC.definition
                .applying(ScaleEdit.Add(degree("4")))
        assertEquals("formula", "1 2 3 4 5 6", result.formula)
        assertEquals("name", "Major Pentatonic add 4", result.name)
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun rootCannotBeAlteredOrRemoved() {
        val root = ScaleDegree.root
        val rootEdits =
            listOf(
                ScaleEdit.Lower(root),
                ScaleEdit.Raise(root),
                ScaleEdit.Remove(root),
                ScaleEdit.Add(degree("♯1")),
            )
        for (edit in rootEdits) {
            assertRefused("$edit", ScaleEditError.RootCannotChange, major, edit)
        }
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun editingAMissingDegreeIsRefused() {
        assertRefused(
            "lower ♭3",
            ScaleEditError.DegreeNotInScale,
            major,
            ScaleEdit.Lower(degree("♭3")),
        )
        assertRefused(
            "remove ♯4",
            ScaleEditError.DegreeNotInScale,
            major,
            ScaleEdit.Remove(degree("♯4")),
        )
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun addingADegreeAlreadyThereIsRefused() {
        val lydian = ScaleType.LYDIAN.definition
        assertRefused(
            "add ♯4",
            ScaleEditError.DegreeAlreadyInScale,
            lydian,
            ScaleEdit.Add(degree("♯4")),
        )
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun addingADegreeBeyondTheSeventhIsRefused() {
        val eighth = ScaleDegree(number = 8, alteration = 0)
        assertRefused("add 8", ScaleEditError.NoSuchDegree, major, ScaleEdit.Add(eighth))
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun loweringPastOneFlatIsRefused() {
        val dorian = ScaleType.DORIAN.definition
        assertRefused(
            "lower ♭3",
            ScaleEditError.AlterationTooFar,
            dorian,
            ScaleEdit.Lower(degree("♭3")),
        )
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun aNoteMayNotSoundTheSameAsAnother() {
        val lydian = ScaleType.LYDIAN.definition
        val sameAsSharpFour = ScaleEditError.SamePitchAsAnotherNote(degree("♯4"))
        assertRefused("add ♭5 to Lydian", sameAsSharpFour, lydian, ScaleEdit.Add(degree("♭5")))
        val sameAsRoot = ScaleEditError.SamePitchAsAnotherNote(ScaleDegree.root)
        assertRefused("raise 7 of Major", sameAsRoot, major, ScaleEdit.Raise(degree("7")))
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun scalesKeepAtLeastThreeNotes() {
        val triad = ScaleDefinition("Triad", listOf(degree("1"), degree("3"), degree("5")))
        assertRefused("remove 3", ScaleEditError.TooFewNotes, triad, ScaleEdit.Remove(degree("3")))
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun majorOffersTheFiveMissingPitchesInBothSpellings() {
        val addedLabels = mutableListOf<String>()
        for (edit in major.legalEdits()) {
            if (edit is ScaleEdit.Add) {
                addedLabels.add(edit.degree.label)
            }
        }
        val expected = listOf("♭2", "♯2", "♭3", "♯4", "♭5", "♯5", "♭6", "♯6", "♭7")
        assertEquals("added labels", expected, addedLabels)
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun everyLegalEditAppliesAndKeepsPitchesDistinct() {
        for (type in ScaleType.entries) {
            val definition = type.definition
            for (edit in definition.legalEdits()) {
                val result = definition.applying(edit)
                val pitches = mutableSetOf<Int>()
                for (resultDegree in result.degrees) {
                    pitches.add(resultDegree.pitchClassAboveRoot)
                }
                val case = "${type.rawValue} $edit"
                assertEquals("$case repeats a pitch", result.degrees.size, pitches.size)
                assertEquals("$case starts on the root", ScaleDegree.root, result.degrees.first())
            }
        }
    }

    // feature: note-edit-revert, transform-lessons
    @Test
    fun editSentencesSpellNotesInTheKey() {
        assertEquals(
            "lower 7 in G",
            "Lowered F♯ (7) to F (♭7).",
            ScaleEdit.Lower(degree("7")).sentence(NoteName(Letter.G)),
        )
        assertEquals(
            "raise ♭3 in A",
            "Raised C (♭3) to C♯ (3).",
            ScaleEdit.Raise(degree("♭3")).sentence(NoteName(Letter.A)),
        )
        assertEquals(
            "remove 4 in C",
            "Removed F (4).",
            ScaleEdit.Remove(degree("4")).sentence(NoteName(Letter.C)),
        )
        assertEquals(
            "add ♭6 in E",
            "Added C (♭6).",
            ScaleEdit.Add(degree("♭6")).sentence(NoteName(Letter.E)),
        )
    }

    // feature: note-edit-revert
    @Test
    fun editChoicesSortEachNotesChangesAndTheAdditions() {
        val choices = EditChoices(major)
        val seventh = degree("7")
        val expectedSeventh = listOf(ScaleEdit.Lower(seventh), ScaleEdit.Remove(seventh))
        assertEquals("edits on 7", expectedSeventh, choices.edits(seventh))
        assertTrue("no edits on the root", choices.edits(ScaleDegree.root).isEmpty())
        assertEquals("additions", 9, choices.additions.size)
        val g = NoteName(Letter.G)
        assertEquals("lower 7 title", "Lower to F (♭7)", ScaleEdit.Lower(seventh).menuTitle(g))
        assertEquals("remove 7 title", "Remove F♯", ScaleEdit.Remove(seventh).menuTitle(g))
        assertEquals(
            "add ♭2 title",
            "Add D♭ (♭2)",
            ScaleEdit.Add(degree("♭2")).menuTitle(NoteName(Letter.C)),
        )
    }

    private fun assertRefused(
        case: String,
        expected: ScaleEditError,
        definition: ScaleDefinition,
        edit: ScaleEdit,
    ) {
        val error = assertThrows(case, ScaleEditError::class.java) { definition.applying(edit) }
        assertEquals(case, expected, error)
    }
}

/** Twin of `ChordChangeReportTests`: the chord-by-chord story of an edit. */
class ChordChangeReportTest {
    // feature: transform-lessons, note-edit-revert
    @Test
    fun loweringTheSeventhOfCMajor() {
        val result = report(NoteName(Letter.C), ScaleType.MAJOR, ScaleEdit.Lower(degree("7")))
        val changes = result.changes
        assertEquals("count", 7, changes.size)
        val expectedKinds =
            listOf(UNCHANGED, UNCHANGED, CHANGED, UNCHANGED, CHANGED, UNCHANGED, CHANGED)
        assertEquals("kinds", expectedKinds, kinds(result))
        assertEquals("I", "I, C major, stays the same: C, E, G.", changes[0].summary)
        assertEquals(
            "iii after",
            "E°",
            changes[2]
                .after
                ?.triad
                ?.name,
        )
        assertEquals(
            "V",
            "V, G major, becomes v, G minor: it had G, B, D and now has G, B♭, D.",
            changes[4].summary,
        )
        val seventh = changes[6]
        assertEquals(
            "vii before",
            "B°",
            seventh.before
                ?.triad
                ?.name,
        )
        assertEquals(
            "vii after",
            "B♭",
            seventh.after
                ?.triad
                ?.name,
        )
        assertEquals("notesGone", listOf(NoteName(Letter.B)), seventh.notesGone)
        assertEquals("notesNew", listOf(NoteName(Letter.B, -1)), seventh.notesNew)
    }

    // feature: transform-lessons, note-edit-revert
    @Test
    fun removingTheFourthDropsOrSuspendsTheChordsThatNeededIt() {
        val result = report(NoteName(Letter.C), ScaleType.MAJOR, ScaleEdit.Remove(degree("4")))
        val expectedKinds =
            listOf(UNCHANGED, CHANGED, UNCHANGED, REMOVED, UNCHANGED, UNCHANGED, REMOVED)
        assertEquals("kinds", expectedKinds, kinds(result))
        assertEquals(
            "ii",
            "ii, D minor, becomes IIsus4, D suspended fourth: it had D, F, A " +
                "and now has D, G, A.",
            result.changes[1].summary,
        )
        assertEquals("IV notesGone", listOf(NoteName(Letter.F)), result.changes[3].notesGone)
        assertEquals(
            "vii°",
            "vii°, B diminished, is gone: it needed F, which is no longer in the scale.",
            result.changes[6].summary,
        )
    }

    // feature: transform-lessons, note-edit-revert
    @Test
    fun addingTheFourthToPentatonicMakesNewChordsAndResolvesASuspension() {
        val result =
            report(NoteName(Letter.C), ScaleType.MAJOR_PENTATONIC, ScaleEdit.Add(degree("4")))
        assertEquals(
            "kinds",
            listOf(UNCHANGED, CHANGED, ADDED, UNCHANGED, UNCHANGED),
            kinds(result),
        )
        assertEquals(
            "IIsus4",
            "IIsus4, D suspended fourth, becomes ii, D minor: it had D, G, A " +
                "and now has D, F, A.",
            result.changes[1].summary,
        )
        assertEquals(
            "IV after",
            "F",
            result.changes[2]
                .after
                ?.triad
                ?.name,
        )
        // There is still no B, so the chord on G keeps its suspended fourth.
        assertEquals(
            "V after",
            "Gsus4",
            result.changes[3]
                .after
                ?.triad
                ?.name,
        )
    }

    // feature: transform-lessons, note-edit-revert
    @Test
    fun raisingTheFlatThreeFollowsTheChordOntoItsNewRoot() {
        val result =
            report(NoteName(Letter.A), ScaleType.NATURAL_MINOR, ScaleEdit.Raise(degree("♭3")))
        val third = result.changes[2]
        assertEquals("kind", CHANGED, third.kind)
        assertEquals(
            "summary",
            "♭III, C major, becomes iii°, C♯ diminished: it had C, E, G and now has C♯, E, G.",
            third.summary,
        )
        assertEquals("notesGone", listOf(NoteName(Letter.C)), third.notesGone)
        assertEquals("notesNew", listOf(NoteName(Letter.C, 1)), third.notesNew)
        assertEquals(
            "i after",
            "A",
            result.changes[0]
                .after
                ?.triad
                ?.name,
        )
        assertEquals(
            "VI after",
            "F+",
            result.changes[5]
                .after
                ?.triad
                ?.name,
        )
    }

    // feature: transform-lessons, note-edit-revert
    @Test
    fun loweringTheFifthFlattensTheHomeChord() {
        val result = report(NoteName(Letter.C), ScaleType.MAJOR, ScaleEdit.Lower(degree("5")))
        assertEquals("kind", CHANGED, result.changes[0].kind)
        assertEquals(
            "summary",
            "I, C major, becomes I(♭5), C major flat five: it had C, E, G " +
                "and now has C, E, G♭.",
            result.changes[0].summary,
        )
        assertEquals(
            "after",
            "C(♭5)",
            result.changes[0]
                .after
                ?.triad
                ?.name,
        )
    }

    // feature: transform-lessons, note-edit-revert
    @Test
    fun raisingTheFifthOfAMinorChordSharpensIt() {
        val result = report(NoteName(Letter.A), ScaleType.DORIAN, ScaleEdit.Raise(degree("5")))
        assertEquals(
            "name",
            "Am(♯5)",
            result.changes[0]
                .after
                ?.triad
                ?.name,
        )
        assertEquals(
            "formulaNumeral",
            "i(♯5)",
            result.changes[0]
                .after
                ?.formulaNumeral,
        )
    }

    // feature: transform-lessons, note-edit-revert
    @Test
    fun identicalScalesReportNoChange() {
        val scale = Scale(NoteName(Letter.E), ScaleType.DORIAN)
        val report = ChordChangeReport(scale, scale)
        assertFalse("changesAnyChord", report.changesAnyChord)
        assertEquals("count", 7, report.changes.size)
    }

    private fun report(
        root: NoteName,
        type: ScaleType,
        edit: ScaleEdit,
    ): ChordChangeReport {
        val transformation = ScaleTransformation(root, type.definition, edit)
        return transformation.chordChanges
    }

    private fun kinds(report: ChordChangeReport): List<ChordChange.Kind> {
        val kinds = mutableListOf<ChordChange.Kind>()
        for (change in report.changes) {
            kinds.add(change.kind)
        }
        return kinds
    }

    private companion object {
        val UNCHANGED = ChordChange.Kind.UNCHANGED
        val CHANGED = ChordChange.Kind.CHANGED
        val REMOVED = ChordChange.Kind.REMOVED
        val ADDED = ChordChange.Kind.ADDED
    }
}

/** Twin of `ScaleIdentifierTests`: naming an edited scale from its sound. */
class ScaleIdentifierTest {
    private val major = ScaleType.MAJOR.definition

    // feature: transform-lessons
    @Test
    fun majorIsCalledMajorNotIonian() {
        val ionianMatch = ScaleIdentifier.match(ScaleType.IONIAN.definition, emptyList())
        assertEquals("Ionian", ScaleMatch.BuiltIn(ScaleType.MAJOR), ionianMatch)
        val aeolianMatch = ScaleIdentifier.match(ScaleType.AEOLIAN.definition, emptyList())
        assertEquals("Aeolian", ScaleMatch.BuiltIn(ScaleType.NATURAL_MINOR), aeolianMatch)
    }

    // feature: transform-lessons
    @Test
    fun majorWithAFlatSeventhIsMixolydian() {
        val edited = major.applying(ScaleEdit.Lower(degree("7")))
        val match = ScaleIdentifier.match(edited, emptyList())
        assertEquals("match", ScaleMatch.BuiltIn(ScaleType.MIXOLYDIAN), match)
        assertEquals("name", "Mixolydian", match?.name)
        assertEquals("reference", ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN), match?.reference)
    }

    // feature: transform-lessons
    @Test
    fun aRespelledScaleIsStillRecognised() {
        val respelled =
            ScaleDefinition(
                "Respelled",
                listOf(
                    degree("1"),
                    degree("2"),
                    degree("3"),
                    degree("♭5"),
                    degree("♭6"),
                    degree("♭7"),
                ),
            )
        val match = ScaleIdentifier.match(respelled, emptyList())
        assertEquals("match", ScaleMatch.BuiltIn(ScaleType.WHOLE_TONE), match)
    }

    // feature: transform-lessons
    @Test
    fun savedScalesAreRecognisedAndNewOnesAreNot() {
        val edited = major.applying(ScaleEdit.Raise(degree("5")))
        assertNull("unsaved", ScaleIdentifier.match(edited, emptyList()))
        val saved = CustomScale(definition = ScaleDefinition("Augmented Major", edited.degrees))
        val match = ScaleIdentifier.match(edited, listOf(saved))
        assertEquals("match", ScaleMatch.Custom(saved), match)
        assertEquals("name", "Augmented Major", match?.name)
        assertEquals("reference", ScaleReference.Custom(saved.id), match?.reference)
    }
}

/** Twin of `ScaleTransformationTests`: a whole lesson, one edit in one key, named and kept,
 *  plus the descriptions a lesson's result reference gives. */
class ScaleTransformationTest {
    private val major = ScaleType.MAJOR.definition

    // feature: transform-lessons
    @Test
    fun gMajorWithALoweredSeventh() {
        val transformation =
            ScaleTransformation(NoteName(Letter.G), major, ScaleEdit.Lower(degree("7")))
        assertEquals("working name", "Major ♭7", transformation.after.name)
        assertEquals("sentence", "Lowered F♯ (7) to F (♭7).", transformation.editSentence)
        val afterNames = mutableListOf<String>()
        for (tone in transformation.afterScale.tones) {
            afterNames.add(tone.name.displayName)
        }
        assertEquals("after notes", listOf("G", "A", "B", "C", "D", "E", "F"), afterNames)
        val named = transformation.naming("Mixolydian")
        assertEquals("title", "G Major → G Mixolydian", named.title)
        assertEquals("degrees kept", transformation.after.degrees, named.after.degrees)
    }

    // feature: transform-lessons
    @Test
    fun anImpossibleEditThrows() {
        val error =
            assertThrows(ScaleEditError::class.java) {
                ScaleTransformation(NoteName(Letter.G), major, ScaleEdit.Remove(ScaleDegree.root))
            }
        assertEquals("error", ScaleEditError.RootCannotChange, error)
    }

    // feature: transform-lessons
    @Test
    fun scaleReferencesDescribeThemselvesAsSwiftDoes() {
        val builtIn = ScaleReference.BuiltIn(ScaleType.NATURAL_MINOR)
        assertEquals("built-in", "builtIn:naturalMinor", builtIn.swiftDescription)
        val id = UUID.fromString("0000abcd-0000-0000-0000-00000000000f")
        val custom = ScaleReference.Custom(id)
        assertEquals(
            "custom",
            "custom:0000ABCD-0000-0000-0000-00000000000F",
            custom.swiftDescription,
        )
    }
}
