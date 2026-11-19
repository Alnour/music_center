// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

// What the note-change menus offer, as plain data: the Scales screen, the Transform tab
// and the Android menus all read these, and `expected/answers.json` pins the titles.
// The SwiftUI menus that show them live in `Views/ScaleEditMenuViews.swift`.

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
