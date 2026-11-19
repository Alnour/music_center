// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import CoreGraphics

/// The text styles the chord table uses, by name. Kept apart from SwiftUI's `Font.TextStyle`
/// so the metrics below are plain data the answers generator can write out and the Android
/// table can map to its own type scale; `ChordTablesView` turns each into a font.
enum ChordTableTextStyle: String, CaseIterable, Sendable {
    case title
    case title2
    case title3
    case callout
    case subheadline
    case footnote
    case caption2
}

/// Column sizes and type styles for the chord table.
///
/// The compact set fits a key's seven chords across an iPhone held upright. On an iPad
/// that set would leave the table in one corner of the screen, so a wide window gets the
/// roomy set, sized to fill an iPad mini held upright and read from a music stand.
struct ChordTableMetrics {
    let keyColumnWidth: CGFloat
    let chordColumnWidth: CGFloat
    let cellHeight: CGFloat
    let keyStyle: ChordTableTextStyle
    let chordStyle: ChordTableTextStyle
    let smallStyle: ChordTableTextStyle
    let headerStyle: ChordTableTextStyle
    let footnoteStyle: ChordTableTextStyle
    /// Caps the switch and footnote at the table's width so all three line up as one
    /// column; the iPhone has no cap because there the table already fills the screen.
    let contentMaxWidth: CGFloat?

    static let columnSpacing: CGFloat = 2

    static let compact = ChordTableMetrics(
        keyColumnWidth: 46,
        chordColumnWidth: 42,
        cellHeight: 36,
        keyStyle: .title3,
        chordStyle: .callout,
        smallStyle: .caption2,
        headerStyle: .footnote,
        footnoteStyle: .footnote,
        contentMaxWidth: nil
    )

    static let roomy = ChordTableMetrics(
        keyColumnWidth: 76,
        chordColumnWidth: 86,
        cellHeight: 56,
        keyStyle: .title,
        chordStyle: .title2,
        smallStyle: .subheadline,
        headerStyle: .title3,
        footnoteStyle: .callout,
        contentMaxWidth: tableWidth(keyColumn: 76, chordColumn: 86)
    )

    /// Width of the key column plus seven chord columns and the gaps between them.
    private static func tableWidth(keyColumn: CGFloat, chordColumn: CGFloat) -> CGFloat {
        let chordColumns = 7 * chordColumn
        let gaps = 7 * columnSpacing
        return keyColumn + chordColumns + gaps
    }
}
