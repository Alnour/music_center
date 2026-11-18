// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation
import Observation

/// Why the library refused a change. The library is left exactly as it was.
enum ScaleLibraryError: Error, Equatable, Sendable {
    /// A new scale, or a renamed one, needs a name with something other than spaces in it.
    case emptyName
    case scaleNotFound
    case lessonNotFound
    /// The file could not be written, with the system's reason.
    case couldNotSave(String)
}

/// The player's own scales and the lessons that led to them, kept on this device.
///
/// Everything lives in one small JSON file in Application Support, written whole after
/// each change. One file keeps scales and lessons consistent with each other (a lesson is
/// never saved without the scale it is filed under), and Application Support is backed up
/// with the device and never cleared by the system. Writes are synchronous: the file is a
/// few kilobytes and only changes when the player taps something.
@MainActor
@Observable
final class ScaleLibrary {
    static let shared = ScaleLibrary(fileURL: ScaleLibrary.standardFileURL)

    nonisolated static var standardFileURL: URL {
        URL.applicationSupportDirectory.appending(path: "Scales/ScaleLibrary.json")
    }

    /// In the order they were made.
    private(set) var scales: [CustomScale]
    /// In the order they were saved, oldest first.
    private(set) var lessons: [TransformationLesson]

    @ObservationIgnored private let fileURL: URL

    /// Opens the library stored at the URL. A missing file is an empty library. A file
    /// that can't be read is moved aside, next to it, rather than overwritten, and the
    /// library starts empty.
    init(fileURL: URL) {
        self.fileURL = fileURL
        let document = Self.loadDocument(from: fileURL)
        self.scales = document.scales
        self.lessons = document.lessons
    }

    func scale(id: UUID) -> CustomScale? {
        for scale in scales where scale.id == id {
            return scale
        }
        return nil
    }

    /// Lessons that ended on the given scale, oldest first.
    func lessons(for reference: ScaleReference) -> [TransformationLesson] {
        var matching: [TransformationLesson] = []
        for lesson in lessons where lesson.result == reference {
            matching.append(lesson)
        }
        return matching
    }

    /// Keeps a transformation as a lesson and returns it.
    ///
    /// When the resulting scale is already known, built in or saved, the lesson is filed
    /// under that scale and takes its name; `newScaleName` is not used. Otherwise the
    /// result is saved as a new scale under `newScaleName`, trimmed of spaces.
    ///
    /// - Throws: `emptyName` when the result is new and the name is blank;
    ///   `couldNotSave` when the file can't be written.
    func saveLesson(_ transformation: ScaleTransformation, newScaleName: String)
        throws(ScaleLibraryError) -> TransformationLesson
    {
        var updatedScales = scales
        let named: ScaleTransformation
        let result: ScaleReference
        if let match = ScaleIdentifier.match(transformation.after, among: scales) {
            named = transformation.naming(match.name)
            result = match.reference
        } else {
            let name = try Self.validName(newScaleName)
            let definition = ScaleDefinition(name: name, degrees: transformation.after.degrees)
            let newScale = CustomScale(definition: definition)
            updatedScales.append(newScale)
            named = transformation.naming(name)
            result = .custom(newScale.id)
        }
        let lesson = TransformationLesson(transformation: named, result: result)
        var updatedLessons = lessons
        updatedLessons.append(lesson)
        try commit(scales: updatedScales, lessons: updatedLessons)
        return lesson
    }

    /// Renames a saved scale. Lessons filed under it take the new name too; its notes
    /// never change.
    ///
    /// - Throws: `emptyName`, `scaleNotFound`, or `couldNotSave`.
    func rename(_ id: UUID, to newName: String) throws(ScaleLibraryError) {
        let name = try Self.validName(newName)
        guard let index = scales.firstIndex(where: { $0.id == id }) else {
            throw .scaleNotFound
        }
        var updatedScales = scales
        updatedScales[index].definition.name = name
        var updatedLessons: [TransformationLesson] = []
        for lesson in lessons {
            guard lesson.result == .custom(id) else {
                updatedLessons.append(lesson)
                continue
            }
            let renamed = TransformationLesson(
                id: lesson.id,
                date: lesson.date,
                transformation: lesson.transformation.naming(name),
                result: lesson.result
            )
            updatedLessons.append(renamed)
        }
        try commit(scales: updatedScales, lessons: updatedLessons)
    }

