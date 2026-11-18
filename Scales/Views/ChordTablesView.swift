// Scales — draws any scale across the guitar neck or a piano keyboard and shows it on a TV.
// Copyright © 2026 Alnour Alharin <alnour.ahmed@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This file is part of Scales, free software: you may redistribute and change it under the
// GNU General Public License, version 3 or later (see LICENSE). It comes with no warranty.

import SwiftUI

/// Tables of every major and minor key with the chords that belong to each.
struct ChordTablesView: View {
    enum KeyFamily: String, CaseIterable, Identifiable {
        case major = "Major keys"
        case minor = "Minor keys"

        var id: String { rawValue }
    }

    @State private var family: KeyFamily = .major
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    private let majorRows = KeyChordTable.majorRows()
    private let minorRows = KeyChordTable.minorRows()

    var body: some View {
        let metrics: ChordTableMetrics = horizontalSizeClass == .regular ? .roomy : .compact
        let rows = family == .major ? majorRows : minorRows
        let grid = KeyChordGrid(rows: rows, metrics: metrics)
        NavigationStack {
            ScrollView(.vertical) {
                VStack(alignment: .leading, spacing: 16) {
                    Picker("Keys", selection: $family) {
                        ForEach(KeyFamily.allCases) { family in
                            Text(family.rawValue).tag(family)
                        }
                    }
                    .pickerStyle(.segmented)

                    // The table scrolls sideways only when the window is too narrow for it,
                    // such as an iPad app sharing the screen with another one.
                    ViewThatFits(in: .horizontal) {
                        grid
                        ScrollView(.horizontal, showsIndicators: false) {
                            grid
                        }
                    }

                    Text(footnote)
                        .font(.system(metrics.footnoteStyle))
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: metrics.contentMaxWidth)
                .frame(maxWidth: .infinity)
                .padding()
            }
            .navigationTitle("Chords")
        }
    }

    private var footnote: String {
        switch family {
        case .major:
            "Each chord stacks every other note of the key's scale. Upper-case numerals are "
                + "major chords, lower case minor, and ° diminished."
        case .minor:
            "The seven chords come from the natural minor scale. Upper-case numerals are "
                + "major chords, lower case minor, and ° diminished."
        }
    }
}

/// Column sizes and type styles for the chord table.
///
/// The compact set fits a key's seven chords across an iPhone held upright. On an iPad
/// that set would leave the table in one corner of the screen, so a wide window gets the
/// roomy set, sized to fill an iPad mini held upright and read from a music stand.
private struct ChordTableMetrics {
    let keyColumnWidth: CGFloat
    let chordColumnWidth: CGFloat
    let cellHeight: CGFloat
    let keyStyle: Font.TextStyle
    let chordStyle: Font.TextStyle
    let smallStyle: Font.TextStyle
    let headerStyle: Font.TextStyle
    let footnoteStyle: Font.TextStyle
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

/// The table itself: a header of Roman numerals, then one row per key.
private struct KeyChordGrid: View {
    let rows: [KeyChordRow]
    let metrics: ChordTableMetrics

    var body: some View {
        Grid(
            alignment: .center,
            horizontalSpacing: ChordTableMetrics.columnSpacing,
            verticalSpacing: 0
        ) {
            if let firstRow = rows.first {
                headerRow(for: firstRow)
                    .padding(.bottom, 6)
                Divider()
            }
            ForEach(rows) { row in
                keyRow(row)
                Divider()
            }
        }
    }

    private func headerRow(for row: KeyChordRow) -> some View {
        GridRow(alignment: .top) {
            Text("Key")
                .frame(width: metrics.keyColumnWidth, alignment: .leading)
            ForEach(Array(row.chords.enumerated()), id: \.offset) { step, chord in
                Text(chord.numeral)
                    .frame(width: metrics.chordColumnWidth)
            }
        }
        .font(.system(metrics.headerStyle, design: .serif).weight(.semibold))
        .foregroundStyle(.secondary)
    }

    private func keyRow(_ row: KeyChordRow) -> some View {
        GridRow {
            VStack(alignment: .leading, spacing: 0) {
                Text(row.key.root.displayName)
                    .font(.system(metrics.keyStyle, design: .rounded).weight(.bold))
                Text(row.key.keySignature)
                    .font(.system(metrics.smallStyle))
                    .foregroundStyle(.secondary)
            }
            .frame(width: metrics.keyColumnWidth, alignment: .leading)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(row.keyName)
            ForEach(Array(row.chords.enumerated()), id: \.offset) { step, chord in
                ChordCell(
                    triad: chord.triad,
                    isHome: step == 0,
                    metrics: metrics
                )
            }
        }
        .padding(.vertical, 8)
    }
}

/// One chord name, tinted by quality so major, minor and diminished read at a glance.
private struct ChordCell: View {
    let triad: Triad
    let isHome: Bool
    let metrics: ChordTableMetrics

    var body: some View {
        Text(triad.name)
            .font(
                .system(metrics.chordStyle, design: .rounded)
                    .weight(isHome ? .heavy : .semibold)
            )
            .foregroundStyle(triad.quality.tint)
            .accessibilityLabel(triad.spokenName)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
            .frame(width: metrics.chordColumnWidth, height: metrics.cellHeight)
            .background {
                if isHome {
                    RoundedRectangle(cornerRadius: 8).fill(Color.accentColor.opacity(0.15))
                }
            }
    }
}
