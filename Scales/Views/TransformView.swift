import SwiftUI

/// A lesson waiting in the sheet: the transformation the player just chose.
private struct PendingLesson: Identifiable {
    let id = UUID()
    let transformation: ScaleTransformation
}

/// The changes a scale allows, sorted once into the menus that offer them: one list per
/// note (lower, raise, remove) and one list of notes that could be added.
private struct EditChoices {
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

/// The Transform tab: pick a scale, change one note, and read what that does to the chords.
///
/// It starts on the scale shown on the Scales screen and follows it whenever it changes
/// there. Choosing a change opens the lesson in a sheet, where the result is named and kept.
/// Below sit the player's own scales and every lesson they have kept, newest first.
struct TransformView: View {
    @Environment(PracticeSession.self) private var session
    @Environment(ScaleLibrary.self) private var library
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    @State private var root = NoteName(.g)
    @State private var start: ScaleReference = .builtIn(.major)
    @State private var hasFollowedTheBoard = false
    @State private var pending: PendingLesson?
    @State private var scaleAwaitingDeletion: CustomScale?
    @State private var libraryError: ScaleLibraryError?

    var body: some View {
        let startScale = Scale(root: root, definition: definition(for: start))
        NavigationStack {
            Form {
                startSection(startScale)
                changeSection(startScale)
                if !library.scales.isEmpty {
                    myScalesSection
                }
                if !library.lessons.isEmpty {
                    lessonsSection
                }
            }
            .navigationTitle("Transform")
        }
        .sheet(item: $pending) { pending in
            LessonSheet(transformation: pending.transformation)
        }
        .confirmationDialog(
            deletionTitle,
            isPresented: isConfirmingDeletion,
            titleVisibility: .visible,
            presenting: scaleAwaitingDeletion
        ) { scale in
            Button("Delete Scale", role: .destructive) {
                delete(scale)
            }
        } message: { scale in
            Text(deletionMessage(for: scale))
        }
        .libraryErrorAlert($libraryError)
        .onAppear(perform: followTheBoardTheFirstTime)
        .onChange(of: session.settings.root) { _, newRoot in
            root = newRoot
        }
        .onChange(of: session.settings.scaleReference) { _, newReference in
            start = newReference
        }
        .onChange(of: library.scales) {
            if case .custom(let id) = start, library.scale(id: id) == nil {
                start = .builtIn(.major)
            }
        }
    }

    // MARK: - Sections

    private func startSection(_ startScale: Scale) -> some View {
        Section("Start from") {
            Picker("Key", selection: $root) {
                ForEach(NoteName.rootChoices, id: \.self) { note in
                    Text(note.displayName).tag(note)
                }
            }
            Picker("Starting scale", selection: $start) {
                ScalePickerOptions(customScales: library.scales)
            }
            ScaleNotesRow(
                scale: startScale,
                palette: FretboardPalette.matching(colorScheme),
                noteSize: horizontalSizeClass == .regular ? 20 : 15
            )
            .frame(maxWidth: .infinity)
            .padding(.vertical, 4)
        }
    }

    private func changeSection(_ startScale: Scale) -> some View {
        let choices = EditChoices(startScale.definition)
        return Section {
            ForEach(startScale.tones, id: \.self) { tone in
                if !tone.isRoot {
                    noteMenu(for: tone, edits: choices.edits(for: tone.degree))
                }
            }
            Menu {
                ForEach(choices.additions, id: \.self) { edit in
                    Button(menuTitle(for: edit)) {
                        begin(edit, from: startScale.definition)
                    }
                }
            } label: {
                Label("Add a note", systemImage: "plus.circle")
            }
            .disabled(choices.additions.isEmpty)
            .accessibilityIdentifier("addNote")
        } header: {
            Text("Change one note")
        } footer: {
            Text(
                "Lower, raise or remove a note, or add one. The lesson shows what happens to "
                    + "every chord, and names the new scale."
            )
        }
    }

