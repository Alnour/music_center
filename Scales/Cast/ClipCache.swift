// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CryptoKit
import Foundation

/// Keeps every board clip the app has made, keyed by the settings that produced it.
///
/// Making a clip takes a second or two, so going back to a scale that was already shown
/// replays the stored clip instead of making it again. G major with the default
/// settings never needs encoding at all: a short clip made ahead of time ships inside
/// the app and only has to be extended, so the very first cast uses a known-good file.
enum ClipCache {
    private static let bundledClipName = "GMajorBoard"
    /// Bumped whenever the way clips are made changes, so clips made the old way are
    /// never replayed. Version 2: one continuous stream instead of stitched copies.
    /// Version 3: a keyboard clip with no stored octave count shows two octaves, not one.
    private static let clipFormatVersion = 3

    /// A short, stable name for one combination of settings. Keys are sorted before
    /// hashing so the same settings always give the same name across launches.
    static func key(for settings: PracticeSettings) -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        var encoded = (try? encoder.encode(settings)) ?? Data()
        encoded.append(contentsOf: Data("v\(clipFormatVersion)".utf8))
        let digest = SHA256.hash(data: encoded)
        var hex = ""
        for byte in digest.prefix(8) {
            hex += String(format: "%02x", byte)
        }
        return hex
    }

    /// The finished clip for these settings, or nil when it still has to be made.
    static func clip(for settings: PracticeSettings) -> Data? {
        let url = fileURL(forKey: key(for: settings))
        return try? Data(contentsOf: url)
    }

    /// The short clip shipped inside the app for these settings, if there is one.
    static func bundledShortClip(for settings: PracticeSettings) -> URL? {
        guard settings == PracticeSettings() else { return nil }
        return Bundle.main.url(forResource: bundledClipName, withExtension: "mp4")
    }

    static func store(_ clip: Data, for settings: PracticeSettings) {
        let url = fileURL(forKey: key(for: settings))
        try? clip.write(to: url, options: .atomic)
        removeOldestClips()
    }

    /// Keeps the cache to the most recent clips; each five-minute clip is a few megabytes.
    private static let maximumStoredClips = 25

    private static func removeOldestClips() {
        let fileManager = FileManager.default
        let keys: [URLResourceKey] = [.contentModificationDateKey]
        let files =
            (try? fileManager.contentsOfDirectory(
                at: directory,
                includingPropertiesForKeys: keys
            )) ?? []
        guard files.count > maximumStoredClips else { return }
        var datedFiles: [(url: URL, date: Date)] = []
        for url in files {
            let values = try? url.resourceValues(forKeys: Set(keys))
            datedFiles.append((url, values?.contentModificationDate ?? .distantPast))
        }
        datedFiles.sort { $0.date < $1.date }
        for file in datedFiles.prefix(files.count - maximumStoredClips) {
            try? fileManager.removeItem(at: file.url)
        }
    }

    private static func fileURL(forKey key: String) -> URL {
        directory.appendingPathComponent("\(key).mp4")
    }

    /// Lives in Caches, which iOS may empty when storage runs low; clips are simply
    /// made again when that happens.
    private static let directory: URL = {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        let directory = caches.appendingPathComponent("BoardClips")
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }()
}
