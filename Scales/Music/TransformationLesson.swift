import Foundation

/// A scale the player made and named. Its notes never change after saving; only the name
/// may, so a lesson that produced it always describes it truthfully.
struct CustomScale: Identifiable, Hashable, Codable, Sendable {
    let id: UUID
    var definition: ScaleDefinition
    let createdAt: Date

    init(id: UUID = UUID(), definition: ScaleDefinition, createdAt: Date = .now) {
        self.id = id
        self.definition = definition
        self.createdAt = createdAt
    }
}

/// Points at a scale by identity: a built-in one, or one of the player's saved scales.
enum ScaleReference: Hashable, Codable, Sendable {
    case builtIn(ScaleType)
    case custom(UUID)
}

/// One edit applied to a scale in one key, with everything a lesson shows worked out from
/// it: both scales spelled, and the chord changes between them.
struct ScaleTransformation: Hashable, Codable, Sendable {
    /// The key the lesson is taught in; the scales themselves work in any key.
    let root: NoteName
    let before: ScaleDefinition
    let edit: ScaleEdit
    private(set) var after: ScaleDefinition

    /// - Throws: the `ScaleEditError` from `ScaleDefinition.applying(_:)` when the edit
    ///   cannot be made to this scale.
    init(root: NoteName, before: ScaleDefinition, edit: ScaleEdit) throws(ScaleEditError) {
        self.root = root
        self.before = before
        self.edit = edit
        self.after = try before.applying(edit)
    }

    /// The same transformation with the resulting scale called by its proper name, once
    /// it has been recognised or the player has named it.
    func naming(_ name: String) -> ScaleTransformation {
        var named = self
        named.after.name = name
        return named
    }

    var beforeScale: Scale {
        Scale(root: root, definition: before)
    }

    var afterScale: Scale {
        Scale(root: root, definition: after)
    }

    var chordChanges: ChordChangeReport {
        ChordChangeReport(before: beforeScale, after: afterScale)
    }

    /// "Lowered F♯ (7) to F (♭7)."
    var editSentence: String {
        edit.sentence(above: root)
    }

    /// "G Major → G Mixolydian".
    var title: String {
        "\(beforeScale.title) → \(afterScale.title)"
    }
}

/// A transformation the player kept, filed under the scale it led to.
struct TransformationLesson: Identifiable, Hashable, Codable, Sendable {
    let id: UUID
    let date: Date
    let transformation: ScaleTransformation
    let result: ScaleReference

    init(
        id: UUID = UUID(),
        date: Date = .now,
        transformation: ScaleTransformation,
        result: ScaleReference
    ) {
        self.id = id
        self.date = date
        self.transformation = transformation
        self.result = result
    }

    var title: String {
        transformation.title
    }
}
