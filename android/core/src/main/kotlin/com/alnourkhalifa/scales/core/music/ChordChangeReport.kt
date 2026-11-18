package com.alnourkhalifa.scales.core.music

/** Twin of `ChordChange` in `Scales/Music/ChordChangeReport.swift`: what one edit did to one
 *  chord of the scale. */
data class ChordChange(
    val kind: Kind,
    val before: ScaleChord?,
    val after: ScaleChord?,
    /** Notes the chord had before and lost, for highlighting. */
    val notesGone: List<NoteName>,
    /** Notes the chord has now and didn't have, for highlighting. */
    val notesNew: List<NoteName>,
    /** The change told the way a teacher would say it. */
    val summary: String,
) {
    /** Twin of `ChordChange.Kind`; [swiftName] is the Swift case name. */
    enum class Kind(
        val swiftName: String,
    ) {
        /** Same chord, same notes. */
        UNCHANGED("unchanged"),

        /** A chord on the same letter is still there, but its notes or quality moved. */
        CHANGED("changed"),

        /** The scale no longer makes a chord on this letter. */
        REMOVED("removed"),

        /** The scale now makes a chord on a letter that had none. */
        ADDED("added"),
    }
}

/**
 * Twin of `ChordChangeReport` in `Scales/Music/ChordChangeReport.swift`: the lesson's heart,
 * every chord of the scale before and after one edit, side by side.
 *
 * Chords are matched by the letter they are built on, because that is how a musician follows a
 * chord through a change: the chord on the fifth step was G and is now Gm. When the edit moves
 * the note a chord is built on (♭3 raised to 3), the old and new chords on that letter are
 * still matched, so ♭III C becomes iii C♯° rather than one chord vanishing and another
 * appearing.
 */
