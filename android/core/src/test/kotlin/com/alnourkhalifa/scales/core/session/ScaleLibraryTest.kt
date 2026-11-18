package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.ScaleDefinition
import com.alnourkhalifa.scales.core.music.ScaleDegree
import com.alnourkhalifa.scales.core.music.ScaleEdit
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.TransformationLesson
import com.alnourkhalifa.scales.core.music.definition
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

// The two suites of `ScalesTests/ScaleLibraryTests.swift`, one Kotlin class per Swift suite
// and test for test under the same names. The two TV-clip tests are iOS-only (AirPlay clips)
// and are not here. Each suite ends with the tests that are Kotlin's own.

/** G major with the 7th lowered: lands on G Mixolydian, a built-in scale. */
private fun gMixolydianLesson(): ScaleTransformation =
    ScaleTransformation(
        NoteName(Letter.G),
        ScaleType.MAJOR.definition,
        ScaleEdit.Lower(ScaleDegree.fromSymbol("7")),
    )

/** Major with the 5th raised (1 2 3 4 ♯5 6 7): no built-in scale has these notes. */
private fun raisedFifthLesson(root: NoteName = NoteName(Letter.G)): ScaleTransformation =
    ScaleTransformation(
        root,
        ScaleType.MAJOR.definition,
        ScaleEdit.Raise(ScaleDegree.fromSymbol("5")),
    )

/** Settings exactly as the first release saved them, line breaks included. */
private const val SAVED_BY_THE_FIRST_RELEASE =
    """{"root":{"letter":4,"accidental":0},"scaleType":"dorian","tuning":"standard",
"fretCount":15,"labelStyle":"noteNames","isLeftHanded":false,
"thinStringOnTop":true,"tvUsesDarkTheme":true}"""

