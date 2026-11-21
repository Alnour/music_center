// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

// Writes expected/answers.json: what the Swift app computes for every root, scale, edit,
// setting, session step, drum bar and drawing size, so the Android tests can check their
// Kotlin twins against the real Swift behaviour rather than against a second copy of it.
//
// Run from the repository root:
//     swift run --quiet ExpectedAnswers expected/answers.json      (or: make answers)
//
// The other Swift files in this folder are symlinks to the app's UI-free sources
// (Scales/Music, Session, Drums and the geometry files under Scales/Views), compiled in
// Swift 6 mode by the root Package.swift. Output is deterministic: sorted keys, sets
// written in a fixed order, doubles rounded to 6 decimals, fixed ids and dates.

import Foundation

/// The whole file. Each property is one top-level section; the Kotlin side has one reader
/// test per section and fails on a section it does not know.
struct Answers: Encodable {
    let meta: MetaSection
    let roots: [NoteAnswer]
    let scaleTypes: [ScaleTypeAnswer]
    let families: [FamilyAnswer]
    let tunings: [TuningAnswer]
    let bassTunings: [TuningAnswer]
    let spellings: [SpellingAnswer]
    let fretboards: [FretboardAnswer]
    let bassFretboards: [FretboardAnswer]
    let capo: CapoSection
    let keyboards: KeyboardsSection
    let edits: [ScaleEditsAnswer]
    let identifier: IdentifierSection
    let settings: SettingsSection
    let session: [SessionStepAnswer]
    let library: LibrarySection
    let keyChordTable: KeyChordTableSection
    let drums: DrumsSection
    let drumRender: DrumRenderSection
    let geometry: GeometrySection
}

@MainActor
func buildAnswers() throws -> Answers {
    Answers(
        meta: metaSection(),
        roots: rootsSection(),
        scaleTypes: scaleTypesSection(),
        families: familiesSection(),
        tunings: tuningsSection(),
        bassTunings: bassTuningsSection(),
        spellings: spellingsSection(),
        fretboards: fretboardsSection(),
        bassFretboards: bassFretboardsSection(),
        capo: capoSection(),
        keyboards: keyboardsSection(),
        edits: try editsSection(),
        identifier: identifierSection(),
        settings: try settingsSection(),
        session: try sessionSection(),
        library: try librarySection(),
        keyChordTable: keyChordTableSection(),
        drums: drumsSection(),
        drumRender: try drumRenderSection(),
        geometry: try geometrySection()
    )
}

@MainActor
func writeAnswers() throws {
    let arguments = CommandLine.arguments
    guard arguments.count == 2 else {
        throw AnswersError.missingArgument
    }
    let outputURL = URL(fileURLWithPath: arguments[1])
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.sortedKeys, .prettyPrinted, .withoutEscapingSlashes]
    var data = try encoder.encode(try buildAnswers())
    // A final newline keeps the file friendly to diff and cat.
    data.append(contentsOf: Array("\n".utf8))
    let folder = outputURL.deletingLastPathComponent()
    try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
    try data.write(to: outputURL, options: .atomic)
}

do {
    try writeAnswers()
} catch AnswersError.missingArgument {
    FileHandle.standardError.write(Data("usage: ExpectedAnswers <output.json>\n".utf8))
    exit(2)
} catch {
    FileHandle.standardError.write(Data("ExpectedAnswers failed: \(error)\n".utf8))
    exit(1)
}
