// Named after its Swift twin, BoardChordChips.swift, so `make mirror` pairs the two files.
@file:Suppress("ktlint:standard:filename", "MatchingDeclarationName")

package com.alnourkhalifa.scales.core.board

import com.alnourkhalifa.scales.core.music.CapoChordShape
import com.alnourkhalifa.scales.core.music.CapoTransposition
import com.alnourkhalifa.scales.core.music.ChordChange
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleChord
import com.alnourkhalifa.scales.core.music.boardChords
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
    /** Notes of a borrowed chord that the scale on the board doesn't hold: a pentatonic's 4
     *  or 7, or the major third of a blues seventh chord. Shown faint and never ringed. */
    val outsideNotes: List<NoteName>,
    /** The shape the chord is fingered as with the capo on; null without a capo. */
    val capoShape: CapoChordShape? = null,
) {
    /** "was Gm", "new", "gone", or a blank line that keeps every card the same height. */
    val caption: String
        get() =
            when (kind) {
                ChordChange.Kind.UNCHANGED -> " "
                ChordChange.Kind.CHANGED -> "was ${before?.name ?: ""}"
                ChordChange.Kind.ADDED -> "new"
                ChordChange.Kind.REMOVED -> "gone"
            }

    /** What TalkBack reads for the card: "V, D major: D, F♯, A", then what happened to the
     *  chord after a change ("Was D major.", "New.", "Gone."), then the shape fingered with a
     *  capo on ("Fingered as C major, an open chord."). */
    val accessibilityLabel: String
        get() {
            val names = mutableListOf<String>()
            for (note in chord.notes) {
                if (note in outsideNotes) {
                    names.add("${note.displayName} (not in the scale)")
                } else {
                    names.add(note.displayName)
                }
            }
            val notes = names.joinToString(", ")
            val spokenChord = "${chord.formulaNumeral}, ${chord.spokenName}: $notes"
            val spokenChange =
                when (kind) {
                    ChordChange.Kind.UNCHANGED -> spokenChord
                    ChordChange.Kind.CHANGED -> "$spokenChord. Was ${before?.spokenName ?: ""}."
                    ChordChange.Kind.ADDED -> "$spokenChord. New."
                    ChordChange.Kind.REMOVED -> "$spokenChord. Gone."
                }
            val shape = capoShape ?: return spokenChange
            // An unchanged card's words end without a full stop, the others with one.
            if (kind == ChordChange.Kind.UNCHANGED) {
                return "$spokenChange. ${shape.spokenSentence}"
            }
            return "$spokenChange ${shape.spokenSentence}"
        }

    /** Swift's static `chips(for:change:capo:)`. */
    companion object {
        /** One card per chord: the scale's chords as they stand, or, after a change, every
         *  chord the scale had or has, with what happened to it. With a capo on, each card
         *  also carries the shape to finger (`PracticeSettings.capoTransposition`). */
        fun chips(
            scale: Scale,
            change: BoardChange?,
            capo: CapoTransposition? = null,
        ): List<BoardChordChipContent> {
            if (change == null) {
                return unchangedChips(scale, capo)
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
                    BoardChordChipContent(
                        chord = shown,
                        before = before,
                        kind = chordChange.kind,
                        newNotes = chordChange.notesNew,
                        outsideNotes = emptyList(),
                        capoShape = capo?.shape(shown),
                    )
                chips.add(chip)
            }
            return chips
        }

        /** The scale's chords as they stand (`Scale.boardChords`), each with the notes the
         *  scale lacks, which only a borrowed chord has. */
        private fun unchangedChips(
            scale: Scale,
            capo: CapoTransposition?,
        ): List<BoardChordChipContent> {
            val scaleNotes = mutableSetOf<NoteName>()
            for (tone in scale.tones) {
                scaleNotes.add(tone.name)
            }
            val chips = mutableListOf<BoardChordChipContent>()
            for (chord in scale.boardChords()) {
                val outsideNotes = mutableListOf<NoteName>()
                for (note in chord.notes) {
                    if (note !in scaleNotes) {
                        outsideNotes.add(note)
                    }
                }
                val chip =
                    BoardChordChipContent(
                        chord = chord,
                        before = null,
                        kind = ChordChange.Kind.UNCHANGED,
                        newNotes = emptyList(),
                        outsideNotes = outsideNotes,
                        capoShape = capo?.shape(chord),
                    )
                chips.add(chip)
            }
            return chips
        }
    }
}
