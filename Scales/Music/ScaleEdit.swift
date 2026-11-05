/// One change to a scale: lower or raise a note by a semitone, remove it, or add a new one.
///
/// Each degree is the one the edit acts on as the scale stands before the edit; for an
/// added note it is the new degree.
enum ScaleEdit: Hashable, Codable, Sendable {
    case lower(ScaleDegree)
    case raise(ScaleDegree)
    case remove(ScaleDegree)
    case add(ScaleDegree)

    var degree: ScaleDegree {
        switch self {
        case .lower(let degree), .raise(let degree), .remove(let degree), .add(let degree):
            degree
        }
    }

    /// Where the edited note ends up, or nil when the note is removed.
    var resultingDegree: ScaleDegree? {
        switch self {
        case .lower(let degree): degree.lowered()
        case .raise(let degree): degree.raised()
        case .remove: nil
        case .add(let degree): degree
        }
    }

    /// The change in formula shorthand, used for a scale's working name ("Major ♭7"):
    /// "♭7", "♯4", "♮4" when a sharp or flat is taken away, "no 4", "add ♭6".
    var shortLabel: String {
        switch self {
        case .lower, .raise:
            guard let result = resultingDegree else { return "" }
            if result.alteration == 0 {
                return "♮\(result.number)"
            }
            return result.label
        case .remove(let degree):
            return "no \(degree.label)"
        case .add(let degree):
            return "add \(degree.label)"
        }
    }

    /// The change in a sentence, with the notes spelled in the key it was made in:
    /// "Lowered F♯ (7) to F (♭7)."
    func sentence(above root: NoteName) -> String {
        let note = Scale.spell(degree, above: root).displayName
        switch self {
        case .lower, .raise:
            guard let result = resultingDegree else { return "" }
            let verb = result < degree ? "Lowered" : "Raised"
            let newNote = Scale.spell(result, above: root).displayName
            return "\(verb) \(note) (\(degree.label)) to \(newNote) (\(result.label))."
        case .remove:
            return "Removed \(note) (\(degree.label))."
        case .add:
            return "Added \(note) (\(degree.label))."
        }
    }
}

/// Why an edit cannot be made.
enum ScaleEditError: Error, Equatable, Sendable {
    /// The root is the note the whole scale is measured from, so it stays where it is.
    case rootCannotChange
    /// Lowering, raising or removing a degree the scale doesn't have.
    case degreeNotInScale
    /// Adding a degree the scale already has.
    case degreeAlreadyInScale
    /// A degree number outside 1 to 7.
    case noSuchDegree
    /// The edited note would sound the same as one already in the scale (♯4 beside ♭5).
    case samePitchAsAnotherNote(ScaleDegree)
    /// More than one flat or sharp on a degree: a 𝄫3 is just a 2, so it is spelled that way.
    case alterationTooFar
    /// Fewer than three notes is no longer a scale.
    case tooFewNotes
}

extension ScaleDefinition {
    /// The smallest scale an edit may leave behind.
    static let minimumNoteCount = 3

    /// The scale after one edit. The result keeps letter order and a working name, the
    /// old name plus the change ("Major ♭7"), until it is identified or named.
    ///
    /// - Throws: `rootCannotChange` for any edit on degree 1; `degreeNotInScale` when
    ///   lowering, raising or removing a missing degree; `degreeAlreadyInScale` or
    ///   `noSuchDegree` for a bad addition; `alterationTooFar` past one flat or sharp;
    ///   `samePitchAsAnotherNote` when the new note duplicates a pitch; `tooFewNotes` when
    ///   a removal would leave fewer than three notes.
    func applying(_ edit: ScaleEdit) throws(ScaleEditError) -> ScaleDefinition {
        let target = edit.degree
        guard target.number != ScaleDegree.root.number else { throw .rootCannotChange }

        var remaining = degrees
        switch edit {
        case .lower, .raise, .remove:
            guard let index = degrees.firstIndex(of: target) else { throw .degreeNotInScale }
            remaining.remove(at: index)
        case .add:
            guard ScaleDegree.numbers.contains(target.number) else { throw .noSuchDegree }
            guard !degrees.contains(target) else { throw .degreeAlreadyInScale }
        }

        let workingName = "\(name) \(edit.shortLabel)"
        guard let newDegree = edit.resultingDegree else {
            guard remaining.count >= Self.minimumNoteCount else { throw .tooFewNotes }
            return ScaleDefinition(name: workingName, degrees: remaining)
        }
        guard abs(newDegree.alteration) <= 1 else { throw .alterationTooFar }
        for other in remaining where other.pitchClassAboveRoot == newDegree.pitchClassAboveRoot {
            throw .samePitchAsAnotherNote(other)
        }
        remaining.append(newDegree)
        return ScaleDefinition(name: workingName, degrees: remaining)
    }

    /// Every edit `applying` accepts, so the interface only offers changes that work:
    /// lower, raise and remove for each note but the root, then each degree that could be
    /// added, in letter order.
    func legalEdits() -> [ScaleEdit] {
        var candidates: [ScaleEdit] = []
        for degree in degrees where degree.number != ScaleDegree.root.number {
            candidates.append(.lower(degree))
            candidates.append(.raise(degree))
            candidates.append(.remove(degree))
        }
        for number in ScaleDegree.numbers where number != ScaleDegree.root.number {
            for alteration in -1...1 {
                candidates.append(.add(ScaleDegree(number: number, alteration: alteration)))
            }
        }

        var legal: [ScaleEdit] = []
        for candidate in candidates where (try? applying(candidate)) != nil {
            legal.append(candidate)
        }
        return legal
    }
}
