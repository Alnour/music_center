// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation

// The saved-state sections: settings as stored, a scripted practice session, and the
// library file. Android stores the same JSON, so a player's data reads the same anywhere.

// MARK: - settings

struct LabelStyleAnswer: Encodable {
    let rawValue: String
    let title: String
    let accessibilityDescription: String
}

struct SettingsSection: Encodable {
    /// `PracticeSettings()` encoded with sorted keys.
    let defaultJSON: String
    /// Every optional field set: a saved scale, the keyboard, one octave.
    let everyOptionalSetJSON: String
    /// Top-level keys of `everyOptionalSetJSON`, sorted.
    let allKeys: [String]
    let fretCountChoices: [Int]
    let keyboardOctaveChoices: [Int]
    let defaultKeyboardOctaves: Int
    let labelStyles: [LabelStyleAnswer]
    let instruments: [String]
    /// Settings saved by the first release, byte for byte.
    let firstReleaseJSON: String
    /// The same settings as this version reads them, re-encoded with sorted keys.
    let firstReleaseDecodedJSON: String
}

func settingsSection() throws -> SettingsSection {
    var everything = PracticeSettings()
    everything.root = NoteName(.e, -1)
    everything.scaleType = .dorian
    everything.tuning = .dropD
    everything.fretCount = 24
    everything.labelStyle = .degrees
    everything.isLeftHanded = true
    everything.thinStringOnTop = false
    everything.tvUsesDarkTheme = false
    everything.customScale = Fixtures.augmentedMajor
    everything.selectedInstrument = .keyboard
    everything.selectedKeyboardOctaves = 1

    let everythingData = try JSONEncoder().encode(everything)
    let object = try JSONSerialization.jsonObject(with: everythingData)
    guard let dictionary = object as? [String: Any] else {
        throw AnswersError.fixtureFailed("settings are not a JSON object")
    }
    let allKeys = dictionary.keys.sorted()

    var labelStyles: [LabelStyleAnswer] = []
    for style in NoteLabelStyle.allCases {
        let answer = LabelStyleAnswer(
            rawValue: style.rawValue,
            title: style.title,
            accessibilityDescription: style.accessibilityDescription
        )
        labelStyles.append(answer)
    }
    var instruments: [String] = []
    for instrument in Instrument.allCases {
        instruments.append(instrument.rawValue)
    }

    let firstReleaseData = Data(Fixtures.firstReleaseSettingsJSON.utf8)
    let firstRelease = try JSONDecoder().decode(PracticeSettings.self, from: firstReleaseData)

    return SettingsSection(
        defaultJSON: try jsonText(PracticeSettings()),
        everyOptionalSetJSON: try jsonText(everything),
        allKeys: allKeys,
        fretCountChoices: PracticeSettings.fretCountChoices,
        keyboardOctaveChoices: PracticeSettings.keyboardOctaveChoices,
        defaultKeyboardOctaves: PracticeSettings.defaultKeyboardOctaves,
        labelStyles: labelStyles,
        instruments: instruments,
        firstReleaseJSON: Fixtures.firstReleaseSettingsJSON,
        firstReleaseDecodedJSON: try jsonText(firstRelease)
    )
}

// MARK: - session

struct ChipAnswer: Encodable {
    let formulaNumeral: String
    let name: String
    let notes: [String]
    /// "unchanged", "changed", "removed" or "added".
    let kind: String
    let caption: String
    let accessibilityLabel: String
    /// True when this card is the ringed chord (`ringedChord == chord`, degree included).
    let isRinged: Bool
}

struct BoardChangeAnswer: Encodable {
    let title: String
    let editSentence: String
    let before: String
    let after: String
}

struct LessonAnswer: Encodable {
    let title: String
    let afterName: String
    let result: String
}

/// What the session shows after one scripted step.
struct SessionStepAnswer: Encodable {
    let step: String
    let scaleTitle: String
    /// The settings as the session would save them, with sorted keys.
    let settingsJSON: String
    let ringedChord: String?
    /// Sorted by pitch class, then letter, then accidental.
    let ringedNotes: [String]
    /// `boardChange` as stored, whether or not the board still shows its result.
    let hasStoredBoardChange: Bool
    let currentBoardChange: BoardChangeAnswer?
    let chips: [ChipAnswer]
    let lessons: [LessonAnswer]
    let savedScaleCount: Int
}

/// `.builtIn(major)` as "builtIn:major"; saved scales as "custom:<UUID>".
func referenceText(_ reference: ScaleReference) -> String {
    switch reference {
    case .builtIn(let type): return "builtIn:\(type.rawValue)"
    case .custom(let id): return "custom:\(id.uuidString)"
    }
}

@MainActor
func snapshot(_ step: String, _ session: PracticeSession, _ library: ScaleLibrary) throws
    -> SessionStepAnswer
{
    var chips: [ChipAnswer] = []
    let contents = BoardChordChipContent.chips(
        for: session.scale,
        change: session.currentBoardChange
    )
    for content in contents {
        let chip = ChipAnswer(
            formulaNumeral: content.chord.formulaNumeral,
            name: content.chord.triad.name,
            notes: displayNames(content.chord.notes),
            kind: String(describing: content.kind),
            caption: content.caption,
            accessibilityLabel: content.accessibilityLabel,
            isRinged: content.chord == session.ringedChord
        )
        chips.append(chip)
    }
    var currentChange: BoardChangeAnswer?
    if let change = session.currentBoardChange {
        currentChange = BoardChangeAnswer(
            title: change.transformation.title,
            editSentence: change.transformation.editSentence,
            before: referenceText(change.before),
            after: referenceText(change.after)
        )
    }
    var lessons: [LessonAnswer] = []
    for lesson in library.lessons {
        let answer = LessonAnswer(
            title: lesson.title,
            afterName: lesson.transformation.after.name,
            result: referenceText(lesson.result)
        )
        lessons.append(answer)
    }
    return SessionStepAnswer(
        step: step,
        scaleTitle: session.scale.title,
        settingsJSON: try jsonText(session.settings),
        ringedChord: session.ringedChord?.spokenTitle,
        ringedNotes: sortedDisplayNames(session.ringedNotes),
        hasStoredBoardChange: session.boardChange != nil,
        currentBoardChange: currentChange,
        chips: chips,
        lessons: lessons,
        savedScaleCount: library.scales.count
    )
}