    /// Deletes a saved scale together with the lessons filed under it.
    ///
    /// - Throws: `scaleNotFound`, or `couldNotSave`.
    func deleteScale(_ id: UUID) throws(ScaleLibraryError) {
        guard scale(id: id) != nil else { throw .scaleNotFound }
        var updatedScales: [CustomScale] = []
        for scale in scales where scale.id != id {
            updatedScales.append(scale)
        }
        var updatedLessons: [TransformationLesson] = []
        for lesson in lessons where lesson.result != .custom(id) {
            updatedLessons.append(lesson)
        }
        try commit(scales: updatedScales, lessons: updatedLessons)
    }

    /// - Throws: `lessonNotFound`, or `couldNotSave`.
    func deleteLesson(_ id: UUID) throws(ScaleLibraryError) {
        guard lessons.contains(where: { $0.id == id }) else { throw .lessonNotFound }
        var updatedLessons: [TransformationLesson] = []
        for lesson in lessons where lesson.id != id {
            updatedLessons.append(lesson)
        }
        try commit(scales: scales, lessons: updatedLessons)
    }

    // MARK: - Storage

    /// The shape of the file. The version is there so a later format can tell old files apart.
    /// Internal rather than private so the answers generator (`scripts/ExpectedAnswers`) can
    /// write one, pinning the exact bytes the Android library must read and write.
    struct LibraryDocument: Codable {
        var version = 1
        var scales: [CustomScale] = []
        var lessons: [TransformationLesson] = []
    }

    /// The file's bytes for a document: compact, dates as seconds since 2001, UUIDs in upper
    /// case. Keys are sorted because Foundation otherwise writes them in a different order
    /// on each run, and a file that only changes when its content does is easier to back up
    /// and compare. One function, so the answers generator writes exactly these bytes.
    nonisolated static func encoded(_ document: LibraryDocument) throws -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.sortedKeys]
        return try encoder.encode(document)
    }

    private static func validName(_ name: String) throws(ScaleLibraryError) -> String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { throw .emptyName }
        return trimmed
    }

    /// Writes the new state to disk first and only then shows it, so a failed write leaves
    /// both the file and the library as they were.
    private func commit(scales newScales: [CustomScale], lessons newLessons: [TransformationLesson])
        throws(ScaleLibraryError)
    {
        let document = LibraryDocument(scales: newScales, lessons: newLessons)
        do {
            let data = try Self.encoded(document)
            let folder = fileURL.deletingLastPathComponent()
            try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
            try data.write(to: fileURL, options: .atomic)
        } catch {
            throw .couldNotSave(error.localizedDescription)
        }
        scales = newScales
        lessons = newLessons
    }

    private static func loadDocument(from url: URL) -> LibraryDocument {
        guard let data = try? Data(contentsOf: url) else { return LibraryDocument() }
        do {
            return try JSONDecoder().decode(LibraryDocument.self, from: data)
        } catch {
            let setAside = unreadableCopyURL(for: url)
            try? FileManager.default.removeItem(at: setAside)
            try? FileManager.default.moveItem(at: url, to: setAside)
            return LibraryDocument()
        }
    }

    /// Where an unreadable library file is moved: "ScaleLibrary-unreadable.json" beside it.
    nonisolated static func unreadableCopyURL(for url: URL) -> URL {
        let baseName = url.deletingPathExtension().lastPathComponent
        return url.deletingLastPathComponent().appending(path: "\(baseName)-unreadable.json")
    }
}
