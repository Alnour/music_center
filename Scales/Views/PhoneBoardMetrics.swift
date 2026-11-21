// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics

/// The sizes of the TV layout when it fills an iPhone turned sideways instead of a TV.
///
/// A phone screen is a fraction of the height of a TV and has no overscan, so the TV's
/// generous margins and stacked header would leave the board barely half the screen. Here
/// the title, the spelled notes, the tuning and the close button share one row, the margins
/// shrink to a hair, and the board takes everything else. The TV keeps `TVLayoutMetrics`.
struct PhoneBoardMetrics {
    let size: CGSize

    /// A hundredth of the screen height, as on the TV.
    var unit: CGFloat { size.height / 100 }

    /// Space between the header row and the board.
    var spacing: CGFloat { unit * 2 }
    var titleFontSize: CGFloat { unit * 6.5 }
    var captionFontSize: CGFloat { unit * 3.2 }
    var notesFontSize: CGFloat { unit * 3.4 }
    var closeIconSize: CGFloat { unit * 6 }
    /// Space between the title and the notes, and between the notes and the tuning.
    var headerSpacing: CGFloat { unit * 5 }
    var horizontalMargin: CGFloat { unit * 2 }
    var verticalMargin: CGFloat { unit * 2 }
}
