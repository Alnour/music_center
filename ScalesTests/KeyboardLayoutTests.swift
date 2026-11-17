// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Testing

@testable import Scales

/// Key-by-key checks of the piano octave, without rendering anything.
struct KeyboardLayoutTests {
    private let gMajor = KeyboardLayout(scale: Scale(root: NoteName(.g), type: .major))

    @Test func octaveRunsFromCToTheNextC() {
        var pitchClasses: [Int] = []
        for key in gMajor.keys {
            pitchClasses.append(key.pitchClass)
        }
        #expect(pitchClasses == [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0])
        #expect(gMajor.whiteKeys.count == 8)
        #expect(gMajor.blackKeys.count == 5)
    }

    @Test func blackKeysAreTheFiveSharpsAndFlats() {
        var blackPitchClasses: [Int] = []
        for key in gMajor.blackKeys {
            blackPitchClasses.append(key.pitchClass)
        }
        #expect(blackPitchClasses == [1, 3, 6, 8, 10])
    }

    @Test func gMajorMarksItsSevenNotesWithGAsRoot() {
        var names: [String] = []
        var rootNames: [String] = []
        for key in gMajor.keys {
            guard let tone = key.tone else { continue }
            names.append(tone.name.displayName)
            if tone.isRoot {
                rootNames.append(tone.name.displayName)
            }
        }
        #expect(names == ["C", "D", "E", "F♯", "G", "A", "B", "C"])
        #expect(rootNames == ["G"])
    }

    @Test func gMajorUsesOneBlackKeyAndSkipsF() {
        var blackKeysInScale: [String] = []
        for key in gMajor.blackKeys {
            if let tone = key.tone {
                blackKeysInScale.append(tone.name.displayName)
            }
        }
        #expect(blackKeysInScale == ["F♯"])
        let fKey = gMajor.keys[5]
        #expect(fKey.pitchClass == 5)
        #expect(fKey.tone == nil)
    }

    @Test func bothCKeysCarryTheSameToneInCMajor() {
        let cMajor = KeyboardLayout(scale: Scale(root: NoteName(.c), type: .major))
        let firstKey = cMajor.keys[0]
        let closingKey = cMajor.keys[12]
        #expect(firstKey.tone?.isRoot == true)
        #expect(closingKey.tone == firstKey.tone)
    }

    @Test func twoOctavesRepeatTheKeysAndCloseOnC() {
        let layout = KeyboardLayout(scale: Scale(root: NoteName(.g), type: .major), octaveCount: 2)
        #expect(layout.keys.count == 25)
        #expect(layout.whiteKeys.count == 15)
        #expect(layout.blackKeys.count == 10)
        #expect(layout.keys[12].pitchClass == 0)
        #expect(layout.keys[24].pitchClass == 0)
        var rootCount = 0
        for key in layout.keys where key.tone?.isRoot == true {
            rootCount += 1
        }
        #expect(rootCount == 2, "G appears once per octave")
    }

    @Test func aPentatonicMarksItsNotesOnBothCKeys() {
        let layout = KeyboardLayout(scale: Scale(root: NoteName(.a), type: .minorPentatonic))
        var markedCount = 0
        for key in layout.keys where key.tone != nil {
            markedCount += 1
        }
        #expect(markedCount == 6, "A C D E G, with C marked at both ends of the octave")
    }
}
