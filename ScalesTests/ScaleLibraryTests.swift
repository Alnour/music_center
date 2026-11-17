import Foundation
import Testing

@testable import Scales

/// A library file in its own temporary folder, removed when the test ends.
private struct TemporaryLibraryFile {
    let folder = FileManager.default.temporaryDirectory
        .appending(path: "ScaleLibraryTests-\(UUID().uuidString)")

    var url: URL {
        folder.appending(path: "ScaleLibrary.json")
    }

    func remove() {
        try? FileManager.default.removeItem(at: folder)
    }
}

/// G major with the 7th lowered: lands on G Mixolydian, a built-in scale.
private func gMixolydianLesson() throws -> ScaleTransformation {
    try ScaleTransformation(
        root: NoteName(.g),
        before: ScaleType.major.definition,
        edit: .lower(ScaleDegree(symbol: "7"))
    )
}

/// Major with the 5th raised (1 2 3 4 ♯5 6 7): no built-in scale has these notes.
private func raisedFifthLesson(root: NoteName = NoteName(.g)) throws -> ScaleTransformation {
    try ScaleTransformation(
        root: root,
        before: ScaleType.major.definition,
        edit: .raise(ScaleDegree(symbol: "5"))
    )
}

@MainActor
struct ScaleLibraryTests {
    @Test func startsEmptyWithoutAFile() {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        #expect(library.scales.isEmpty)
        #expect(library.lessons.isEmpty)
    }

    @Test func aKnownResultIsFiledUnderTheBuiltInScale() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)

        let lesson = try library.saveLesson(gMixolydianLesson(), newScaleName: "")

        #expect(lesson.result == .builtIn(.mixolydian))
        #expect(lesson.title == "G Major → G Mixolydian")
        #expect(library.scales.isEmpty)
        #expect(library.lessons(for: .builtIn(.mixolydian)) == [lesson])
    }

    @Test func aNewResultNeedsAName() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let transformation = try raisedFifthLesson()

        #expect(throws: ScaleLibraryError.emptyName) {
            try library.saveLesson(transformation, newScaleName: "   ")
        }
        #expect(library.lessons.isEmpty)
    }

    @Test func aNewResultIsSavedAsTheirOwnScale() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)

        let lesson = try library.saveLesson(raisedFifthLesson(), newScaleName: " Augmented Major ")

        let saved = try #require(library.scales.first)
        #expect(library.scales.count == 1)
        #expect(saved.definition.name == "Augmented Major")
        #expect(saved.definition.formula == "1 2 3 4 ♯5 6 7")
        #expect(lesson.result == .custom(saved.id))
        #expect(lesson.title == "G Major → G Augmented Major")
    }

    @Test func savingTheSameNewScaleAgainFilesItUnderTheFirst() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let first = try library.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")

        let second = try library.saveLesson(
            raisedFifthLesson(root: NoteName(.d)),
            newScaleName: "Some Other Name"
        )

        #expect(library.scales.count == 1)
        #expect(second.result == first.result)
        #expect(second.title == "D Major → D Augmented Major")
    }

    @Test func scalesAndLessonsSurviveARelaunch() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let firstLaunch = ScaleLibrary(fileURL: file.url)
        _ = try firstLaunch.saveLesson(gMixolydianLesson(), newScaleName: "")
        _ = try firstLaunch.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")

        let secondLaunch = ScaleLibrary(fileURL: file.url)

        #expect(secondLaunch.scales == firstLaunch.scales)
        #expect(secondLaunch.lessons == firstLaunch.lessons)
        #expect(secondLaunch.lessons.count == 2)
    }

    @Test func renamingUpdatesTheScaleAndItsLessons() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let lesson = try library.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")
        let id = try #require(library.scales.first?.id)

        try library.rename(id, to: "Ionian ♯5")

        #expect(library.scale(id: id)?.definition.name == "Ionian ♯5")
        #expect(library.scale(id: id)?.definition.formula == "1 2 3 4 ♯5 6 7")
        #expect(library.lessons.first?.id == lesson.id)
        #expect(library.lessons.first?.title == "G Major → G Ionian ♯5")
    }

    @Test func renamingRefusesABlankNameOrAnUnknownScale() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        _ = try library.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")
        let id = try #require(library.scales.first?.id)

        #expect(throws: ScaleLibraryError.emptyName) { try library.rename(id, to: "") }
        #expect(throws: ScaleLibraryError.scaleNotFound) {
            try library.rename(UUID(), to: "Anything")
        }
        #expect(library.scale(id: id)?.definition.name == "Augmented Major")
    }

    @Test func deletingAScaleRemovesItsLessonsOnly() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let kept = try library.saveLesson(gMixolydianLesson(), newScaleName: "")
        _ = try library.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")
        let id = try #require(library.scales.first?.id)

        try library.deleteScale(id)

        #expect(library.scales.isEmpty)
        #expect(library.lessons == [kept])
        #expect(throws: ScaleLibraryError.scaleNotFound) { try library.deleteScale(id) }
    }

    @Test func deletingALesson() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        let lesson = try library.saveLesson(gMixolydianLesson(), newScaleName: "")

        try library.deleteLesson(lesson.id)

        #expect(library.lessons.isEmpty)
        #expect(throws: ScaleLibraryError.lessonNotFound) { try library.deleteLesson(lesson.id) }
    }

    @Test func anUnreadableFileIsSetAsideAndTheLibraryStartsEmpty() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        try FileManager.default.createDirectory(at: file.folder, withIntermediateDirectories: true)
        let garbage = Data("not a library".utf8)
        try garbage.write(to: file.url)

        let library = ScaleLibrary(fileURL: file.url)

        #expect(library.scales.isEmpty)
        let setAside = ScaleLibrary.unreadableCopyURL(for: file.url)
        #expect(try Data(contentsOf: setAside) == garbage)
        #expect(!FileManager.default.fileExists(atPath: file.url.path()))
    }

    @Test func aFailedSaveLeavesTheLibraryUnchanged() throws {
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        // A plain file where the library's folder should be, so the folder can't be made.
        try FileManager.default.createDirectory(at: file.folder, withIntermediateDirectories: true)
        let blocker = file.folder.appending(path: "blocker")
        try Data().write(to: blocker)
        let library = ScaleLibrary(fileURL: blocker.appending(path: "ScaleLibrary.json"))
        let transformation = try gMixolydianLesson()

        do {
            _ = try library.saveLesson(transformation, newScaleName: "")
            Issue.record("saving should have failed")
        } catch {
            guard case .couldNotSave = error else {
                Issue.record("expected couldNotSave, got \(error)")
                return
            }
        }
        #expect(library.lessons.isEmpty)
    }
}

