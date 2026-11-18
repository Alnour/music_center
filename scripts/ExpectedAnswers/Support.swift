// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics
import Foundation

/// Every floating-point number in the answers file goes through here.
///
/// Six decimals is far finer than a pixel or an audible difference, and coarse enough that
/// last-bit differences between Swift's and Kotlin's maths libraries never reach the file,
/// so regenerating on another Mac gives the same bytes. Negative zero is written as zero.
func rounded(_ value: Double) -> Double {
    let scaled = (value * 1_000_000).rounded()
    let result = scaled / 1_000_000
    if result == 0 {
        return 0
    }
    return result
}

func rounded(_ value: CGFloat) -> Double {
    rounded(Double(value))
}

func rounded(_ values: [CGFloat]) -> [Double] {
    var result: [Double] = []
    for value in values {
        result.append(rounded(value))
    }
    return result
}

/// A point as the answers file writes it.
struct PointAnswer: Encodable {
    let x: Double
    let y: Double

    init(_ point: CGPoint) {
        x = rounded(point.x)
        y = rounded(point.y)
    }
}

/// A rectangle as its origin and size, the way both platforms build one.
struct RectAnswer: Encodable {
    let x: Double
    let y: Double
    let width: Double
    let height: Double

    init(_ rect: CGRect) {
        x = rounded(rect.origin.x)
        y = rounded(rect.origin.y)
        width = rounded(rect.size.width)
        height = rounded(rect.size.height)
    }
}

/// A spelled note with everything a Kotlin twin must agree on. `letter` is the raw value
/// Swift stores in saved JSON (C is 0, B is 6).
struct NoteAnswer: Encodable {
    let letter: Int
    let accidental: Int
    let displayName: String
    let pitchClass: Int

    init(_ note: NoteName) {
        letter = note.letter.rawValue
        accidental = note.accidental
        displayName = note.displayName
        pitchClass = note.pitchClass
    }
}

/// Note names as their display names, in the order given.
func displayNames(_ notes: [NoteName]) -> [String] {
    var names: [String] = []
    for note in notes {
        names.append(note.displayName)
    }
    return names
}

/// A set of notes in a fixed order: by pitch class, then letter, then accidental. Plain
/// integers decide the order, so Kotlin sorts the same way without comparing strings.
func sortedDisplayNames(_ notes: Set<NoteName>) -> [String] {
    let ordered = notes.sorted { first, second in
        if first.pitchClass != second.pitchClass {
            return first.pitchClass < second.pitchClass
        }
        if first.letter.rawValue != second.letter.rawValue {
            return first.letter.rawValue < second.letter.rawValue
        }
        return first.accidental < second.accidental
    }
    return displayNames(ordered)
}

/// A scale tone as one compact string, "F♯|7": the note's name and its degree label.
func toneLabel(_ tone: ScaleTone?) -> String? {
    guard let tone else { return nil }
    return "\(tone.name.displayName)|\(tone.degree.label)"
}

/// A value's compact JSON with sorted keys, as text. Sorted because Foundation's encoder
/// otherwise writes a struct's keys in a different order on each run.
func jsonText<Value: Encodable>(_ value: Value) throws -> String {
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.sortedKeys]
    let data = try encoder.encode(value)
    guard let text = String(data: data, encoding: .utf8) else {
        throw AnswersError.notUTF8
    }
    return text
}

enum AnswersError: Error {
    case notUTF8
    case missingArgument
    case fixtureFailed(String)
}
