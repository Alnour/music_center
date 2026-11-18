// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

/// What the session needs to know about casting to say what the TV shows.
///
/// `PracticeSession` reads only these three facts to work out `TVStatus`, so it depends on
/// this protocol rather than on `AirPlayCaster`. That keeps the session free of AVFoundation
/// and UIKit: tests and the answers generator (`scripts/ExpectedAnswers`, run on macOS with no
/// simulator) build it with `NoCaster`, and only the app hands it the real AirPlay caster.
/// Main-actor isolated because the session and the caster both live on the main actor.
@MainActor
protocol BoardCaster: AnyObject {
    /// Set when the TV gave up on the clip, so the status line can say so.
    var playbackError: String? { get }
    /// True while the clip is playing on a TV rather than on the phone.
    var isPlayingOnTV: Bool { get }
    /// The TV's name as the casting route reports it, for the status line.
    var receiverName: String? { get }
}

/// A caster that never casts: the default for sessions built outside the app (unit tests,
/// the answers generator), where starting AirPlay and a clip server would be wasted work.
@MainActor
final class NoCaster: BoardCaster {
    let playbackError: String? = nil
    let isPlayingOnTV = false
    let receiverName: String? = nil

    init() {}
}
