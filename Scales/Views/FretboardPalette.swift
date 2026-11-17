// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

/// Colours for the board and the note chips.
///
/// The light palette follows the reference chart: grey-blue circles with the roots in deep
/// navy. Navy disappears on a dark background, so the dark palette marks roots in gold.
/// Text colours were picked for at least 4.5:1 contrast against their circles.
struct FretboardPalette: Sendable {
    let background: Color
    let fretWire: Color
    let nut: Color
    let string: Color
    let toneFill: Color
    let toneText: Color
    let rootFill: Color
    let rootText: Color
    let fretNumber: Color
    let inlay: Color
    let title: Color
    let secondaryText: Color
    /// Piano keys outside the scale keep their natural colours; keys in the scale take
    /// `toneFill` or `rootFill`, the same colours as the fretboard circles.
    let whiteKey: Color
    let blackKey: Color
    let keyOutline: Color

    static let light = FretboardPalette(
        background: Color(hex: 0xFFFFFF),
        fretWire: Color(hex: 0x8A8F9C),
        nut: Color(hex: 0x3A3F4B),
        string: Color(hex: 0x7A7F8C),
        toneFill: Color(hex: 0x8E9AB5),
        toneText: Color(hex: 0x1C2440),
        rootFill: Color(hex: 0x3D4A73),
        rootText: Color(hex: 0xFFFFFF),
        fretNumber: Color(hex: 0x5F6472),
        inlay: Color(hex: 0x2B2E36),
        title: Color(hex: 0x1C2440),
        secondaryText: Color(hex: 0x5B6275),
        whiteKey: Color(hex: 0xFAFAF7),
        blackKey: Color(hex: 0x2B2E36),
        keyOutline: Color(hex: 0x8A8F9C)
    )

    static let dark = FretboardPalette(
        background: Color(hex: 0x12151C),
        fretWire: Color(hex: 0x4A5162),
        nut: Color(hex: 0xC9CED8),
        string: Color(hex: 0x6A7184),
        toneFill: Color(hex: 0x8E9AB5),
        toneText: Color(hex: 0x10141E),
        rootFill: Color(hex: 0xF2C14E),
        rootText: Color(hex: 0x1A1408),
        fretNumber: Color(hex: 0x9AA1B2),
        inlay: Color(hex: 0x9AA1B2),
        title: Color(hex: 0xF2F4F8),
        secondaryText: Color(hex: 0x9AA1B2),
        whiteKey: Color(hex: 0xE4E7EE),
        blackKey: Color(hex: 0x1C2029),
        keyOutline: Color(hex: 0x4A5162)
    )

    static func matching(_ colorScheme: ColorScheme) -> FretboardPalette {
        colorScheme == .dark ? .dark : .light
    }
}

extension Color {
    /// Builds a colour from a 0xRRGGBB literal, the form designers hand over.
    init(hex: UInt32) {
        let red = Double((hex >> 16) & 0xFF) / 255
        let green = Double((hex >> 8) & 0xFF) / 255
        let blue = Double(hex & 0xFF) / 255
        self.init(red: red, green: green, blue: blue)
    }
}