    private func noteMenu(for tone: ScaleTone, edits: [ScaleEdit]) -> some View {
        Menu {
            ForEach(edits, id: \.self) { edit in
                Button(menuTitle(for: edit)) {
                    begin(edit, from: definition(for: start))
                }
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

    private var myScalesSection: some View {
        Section("My Scales") {
            ForEach(library.scales) { customScale in
                NavigationLink {
                    CustomScaleDetailView(scaleID: customScale.id) { reference in
                        start = reference
                    }
                } label: {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(customScale.definition.name)
                        Text(customScale.definition.formula)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .accessibilityIdentifier("myScale-\(customScale.definition.name)")
                .swipeActions {
                    Button("Delete", systemImage: "trash") {
                        scaleAwaitingDeletion = customScale
                    }
                    .tint(.red)
                }
            }
        }
    }

    private var lessonsSection: some View {
        Section("Lessons") {
            ForEach(Array(library.lessons.reversed())) { lesson in
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
                .swipeActions {
                    Button("Delete", systemImage: "trash") {
                        deleteLesson(lesson)
                    }
                    .tint(.red)
                }
            }
        }
    }

    // MARK: - Actions

    /// Takes the Scales screen's key and scale on the first visit, so the tab opens on
    /// whatever the player is already practising.
    private func followTheBoardTheFirstTime() {
        guard !hasFollowedTheBoard else { return }
        hasFollowedTheBoard = true
        root = session.settings.root
        start = session.settings.scaleReference
    }

    private func begin(_ edit: ScaleEdit, from definition: ScaleDefinition) {
        guard
            let transformation = try? ScaleTransformation(
                root: root,
                before: definition,
                edit: edit
            )
        else { return }
        pending = PendingLesson(transformation: transformation)
    }

    private func delete(_ scale: CustomScale) {
        do {
            try library.deleteScale(scale.id)
        } catch {
            libraryError = error
        }
    }

    private func deleteLesson(_ lesson: TransformationLesson) {
        do {
            try library.deleteLesson(lesson.id)
        } catch {
            libraryError = error
        }
    }

    // MARK: - Helpers

    /// The definition behind a picker choice. A saved scale deleted elsewhere falls back
    /// to Major until the picker catches up.
    private func definition(for reference: ScaleReference) -> ScaleDefinition {
        switch reference {
        case .builtIn(let type):
            return type.definition
        case .custom(let id):
            return library.scale(id: id)?.definition ?? ScaleType.major.definition
        }
    }

    /// "Lower to F (♭7)", "Raise to G♯ (♯4)", "Remove F♯", "Add D♭ (♭2)".
    private func menuTitle(for edit: ScaleEdit) -> String {
        let note = Scale.spell(edit.degree, above: root).displayName
        guard let result = edit.resultingDegree else {
            return "Remove \(note)"
        }
        let newNote = Scale.spell(result, above: root).displayName
        switch edit {
        case .lower:
            return "Lower to \(newNote) (\(result.label))"
        case .raise:
            return "Raise to \(newNote) (\(result.label))"
        case .add, .remove:
            return "Add \(newNote) (\(result.label))"
        }
    }

    private var isConfirmingDeletion: Binding<Bool> {
        Binding(
            get: { scaleAwaitingDeletion != nil },
            set: { isShowing in
                if !isShowing {
                    scaleAwaitingDeletion = nil
                }
            }
        )
    }

    private var deletionTitle: String {
        guard let scale = scaleAwaitingDeletion else { return "" }
        return "Delete \(scale.definition.name)?"
    }

    private func deletionMessage(for scale: CustomScale) -> String {
        let lessonCount = library.lessons(for: .custom(scale.id)).count
        switch lessonCount {
        case 0: return "The scale will be removed from this device."
        case 1: return "The scale and the lesson that made it will be removed from this device."
        default:
            return "The scale and its \(lessonCount) lessons will be removed from this device."
        }
    }
}
