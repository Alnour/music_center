/// What one edit did to one chord of the scale.
struct ChordChange: Hashable, Sendable {
    enum Kind: Hashable, Sendable {
        /// Same chord, same notes.
        case unchanged
        /// A chord on the same letter is still there, but its notes or quality moved.
        case changed
        /// The scale no longer makes a chord on this letter.
        case removed
        /// The scale now makes a chord on a letter that had none.
        case added
    }

    let kind: Kind
    let before: ScaleChord?
    let after: ScaleChord?
    /// Notes the chord had before and lost, for highlighting.
    let notesGone: [NoteName]
    /// Notes the chord has now and didn't have, for highlighting.
    let notesNew: [NoteName]
    /// The change told the way a teacher would say it.
    let summary: String
}

/// The lesson's heart: every chord of the scale before and after one edit, side by side.
///
/// Chords are matched by the letter they are built on, because that is how a musician
/// follows a chord through a change: the chord on the fifth step was G and is now Gm. When
/// the edit moves the note a chord is built on (♭3 raised to 3), the old and new chords on
/// that letter are still matched, so ♭III C becomes iii C♯° rather than one chord vanishing
/// and another appearing.
struct ChordChangeReport: Sendable {
    /// One entry per letter that has a chord before or after, in letter order.
    let changes: [ChordChange]

    init(before: Scale, after: Scale) {
        var beforeByNumber: [Int: [ScaleChord]] = [:]
        for chord in before.letterTriads() {
            beforeByNumber[chord.degree.number, default: []].append(chord)
        }
        var afterByNumber: [Int: [ScaleChord]] = [:]
        for chord in after.letterTriads() {
            afterByNumber[chord.degree.number, default: []].append(chord)
        }
        var afterNoteNames = Set<NoteName>()
        for tone in after.tones {
            afterNoteNames.insert(tone.name)
        }

        var changes: [ChordChange] = []
        for number in ScaleDegree.numbers {
            let oldChords = beforeByNumber[number] ?? []
            var newChords = afterByNumber[number] ?? []
            var leftoverOldChords: [ScaleChord] = []
            var pairs: [(ScaleChord, ScaleChord)] = []
            for oldChord in oldChords {
                if let index = newChords.firstIndex(where: { $0.degree == oldChord.degree }) {
                    pairs.append((oldChord, newChords.remove(at: index)))
                } else {
                    leftoverOldChords.append(oldChord)
                }
            }
            if leftoverOldChords.count == 1, newChords.count == 1 {
                pairs.append((leftoverOldChords[0], newChords[0]))
                leftoverOldChords = []
                newChords = []
            }

            for (oldChord, newChord) in pairs {
                changes.append(Self.pairedChange(from: oldChord, to: newChord))
            }
            for oldChord in leftoverOldChords {
                changes.append(Self.removal(of: oldChord, keeping: afterNoteNames))
            }
            for newChord in newChords {
                changes.append(Self.addition(of: newChord))
            }
        }
        self.changes = changes
    }

    /// True when at least one chord changed, appeared or disappeared.
    var changesAnyChord: Bool {
        for change in changes where change.kind != .unchanged {
            return true
        }
        return false
    }

    private static func pairedChange(from oldChord: ScaleChord, to newChord: ScaleChord)
        -> ChordChange
    {
        let notesGone = notes(of: oldChord, missingFrom: newChord.notes)
        let notesNew = notes(of: newChord, missingFrom: oldChord.notes)
        let isSame = oldChord.triad == newChord.triad && notesGone.isEmpty && notesNew.isEmpty
        if isSame {
            let summary = "\(oldChord.spokenTitle), stays the same: \(spelled(oldChord.notes))."
            return ChordChange(
                kind: .unchanged,
                before: oldChord,
                after: newChord,
                notesGone: [],
                notesNew: [],
                summary: summary
            )
        }
        let summary =
            "\(oldChord.spokenTitle), becomes \(newChord.spokenTitle): it had "
            + "\(spelled(oldChord.notes)) and now has \(spelled(newChord.notes))."
        return ChordChange(
            kind: .changed,
            before: oldChord,
            after: newChord,
            notesGone: notesGone,
            notesNew: notesNew,
            summary: summary
        )
    }

    private static func removal(of oldChord: ScaleChord, keeping afterNoteNames: Set<NoteName>)
        -> ChordChange
    {
        var missing: [NoteName] = []
        for note in oldChord.notes where !afterNoteNames.contains(note) {
            missing.append(note)
        }
        let reason: String
        if missing.isEmpty {
            reason = "its notes no longer stack into a major, minor, diminished or augmented chord"
        } else {
            let verb = missing.count == 1 ? "is" : "are"
            reason = "it needed \(list(missing)), which \(verb) no longer in the scale"
        }
        return ChordChange(
            kind: .removed,
            before: oldChord,
            after: nil,
            notesGone: missing,
            notesNew: [],
            summary: "\(oldChord.spokenTitle), is gone: \(reason)."
        )
    }

    private static func addition(of newChord: ScaleChord) -> ChordChange {
        let notes = spelled(newChord.notes)
        let summary = "\(newChord.spokenTitle), is new: \(notes) are all in the scale now."
        return ChordChange(
            kind: .added,
            before: nil,
            after: newChord,
            notesGone: [],
            notesNew: newChord.notes,
            summary: summary
        )
    }

    private static func notes(of chord: ScaleChord, missingFrom others: [NoteName]) -> [NoteName] {
        var missing: [NoteName] = []
        for note in chord.notes where !others.contains(note) {
            missing.append(note)
        }
        return missing
    }

    /// A chord's notes as musicians list them: "G, B, D".
    private static func spelled(_ notes: [NoteName]) -> String {
        var names: [String] = []
        for note in notes {
            names.append(note.displayName)
        }
        return names.joined(separator: ", ")
    }

    /// Missing notes in a sentence: "C", "C and E", "C, E and G".
    private static func list(_ notes: [NoteName]) -> String {
        var names: [String] = []
        for note in notes {
            names.append(note.displayName)
        }
        guard names.count > 1, let last = names.popLast() else {
            return names.first ?? ""
        }
        return names.joined(separator: ", ") + " and " + last
    }
}
