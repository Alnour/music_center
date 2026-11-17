import SwiftUI

/// The changes a scale allows, sorted once into the menus that offer them: one list per
/// note (lower, raise, remove) and one list of notes that could be added.
struct EditChoices {
    private var editsByDegree: [ScaleDegree: [ScaleEdit]] = [:]
    private(set) var additions: [ScaleEdit] = []

    init(_ definition: ScaleDefinition) {
        for edit in definition.legalEdits() {
            if case .add = edit {
                additions.append(edit)
            } else {
                editsByDegree[edit.degree, default: []].append(edit)
            }
        }
    }

    func edits(for degree: ScaleDegree) -> [ScaleEdit] {
        editsByDegree[degree] ?? []
    }
}

/// The rows that change one note of a scale: a menu per note but the root (lower, raise,
/// remove) and a menu of notes that could be added. Shared by the Transform screen and the
/// chords on the Scales screen, so a change is offered the same way in both places.
struct ScaleEditMenus: View {
    let scale: Scale
    let onPick: (ScaleEdit) -> Void

    var body: some View {
        let choices = EditChoices(scale.definition)
        ForEach(scale.tones, id: \.self) { tone in
            if !tone.isRoot {
                noteMenu(for: tone, edits: choices.edits(for: tone.degree))
            }
        }
        Menu {
            ForEach(choices.additions, id: \.self) { edit in
                Button(edit.menuTitle(above: scale.root), action: { onPick(edit) })
            }
        } label: {
            Label("Add a note", systemImage: "plus.circle")
        }
        .disabled(choices.additions.isEmpty)
        .accessibilityIdentifier("addNote")
    }

    private func noteMenu(for tone: ScaleTone, edits: [ScaleEdit]) -> some View {
        Menu {
            ForEach(edits, id: \.self) { edit in
                Button(edit.menuTitle(above: scale.root), action: { onPick(edit) })
            }
        } label: {
            HStack {
                Text(tone.name.displayName)
                    .font(.body.weight(.semibold))
                    .foregroundStyle(.primary)
                Text(tone.degree.label)
                    .foregroundStyle(.secondary)
                Spacer()
                Image(systemName: "chevron.up.chevron.down")
                    .foregroundStyle(.secondary)
            }
            .contentShape(Rectangle())
        }
        .disabled(edits.isEmpty)
        .accessibilityLabel("Change \(tone.name.displayName), the \(tone.degree.label)")
        .accessibilityIdentifier("editNote-\(tone.degree.label)")
    }
}

/// The same choices folded into one button: a menu whose items open a submenu per note and
/// one for added notes. For the Scales screen, where the board and its chords need the
/// room and a row per note would push them off the screen.
struct CompactScaleEditMenu: View {
    let scale: Scale
    let onPick: (ScaleEdit) -> Void

    var body: some View {
        let choices = EditChoices(scale.definition)
        Menu {
            ForEach(scale.tones, id: \.self) { tone in
                if !tone.isRoot {
                    Menu("\(tone.name.displayName) (\(tone.degree.label))") {
                        ForEach(choices.edits(for: tone.degree), id: \.self) { edit in
                            Button(edit.menuTitle(above: scale.root), action: { onPick(edit) })
                        }
                    }
                }
            }
            Menu("Add a note") {
                ForEach(choices.additions, id: \.self) { edit in
                    Button(edit.menuTitle(above: scale.root), action: { onPick(edit) })
                }
            }
            .disabled(choices.additions.isEmpty)
        } label: {
            Label("Change a note", systemImage: "wand.and.stars")
        }
        .accessibilityIdentifier("changeBoardNote")
    }
}

extension ScaleEdit {
    /// The edit as a menu item, with the notes spelled in the key it is offered in:
    /// "Lower to F (♭7)", "Raise to G♯ (♯4)", "Remove F♯", "Add D♭ (♭2)".
    func menuTitle(above root: NoteName) -> String {
        let note = Scale.spell(degree, above: root).displayName
        guard let result = resultingDegree else {
            return "Remove \(note)"
        }
        let newNote = Scale.spell(result, above: root).displayName
        switch self {
        case .lower:
            return "Lower to \(newNote) (\(result.label))"
        case .raise:
            return "Raise to \(newNote) (\(result.label))"
        case .add, .remove:
            return "Add \(newNote) (\(result.label))"
        }
    }
}
