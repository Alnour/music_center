// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

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