/// The chord on a degree of the scale on the board, for tapping a card.
@MainActor
func chord(numbered number: Int, in session: PracticeSession) throws -> ScaleChord {
    for chord in session.scale.letterTriads() where chord.degree.number == number {
        return chord
    }
    throw AnswersError.fixtureFailed("no chord on degree \(number) of \(session.scale.title)")
}

/// A fresh session (cleared settings suite, empty library in a temporary folder) driven
/// through the steps a player takes on the Scales screen, with a snapshot after each.
@MainActor
func sessionSection() throws -> [SessionStepAnswer] {
    let suiteName = "answers"
    guard let defaults = UserDefaults(suiteName: suiteName) else {
        throw AnswersError.fixtureFailed("no settings suite")
    }
    defaults.removePersistentDomain(forName: suiteName)
    let folder = FileManager.default.temporaryDirectory
        .appending(path: "ExpectedAnswers-\(ProcessInfo.processInfo.processIdentifier)")
    try? FileManager.default.removeItem(at: folder)
    defer {
        defaults.removePersistentDomain(forName: suiteName)
        try? FileManager.default.removeItem(at: folder)
    }
    let library = ScaleLibrary(fileURL: folder.appending(path: "ScaleLibrary.json"))
    let session = PracticeSession(defaults: defaults)
    var steps: [SessionStepAnswer] = []

    steps.append(try snapshot("fresh session", session, library))

    session.toggleRing(for: try chord(numbered: 5, in: session))
    steps.append(try snapshot("ring the V chord of G Major", session, library))

    session.choose(.builtIn(.major), root: NoteName(.d), from: library)
    steps.append(
        try snapshot("choose D Major: the rings stay, no card is marked", session, library)
    )

    session.choose(.builtIn(.major), root: NoteName(.g), from: library)
    steps.append(try snapshot("choose G Major again", session, library))

    try session.transform(.lower(ScaleDegree(symbol: "7")), from: library)
    steps.append(try snapshot("lower the 7th: G Mixolydian, kept as a lesson", session, library))

    session.choose(.builtIn(.minorPentatonic), root: NoteName(.a), from: library)
    steps.append(
        try snapshot("choose A Minor Pentatonic: the change is no longer shown", session, library)
    )

    session.revertBoardChange(from: library)
    steps.append(
        try snapshot("revert with no change on the board: nothing happens", session, library)
    )

    session.choose(.builtIn(.mixolydian), root: NoteName(.g), from: library)
    steps.append(
        try snapshot("choose G Mixolydian: the stored change shows again", session, library)
    )

    session.revertBoardChange(from: library)
    steps.append(try snapshot("revert: G Major, the lesson stays", session, library))

    session.choose(.custom(Fixtures.unknownID), from: library)
    steps.append(try snapshot("choose an unknown saved scale: nothing changes", session, library))

    let tonic = try chord(numbered: 1, in: session)
    session.toggleRing(for: tonic)
    steps.append(try snapshot("ring the I chord", session, library))

    session.toggleRing(for: tonic)
    steps.append(try snapshot("ring the I chord again: the rings clear", session, library))

    return steps
}

// MARK: - library

struct LibrarySection: Encodable {
    /// The file's bytes as `ScaleLibrary` writes them (`ScaleLibrary.encoded`): keys in
    /// declaration order, not sorted. Compare parsed JSON trees, not bytes, on Android.
    let documentJSON: String
    let scaleCount: Int
    let lessonTitles: [String]
}

func librarySection() throws -> LibrarySection {
    let lowered = try ScaleTransformation(
        root: NoteName(.g),
        before: ScaleType.major.definition,
        edit: .lower(ScaleDegree(symbol: "7"))
    )
    let loweredLesson = TransformationLesson(
        id: Fixtures.secondID,
        date: Fixtures.date,
        transformation: lowered.naming(ScaleType.mixolydian.displayName),
        result: .builtIn(.mixolydian)
    )
    let added = try ScaleTransformation(
        root: NoteName(.g),
        before: ScaleType.major.definition,
        edit: .add(ScaleDegree(symbol: "♯5"))
    )
    // The added note makes a scale no built-in has, so it is saved and the lesson is filed
    // under it, the way `ScaleLibrary.saveLesson` files a new result.
    let bebopMajor = CustomScale(
        id: Fixtures.firstID,
        definition: ScaleDefinition(name: "Bebop Major", degrees: added.after.degrees),
        createdAt: Fixtures.date
    )
    let addedLesson = TransformationLesson(
        id: Fixtures.thirdID,
        date: Fixtures.date,
        transformation: added.naming(bebopMajor.definition.name),
        result: .custom(bebopMajor.id)
    )
    let document = ScaleLibrary.LibraryDocument(
        scales: [bebopMajor],
        lessons: [loweredLesson, addedLesson]
    )
    let data = try ScaleLibrary.encoded(document)
    guard let text = String(data: data, encoding: .utf8) else {
        throw AnswersError.notUTF8
    }
    return LibrarySection(
        documentJSON: text,
        scaleCount: document.scales.count,
        lessonTitles: [loweredLesson.title, addedLesson.title]
    )
}
