// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics
import Foundation

/// Where everything sits on the drum staff for a view of this size.
///
/// Every size is a share of the view's height (or width, along the bar), so the staff reads
/// the same on a phone and a TV. Kept free of SwiftUI so the Android staff can mirror it and
/// `expected/answers.json` can pin the numbers.
struct DrumStaffLayout {
    let groove: DrumGroove
    let size: CGSize

    var lineSpacing: CGFloat { size.height * 0.075 }
    var staffTop: CGFloat { size.height * 0.6 }
    var staffBottom: CGFloat { staffTop + lineSpacing * 4 }
    var staffLeft: CGFloat { size.width * 0.02 }
    var staffRight: CGFloat { size.width * 0.98 }
    var beamY: CGFloat { staffTop - lineSpacing * 2.6 }
    var countY: CGFloat { staffTop - lineSpacing * 4.6 }
    var ballRestY: CGFloat { countY }
    var bounceHeight: CGFloat { size.height * 0.16 }

    /// The five staff lines.
    var staffLineWidth: CGFloat { lineSpacing * 0.08 }
    /// The closing bar line, heavier than the staff lines.
    var barLineWidth: CGFloat { lineSpacing * 0.2 }
    var stemWidth: CGFloat { lineSpacing * 0.1 }
    var beamWidth: CGFloat { lineSpacing * 0.35 }
    /// Sixteenth notes get a second beam this far below the first.
    var secondBeamOffset: CGFloat { lineSpacing * 0.55 }
    /// Half the width of a hi-hat cross; snare and kick heads are ellipses built from it.
    var headRadius: CGFloat { lineSpacing * 0.45 }
    var ballRadius: CGFloat { lineSpacing * 0.75 }
    /// The counts ("1 & 2 &") written above the staff.
    var countFontSize: CGFloat { lineSpacing * 1.7 }
    /// The bar behind the count being played, as wide as nearly one staff space.
    var countMarkerWidth: CGFloat { lineSpacing * 0.9 }

    var columnWidth: CGFloat {
        (staffRight - staffLeft) / CGFloat(groove.stepsPerBar + 1)
    }

    func columnX(_ step: Int) -> CGFloat {
        staffLeft + columnWidth * (CGFloat(step) + 0.6)
    }

    /// Stems hang off the right side of the head, as engraved music does.
    func stemX(_ step: Int) -> CGFloat {
        columnX(step) + lineSpacing * 0.5
    }

    func headY(for voice: DrumVoice) -> CGFloat {
        switch voice {
        case .hiHat: return staffTop - lineSpacing * 0.5
        case .snare: return staffTop + lineSpacing * 1.5
        case .kick: return staffTop + lineSpacing * 3.5
        }
    }
}
