// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics

/// The sizes of the TV layout (`TVDisplayView`) for a screen of a given size.
///
/// Everything is a multiple of one unit, a hundredth of the screen height, capped so a
/// screen wider than 16:9 does not grow the text past what the height allows. That keeps
/// a 720p, 1080p and 4K picture identical in shape. The margins keep everything clear of
/// TV overscan, where some sets crop the outer edge of the picture. Free of SwiftUI so the
/// Android TV screen can mirror it and `expected/answers.json` can pin it.
struct TVLayoutMetrics {
    let size: CGSize

    var unit: CGFloat {
        min(size.height / 100, size.width / 178)
    }

    /// Space between the title row, the notes row and the board.
    var spacing: CGFloat { unit * 3 }
    var titleFontSize: CGFloat { unit * 8 }
    /// The tuning (or "Keyboard") written beside the title.
    var captionFontSize: CGFloat { unit * 3.5 }
    var notesFontSize: CGFloat { unit * 3.6 }
    /// The close button, shown only when the layout fills the phone.
    var closeIconSize: CGFloat { unit * 6 }
    var closeLeadingPadding: CGFloat { unit * 3 }
    var horizontalMargin: CGFloat { size.width * 0.04 }
    var verticalMargin: CGFloat { size.height * 0.05 }
}
