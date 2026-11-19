// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation

/// Fixed ids, dates and scales, so every run writes the same bytes. Random ids and the
/// clock would change the file on each run and `make answers-check` could never pass.
enum Fixtures {
    static let firstID = uuid("00000000-0000-0000-0000-000000000001")
    static let secondID = uuid("00000000-0000-0000-0000-000000000002")
    static let thirdID = uuid("00000000-0000-0000-0000-000000000003")
    /// An id no library holds, for `choose(.custom(_:))` on an unknown scale.
    static let unknownID = uuid("00000000-0000-0000-0000-0000000000FF")
    /// 9 May 2026, as seconds since 1 January 2001, the epoch Swift's JSON dates use.
    static let date = Date(timeIntervalSinceReferenceDate: 800_000_000)

    /// Major with a raised fifth (1 2 3 4 ♯5 6 7): no built-in scale has these notes.
    static let augmentedMajor = CustomScale(
        id: firstID,
        definition: ScaleDefinition(name: "Augmented Major", degrees: degrees("1 2 3 4 ♯5 6 7")),
        createdAt: date
    )

    /// Settings exactly as the first release saved them, before saved scales, the keyboard
    /// and its octaves existed. Copied from `ScalesTests/ScaleLibraryTests.swift`
    /// (`settingsSavedBeforeCustomScalesStillLoad`), line breaks included.
    static let firstReleaseSettingsJSON = """
        {"root":{"letter":4,"accidental":0},"scaleType":"dorian","tuning":"standard",
        "fretCount":15,"labelStyle":"noteNames","isLeftHanded":false,
        "thinStringOnTop":true,"tvUsesDarkTheme":true}
        """

    static func degrees(_ formula: String) -> [ScaleDegree] {
        var result: [ScaleDegree] = []
        for symbol in formula.split(separator: " ") {
            result.append(ScaleDegree(symbol: String(symbol)))
        }
        return result
    }

    /// The fixed ids above are valid literals, so a nil here is a typo in this file.
    private static func uuid(_ text: String) -> UUID {
        guard let id = UUID(uuidString: text) else {
            preconditionFailure("Bad fixture UUID \(text)")
        }
        return id
    }
}