/// The board's settings and the player's own scales.
@MainActor
struct CustomScaleSettingsTests {
    @Test func settingsSavedBeforeCustomScalesStillLoad() throws {
        let savedByTheFirstRelease = """
            {"root":{"letter":4,"accidental":0},"scaleType":"dorian","tuning":"standard",
            "fretCount":15,"labelStyle":"noteNames","isLeftHanded":false,
            "thinStringOnTop":true,"tvUsesDarkTheme":true}
            """
        let settings = try JSONDecoder().decode(
            PracticeSettings.self,
            from: Data(savedByTheFirstRelease.utf8)
        )
        #expect(settings.scaleType == .dorian)
        #expect(settings.customScale == nil)
        #expect(settings.scaleReference == .builtIn(.dorian))
    }

    @Test func builtInSettingsAreSavedExactlyAsBefore() throws {
        let data = try JSONEncoder().encode(PracticeSettings())
        let text = try #require(String(data: data, encoding: .utf8))
        #expect(!text.contains("customScale"))
    }

    @Test func theBoardDrawsAChosenScaleAndRemembersIt() throws {
        let suiteName = "ScalesTests-\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        _ = try library.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")
        let saved = try #require(library.scales.first)
        let session = PracticeSession(defaults: defaults)

        session.choose(.custom(saved.id), from: library)

