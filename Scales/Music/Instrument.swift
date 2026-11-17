// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// Which instrument the board is drawn for.
///
/// The guitar gets the fretboard; the keyboard gets one piano octave. The choice only
/// changes how the scale is pictured, so the scale, root and labels are shared.
enum Instrument: String, CaseIterable, Codable, Identifiable, Sendable {
    case guitar
    case keyboard

    var id: String { rawValue }

    var title: String {
        switch self {
        case .guitar: "Guitar"
        case .keyboard: "Keyboard"
        }
    }
}
