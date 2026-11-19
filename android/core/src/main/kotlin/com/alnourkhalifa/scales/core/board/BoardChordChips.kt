// Named after its Swift twin, BoardChordChips.swift, so `make mirror` pairs the two files.
@file:Suppress("ktlint:standard:filename", "MatchingDeclarationName")

package com.alnourkhalifa.scales.core.board

import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleChord
import com.alnourkhalifa.scales.core.music.letterTriads
import com.alnourkhalifa.scales.core.session.BoardChange

/**
 * Twin of `BoardChordChipContent` in `Scales/Views/BoardChordChips.swift`: what one chord card
 * under the board says, as plain data: the chord, what it was before the last note change,
 * and its caption and spoken label.
 *
 * Kept apart from the Compose card so the words are computed in one place, and
 * `expected/answers.json` pins the cards a scripted session shows, spoken labels included.
 */
data class BoardChordChipContent(
    val chord: ScaleChord,
    /** The chord this one replaced, when the change moved one of its notes. */
    val before: ScaleChord?,
    val kind: ChordChange.Kind,
    /** Notes the chord gained with the change, shown in green. */
    val newNotes: List<NoteName>,
) {
    /** "was Gm", "new", "gone", or a blank line that keeps every card the same height. */
    val caption: String
        get() =
            when (kind) {
                ChordChange.Kind.UNCHANGED -> " "
                ChordChange.Kind.CHANGED -> "was ${before?.triad?.name ?: ""}"
                ChordChange.Kind.ADDED -> "new"
                ChordChange.Kind.REMOVED -> "gone"
            }

    /** What TalkBack reads for the card: "V, D major: D, F♯, A", then what happened to the
     *  chord after a change ("Was D major.", "New.", "Gone."). */
    val accessibilityLabel: String
        get() {
            val names = mutableListOf<String>()
            for (note in chord.notes) {
                names.add(note.displayName)
            }
            val notes = names.joinToString(", ")
            val spokenChord = "${chord.formulaNumeral}, ${chord.triad.spokenName}: $notes"
            return when (kind) {
                ChordChange.Kind.UNCHANGED -> spokenChord
                ChordChange.Kind.CHANGED -> "$spokenChord. Was ${before?.triad?.spokenName ?: ""}."
                ChordChange.Kind.ADDED -> "$spokenChord. New."
                ChordChange.Kind.REMOVED -> "$spokenChord. Gone."
            }
        }

    /** Swift's static `chips(for:change:)`. */
    companion object {
        /** One card per chord: the scale's chords as they stand, or, after a change, every
         *  chord the scale had or has, with what happened to it. */
        fun chips(
            scale: Scale,
            change: BoardChange?,
        ): List<BoardChordChipContent> {
            if (change == null) {
                return unchangedChips(scale)
            }
            val chips = mutableListOf<BoardChordChipContent>()
            for (chordChange in change.transformation.chordChanges.changes) {
                val shown = chordChange.after ?: chordChange.before ?: continue
                val before =
                    if (chordChange.kind ==
                        ChordChange.Kind.CHANGED
                    ) {
                        chordChange.before
                    } else {
                        null
                    }
                val chip =
                    BoardChordChipContent(shown, before, chordChange.kind, chordChange.notesNew)
                chips.add(chip)
            }
            return chips
        }

        private fun unchangedChips(scale: Scale): List<BoardChordChipContent> {
            val chips = mutableListOf<BoardChordChipContent>()
            for (chord in scale.letterTriads()) {
                val chip =
                    BoardChordChipContent(chord, null, ChordChange.Kind.UNCHANGED, emptyList())
                chips.add(chip)
            }
            return chips
        }
    }
}
