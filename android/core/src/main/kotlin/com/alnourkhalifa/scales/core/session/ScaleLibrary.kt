package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.CustomScale
import com.alnourkhalifa.scales.core.music.ScaleDefinition
import com.alnourkhalifa.scales.core.music.ScaleIdentifier
import com.alnourkhalifa.scales.core.music.ScaleReference
import com.alnourkhalifa.scales.core.music.ScaleTransformation
import com.alnourkhalifa.scales.core.music.TransformationLesson
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Twin of `ScaleLibraryError` in `Scales/Session/ScaleLibrary.swift`: why the library refused
 * a change. The library is left exactly as it was.
 *
 * Differs from Swift: an exception, so the library can throw it as Swift's typed
 * `throws(ScaleLibraryError)` does, shaped like [com.alnourkhalifa.scales.core.music.ScaleEditError].
 * The three refusals are ordinary answers to a player's tap rather than faults, so they are
 * singletons with no stack trace; [CouldNotSave] is a real failure and keeps its cause.
 */
sealed class ScaleLibraryError(
    message: String,
    cause: Throwable? = null,
    recordsStackTrace: Boolean = false,
) : Exception(message, cause, false, recordsStackTrace) {
    /** A new scale, or a renamed one, needs a name with something other than spaces in it. */
    data object EmptyName : ScaleLibraryError("emptyName")

    /** No saved scale has the id given. */
    data object ScaleNotFound : ScaleLibraryError("scaleNotFound")

    /** No lesson has the id given. */
    data object LessonNotFound : ScaleLibraryError("lessonNotFound")

    /** The file could not be written, with the system's reason. */
    class CouldNotSave(
        val reason: String,
        cause: Throwable? = null,
    ) : ScaleLibraryError("couldNotSave: $reason", cause, recordsStackTrace = true)
}

/**
 * Twin of `ScaleLibrary.LibraryDocument` in `Scales/Session/ScaleLibrary.swift`: the shape of
 * the file. The version is there so a later format can tell old files apart.
 */
data class LibraryDocument(
    val version: Int = 1,
    val scales: List<CustomScale> = emptyList(),
    val lessons: List<TransformationLesson> = emptyList(),
)

/**
 * Twin of `ScaleLibrary` in `Scales/Session/ScaleLibrary.swift`: the player's own scales and
 * the lessons that led to them, kept on this device.
 *
 * Everything lives in one small JSON file, written whole after each change. One file keeps
 * scales and lessons consistent with each other (a lesson is never saved without the scale it
 * is filed under). Writes are synchronous: the file is a few kilobytes and only changes when
 * the player taps something.
 *
 * Differs from Swift: [onChange] replaces `@Observable`; the app calls it to refresh its
 * screens and the board's copy of a saved scale ([PracticeSession.refreshCustomScale]).
 * Errors are thrown as [ScaleLibraryError] subclasses.
 *
 * @param file the library file; [standardFile] gives the app's own.
 */
