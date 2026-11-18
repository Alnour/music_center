// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation

// The note-change sections: every edit each scale allows, what it is called in the menus
// and lessons, what it turns the scale into, and what happens to every chord.

// MARK: - edits

/// An edit as readable fields; `json` is the exact text Swift writes for it in a lesson.
struct EditAnswer: Encodable {
    /// "lower", "raise", "remove" or "add".
    let kind: String
    let degree: String
    let json: String

    init(_ edit: ScaleEdit) throws {
        switch edit {
        case .lower: kind = "lower"
        case .raise: kind = "raise"
        case .remove: kind = "remove"
        case .add: kind = "add"
        }
        degree = edit.degree.label
        json = try jsonText(edit)
    }
}

struct ChordSummaryAnswer: Encodable {
    let numeral: String
    let formulaNumeral: String
    let name: String

    init(_ chord: ScaleChord) {
        numeral = chord.numeral
        formulaNumeral = chord.formulaNumeral
        name = chord.triad.name
    }
}

struct ChordChangeAnswer: Encodable {
    /// "unchanged", "changed", "removed" or "added".
    let kind: String
    let before: ChordSummaryAnswer?
    let after: ChordSummaryAnswer?
    let notesGone: [String]
    let notesNew: [String]
    let summary: String
}

func chordChangeAnswers(_ report: ChordChangeReport) -> [ChordChangeAnswer] {
    var answers: [ChordChangeAnswer] = []
    for change in report.changes {
        var before: ChordSummaryAnswer?
        if let chord = change.before {
            before = ChordSummaryAnswer(chord)
        }
        var after: ChordSummaryAnswer?
        if let chord = change.after {
            after = ChordSummaryAnswer(chord)
        }
        let answer = ChordChangeAnswer(
            kind: String(describing: change.kind),
            before: before,
            after: after,
            notesGone: displayNames(change.notesGone),
            notesNew: displayNames(change.notesNew),
            summary: change.summary
        )
        answers.append(answer)
    }
    return answers
}

struct LegalEditAnswer: Encodable {
    let edit: EditAnswer
    let shortLabel: String
    /// `ScaleEdit.menuTitle(above:)` in the section's key.
    let menuTitle: String
    /// `ScaleEdit.sentence(above:)` in the section's key.
    let sentence: String
    /// The result's working name, "Major ♭7", before it is recognised or named.
    let workingName: String
    let formula: String
    /// `ScaleTransformation.title` with the working name: "C Major → C Major ♭7".
    let title: String
    /// The name `ScaleIdentifier.match` gives the result among built-ins, or null.
    let identifiedAs: String?
    let changesAnyChord: Bool
    let chordChanges: [ChordChangeAnswer]
}

struct RejectedEditAnswer: Encodable {
    let edit: EditAnswer
    /// The `ScaleEditError` case name.
    let error: String
    /// For `samePitchAsAnotherNote`, the degree already on that pitch; else null.
    let clashesWith: String?
}

struct ScaleEditsAnswer: Encodable {
    let scaleType: String
    let root: String
    let formula: String
    let legal: [LegalEditAnswer]
    let rejected: [RejectedEditAnswer]
}

/// The candidates `legalEdits()` tries, in the same order: lower, raise and remove for
/// every note but the root, then every degree 2 to 7 flat, natural and sharp as an addition.
func editCandidates(for definition: ScaleDefinition) -> [ScaleEdit] {
    var candidates: [ScaleEdit] = []
    for degree in definition.degrees where degree.number != ScaleDegree.root.number {
        candidates.append(.lower(degree))
        candidates.append(.raise(degree))
        candidates.append(.remove(degree))
    }
    for number in ScaleDegree.numbers where number != ScaleDegree.root.number {
        for alteration in -1...1 {
            candidates.append(.add(ScaleDegree(number: number, alteration: alteration)))
        }
    }
    return candidates
}

func legalEditAnswer(_ edit: ScaleEdit, root: NoteName, type: ScaleType) throws -> LegalEditAnswer {
    let transformation = try ScaleTransformation(root: root, before: type.definition, edit: edit)
    let match = ScaleIdentifier.match(transformation.after, among: [])
    let report = transformation.chordChanges
    return LegalEditAnswer(
        edit: try EditAnswer(edit),
        shortLabel: edit.shortLabel,
        menuTitle: edit.menuTitle(above: root),
        sentence: edit.sentence(above: root),
        workingName: transformation.after.name,
        formula: transformation.after.formula,
        title: transformation.title,
        identifiedAs: match?.name,
        changesAnyChord: report.changesAnyChord,
        chordChanges: chordChangeAnswers(report)
    )
}

func rejectedEditAnswer(_ edit: ScaleEdit, error: ScaleEditError) throws -> RejectedEditAnswer {
    var caseName: String
    var clashesWith: String?
    switch error {
    case .rootCannotChange: caseName = "rootCannotChange"
    case .degreeNotInScale: caseName = "degreeNotInScale"
    case .degreeAlreadyInScale: caseName = "degreeAlreadyInScale"
    case .noSuchDegree: caseName = "noSuchDegree"
    case .samePitchAsAnotherNote(let other):
        caseName = "samePitchAsAnotherNote"
        clashesWith = other.label
    case .alterationTooFar: caseName = "alterationTooFar"
    case .tooFewNotes: caseName = "tooFewNotes"
    }
    return RejectedEditAnswer(edit: try EditAnswer(edit), error: caseName, clashesWith: clashesWith)
}

