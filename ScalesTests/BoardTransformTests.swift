// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Testing

@testable import Scales

/// A library file in its own temporary folder, removed when the test ends.
private struct TemporaryLibraryFile {
    let folder = FileManager.default.temporaryDirectory
        .appending(path: "BoardTransformTests-\(UUID().uuidString)")

    var url: URL {
        folder.appending(path: "ScaleLibrary.json")
    }

    func remove() {
        try? FileManager.default.removeItem(at: folder)
    }
}

/// Settings that never touch the player's own, in a defaults suite of their own.
@MainActor
private func freshSession() -> PracticeSession {
    let suiteName = "BoardTransformTests-\(UUID().uuidString)"
    let defaults = UserDefaults(suiteName: suiteName)!
    defaults.removePersistentDomain(forName: suiteName)
    return PracticeSession(defaults: defaults)
}

/// Changing one note from the Scales screen: the board follows, the chords that used the
/// note change with it, and the change can be undone.
@MainActor
struct BoardTransformTests {
    @Test func loweringTheSeventhPutsMixolydianOnTheBoard() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()

        try session.transform(.lower(ScaleDegree(symbol: "7")), from: library)

        #expect(session.settings.scaleReference == .builtIn(.mixolydian))
        #expect(session.scale.title == "G Mixolydian")
        #expect(library.scales.isEmpty)
        #expect(library.lessons.count == 1)
        let change = try #require(session.currentBoardChange)
        #expect(change.before == .builtIn(.major))
        #expect(change.after == .builtIn(.mixolydian))
    }

    /// Every chord that had F♯ now has F instead: iii Bm becomes iii° B°, V D becomes v Dm,
    /// vii° F♯° becomes ♭VII F. The others keep their notes.
    @Test func chordsThatUsedTheOldNoteTakeTheNewOne() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()
        let fSharp = NoteName(.f, 1)
        let f = NoteName(.f)

        try session.transform(.lower(ScaleDegree(symbol: "7")), from: library)

        let change = try #require(session.currentBoardChange)
        var changedNames: [String] = []
        for chordChange in change.transformation.chordChanges.changes {
            let before = try #require(chordChange.before)
            let after = try #require(chordChange.after)
            if before.notes.contains(fSharp) {
                #expect(chordChange.kind == .changed)
                #expect(after.notes.contains(f))
                #expect(!after.notes.contains(fSharp))
                #expect(chordChange.notesGone == [fSharp])
                #expect(chordChange.notesNew == [f])
                changedNames.append("\(before.triad.name) → \(after.triad.name)")
            } else {
                #expect(chordChange.kind == .unchanged)
                #expect(before.notes == after.notes)
            }
        }
        #expect(changedNames == ["Bm → B°", "D → Dm", "F♯° → F"])
    }

    @Test func anUnknownResultIsSavedUnderItsWorkingName() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()

        try session.transform(.raise(ScaleDegree(symbol: "5")), from: library)

        let saved = try #require(library.scales.first)
        #expect(saved.definition.name == "Major ♯5")
        #expect(session.settings.scaleReference == .custom(saved.id))
        #expect(session.scale.title == "G Major ♯5")
    }

    @Test func revertingPutsTheStartingScaleBack() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()
        try session.transform(.lower(ScaleDegree(symbol: "7")), from: library)

        session.revertBoardChange(from: library)

        #expect(session.settings.scaleReference == .builtIn(.major))
        #expect(session.currentBoardChange == nil)
        #expect(library.lessons.count == 1)
    }

    @Test func pickingAnotherScaleOrKeyDropsTheChange() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()
        try session.transform(.lower(ScaleDegree(symbol: "7")), from: library)
        #expect(session.currentBoardChange != nil)

        session.settings.root = NoteName(.a)
        #expect(session.currentBoardChange == nil)

        session.settings.root = NoteName(.g)
        #expect(session.currentBoardChange != nil)
        session.choose(.builtIn(.major), from: library)
        #expect(session.currentBoardChange == nil)
    }

    @Test func anIllegalEditChangesNothing() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()

        try session.transform(.remove(ScaleDegree(symbol: "♭3")), from: library)

        #expect(session.settings.scaleReference == .builtIn(.major))
        #expect(session.currentBoardChange == nil)
        #expect(library.lessons.isEmpty)
    }

    @Test func pickingAChordRingsItsNotesUntilTheScaleLosesOne() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()
        let dominant = try #require(
            session.scale.letterTriads().first { $0.triad.name == "D" }
        )

        session.toggleRing(for: dominant)
        #expect(session.ringedNotes == [NoteName(.d), NoteName(.f, 1), NoteName(.a)])

        // F♯ leaves the scale with the lowered seventh, so the rings go too.
        try session.transform(.lower(ScaleDegree(symbol: "7")), from: library)
        #expect(session.ringedNotes.isEmpty)
        #expect(session.ringedChord == dominant, "the pick is kept while the scale changes")

        session.toggleRing(for: dominant)
        #expect(session.ringedChord == nil)
    }

    @Test func aBorrowedChordRingsOnlyTheNotesTheScaleHolds() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let session = freshSession()
        session.choose(.builtIn(.blues), root: NoteName(.a), from: library)
        let tonicSeventh = try #require(session.scale.boardChords().first)
        #expect(tonicSeventh.name == "A7")

        session.toggleRing(for: tonicSeventh)
        // C♯ is the chord's major third; the blues scale has C instead, so it stays unringed.
        #expect(session.ringedNotes == [NoteName(.a), NoteName(.e), NoteName(.g)])

        // Another scale's chord is not one of the cards, so it rings nothing.
        session.choose(.builtIn(.minorPentatonic), from: library)
        #expect(session.ringedNotes.isEmpty)
    }
}