class ScaleLibrary(
    private val file: File,
) {
    /** In the order they were made. */
    var scales: List<CustomScale>
        private set

    /** In the order they were saved, oldest first. */
    var lessons: List<TransformationLesson>
        private set

    /** Called after every change that reached the file. */
    var onChange: (() -> Unit)? = null

    init {
        val document = loadDocument(file)
        scales = document.scales
        lessons = document.lessons
    }

    fun scale(id: UUID): CustomScale? {
        for (scale in scales) {
            if (scale.id == id) {
                return scale
            }
        }
        return null
    }

    /** Lessons that ended on the given scale, oldest first. */
    fun lessons(reference: ScaleReference): List<TransformationLesson> {
        val matching = mutableListOf<TransformationLesson>()
        for (lesson in lessons) {
            if (lesson.result == reference) {
                matching.add(lesson)
            }
        }
        return matching
    }

    /**
     * Keeps a transformation as a lesson and returns it.
     *
     * When the resulting scale is already known, built in or saved, the lesson is filed under
     * that scale and takes its name; [newScaleName] is not used. Otherwise the result is saved
     * as a new scale under [newScaleName], trimmed of spaces.
     *
     * @throws ScaleLibraryError.EmptyName when the result is new and the name is blank.
     * @throws ScaleLibraryError.CouldNotSave when the file can't be written.
     */
    fun saveLesson(
        transformation: ScaleTransformation,
        newScaleName: String,
    ): TransformationLesson {
        val updatedScales = scales.toMutableList()
        val named: ScaleTransformation
        val result: ScaleReference
        val match = ScaleIdentifier.match(transformation.after, scales)
        if (match != null) {
            named = transformation.naming(match.name)
            result = match.reference
        } else {
            val name = validName(newScaleName)
            val definition = ScaleDefinition(name, transformation.after.degrees)
            val newScale = CustomScale(definition = definition)
            updatedScales.add(newScale)
            named = transformation.naming(name)
            result = ScaleReference.Custom(newScale.id)
        }
        val lesson = TransformationLesson(transformation = named, result = result)
        val updatedLessons = lessons + lesson
        commit(updatedScales, updatedLessons)
        return lesson
    }

    /**
     * Renames a saved scale. Lessons filed under it take the new name too; its notes never
     * change.
     *
     * @throws ScaleLibraryError.EmptyName, [ScaleLibraryError.ScaleNotFound] or
     *   [ScaleLibraryError.CouldNotSave].
     */
    fun rename(
        id: UUID,
        newName: String,
    ) {
        val name = validName(newName)
        val existing = scale(id) ?: throw ScaleLibraryError.ScaleNotFound
        val renamedScale = existing.copy(definition = existing.definition.copy(name = name))
        val updatedScales = mutableListOf<CustomScale>()
        for (scale in scales) {
            val kept = if (scale.id == id) renamedScale else scale
            updatedScales.add(kept)
        }
        val filedUnder = ScaleReference.Custom(id)
        val updatedLessons = mutableListOf<TransformationLesson>()
        for (lesson in lessons) {
            if (lesson.result != filedUnder) {
                updatedLessons.add(lesson)
                continue
            }
            val renamedTransformation = lesson.transformation.naming(name)
            updatedLessons.add(lesson.copy(transformation = renamedTransformation))
        }
        commit(updatedScales, updatedLessons)
    }

    /**
     * Deletes a saved scale together with the lessons filed under it.
     *
     * @throws ScaleLibraryError.ScaleNotFound or [ScaleLibraryError.CouldNotSave].
     */
    fun deleteScale(id: UUID) {
        if (scale(id) == null) {
            throw ScaleLibraryError.ScaleNotFound
        }
        val updatedScales = mutableListOf<CustomScale>()
        for (scale in scales) {
            if (scale.id != id) {
                updatedScales.add(scale)
            }
        }
        val filedUnder = ScaleReference.Custom(id)
        val updatedLessons = mutableListOf<TransformationLesson>()
        for (lesson in lessons) {
            if (lesson.result != filedUnder) {
                updatedLessons.add(lesson)
            }
        }
        commit(updatedScales, updatedLessons)
    }

    /** @throws ScaleLibraryError.LessonNotFound or [ScaleLibraryError.CouldNotSave]. */
    fun deleteLesson(id: UUID) {
        val updatedLessons = mutableListOf<TransformationLesson>()
        for (lesson in lessons) {
            if (lesson.id != id) {
                updatedLessons.add(lesson)
            }
        }
        if (updatedLessons.size == lessons.size) {
            throw ScaleLibraryError.LessonNotFound
        }
        commit(scales, updatedLessons)
    }

    /**
     * Writes the new state to disk first and only then shows it, so a failed write leaves
     * both the file and the library as they were.
     */
    private fun commit(
        newScales: List<CustomScale>,
        newLessons: List<TransformationLesson>,
    ) {
        val document = LibraryDocument(scales = newScales, lessons = newLessons)
        val text = ScaleLibraryJson.encodeDocument(document)
        try {
            writeAtomically(text)
        } catch (error: IOException) {
            val reason = error.message ?: error.javaClass.simpleName
            throw ScaleLibraryError.CouldNotSave(reason, error)
        }
        scales = newScales
        lessons = newLessons
        onChange?.invoke()
    }

    /**
     * The bytes go to a temporary file beside the library and are then moved over it, the
     * same as Swift's `.atomic` write: a crash mid-write never leaves half a library. The
     * temporary file is removed whatever happens, so a failed save leaves nothing behind.
     * `Files.write` rather than `writeString`, which Android only has from API 33.
     */
    private fun writeAtomically(text: String) {
        val folderFile = file.absoluteFile.parentFile
        val folder = folderFile.toPath()
        Files.createDirectories(folder)
        val temporary = Files.createTempFile(folder, file.name, ".tmp")
        try {
            Files.write(temporary, text.toByteArray(Charsets.UTF_8))
            Files.move(
                temporary,
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    /** Swift's static members. */
    companion object {
        /** The app's library, `filesDir/Scales/ScaleLibrary.json`, shared by the phone and TV
         *  activities. */
        fun standardFile(filesDir: File): File = File(filesDir, "Scales/ScaleLibrary.json")

        /** Where an unreadable library file is moved: "ScaleLibrary-unreadable.json" beside it. */
        fun unreadableCopyFile(file: File): File {
            val baseName = file.nameWithoutExtension
            return File(file.absoluteFile.parentFile, "$baseName-unreadable.json")
        }

        private fun validName(name: String): String {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                throw ScaleLibraryError.EmptyName
            }
            return trimmed
        }

        /**
         * Opens the library stored in the file. A missing or unreadable-from-disk file is an
         * empty library. A file whose contents can't be decoded is moved aside, next to it,
         * rather than overwritten, and the library starts empty.
         */
        private fun loadDocument(file: File): LibraryDocument {
            val text =
                try {
                    file.readText()
                } catch (ignored: IOException) {
                    return LibraryDocument()
                }
            return try {
                ScaleLibraryJson.decodeDocument(text)
            } catch (ignored: IllegalArgumentException) {
                setAside(file)
                LibraryDocument()
            }
        }

        private fun setAside(file: File) {
            val copy = unreadableCopyFile(file)
            try {
                Files.move(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (ignored: IOException) {
                // Like Swift's `try?`: the library still opens empty; the next save replaces
                // the file it could not move.
            }
        }
    }
}
