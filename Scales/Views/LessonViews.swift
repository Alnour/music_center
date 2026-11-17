// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

// MARK: - The lesson sheet

/// Opens when the player picks a change: the lesson itself, then naming and saving it.
///
/// A result the app recognises is named for the player ("This is G Mixolydian"); a new one
/// asks for a name before it can be saved, and is then added to My Scales.
struct LessonSheet: View {
    let transformation: ScaleTransformation

    @Environment(ScaleLibrary.self) private var library
    @Environment(\.dismiss) private var dismiss
    @State private var newName = ""
    @State private var libraryError: ScaleLibraryError?

    var body: some View {
        let match = ScaleIdentifier.match(transformation.after, among: library.scales)
        let shownName = match?.name ?? workingName
        NavigationStack {
            Form {
                LessonSummarySection(transformation: transformation.naming(shownName))
                resultSection(match: match)
                ChordChangesSection(report: transformation.chordChanges)
            }
            .navigationTitle("Lesson")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        save()
                    }
                    .disabled(match == nil && trimmedName.isEmpty)
                    .accessibilityIdentifier("saveLessonButton")
                }
            }
        }
        .libraryErrorAlert($libraryError)
    }

    private func resultSection(match: ScaleMatch?) -> some View {
        Section {
            if let match {
                HStack(spacing: 10) {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundStyle(.green)
                        .accessibilityHidden(true)
                    Text("This is \(transformation.root.displayName) \(match.name)")
                        .font(.headline)
                }
            } else {
                Text("The app doesn't know this scale yet. Name it to keep it with this lesson.")
                TextField("Name", text: $newName, prompt: Text(transformation.after.name))
                    .textInputAutocapitalization(.words)
                    .submitLabel(.done)
                    .accessibilityIdentifier("scaleNameField")
            }
        } header: {
            Text("The result")
        } footer: {
            Text(resultFooter(match: match))
        }
    }

    private func resultFooter(match: ScaleMatch?) -> String {
        switch match {
        case .builtIn(let type):
            "Saving files this lesson under \(type.displayName)."
        case .custom(let customScale):
            "Saving files this lesson under your scale \(customScale.definition.name)."
        case nil:
            "Saving adds the scale to My Scales, so you can play it on the fretboard and the TV."
        }
    }

    private var trimmedName: String {
        newName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// The name shown while the player types, or the working name ("Major ♯5") before.
    private var workingName: String {
        trimmedName.isEmpty ? transformation.after.name : trimmedName
    }

    private func save() {
        do {
            _ = try library.saveLesson(transformation, newScaleName: newName)
            dismiss()
        } catch {
            libraryError = error
        }
    }
}

// MARK: - A saved lesson

/// A lesson the player kept, read again from the Lessons list.
struct LessonDetailView: View {
    let lessonID: UUID

    @Environment(PracticeSession.self) private var session
    @Environment(ScaleLibrary.self) private var library
    @Environment(AppNavigation.self) private var navigation

    var body: some View {
        if let lesson {
            Form {
                LessonSummarySection(transformation: lesson.transformation)
                Section {
                    Button(
                        "Show \(lesson.transformation.afterScale.title) on the Fretboard",
                        systemImage: "guitars"
                    ) {
                        session.choose(
                            lesson.result,
                            root: lesson.transformation.root,
                            from: library
                        )
                        navigation.selectedTab = .scales
                    }
                } footer: {
                    Text("Saved \(lesson.date.formatted(date: .abbreviated, time: .shortened)).")
                }
                ChordChangesSection(report: lesson.transformation.chordChanges)
            }
            .navigationTitle("Lesson")
            .navigationBarTitleDisplayMode(.inline)
        } else {
            ContentUnavailableView("Lesson Deleted", systemImage: "trash")
        }
    }

    private var lesson: TransformationLesson? {
        for lesson in library.lessons where lesson.id == lessonID {
            return lesson
        }
        return nil
    }
}

// MARK: - One of the player's scales

/// One of the player's own scales: rename it, play it, change it again, or delete it, and
/// read the lessons that led to it.
struct CustomScaleDetailView: View {
    let scaleID: UUID
    /// Sends the scale back to the Transform screen as the next starting point.
    let startFrom: (ScaleReference) -> Void

    @Environment(PracticeSession.self) private var session
    @Environment(ScaleLibrary.self) private var library
    @Environment(AppNavigation.self) private var navigation
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @State private var name = ""
    @State private var isConfirmingDeletion = false
    @State private var libraryError: ScaleLibraryError?

    var body: some View {
        if let customScale = library.scale(id: scaleID) {
            form(for: customScale)
        } else {
            ContentUnavailableView("Scale Deleted", systemImage: "trash")
        }
    }