class ChordChangeReport(
    before: Scale,
    after: Scale,
) {
    /** One entry per letter that has a chord before or after, in letter order. */
    val changes: List<ChordChange>

    init {
        val beforeByNumber = chordsByLetter(before)
        val afterByNumber = chordsByLetter(after)
        val afterNoteNames = mutableSetOf<NoteName>()
        for (tone in after.tones) {
            afterNoteNames.add(tone.name)
        }

        val changes = mutableListOf<ChordChange>()
        for (number in ScaleDegree.numbers) {
            val oldChords = beforeByNumber[number] ?: emptyList()
            val newChords = afterByNumber[number] ?: emptyList()
            changes.addAll(letterChanges(oldChords, newChords, afterNoteNames))
        }
        this.changes = changes
    }

    /** True when at least one chord changed, appeared or disappeared. */
    val changesAnyChord: Boolean
        get() {
            for (change in changes) {
                if (change.kind != ChordChange.Kind.UNCHANGED) {
                    return true
                }
            }
            return false
        }

    private companion object {
        /** A scale's chords grouped by the letter (degree number) they are built on; a letter
         *  holds two chords when the scale has two tones on it, as blues has ♭5 and 5. */
        fun chordsByLetter(scale: Scale): Map<Int, List<ScaleChord>> {
            val chordsByNumber = mutableMapOf<Int, MutableList<ScaleChord>>()
            for (chord in scale.letterTriads()) {
                val letterChords = chordsByNumber.getOrPut(chord.degree.number) { mutableListOf() }
                letterChords.add(chord)
            }
            return chordsByNumber
        }

        /**
         * The changes on one letter. Chords on the same degree pair first; when exactly one old
         * and one new chord are left over, they pair too, because the edit moved the note the
         * chord is built on. Anything still unpaired was removed or added.
         */
        fun letterChanges(
            oldChords: List<ScaleChord>,
            newChords: List<ScaleChord>,
            afterNoteNames: Set<NoteName>,
        ): List<ChordChange> {
            val unpairedNewChords = newChords.toMutableList()
            val leftoverOldChords = mutableListOf<ScaleChord>()
            val pairs = mutableListOf<Pair<ScaleChord, ScaleChord>>()
            for (oldChord in oldChords) {
                val index =
                    unpairedNewChords.indexOfFirst { chord ->
                        chord.degree ==
                            oldChord.degree
                    }
                if (index >= 0) {
                    val newChord = unpairedNewChords.removeAt(index)
                    pairs.add(Pair(oldChord, newChord))
                } else {
                    leftoverOldChords.add(oldChord)
                }
            }
            if (leftoverOldChords.size == 1 && unpairedNewChords.size == 1) {
                pairs.add(Pair(leftoverOldChords[0], unpairedNewChords[0]))
                leftoverOldChords.clear()
                unpairedNewChords.clear()
            }

            val changes = mutableListOf<ChordChange>()
            for ((oldChord, newChord) in pairs) {
                changes.add(pairedChange(oldChord, newChord))
            }
            for (oldChord in leftoverOldChords) {
                changes.add(removal(oldChord, afterNoteNames))
            }
            for (newChord in unpairedNewChords) {
                changes.add(addition(newChord))
            }
            return changes
        }

        fun pairedChange(
            oldChord: ScaleChord,
            newChord: ScaleChord,
        ): ChordChange {
            val notesGone = notesMissing(oldChord, newChord.notes)
            val notesNew = notesMissing(newChord, oldChord.notes)
            val isSameTriad = oldChord.triad == newChord.triad
            val isSame = isSameTriad && notesGone.isEmpty() && notesNew.isEmpty()
            if (isSame) {
                val summary = "${oldChord.spokenTitle}, stays the same: ${spelled(oldChord.notes)}."
                return ChordChange(
                    kind = ChordChange.Kind.UNCHANGED,
                    before = oldChord,
                    after = newChord,
                    notesGone = emptyList(),
                    notesNew = emptyList(),
                    summary = summary,
                )
            }
            val summary =
                "${oldChord.spokenTitle}, becomes ${newChord.spokenTitle}: it had " +
                    "${spelled(oldChord.notes)} and now has ${spelled(newChord.notes)}."
            return ChordChange(
                kind = ChordChange.Kind.CHANGED,
                before = oldChord,
                after = newChord,
                notesGone = notesGone,
                notesNew = notesNew,
                summary = summary,
            )
        }

        fun removal(
            oldChord: ScaleChord,
            afterNoteNames: Set<NoteName>,
        ): ChordChange {
            val missing = mutableListOf<NoteName>()
            for (note in oldChord.notes) {
                if (note !in afterNoteNames) {
                    missing.add(note)
                }
            }
            val reason: String
            if (missing.isEmpty()) {
                reason = "its notes no longer stack into a chord with a name"
            } else {
                val verb = if (missing.size == 1) "is" else "are"
                reason = "it needed ${list(missing)}, which $verb no longer in the scale"
            }
            return ChordChange(
                kind = ChordChange.Kind.REMOVED,
                before = oldChord,
                after = null,
                notesGone = missing,
                notesNew = emptyList(),
                summary = "${oldChord.spokenTitle}, is gone: $reason.",
            )
        }

        fun addition(newChord: ScaleChord): ChordChange {
            val notes = spelled(newChord.notes)
            val summary = "${newChord.spokenTitle}, is new: $notes are all in the scale now."
            return ChordChange(
                kind = ChordChange.Kind.ADDED,
                before = null,
                after = newChord,
                notesGone = emptyList(),
                notesNew = newChord.notes,
                summary = summary,
            )
        }

        fun notesMissing(
            chord: ScaleChord,
            others: List<NoteName>,
        ): List<NoteName> {
            val missing = mutableListOf<NoteName>()
            for (note in chord.notes) {
                if (note !in others) {
                    missing.add(note)
                }
            }
            return missing
        }

        /** A chord's notes as musicians list them: "G, B, D". */
        fun spelled(notes: List<NoteName>): String {
            val names = mutableListOf<String>()
            for (note in notes) {
                names.add(note.displayName)
            }
            return names.joinToString(", ")
        }

        /** Missing notes in a sentence: "C", "C and E", "C, E and G". */
        fun list(notes: List<NoteName>): String {
            val names = mutableListOf<String>()
            for (note in notes) {
                names.add(note.displayName)
            }
            if (names.size <= 1) {
                return names.firstOrNull() ?: ""
            }
            val last = names.removeAt(names.lastIndex)
            return names.joinToString(", ") + " and " + last
        }
    }
}
