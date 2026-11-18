package com.alnourkhalifa.scales.core.music

import kotlin.math.abs

/**
 * Twin of `ScaleEdit` in `Scales/Music/ScaleEdit.swift`: one change to a scale, lower or raise
 * a note by a semitone, remove it, or add a new one.
 *
 * Each degree is the one the edit acts on as the scale stands before the edit; for an added
 * note it is the new degree.
 *
 * Differs from Swift: a sealed class whose four subclasses are Swift's four cases, plus
 * [swiftName], the case name Swift's JSON uses as the key (`{"lower":{"_0":…}}`).
 */
sealed class ScaleEdit {
    abstract val degree: ScaleDegree

    /** Swift's case name: "lower", "raise", "remove" or "add". */
    abstract val swiftName: String

    /** Swift's `.lower`: the note a semitone lower, keeping its letter. */
    data class Lower(
        override val degree: ScaleDegree,
    ) : ScaleEdit() {
        override val swiftName: String = "lower"
    }

    /** Swift's `.raise`: the note a semitone higher, keeping its letter. */
    data class Raise(
        override val degree: ScaleDegree,
    ) : ScaleEdit() {
        override val swiftName: String = "raise"
    }

    /** Swift's `.remove`: the note taken out of the scale. */
    data class Remove(
        override val degree: ScaleDegree,
    ) : ScaleEdit() {
        override val swiftName: String = "remove"
    }

    /** Swift's `.add`: a new note, which [degree] names. */
    data class Add(
        override val degree: ScaleDegree,
    ) : ScaleEdit() {
        override val swiftName: String = "add"
    }

    /** Where the edited note ends up, or null when the note is removed. */
    val resultingDegree: ScaleDegree?
        get() =
            when (this) {
                is Lower -> degree.lowered()
                is Raise -> degree.raised()
                is Remove -> null
                is Add -> degree
            }

    /**
     * The change in formula shorthand, used for a scale's working name ("Major ♭7"): "♭7",
     * "♯4", "♮4" when a sharp or flat is taken away, "no 4", "add ♭6".
     */
    val shortLabel: String
        get() =
            when (this) {
                is Lower, is Raise -> movedLabel()
                is Remove -> "no ${degree.label}"
                is Add -> "add ${degree.label}"
            }

    /** A moved note is written as where it lands; landing on the plain degree needs a natural
     *  sign, or "4" alone would not say anything changed. */
    private fun movedLabel(): String {
        val result = resultingDegree ?: return ""
        if (result.alteration == 0) {
            return "♮${result.number}"
        }
        return result.label
    }

    /** The change in a sentence, with the notes spelled in the key it was made in:
     *  "Lowered F♯ (7) to F (♭7)." */
    fun sentence(above: NoteName): String {
        val note = Scale.spell(degree, above).displayName
        return when (this) {
            is Lower, is Raise -> movedSentence(note, above)
            is Remove -> "Removed $note (${degree.label})."
            is Add -> "Added $note (${degree.label})."
        }
    }

    private fun movedSentence(
        note: String,
        root: NoteName,
    ): String {
        val result = resultingDegree ?: return ""
        val verb = if (result < degree) "Lowered" else "Raised"
        val newNote = Scale.spell(result, root).displayName
        return "$verb $note (${degree.label}) to $newNote (${result.label})."
    }
}

/**
 * Twin of `ScaleEditError` in `Scales/Music/ScaleEdit.swift`: why an edit cannot be made.
 *
 * Differs from Swift: an exception, so [applying] and the [ScaleTransformation] constructor can
 * throw it as Swift's typed `throws(ScaleEditError)` does. Refusals are ordinary answers rather
 * than faults, so no stack trace is recorded. [swiftName] is the Swift case name.
 */
sealed class ScaleEditError(
    val swiftName: String,
) : Exception(swiftName, null, false, false) {
    /** The root is the note the whole scale is measured from, so it stays where it is. */
    data object RootCannotChange : ScaleEditError("rootCannotChange")

    /** Lowering, raising or removing a degree the scale doesn't have. */
    data object DegreeNotInScale : ScaleEditError("degreeNotInScale")

    /** Adding a degree the scale already has. */
    data object DegreeAlreadyInScale : ScaleEditError("degreeAlreadyInScale")

    /** A degree number outside 1 to 7. */
    data object NoSuchDegree : ScaleEditError("noSuchDegree")

    /** The edited note would sound the same as one already in the scale (♯4 beside ♭5). */
    data class SamePitchAsAnotherNote(
        val clashesWith: ScaleDegree,
    ) : ScaleEditError("samePitchAsAnotherNote")

    /** More than one flat or sharp on a degree: a 𝄫3 is just a 2, so it is spelled that way. */
    data object AlterationTooFar : ScaleEditError("alterationTooFar")

    /** Fewer than three notes is no longer a scale. */
    data object TooFewNotes : ScaleEditError("tooFewNotes")
}

/** The smallest scale an edit may leave behind; Swift's `ScaleDefinition.minimumNoteCount`. */
private const val MINIMUM_NOTE_COUNT = 3

/** One flat or one sharp is as far as a degree may be altered. */
private const val MAXIMUM_ALTERATION = 1