    private func form(for customScale: CustomScale) -> some View {
        let root = session.settings.root
        let shown = Scale(root: root, definition: customScale.definition)
        let lessons = library.lessons(for: .custom(scaleID))
        return Form {
            Section("Name") {
                TextField("Name", text: $name)
                    .submitLabel(.done)
                    .onSubmit(rename)
            }
            Section("In \(root.displayName)") {
                ScaleNotesRow(
                    scale: shown,
                    palette: FretboardPalette.matching(colorScheme),
                    noteSize: 15
                )
                .frame(maxWidth: .infinity)
                .padding(.vertical, 4)
                LabeledContent("Formula", value: customScale.definition.formula)
            }
            Section {
                Button("Show on the Fretboard", systemImage: "guitars") {
                    session.choose(.custom(scaleID), from: library)
                    navigation.selectedTab = .scales
                }
                Button("Change a Note in This Scale", systemImage: "wand.and.stars") {
                    startFrom(.custom(scaleID))
                    dismiss()
                }
            }
            if !lessons.isEmpty {
                Section("How you made it") {
                    ForEach(lessons) { lesson in
                        NavigationLink {
                            LessonDetailView(lessonID: lesson.id)
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(lesson.title)
                                Text(lesson.transformation.editSentence)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            Section {
                Button("Delete Scale", systemImage: "trash", role: .destructive) {
                    isConfirmingDeletion = true
                }
            }
        }
        .navigationTitle(customScale.definition.name)
        .onAppear {
            name = customScale.definition.name
        }
        .onDisappear(perform: rename)
        .confirmationDialog(
            "Delete \(customScale.definition.name)?",
            isPresented: $isConfirmingDeletion,
            titleVisibility: .visible
        ) {
            Button("Delete Scale", role: .destructive, action: delete)
        } message: {
            Text("The scale and the lessons filed under it will be removed from this device.")
        }
        .libraryErrorAlert($libraryError)
    }

    /// Saves a changed name. A blank name puts the old one back.
    private func rename() {
        guard let current = library.scale(id: scaleID) else { return }
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed != current.definition.name else { return }
        do {
            try library.rename(scaleID, to: name)
        } catch {
            name = current.definition.name
            if error != .emptyName {
                libraryError = error
            }
        }
    }

    private func delete() {
        do {
            try library.deleteScale(scaleID)
            dismiss()
        } catch {
            libraryError = error
        }
    }
}

// MARK: - Shared pieces

/// The top of every lesson: what was changed, and the scale before and after with the
/// changed note ringed in both.
struct LessonSummarySection: View {
    let transformation: ScaleTransformation

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        Section {
            VStack(alignment: .leading, spacing: 4) {
                Text(transformation.title)
                    .font(.title3.weight(.bold))
                Text(transformation.editSentence)
                    .foregroundStyle(.secondary)
            }
            scaleRow("Before", scale: transformation.beforeScale, ringed: beforeRing)
            scaleRow("After", scale: transformation.afterScale, ringed: afterRing)
        }
    }

    private func scaleRow(_ label: String, scale: Scale, ringed: Set<NoteName>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
            ScaleNotesRow(
                scale: scale,
                palette: FretboardPalette.matching(colorScheme),
                noteSize: horizontalSizeClass == .regular ? 20 : 14,
                highlightedNotes: ringed
            )
            .frame(maxWidth: .infinity)
        }
        .padding(.vertical, 2)
    }

    /// The note that was lowered, raised or removed; nothing when a note was added.
    private var beforeRing: Set<NoteName> {
        if case .add = transformation.edit {
            return []
        }
        return [Scale.spell(transformation.edit.degree, above: transformation.root)]
    }

    /// Where the edited note ended up; nothing when it was removed.
    private var afterRing: Set<NoteName> {
        guard let result = transformation.edit.resultingDegree else { return [] }
        return [Scale.spell(result, above: transformation.root)]
    }
}

/// Every chord of the scale, before and after, each with its sentence.
struct ChordChangesSection: View {
    let report: ChordChangeReport

    var body: some View {
        Section {
            ForEach(Array(report.changes.enumerated()), id: \.offset) { _, change in
                ChordChangeRow(change: change)
            }
        } header: {
            Text("Chords")
        } footer: {
            Text(tally)
        }
    }

    /// "2 chords change, 1 chord is gone, 4 stay the same."
    private var tally: String {
        guard !report.changes.isEmpty else {
            return "Neither scale stacks into three-note chords."
        }
        var changed = 0
        var removed = 0
        var added = 0
        var unchanged = 0
        for change in report.changes {
            switch change.kind {
            case .changed: changed += 1
            case .removed: removed += 1
            case .added: added += 1
            case .unchanged: unchanged += 1
            }
        }
        var parts: [String] = []
        if changed > 0 {
            parts.append(changed == 1 ? "1 chord changes" : "\(changed) chords change")
        }
        if removed > 0 {
            parts.append(removed == 1 ? "1 chord is gone" : "\(removed) chords are gone")
        }
        if added > 0 {
            parts.append(added == 1 ? "1 new chord appears" : "\(added) new chords appear")
        }
        if unchanged > 0 {
            parts.append(unchanged == 1 ? "1 stays the same" : "\(unchanged) stay the same")
        }
        return parts.joined(separator: ", ") + "."
    }
}

/// One chord's story: the chord before and after, with lost notes in red and new ones in
/// green, a badge saying what happened, and the sentence that explains it.
struct ChordChangeRow: View {
    let change: ChordChange

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 12) {
                if change.kind == .unchanged {
                    ChordChip(chord: change.before, highlighted: [], highlightColor: .primary)
                } else {
                    ChordChip(
                        chord: change.before,
                        highlighted: change.notesGone,
                        highlightColor: .red
                    )
                    Image(systemName: "arrow.right")
                        .foregroundStyle(.secondary)
                        .padding(.top, 20)
                    ChordChip(
                        chord: change.after,
                        highlighted: change.notesNew,
                        highlightColor: .green
                    )
                }
                Spacer(minLength: 0)
                KindBadge(kind: change.kind)
            }
            // The sentence below says everything the chips show, so VoiceOver reads only it.
            .accessibilityHidden(true)
            Text(change.summary)
                .font(.subheadline)
                .foregroundStyle(change.kind == .unchanged ? .secondary : .primary)
        }
        .padding(.vertical, 4)
    }
}

