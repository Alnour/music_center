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

    private var chips: [BoardChordChipContent] {
        BoardChordChipContent.chips(for: scale, change: change)
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

    let chip: BoardChordChipContent
    let isRinged: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(chip.chord.formulaNumeral)
                    .font(.system(.subheadline, design: .serif).weight(.semibold))
                    .foregroundStyle(.secondary)
                Spacer(minLength: 0)
                Text(chip.caption)
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
        .accessibilityLabel(chip.accessibilityLabel)
        .accessibilityAddTraits(isRinged ? .isSelected : [])
        .accessibilityHint("Rings the chord's notes on the board")
    }

    /// The same green the board paints the chord's notes in, so card and board match.
    private var ringColor: Color {
        FretboardPalette.matching(colorScheme).chordRing
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
}