/**
 * The scale after one edit. The result keeps letter order and a working name, the old name
 * plus the change ("Major ♭7"), until it is identified or named.
 *
 * @throws ScaleEditError [ScaleEditError.RootCannotChange] for any edit on degree 1;
 *   [ScaleEditError.DegreeNotInScale] when lowering, raising or removing a missing degree;
 *   [ScaleEditError.DegreeAlreadyInScale] or [ScaleEditError.NoSuchDegree] for a bad addition;
 *   [ScaleEditError.AlterationTooFar] past one flat or sharp;
 *   [ScaleEditError.SamePitchAsAnotherNote] when the new note duplicates a pitch;
 *   [ScaleEditError.TooFewNotes] when a removal would leave fewer than three notes.
 */
fun ScaleDefinition.applying(edit: ScaleEdit): ScaleDefinition {
    val refusal = refusal(edit)
    if (refusal != null) {
        throw refusal
    }

    val remaining = degreesWithout(edit)
    val workingName = "$name ${edit.shortLabel}"
    val newDegree = edit.resultingDegree
    if (newDegree != null) {
        remaining.add(newDegree)
    }
    return ScaleDefinition(workingName, remaining)
}

/**
 * Every edit [applying] accepts, so the interface only offers changes that work: lower, raise
 * and remove for each note but the root, then each degree that could be added, in letter
 * order. Candidates are checked without throwing, since most refusals are expected.
 */
fun ScaleDefinition.legalEdits(): List<ScaleEdit> {
    val legal = mutableListOf<ScaleEdit>()
    for (candidate in editCandidates()) {
        if (refusal(candidate) == null) {
            legal.add(candidate)
        }
    }
    return legal
}

/** The edits [legalEdits] tries, in menu order: lower, raise and remove for every note but the
 *  root, then every degree 2 to 7 flat, natural and sharp as an addition. */
private fun ScaleDefinition.editCandidates(): List<ScaleEdit> {
    val candidates = mutableListOf<ScaleEdit>()
    for (degree in degrees) {
        if (degree.number == ScaleDegree.root.number) {
            continue
        }
        candidates.add(ScaleEdit.Lower(degree))
        candidates.add(ScaleEdit.Raise(degree))
        candidates.add(ScaleEdit.Remove(degree))
    }
    for (number in ScaleDegree.numbers) {
        if (number == ScaleDegree.root.number) {
            continue
        }
        for (alteration in -MAXIMUM_ALTERATION..MAXIMUM_ALTERATION) {
            candidates.add(ScaleEdit.Add(ScaleDegree(number, alteration)))
        }
    }
    return candidates
}

/** Why the edit cannot be made, or null when it can: first whether it may act on its degree,
 *  then whether what it leaves behind is still a scale. */
private fun ScaleDefinition.refusal(edit: ScaleEdit): ScaleEditError? {
    val targetRefusal = targetRefusal(edit)
    if (targetRefusal != null) {
        return targetRefusal
    }
    val remaining = degreesWithout(edit)
    return resultRefusal(edit, remaining)
}

/** The scale's degrees minus the one a lower, raise or remove acts on. An addition acts on a
 *  degree that is not there, so nothing is taken out. */
private fun ScaleDefinition.degreesWithout(edit: ScaleEdit): MutableList<ScaleDegree> {
    val remaining = degrees.toMutableList()
    if (edit !is ScaleEdit.Add) {
        remaining.remove(edit.degree)
    }
    return remaining
}

/** Whether the edit may act on its degree at all, checked in Swift's order: the root first,
 *  then whether the degree is present (or, for an addition, valid and absent). */
private fun ScaleDefinition.targetRefusal(edit: ScaleEdit): ScaleEditError? {
    val target = edit.degree
    val isAddition = edit is ScaleEdit.Add
    val isPresent = target in degrees
    return when {
        target.number == ScaleDegree.root.number -> ScaleEditError.RootCannotChange
        !isAddition && !isPresent -> ScaleEditError.DegreeNotInScale
        isAddition && target.number !in ScaleDegree.numbers -> ScaleEditError.NoSuchDegree
        isAddition && isPresent -> ScaleEditError.DegreeAlreadyInScale
        else -> null
    }
}

/** Whether what the edit leaves behind is still a scale: enough notes after a removal, and
 *  otherwise a new note with at most one accidental that sounds unlike every other note. */
private fun resultRefusal(
    edit: ScaleEdit,
    remaining: List<ScaleDegree>,
): ScaleEditError? {
    val newDegree = edit.resultingDegree
    if (newDegree == null) {
        val isTooSmall = remaining.size < MINIMUM_NOTE_COUNT
        return if (isTooSmall) ScaleEditError.TooFewNotes else null
    }
    if (abs(newDegree.alteration) > MAXIMUM_ALTERATION) {
        return ScaleEditError.AlterationTooFar
    }
    return clashRefusal(newDegree, remaining)
}

/** Two spellings of one pitch (♯4 beside ♭5) would put one note on the board twice, so the
 *  first remaining degree on the new note's pitch is named as the clash. */
private fun clashRefusal(
    newDegree: ScaleDegree,
    remaining: List<ScaleDegree>,
): ScaleEditError? {
    for (other in remaining) {
        if (other.pitchClassAboveRoot == newDegree.pitchClassAboveRoot) {
            return ScaleEditError.SamePitchAsAnotherNote(other)
        }
    }
    return null
}