/** Twin of `ScaleLibraryTests`. */
class ScaleLibraryTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** The library file in this test's own temporary folder, removed when the test ends. */
    private val libraryFile: File
        get() = File(folder.root, "Scales/ScaleLibrary.json")

    // feature: transform-lessons
    @Test
    fun startsEmptyWithoutAFile() {
        val library = ScaleLibrary(libraryFile)
        assertTrue(library.scales.isEmpty())
        assertTrue(library.lessons.isEmpty())
    }

    // feature: transform-lessons
    @Test
    fun aKnownResultIsFiledUnderTheBuiltInScale() {
        val library = ScaleLibrary(libraryFile)

        val lesson = library.saveLesson(gMixolydianLesson(), newScaleName = "")

        assertEquals(ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN), lesson.result)
        assertEquals("G Major → G Mixolydian", lesson.title)
        assertTrue(library.scales.isEmpty())
        assertEquals(listOf(lesson), library.lessons(ScaleReference.BuiltIn(ScaleType.MIXOLYDIAN)))
    }

    // feature: transform-lessons
    @Test
    fun aNewResultNeedsAName() {
        val library = ScaleLibrary(libraryFile)
        val transformation = raisedFifthLesson()

        val error =
            assertThrows(ScaleLibraryError::class.java) {
                library.saveLesson(transformation, newScaleName = "   ")
            }
        assertSame(ScaleLibraryError.EmptyName, error)
        assertTrue(library.lessons.isEmpty())
    }

    // feature: transform-lessons
    @Test
    fun aNewResultIsSavedAsTheirOwnScale() {
        val library = ScaleLibrary(libraryFile)

        val lesson = library.saveLesson(raisedFifthLesson(), newScaleName = " Augmented Major ")

        val saved = library.scales.first()
        assertEquals(1, library.scales.size)
        assertEquals("Augmented Major", saved.definition.name)
        assertEquals("1 2 3 4 ♯5 6 7", saved.definition.formula)
        assertEquals(ScaleReference.Custom(saved.id), lesson.result)
        assertEquals("G Major → G Augmented Major", lesson.title)
    }

    // feature: transform-lessons
    @Test
    fun savingTheSameNewScaleAgainFilesItUnderTheFirst() {
        val library = ScaleLibrary(libraryFile)
        val first = library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")

        val second =
            library.saveLesson(
                raisedFifthLesson(root = NoteName(Letter.D)),
                newScaleName = "Some Other Name",
            )

        assertEquals(1, library.scales.size)
        assertEquals(first.result, second.result)
        assertEquals("D Major → D Augmented Major", second.title)
    }

    // feature: transform-lessons
    @Test
    fun scalesAndLessonsSurviveARelaunch() {
        val firstLaunch = ScaleLibrary(libraryFile)
        firstLaunch.saveLesson(gMixolydianLesson(), newScaleName = "")
        firstLaunch.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")

        val secondLaunch = ScaleLibrary(libraryFile)

        assertEquals(firstLaunch.scales, secondLaunch.scales)
        assertEquals(firstLaunch.lessons, secondLaunch.lessons)
        assertEquals(2, secondLaunch.lessons.size)
    }

    // feature: transform-lessons
    @Test
    fun renamingUpdatesTheScaleAndItsLessons() {
        val library = ScaleLibrary(libraryFile)
        val lesson = library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        val id =
            library.scales
                .first()
                .id

        library.rename(id, newName = "Ionian ♯5")

        assertEquals(
            "Ionian ♯5",
            library
                .scale(id)
                ?.definition
                ?.name,
        )
        assertEquals(
            "1 2 3 4 ♯5 6 7",
            library
                .scale(id)
                ?.definition
                ?.formula,
        )
        assertEquals(
            lesson.id,
            library.lessons
                .first()
                .id,
        )
        assertEquals(
            "G Major → G Ionian ♯5",
            library.lessons
                .first()
                .title,
        )
    }

    // feature: transform-lessons
    @Test
    fun renamingRefusesABlankNameOrAnUnknownScale() {
        val library = ScaleLibrary(libraryFile)
        library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        val id =
            library.scales
                .first()
                .id

        val blank = assertThrows(ScaleLibraryError::class.java) { library.rename(id, newName = "") }
        assertSame(ScaleLibraryError.EmptyName, blank)
        val unknown =
            assertThrows(ScaleLibraryError::class.java) {
                library.rename(UUID.randomUUID(), newName = "Anything")
            }
        assertSame(ScaleLibraryError.ScaleNotFound, unknown)
        assertEquals(
            "Augmented Major",
            library
                .scale(id)
                ?.definition
                ?.name,
        )
    }

    // feature: transform-lessons
    @Test
    fun deletingAScaleRemovesItsLessonsOnly() {
        val library = ScaleLibrary(libraryFile)
        val kept = library.saveLesson(gMixolydianLesson(), newScaleName = "")
        library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        val id =
            library.scales
                .first()
                .id

        library.deleteScale(id)

        assertTrue(library.scales.isEmpty())
        assertEquals(listOf(kept), library.lessons)
        val again = assertThrows(ScaleLibraryError::class.java) { library.deleteScale(id) }
        assertSame(ScaleLibraryError.ScaleNotFound, again)
    }

    // feature: transform-lessons
    @Test
    fun deletingALesson() {
        val library = ScaleLibrary(libraryFile)
        val lesson = library.saveLesson(gMixolydianLesson(), newScaleName = "")

        library.deleteLesson(lesson.id)

        assertTrue(library.lessons.isEmpty())
        val again = assertThrows(ScaleLibraryError::class.java) { library.deleteLesson(lesson.id) }
        assertSame(ScaleLibraryError.LessonNotFound, again)
    }

    // feature: transform-lessons
    @Test
    fun anUnreadableFileIsSetAsideAndTheLibraryStartsEmpty() {
        libraryFile.parentFile.mkdirs()
        val garbage = "not a library".toByteArray()
        libraryFile.writeBytes(garbage)

        val library = ScaleLibrary(libraryFile)

        assertTrue(library.scales.isEmpty())
        val setAside = ScaleLibrary.unreadableCopyFile(libraryFile)
        assertArrayEquals(garbage, setAside.readBytes())
        assertFalse(libraryFile.exists())
    }

    // feature: transform-lessons
    @Test
    fun aFailedSaveLeavesTheLibraryUnchanged() {
        // A plain file where the library's folder should be, so the folder can't be made.
        val blocker = folder.newFile("blocker")
        val library = ScaleLibrary(File(blocker, "ScaleLibrary.json"))
        val transformation = gMixolydianLesson()

        val error =
            assertThrows(ScaleLibraryError::class.java) {
                library.saveLesson(transformation, newScaleName = "")
            }

        assertTrue("expected couldNotSave, got $error", error is ScaleLibraryError.CouldNotSave)
        assertTrue(library.lessons.isEmpty())
    }

    // Kotlin's own: the JSON round trips of `ScalesTests/TransformationTests.swift`, which
    // live with the serializers here, and the library's listener.

    // feature: roots-scales, transform-lessons
    @Test
    fun definitionsRoundTripThroughJSON() {
        val definition = ScaleType.PHRYGIAN_DOMINANT.definition
        val text = ScaleLibraryJson.encodeDefinition(definition)
        val decoded = ScaleLibraryJson.decodeDefinition(text)
        assertEquals(definition, decoded)
    }

    // feature: transform-lessons
    @Test
    fun lessonsRoundTripThroughJSON() {
        val transformation =
            ScaleTransformation(
                NoteName(Letter.E, -1),
                ScaleType.NATURAL_MINOR.definition,
                ScaleEdit.Raise(ScaleDegree.fromSymbol("♭7")),
            )
        val lesson =
            TransformationLesson(
                transformation = transformation.naming("Harmonic Minor"),
                result = ScaleReference.BuiltIn(ScaleType.HARMONIC_MINOR),
            )
        val text = ScaleLibraryJson.encodeLesson(lesson)
        val decoded = ScaleLibraryJson.decodeLesson(text)
        assertEquals(lesson, decoded)
        assertEquals("E♭ Natural Minor → E♭ Harmonic Minor", decoded.title)
    }

    // feature: transform-lessons
    @Test
    fun everySavedChangeIsAnnouncedAndARefusalIsNot() {
        val library = ScaleLibrary(libraryFile)
        var announcements = 0
        library.onChange = { announcements += 1 }

        library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        library.rename(
            library.scales
                .first()
                .id,
            newName = "Ionian ♯5",
        )
        assertThrows(ScaleLibraryError::class.java) { library.deleteLesson(UUID.randomUUID()) }

        assertEquals(2, announcements)
    }
}

