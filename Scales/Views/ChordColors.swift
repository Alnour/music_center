// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

extension TriadQuality {
    /// One colour per chord quality, shared by the key tables and the lessons so major,
    /// minor, diminished and suspended read the same at a glance everywhere in the app.
    var tint: Color {
        switch self {
        case .major: .primary
        case .minor: .blue
        case .diminished, .augmented, .majorFlatFive, .minorSharpFive: .orange
        case .suspendedFourth, .suspendedSecond, .suspendedFourthFlatFive,
            .suspendedSecondFlatFive, .suspendedFourthSharpFive, .suspendedSecondSharpFive:
            .teal
        }
    }
}
