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
    /// Notes of a borrowed chord that the scale on the board doesn't hold: a pentatonic's
    /// 4 or 7, or the major third of a blues seventh chord. Shown faint and never ringed.
    let outsideNotes: [NoteName]
    /// The shape the chord is fingered as with the capo on; nil without a capo.
    let capoShape: CapoChordShape?

    /// One card per chord: the scale's chords as they stand, or, after a change, every
    /// chord the scale had or has, with what happened to it. With a capo on, each card
    /// also carries the shape to finger (`PracticeSettings.capoTransposition`).
    static func chips(
        for scale: Scale,
        change: BoardChange?,
        capo: CapoTransposition? = nil
    ) -> [BoardChordChipContent] {
        var chips: [BoardChordChipContent] = []
        guard let change else {
            var scaleNotes = Set<NoteName>()
            for tone in scale.tones {
                scaleNotes.insert(tone.name)
            }
            for chord in scale.boardChords() {
                var outsideNotes: [NoteName] = []
                for note in chord.notes where !scaleNotes.contains(note) {
                    outsideNotes.append(note)
                }
                let chip = BoardChordChipContent(
                    chord: chord,
                    before: nil,
                    kind: .unchanged,
                    newNotes: [],
                    outsideNotes: outsideNotes,
                    capoShape: capo?.shape(for: chord)
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
                newNotes: chordChange.notesNew,
                outsideNotes: [],
                capoShape: capo?.shape(for: shown)
            )
            chips.append(chip)
        }
        return chips
    }

    /// "was Gm", "new", "gone", or a blank line that keeps every card the same height.
    var caption: String {
        switch kind {
        case .unchanged: " "
        case .changed: "was \(before?.name ?? "")"
        case .added: "new"
        case .removed: "gone"
        }
    }

    /// What VoiceOver reads for the card: "V, D major: D, F♯, A", then what happened to the
    /// chord after a change ("Was D major.", "New.", "Gone."), then the shape fingered
    /// with a capo on ("Fingered as C major, an open chord.").
    var accessibilityLabel: String {
        var names: [String] = []
        for note in chord.notes {
            if outsideNotes.contains(note) {
                names.append("\(note.displayName) (not in the scale)")
            } else {
                names.append(note.displayName)
            }
        }
        let notes = names.joined(separator: ", ")
        let spokenChord = "\(chord.formulaNumeral), \(chord.spokenName): \(notes)"
        let spokenChange: String
        switch kind {
        case .unchanged: spokenChange = spokenChord
        case .changed: spokenChange = "\(spokenChord). Was \(before?.spokenName ?? "")."
        case .added: spokenChange = "\(spokenChord). New."
        case .removed: spokenChange = "\(spokenChord). Gone."
        }
        guard let capoShape else { return spokenChange }
        if kind == .unchanged {
            return "\(spokenChange). \(capoShape.spokenSentence)"
        }
        return "\(spokenChange) \(capoShape.spokenSentence)"
    }
}
