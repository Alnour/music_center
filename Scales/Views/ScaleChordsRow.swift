import SwiftUI

/// The chords the scale on the board makes, in one row under the board on the Scales screen.
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

    var body: some View {
        if chips.isEmpty {
            Text("This scale's notes don't stack into three-note chords.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(alignment: .top, spacing: 6) {
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
            }
            .accessibilityIdentifier("boardChords")
        }
    }

    /// One chip per chord: the scale's chords as they stand, or, after a change, every
    /// chord the scale had or has, with what happened to it.
    private var chips: [BoardChordChip.Content] {
        var chips: [BoardChordChip.Content] = []
        guard let change else {
            for chord in scale.letterTriads() {
                let chip = BoardChordChip.Content(
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
            chips.append(
                BoardChordChip.Content(
                    chord: shown,
                    before: chordChange.kind == .changed ? chordChange.before : nil,
                    kind: chordChange.kind,
                    newNotes: chordChange.notesNew
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

/// A chord's numeral, name and notes. After a change, a line above says what the chord
/// was, a gained note is green, and a chord the scale lost is crossed out. The chord whose
/// notes are ringed on the board sits on a tinted pad with the same orange edge.
private struct BoardChordChip: View {
    struct Content {
        let chord: ScaleChord
        /// The chord this one replaced, when the change moved one of its notes.
        let before: ScaleChord?
        let kind: ChordChange.Kind
        let newNotes: [NoteName]
    }

    let chip: Content
    let isRinged: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(caption)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(captionColor)
            Text(chip.chord.formulaNumeral)
                .font(.system(.caption, design: .serif).weight(.semibold))
                .foregroundStyle(.secondary)
            Text(chip.chord.triad.name)
                .font(.system(.title3, design: .rounded).weight(.bold))
                .foregroundStyle(nameColor)
                .strikethrough(chip.kind == .removed)
            Text(notesText)
                .font(.caption)
                .foregroundStyle(chip.kind == .removed ? .tertiary : .primary)
        }
        .frame(minWidth: 56, alignment: .leading)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background {
            if isRinged {
                RoundedRectangle(cornerRadius: 8)
                    .fill(ChordRing.color.opacity(0.12))
                    .strokeBorder(ChordRing.color, lineWidth: 1.5)
            }
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(isRinged ? .isSelected : [])
        .accessibilityHint("Rings the chord's notes on the board")
    }

    /// "was Gm", "new", "gone", or a blank line that keeps every chip the same height.
    private var caption: String {
        switch chip.kind {
        case .unchanged: " "
        case .changed: "was \(chip.before?.triad.name ?? "")"
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
                piece.font = .caption.bold()
            }
            text += piece
        }
        return text
    }

    private var accessibilityLabel: String {
        var names: [String] = []
        for note in chip.chord.notes {
            names.append(note.displayName)
        }
        let notes = names.joined(separator: ", ")
        let chord = "\(chip.chord.formulaNumeral), \(chip.chord.triad.spokenName): \(notes)"
        switch chip.kind {
        case .unchanged: return chord
        case .changed: return "\(chord). Was \(chip.before?.triad.spokenName ?? "")."
        case .added: return "\(chord). New."
        case .removed: return "\(chord). Gone."
        }
    }
}
