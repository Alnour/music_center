// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation

/// What one chord card under the board says, as plain data: the chord, what it was before
/// the last note change, and its caption and spoken label.
///
/// Kept apart from the SwiftUI card (`ScaleChordsRow`) so the words are computed in one
/// place: the Android cards mirror this file, and `expected/answers.json` pins the cards a
/// scripted session shows, spoken labels included.
struct BoardChordChipContent: Equatable, Sendable {
    let chord: ScaleChord
    /// The chord this one replaced, when the change moved one of its notes.
    let before: ScaleChord?
    let kind: ChordChange.Kind
    /// Notes the chord gained with the change, shown in green.
    let newNotes: [NoteName]

    /// One card per chord: the scale's chords as they stand, or, after a change, every
    /// chord the scale had or has, with what happened to it.
    static func chips(for scale: Scale, change: BoardChange?) -> [BoardChordChipContent] {
        var chips: [BoardChordChipContent] = []
        guard let change else {
            for chord in scale.letterTriads() {
                let chip = BoardChordChipContent(
                    chord: chord,
                    before: nil,
                    kind: .unchanged,
                    newNotes: []
                )
                chips.append(chip)
            }
            return chips
        }
        for chordChange in change.transformation.chordChanges.changes {
            let shown = chordChange.after ?? chordChange.before
            guard let shown else { continue }
            var before: ScaleChord?
            if chordChange.kind == .changed {
                before = chordChange.before
            }
            let chip = BoardChordChipContent(
                chord: shown,
                before: before,
                kind: chordChange.kind,
                newNotes: chordChange.notesNew
            )
            chips.append(chip)
        }
        return chips
    }

    /// "was Gm", "new", "gone", or a blank line that keeps every card the same height.
    var caption: String {
        switch kind {
        case .unchanged: " "
        case .changed: "was \(before?.triad.name ?? "")"
        case .added: "new"
        case .removed: "gone"
        }
    }

    /// What VoiceOver reads for the card: "V, D major: D, F♯, A", then what happened to the
    /// chord after a change ("Was D major.", "New.", "Gone.").
    var accessibilityLabel: String {
        var names: [String] = []
        for note in chord.notes {
            names.append(note.displayName)
        }
        let notes = names.joined(separator: ", ")
        let spokenChord = "\(chord.formulaNumeral), \(chord.triad.spokenName): \(notes)"
        switch kind {
        case .unchanged: return spokenChord
        case .changed: return "\(spokenChord). Was \(before?.triad.spokenName ?? "")."
        case .added: return "\(spokenChord). New."
        case .removed: return "\(spokenChord). Gone."
        }
    }
}
