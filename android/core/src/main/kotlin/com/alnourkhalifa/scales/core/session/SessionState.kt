package com.alnourkhalifa.scales.core.session

import com.alnourkhalifa.scales.core.music.FretboardLayout
import com.alnourkhalifa.scales.core.music.KeyboardLayout
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleChord

/**
 * One immutable snapshot of what the board shows: the stored part of `PracticeSession` in
 * `Scales/Session/PracticeSession.swift`, with its derived values.
 *
 * Differs from Swift: Swift's session is one `@Observable` object that views read directly.
 * Compose recomposes when a state reference changes, so [PracticeSession] replaces this whole
 * snapshot after every change and hands it to its listener.
 */
data class SessionState(
    val settings: PracticeSettings = PracticeSettings(),
    /**
     * The last note change made from the Scales screen. Kept only for this run, so the chords
     * under the board can show what the change did until the player picks another scale or
     * key (see [currentBoardChange]).
     */
    val boardChange: BoardChange? = null,
    /**
     * The chord whose notes are ringed on the board, picked from the chords under it. Kept for
     * this run only, and kept while the scale changes; [ringedNotes] ignores it once the
     * scale no longer holds all its notes, so the rings vanish with the chord instead of
     * pointing at nothing.
     */
    val ringedChord: ScaleChord? = null,
) {
    val scale: Scale
        get() = Scale(settings.root, settings.scaleDefinition)

    /** The notes to ring on the board: the picked chord's notes, in the chord's order, while
     *  every one of them is in the scale as spelled now. */
    val ringedNotes: Set<NoteName>
        get() {
            val chord = ringedChord ?: return emptySet()
            val scaleNotes = mutableSetOf<NoteName>()
            for (tone in scale.tones) {
                scaleNotes.add(tone.name)
            }
            val chordNotes = chord.notes.toSet()
            if (!scaleNotes.containsAll(chordNotes)) {
                return emptySet()
            }
            return chordNotes
        }

    /** The last board change while the board still shows its result; null once the player
     *  has moved on to another scale or key, when there is nothing left to compare against.
     *  Picking that scale in that key again shows the change again, as on iOS. */
    val currentBoardChange: BoardChange?
        get() {
            val change = boardChange ?: return null
            val showsResult = change.after == settings.scaleReference
            val sameKey = change.transformation.root == settings.root
            return if (showsResult && sameKey) change else null
        }

    val layout: FretboardLayout
        get() = FretboardLayout(scale, settings.tuning, settings.fretCount)

    val keyboardLayout: KeyboardLayout
        get() = KeyboardLayout(scale, settings.selectedKeyboardOctaves)
}