/** Twin of `CustomScaleSettingsTests`: the board's settings and the player's own scales. */
class CustomScaleSettingsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun library(): ScaleLibrary = ScaleLibrary(File(folder.root, "ScaleLibrary.json"))

    // feature: settings-persist
    @Test
    fun settingsSavedBeforeCustomScalesStillLoad() {
        val settings = ScaleLibraryJson.decodeSettings(SAVED_BY_THE_FIRST_RELEASE)
        assertEquals(ScaleType.DORIAN, settings.scaleType)
        assertNull(settings.customScale)
        assertEquals(ScaleReference.BuiltIn(ScaleType.DORIAN), settings.scaleReference)
    }

    // feature: settings-persist
    @Test
    fun builtInSettingsAreSavedExactlyAsBefore() {
        val text = ScaleLibraryJson.encodeSettings(PracticeSettings())
        assertFalse(text.contains("customScale"))
    }

    // feature: transform-lessons, settings-persist
    @Test
    fun theBoardDrawsAChosenScaleAndRemembersIt() {
        val values = InMemoryKeyValueStore()
        val library = library()
        library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        val saved = library.scales.first()
        val session = PracticeSession(SettingsStore(values), library)

        session.choose(ScaleReference.Custom(saved.id))

        assertEquals("G Augmented Major", session.state.scale.title)
        assertEquals(ScaleReference.Custom(saved.id), session.state.settings.scaleReference)
        val names = mutableListOf<String>()
        for (tone in session.state.scale.tones) {
            names.add(tone.name.displayName)
        }
        assertEquals(listOf("G", "A", "B", "C", "D♯", "E", "F♯"), names)
        val relaunched = PracticeSession(SettingsStore(values), library)
        assertEquals("G Augmented Major", relaunched.state.scale.title)
    }

    // feature: roots-scales, transform-lessons
    @Test
    fun choosingABuiltInScaleOrAnUnknownIdBehaves() {
        val library = library()
        library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        val saved = library.scales.first()
        val session = PracticeSession(SettingsStore(InMemoryKeyValueStore()), library)
        session.choose(ScaleReference.Custom(saved.id))

        session.choose(ScaleReference.Custom(UUID.randomUUID()))
        assertEquals(
            "unknown id is ignored",
            ScaleReference.Custom(saved.id),
            session.state.settings.scaleReference,
        )

        session.choose(ScaleReference.BuiltIn(ScaleType.LYDIAN))
        assertNull(session.state.settings.customScale)
        assertEquals("G Lydian", session.state.scale.title)

        session.choose(ScaleReference.Custom(saved.id), root = NoteName(Letter.E, -1))
        assertEquals("E♭ Augmented Major", session.state.scale.title)
    }

    // feature: transform-lessons
    @Test
    fun renamingAndDeletingReachTheBoard() {
        val library = library()
        library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        val saved = library.scales.first()
        val session = PracticeSession(SettingsStore(InMemoryKeyValueStore()), library)
        session.updateSettings { settings -> settings.copy(scaleType = ScaleType.DORIAN) }
        session.choose(ScaleReference.Custom(saved.id))

        library.rename(saved.id, newName = "Ionian ♯5")
        session.refreshCustomScale()
        assertEquals("G Ionian ♯5", session.state.scale.title)

        library.deleteScale(saved.id)
        session.refreshCustomScale()
        assertNull(session.state.settings.customScale)
        assertEquals(
            "falls back to the built-in chosen before",
            "G Dorian",
            session.state.scale.title,
        )
    }

    // feature: settings-persist, keyboard
    @Test
    fun settingsSavedBeforeTheKeyboardDefaultToTheGuitar() {
        val settings = ScaleLibraryJson.decodeSettings(SAVED_BY_THE_FIRST_RELEASE)
        assertNull(settings.instrument)
        assertEquals(Instrument.GUITAR, settings.selectedInstrument)
    }

    // feature: settings-persist, keyboard
    @Test
    fun guitarSettingsAreSavedWithoutAnInstrumentKey() {
        val keyboard = PracticeSettings().withSelectedInstrument(Instrument.KEYBOARD)
        val settings = keyboard.withSelectedInstrument(Instrument.GUITAR)
        assertEquals("choosing the guitar again stores null", PracticeSettings(), settings)
        val text = ScaleLibraryJson.encodeSettings(settings)
        assertFalse(text.contains("instrument"))
        assertFalse(text.contains("keyboardOctaves"))
    }

    // Kotlin's own.

    // feature: transform-lessons
    @Test
    fun aRenamedScaleKeepsItsNameWhenTheLibraryIsReadAgain() {
        val file = File(folder.root, "ScaleLibrary.json")
        val library = ScaleLibrary(file)
        library.saveLesson(raisedFifthLesson(), newScaleName = "Augmented Major")
        library.rename(
            library.scales
                .first()
                .id,
            newName = "Ionian ♯5",
        )

        val reread = ScaleLibrary(file)

        val after =
            reread.lessons
                .first()
                .transformation.after
        assertEquals(ScaleDefinition("Ionian ♯5", after.degrees), after)
        assertEquals(
            "G Major → G Ionian ♯5",
            reread.lessons
                .first()
                .title,
        )
    }
}