/// Why the scale refuses an edit, or nil when it accepts it.
func refusal(of edit: ScaleEdit, by definition: ScaleDefinition) -> ScaleEditError? {
    do {
        _ = try definition.applying(edit)
        return nil
    } catch {
        return error
    }
}

/// Every scale type in C: each legal edit in full, and each refused candidate with why.
func editsSection() throws -> [ScaleEditsAnswer] {
    let root = NoteName(.c)
    var sections: [ScaleEditsAnswer] = []
    for type in ScaleType.allCases {
        let definition = type.definition
        var legal: [LegalEditAnswer] = []
        var rejected: [RejectedEditAnswer] = []
        for candidate in editCandidates(for: definition) {
            if let error = refusal(of: candidate, by: definition) {
                rejected.append(try rejectedEditAnswer(candidate, error: error))
            } else {
                legal.append(try legalEditAnswer(candidate, root: root, type: type))
            }
        }
        let legalCount = definition.legalEdits().count
        guard legalCount == legal.count else {
            throw AnswersError.fixtureFailed("legalEdits() disagrees for \(type.rawValue)")
        }
        let answer = ScaleEditsAnswer(
            scaleType: type.rawValue,
            root: root.displayName,
            formula: definition.formula,
            legal: legal,
            rejected: rejected
        )
        sections.append(answer)
    }
    return sections
}

// MARK: - identifier

struct BuiltInMatchAnswer: Encodable {
    let scaleType: String
    let matchedName: String?
    let matchedType: String?
}

struct EditResultMatchAnswer: Encodable {
    let scaleType: String
    let edit: String
    let resultFormula: String
    let matchedName: String?
}

struct CustomMatchAnswer: Encodable {
    let description: String
    let formula: String
    let savedScales: [String]
    let matchedName: String?
    /// "builtIn", "custom" or null.
    let matchedKind: String?
}

struct IdentifierSection: Encodable {
    let builtIns: [BuiltInMatchAnswer]
    let editResults: [EditResultMatchAnswer]
    let custom: [CustomMatchAnswer]
}

func matchedType(_ match: ScaleMatch?) -> String? {
    guard let match else { return nil }
    switch match {
    case .builtIn(let type): return type.rawValue
    case .custom: return nil
    }
}

func matchedKind(_ match: ScaleMatch?) -> String? {
    guard let match else { return nil }
    switch match {
    case .builtIn: return "builtIn"
    case .custom: return "custom"
    }
}

func identifierSection() -> IdentifierSection {
    var builtIns: [BuiltInMatchAnswer] = []
    var editResults: [EditResultMatchAnswer] = []
    for type in ScaleType.allCases {
        let match = ScaleIdentifier.match(type.definition, among: [])
        let builtIn = BuiltInMatchAnswer(
            scaleType: type.rawValue,
            matchedName: match?.name,
            matchedType: matchedType(match)
        )
        builtIns.append(builtIn)
        for edit in type.definition.legalEdits() {
            guard let result = try? type.definition.applying(edit) else { continue }
            let resultMatch = ScaleIdentifier.match(result, among: [])
            let answer = EditResultMatchAnswer(
                scaleType: type.rawValue,
                edit: edit.shortLabel,
                resultFormula: result.formula,
                matchedName: resultMatch?.name
            )
            editResults.append(answer)
        }
    }
    return IdentifierSection(builtIns: builtIns, editResults: editResults, custom: customMatches())
}

/// A saved scale is recognised by its sound, even spelled differently; built-ins still win.
func customMatches() -> [CustomMatchAnswer] {
    let saved = Fixtures.augmentedMajor
    let respelled = ScaleDefinition(name: "Respelled", degrees: Fixtures.degrees("1 2 3 4 ♭6 6 7"))
    let majorCopy = CustomScale(
        id: Fixtures.secondID,
        definition: ScaleDefinition(name: "My Major", degrees: ScaleType.major.degrees),
        createdAt: Fixtures.date
    )
    struct Case {
        let description: String
        let definition: ScaleDefinition
        let saved: [CustomScale]
    }
    let cases = [
        Case(description: "saved scale, respelled ♯5 as ♭6", definition: respelled, saved: [saved]),
        Case(description: "same notes with nothing saved", definition: respelled, saved: []),
        Case(
            description: "a saved copy of Major loses to the built-in",
            definition: ScaleType.major.definition,
            saved: [majorCopy]
        ),
    ]
    var answers: [CustomMatchAnswer] = []
    for item in cases {
        let match = ScaleIdentifier.match(item.definition, among: item.saved)
        var savedNames: [String] = []
        for scale in item.saved {
            savedNames.append(scale.definition.name)
        }
        let answer = CustomMatchAnswer(
            description: item.description,
            formula: item.definition.formula,
            savedScales: savedNames,
            matchedName: match?.name,
            matchedKind: matchedKind(match)
        )
        answers.append(answer)
    }
    return answers
}
