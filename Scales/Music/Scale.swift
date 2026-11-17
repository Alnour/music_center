// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// One note of a scale: how it is spelled and which degree it plays.
struct ScaleTone: Hashable, Sendable {
    let name: NoteName
    let degree: ScaleDegree

    var isRoot: Bool { degree.isRoot }
}

/// A concrete scale, such as G major, with every tone spelled.
struct Scale: Sendable {
    let root: NoteName
    /// The scale's name and formula, which may be built in or one the player saved.
    let definition: ScaleDefinition
    /// Tones in ascending degree order, starting at the root.
    let tones: [ScaleTone]
    /// Answers "is this fretted pitch in the scale, and which tone is it?" in one lookup,
    /// which the fretboard asks for every string and fret.
    private let toneByPitchClass: [Int: ScaleTone]

    init(root: NoteName, definition: ScaleDefinition) {
        var tones: [ScaleTone] = []
        var toneByPitchClass: [Int: ScaleTone] = [:]
        for degree in definition.degrees {
            let name = Scale.spell(degree, above: root)
            let tone = ScaleTone(name: name, degree: degree)
            tones.append(tone)
            toneByPitchClass[name.pitchClass] = tone
        }
        self.root = root
        self.definition = definition
        self.tones = tones
        self.toneByPitchClass = toneByPitchClass
    }

    init(root: NoteName, type: ScaleType) {
        self.init(root: root, definition: type.definition)
    }

    var title: String {
        "\(root.displayName) \(definition.name)"
    }

    func tone(forPitchClass pitchClass: Int) -> ScaleTone? {
        toneByPitchClass[pitchClass]
    }

    /// Spells a degree above a root: the degree number picks the letter (the 3rd of G is
    /// some kind of B), and the semitone distance decides how many sharps or flats that
    /// letter needs to reach the right pitch.
    static func spell(_ degree: ScaleDegree, above root: NoteName) -> NoteName {
        let letter = root.letter.advanced(by: degree.number - 1)
        let targetPitchClass = PitchMath.wrap(root.pitchClass + degree.semitonesAboveRoot)
        let accidental = PitchMath.signedDistance(
            from: letter.naturalPitchClass,
            to: targetPitchClass
        )
        return NoteName(letter, accidental)
    }
}