/// A chord's numeral, name and notes, or a quiet "none" where a chord is missing.
private struct ChordChip: View {
    let chord: ScaleChord?
    let highlighted: [NoteName]
    let highlightColor: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let chord {
                Text(chord.formulaNumeral)
                    .font(.system(.caption, design: .serif).weight(.semibold))
                    .foregroundStyle(.secondary)
                Text(chord.triad.name)
                    .font(.system(.title3, design: .rounded).weight(.bold))
                    .foregroundStyle(chord.triad.quality.tint)
                Text(notesText(chord.notes))
                    .font(.caption)
            } else {
                Text(" ")
                    .font(.caption)
                Text("none")
                    .font(.system(.title3, design: .rounded))
                    .foregroundStyle(.tertiary)
            }
        }
        .frame(minWidth: 56, alignment: .leading)
    }

    private func notesText(_ notes: [NoteName]) -> AttributedString {
        var text = AttributedString()
        for (index, note) in notes.enumerated() {
            if index > 0 {
                text += AttributedString(" ")
            }
            var piece = AttributedString(note.displayName)
            if highlighted.contains(note) {
                piece.foregroundColor = highlightColor
                piece.font = .caption.bold()
            }
            text += piece
        }
        return text
    }
}

private struct KindBadge: View {
    let kind: ChordChange.Kind

    var body: some View {
        Text(title)
            .font(.caption2.weight(.semibold))
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .foregroundStyle(color)
            .background(Capsule().fill(color.opacity(0.15)))
    }

    private var title: String {
        switch kind {
        case .unchanged: "same"
        case .changed: "changed"
        case .removed: "gone"
        case .added: "new"
        }
    }

    private var color: Color {
        switch kind {
        case .unchanged: .gray
        case .changed: .orange
        case .removed: .red
        case .added: .green
        }
    }
}

extension ScaleLibraryError {
    /// What went wrong, in words for the player.
    var message: String {
        switch self {
        case .emptyName: "Give the new scale a name first."
        case .scaleNotFound: "That scale is no longer in your library."
        case .lessonNotFound: "That lesson was already deleted."
        case .couldNotSave(let reason): "Your scales couldn't be saved on this device. \(reason)"
        }
    }
}

private struct LibraryErrorAlert: ViewModifier {
    @Binding var error: ScaleLibraryError?

    func body(content: Content) -> some View {
        content.alert(
            "Couldn't Update Your Scales",
            isPresented: isShowing,
            presenting: error
        ) { _ in
            Button("OK", role: .cancel) {}
        } message: { error in
            Text(error.message)
        }
    }

    private var isShowing: Binding<Bool> {
        Binding(
            get: { error != nil },
            set: { isPresented in
                if !isPresented {
                    error = nil
                }
            }
        )
    }
}

extension View {
    /// Shows a library error as an alert, and clears it when dismissed.
    func libraryErrorAlert(_ error: Binding<ScaleLibraryError?>) -> some View {
        modifier(LibraryErrorAlert(error: error))
    }
}
