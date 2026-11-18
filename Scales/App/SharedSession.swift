// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import Foundation

extension PracticeSession {
    /// The session the app's phone and TV scenes share, saving to the standard defaults and
    /// casting through AirPlay. Declared here rather than in `PracticeSession.swift` so that
    /// file never names `AirPlayCaster`: the answers generator compiles it on macOS without
    /// AVFoundation or UIKit.
    static let shared = PracticeSession(defaults: .standard, caster: AirPlayCaster.shared)
}