        #expect(session.scale.title == "G Augmented Major")
        #expect(session.settings.scaleReference == .custom(saved.id))
        var names: [String] = []
        for tone in session.scale.tones {
            names.append(tone.name.displayName)
        }
        #expect(names == ["G", "A", "B", "C", "D♯", "E", "F♯"])
        let relaunched = PracticeSession(defaults: defaults)
        #expect(relaunched.scale.title == "G Augmented Major")
    }

    @Test func choosingABuiltInScaleOrAnUnknownIdBehaves() throws {
        let suiteName = "ScalesTests-\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        _ = try library.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")
        let saved = try #require(library.scales.first)
        let session = PracticeSession(defaults: defaults)
        session.choose(.custom(saved.id), from: library)

        session.choose(.custom(UUID()), from: library)
        #expect(session.settings.scaleReference == .custom(saved.id), "unknown id is ignored")

        session.choose(.builtIn(.lydian), from: library)
        #expect(session.settings.customScale == nil)
        #expect(session.scale.title == "G Lydian")

        session.choose(.custom(saved.id), root: NoteName(.e, -1), from: library)
        #expect(session.scale.title == "E♭ Augmented Major")
    }

    @Test func renamingAndDeletingReachTheBoard() throws {
        let suiteName = "ScalesTests-\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        let file = TemporaryLibraryFile()
        defer { file.remove() }
        let library = ScaleLibrary(fileURL: file.url)
        _ = try library.saveLesson(raisedFifthLesson(), newScaleName: "Augmented Major")
        let saved = try #require(library.scales.first)
        let session = PracticeSession(defaults: defaults)
        session.settings.scaleType = .dorian
        session.choose(.custom(saved.id), from: library)

        try library.rename(saved.id, to: "Ionian ♯5")
        session.refreshCustomScale(from: library)
        #expect(session.scale.title == "G Ionian ♯5")

        try library.deleteScale(saved.id)
        session.refreshCustomScale(from: library)
        #expect(session.settings.customScale == nil)
        #expect(session.scale.title == "G Dorian", "falls back to the built-in chosen before")
    }

    @Test func settingsSavedBeforeTheKeyboardDefaultToTheGuitar() throws {
        let savedByTheFirstRelease = """
            {"root":{"letter":4,"accidental":0},"scaleType":"dorian","tuning":"standard",
            "fretCount":15,"labelStyle":"noteNames","isLeftHanded":false,
            "thinStringOnTop":true,"tvUsesDarkTheme":true}
            """
        let settings = try JSONDecoder().decode(
            PracticeSettings.self,
            from: Data(savedByTheFirstRelease.utf8)
        )
        #expect(settings.instrument == nil)
        #expect(settings.selectedInstrument == .guitar)
    }

    @Test func guitarSettingsAreSavedWithoutAnInstrumentKey() throws {
        var settings = PracticeSettings()
        settings.selectedInstrument = .keyboard
        settings.selectedInstrument = .guitar
        #expect(settings == PracticeSettings(), "choosing the guitar again stores nil")
        let data = try JSONEncoder().encode(settings)
        let text = try #require(String(data: data, encoding: .utf8))
        #expect(!text.contains("instrument"))
        #expect(!text.contains("keyboardOctaves"))
    }

    @Test func theKeyboardGetsItsOwnTVClip() {
        var keyboard = PracticeSettings()
        keyboard.selectedInstrument = .keyboard

        #expect(keyboard.instrument == .keyboard)
        #expect(ClipCache.key(for: keyboard) != ClipCache.key(for: PracticeSettings()))
        #expect(ClipCache.bundledShortClip(for: keyboard) == nil)
    }

    @Test func aCustomScaleGetsItsOwnTVClip() {
        let definition = ScaleDefinition(
            name: "Augmented Major",
            degrees: ScaleType.major.definition.degrees
        )
        var custom = PracticeSettings()
        custom.customScale = CustomScale(definition: definition)
        var renamed = custom
        renamed.customScale?.definition.name = "Ionian ♯5"

        #expect(ClipCache.key(for: custom) != ClipCache.key(for: PracticeSettings()))
        #expect(ClipCache.key(for: custom) != ClipCache.key(for: renamed))
        #expect(ClipCache.bundledShortClip(for: custom) == nil)
    }
}
