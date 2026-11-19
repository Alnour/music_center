// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

/// The chords under the board on the Scales screen: the chords the scale makes, or, for
/// a pentatonic or blues scale, the chords it is played over (`Scale.boardChords`), with
/// the notes of a chord the scale doesn't hold shown faint and a line saying where the
/// chords come from.
///
/// After a note change, every chord is shown as it is now, and the chords that used the
/// changed note are marked: what each one was, the note it gained in green, and the chords
/// the scale no longer makes crossed out. The marks stay until the player picks another scale.
struct ScaleChordsRow: View {
    let scale: Scale
    let change: BoardChange?
    /// The chord whose notes are ringed on the board, if any.
    let ringedChord: ScaleChord?
    /// Called with the chord the player tapped.
    let onTap: (ScaleChord) -> Void
    /// The narrowest a card may be. A phone uses narrow cards so seven chords make two
    /// rows; a wide window has room for roomier ones.
    var cardMinimumWidth: CGFloat = 84

    var body: some View {
        if chips.isEmpty {
            Text("This scale's notes don't stack into three-note chords.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        } else {
            // Cards wrap into as many rows as the width needs, so a Mac window shows them
            // in one row and a phone in two or three, each card large enough to read from
            // playing distance.
            VStack(alignment: .leading, spacing: 6) {
                LazyVGrid(
                    columns: [
                        GridItem(.adaptive(minimum: cardMinimumWidth, maximum: 200), spacing: 8)
                    ],
                    spacing: 8
                ) {
                    ForEach(Array(chips.enumerated()), id: \.offset) { _, chip in
                        Button {
                            onTap(chip.chord)
                        } label: {
                            BoardChordChip(chip: chip, isRinged: chip.chord == ringedChord)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.vertical, 2)
                .accessibilityIdentifier("boardChords")
                if change == nil, let note = scale.harmonyNote {
                    Text(note)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .accessibilityIdentifier("harmonyNote")
                }
            }
        }
    }

    /// The scale's notes as spelled, to tell which notes of a borrowed chord it lacks.
    private var scaleNotes: Set<NoteName> {
        var notes = Set<NoteName>()
        for tone in scale.tones {
            notes.insert(tone.name)
        }
        return notes
    }

    /// The notes of a chord the scale doesn't hold: a borrowed chord's 4 or 7, or the
    /// major third of a blues seventh chord.
    private func outsideNotes(of chord: ScaleChord) -> [NoteName] {
        var outside: [NoteName] = []
        for note in chord.notes where !scaleNotes.contains(note) {
            outside.append(note)
        }
        return outside
    }

    /// One chip per chord: the scale's chords as they stand, or, after a change, every
    /// chord the scale had or has, with what happened to it.
    private var chips: [BoardChordChip.Content] {
        var chips: [BoardChordChip.Content] = []
        guard let change else {
            for chord in scale.boardChords() {
                let chip = BoardChordChip.Content(
                    chord: chord,
                    before: nil,
                    kind: .unchanged,
                    newNotes: [],
                    outsideNotes: outsideNotes(of: chord)
                )
                chips.append(chip)
            }
            return chips
        }
        for chordChange in change.transformation.chordChanges.changes {
            let shown = chordChange.after ?? chordChange.before
            guard let shown else { continue }
            chips.append(
                BoardChordChip.Content(
                    chord: shown,
                    before: chordChange.kind == .changed ? chordChange.before : nil,
                    kind: chordChange.kind,
                    newNotes: chordChange.notesNew,
                    outsideNotes: []
                )
            )
        }
        return chips
    }
}

/// The last note change spelled out, with the way back to the scale it started from.
struct BoardChangeRow: View {
    let change: BoardChange
    let onRevert: () -> Void

    var body: some View {
        HStack {
            Text(change.transformation.editSentence)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Spacer()
            Button(
                "Back to \(change.transformation.beforeScale.title)",
                systemImage: "arrow.uturn.backward",
                action: onRevert
            )
            .font(.footnote)
            .accessibilityIdentifier("revertBoardChange")
        }
    }
}

/// A chord card: numeral, name and notes. A note the scale doesn't hold is faint. After a
/// change, a line above says what the chord was, a gained note is green, and a chord the
/// scale lost is crossed out. The chord whose
/// notes are ringed on the board sits on a tinted card edged in the board's chord colour.
private struct BoardChordChip: View {
    @Environment(\.colorScheme) private var colorScheme

    struct Content {
        let chord: ScaleChord
        /// The chord this one replaced, when the change moved one of its notes.
        let before: ScaleChord?
        let kind: ChordChange.Kind
        let newNotes: [NoteName]
        /// Notes of a borrowed chord that the scale on the board doesn't hold.
        let outsideNotes: [NoteName]
    }

    let chip: Content
    let isRinged: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(chip.chord.formulaNumeral)
                    .font(.system(.subheadline, design: .serif).weight(.semibold))
                    .foregroundStyle(.secondary)
                Spacer(minLength: 0)
                Text(caption)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(captionColor)
            }
            Text(chip.chord.name)
                .font(.system(.title2, design: .rounded).weight(.bold))
                .foregroundStyle(nameColor)
                .strikethrough(chip.kind == .removed)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(notesText)
                .font(.subheadline)
                .foregroundStyle(chip.kind == .removed ? .tertiary : .primary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background {
            RoundedRectangle(cornerRadius: 10)
                .fill(isRinged ? ringColor.opacity(0.12) : Color(.secondarySystemFill))
            if isRinged {
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(ringColor, lineWidth: 2)
            }
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(isRinged ? .isSelected : [])
        .accessibilityHint("Rings the chord's notes on the board")
    }

    /// The same green the board paints the chord's notes in, so card and board match.
    private var ringColor: Color {
        FretboardPalette.matching(colorScheme).chordRing
    }

    /// "was Gm", "new", "gone", or a blank line that keeps every chip the same height.
    private var caption: String {
        switch chip.kind {
        case .unchanged: " "
        case .changed: "was \(chip.before?.name ?? "")"
        case .added: "new"
        case .removed: "gone"
        }
    }

    private var captionColor: Color {
        switch chip.kind {
        case .unchanged: .clear
        case .changed: .orange
        case .added: .green
        case .removed: .red
        }
    }

    private var nameColor: Color {
        chip.kind == .removed ? .secondary : chip.chord.triad.quality.tint
    }

    private var notesText: AttributedString {
        var text = AttributedString()
        for (index, note) in chip.chord.notes.enumerated() {
            if index > 0 {
                text += AttributedString(" ")
            }
            var piece = AttributedString(note.displayName)
            if chip.newNotes.contains(note) {
                piece.foregroundColor = .green
                piece.font = .subheadline.bold()
            } else if chip.outsideNotes.contains(note) {
                piece.foregroundColor = .secondary
            }
            text += piece
        }
        return text
    }

    private var accessibilityLabel: String {
        var names: [String] = []
        for note in chip.chord.notes {
            if chip.outsideNotes.contains(note) {
                names.append("\(note.displayName) (not in the scale)")
            } else {
                names.append(note.displayName)
            }
        }
        let notes = names.joined(separator: ", ")
        let chord = "\(chip.chord.formulaNumeral), \(chip.chord.spokenName): \(notes)"
        switch chip.kind {
        case .unchanged: return chord
        case .changed: return "\(chord). Was \(chip.before?.spokenName ?? "")."
        case .added: return "\(chord). New."
        case .removed: return "\(chord). Gone."
        }
    }
}
