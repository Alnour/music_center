package com.alnourkhalifa.scales.core.music

import java.util.UUID

/**
 * A moment as Swift's `Date` stores and encodes it: seconds since 1 January 2001 (UTC), Apple's
 * reference date, which is what Swift's default JSON writes for a date. Keeping the same number
 * means a saved library moves between the iPhone and Android with no conversion.
 */
@JvmInline
value class ReferenceDate(
    val secondsSinceReferenceDate: Double,
) {
    /** Building a date from the clock. */
    companion object {
        /** 1 January 2001 00:00 UTC in Unix seconds, Foundation's
         *  `timeIntervalBetween1970AndReferenceDate`. */
        const val UNIX_SECONDS_AT_REFERENCE_DATE = 978_307_200.0

        private const val MILLISECONDS_PER_SECOND = 1000.0

        /** Now, Swift's `Date.now`. */
        fun now(): ReferenceDate {
            val unixSeconds = System.currentTimeMillis() / MILLISECONDS_PER_SECOND
            return ReferenceDate(unixSeconds - UNIX_SECONDS_AT_REFERENCE_DATE)
        }
    }
}

/**
 * Twin of `CustomScale` in `Scales/Music/TransformationLesson.swift`: a scale the player made
 * and named. Its notes never change after saving; only the name may, so a lesson that produced
 * it always describes it truthfully.
 *
 * Differs from Swift: `var definition` becomes `copy(definition = …)`; `createdAt` is a
 * [ReferenceDate] and `id` a [java.util.UUID].
 */
data class CustomScale(
    val id: UUID = UUID.randomUUID(),
    val definition: ScaleDefinition,
    val createdAt: ReferenceDate = ReferenceDate.now(),
)

/**
 * Twin of `ScaleReference` in `Scales/Music/TransformationLesson.swift`: points at a scale by
 * identity, a built-in one or one of the player's saved scales. [swiftDescription] is one
 * stable string per reference ("builtIn:major", "custom:<UUID in upper case>", the case Swift
 * prints a UUID in), for keys and test messages.
 */
sealed class ScaleReference {
    abstract val swiftDescription: String

    /** One of the app's own scales. */
    data class BuiltIn(
        val type: ScaleType,
    ) : ScaleReference() {
        override val swiftDescription: String
            get() = "builtIn:${type.rawValue}"
    }

    /** One of the player's saved scales, by its id. */
    data class Custom(
        val id: UUID,
    ) : ScaleReference() {
        override val swiftDescription: String
            get() = "custom:${id.toString().uppercase()}"
    }
}

/**
 * Twin of `ScaleTransformation` in `Scales/Music/TransformationLesson.swift`: one edit applied
 * to a scale in one key, with everything a lesson shows worked out from it, both scales
 * spelled and the chord changes between them.
 *
 * Differs from Swift: the public constructor works out [after] and throws [ScaleEditError] as
 * Swift's throwing initializer does. The four-field constructor is internal so the library's
 * JSON reader can rebuild a saved lesson whose result was renamed, as Swift's synthesized
 * decoder does.
 */
@ConsistentCopyVisibility
data class ScaleTransformation internal constructor(
    /** The key the lesson is taught in; the scales themselves work in any key. */
    val root: NoteName,
    val before: ScaleDefinition,
    val edit: ScaleEdit,
    val after: ScaleDefinition,
) {
    /** @throws ScaleEditError from [applying] when the edit cannot be made to this scale. */
    constructor(
        root: NoteName,
        before: ScaleDefinition,
        edit: ScaleEdit,
    ) : this(root, before, edit, before.applying(edit))

    /** The same transformation with the resulting scale called by its proper name, once it
     *  has been recognised or the player has named it. */
    fun naming(name: String): ScaleTransformation {
        val namedAfter = after.copy(name = name)
        return ScaleTransformation(root, before, edit, namedAfter)
    }

    val beforeScale: Scale
        get() = Scale(root, before)

    val afterScale: Scale
        get() = Scale(root, after)

    val chordChanges: ChordChangeReport
        get() = ChordChangeReport(beforeScale, afterScale)

    /** "Lowered F♯ (7) to F (♭7)." */
    val editSentence: String
        get() = edit.sentence(root)

    /** "G Major → G Mixolydian". */
    val title: String
        get() = "${beforeScale.title} → ${afterScale.title}"
}

/**
 * Twin of `TransformationLesson` in `Scales/Music/TransformationLesson.swift`: a transformation
 * the player kept, filed under the scale it led to. `id` is a [java.util.UUID] and `date` a
 * [ReferenceDate].
 */
data class TransformationLesson(
    val id: UUID = UUID.randomUUID(),
    val date: ReferenceDate = ReferenceDate.now(),
    val transformation: ScaleTransformation,
    val result: ScaleReference,
) {
    val title: String
        get() = transformation.title
}
