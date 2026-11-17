// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// What makes a scale that scale, whatever key it is played in: a name and its degrees.
///
/// Built-in scales and the player's own scales are both described this way, so the
/// fretboard, the chord builder and the lessons treat them alike. A saved scale keeps
/// only its definition; the key it is shown in comes from the root the player picks.
struct ScaleDefinition: Hashable, Codable, Sendable {
    var name: String
    /// In letter order, starting with the root, with no two degrees on the same pitch.
    let degrees: [ScaleDegree]

    /// Sorts the degrees into letter order so every definition reads like a formula.
    init(name: String, degrees: [ScaleDegree]) {
        self.name = name
        self.degrees = degrees.sorted()
    }

    /// The formula as musicians write it: "1 2 ♭3 4 5 6 ♭7".
    var formula: String {
        var labels: [String] = []
        for degree in degrees {
            labels.append(degree.label)
        }
        return labels.joined(separator: " ")
    }

    /// True when both scales sound the same: they hold the same pitches above the root,
    /// even if one spells a note differently (♯4 against ♭5) or carries another name.
    func hasSamePitches(as other: ScaleDefinition) -> Bool {
        pitchClassesAboveRoot == other.pitchClassesAboveRoot
    }

    private var pitchClassesAboveRoot: Set<Int> {
        var pitchClasses = Set<Int>()
        for degree in degrees {
            pitchClasses.insert(degree.pitchClassAboveRoot)
        }
        return pitchClasses
    }
}

extension ScaleType {
    /// The built-in scale as a definition, named the way the scale picker names it.
    var definition: ScaleDefinition {
        ScaleDefinition(name: displayName, degrees: degrees)
    }
}
