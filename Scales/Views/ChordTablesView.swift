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
                        .font(.system(metrics.footnoteStyle.textStyle))
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
            "The seven chords come from the natural minor scale. The small chords under v "
                + "and VII come from harmonic minor, which raises the seventh note: most "
                + "minor-key songs swap in that major V to lead back to the home chord."
        }
    }
}

extension ChordTableTextStyle {
    /// The SwiftUI text style of the same name, so the table follows Dynamic Type.
    var textStyle: Font.TextStyle {
        switch self {
        case .title: .title
        case .title2: .title2
        case .title3: .title3
        case .callout: .callout
        case .subheadline: .subheadline
        case .footnote: .footnote
        case .caption2: .caption2
        }
    }
}

/// The table itself: a header of Roman numerals, then one row per key. In minor keys the
/// harmonic-minor alternatives sit in small type under the chord they replace.
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
                VStack(spacing: 0) {
                    Text(chord.numeral)
                    if let alternative = row.alternatives[step] {
                        Text("or \(alternative.numeral)")
                            .font(.system(metrics.smallStyle.textStyle, design: .serif))
                    }
                }
                .frame(width: metrics.chordColumnWidth)
            }
        }
        .font(.system(metrics.headerStyle.textStyle, design: .serif).weight(.semibold))
        .foregroundStyle(.secondary)
    }

    private func keyRow(_ row: KeyChordRow) -> some View {
        GridRow {
            VStack(alignment: .leading, spacing: 0) {
                Text(row.key.root.displayName)
                    .font(.system(metrics.keyStyle.textStyle, design: .rounded).weight(.bold))
                Text(row.key.keySignature)
                    .font(.system(metrics.smallStyle.textStyle))
                    .foregroundStyle(.secondary)
            }
            .frame(width: metrics.keyColumnWidth, alignment: .leading)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(row.keyName)
            ForEach(Array(row.chords.enumerated()), id: \.offset) { step, chord in
                ChordCell(
                    triad: chord.triad,
                    alternative: row.alternatives[step]?.triad,
                    isHome: step == 0,
                    metrics: metrics
                )
            }
        }
        .padding(.vertical, 8)
    }
}

/// One chord name, tinted by quality so major, minor and diminished read at a glance,
/// with an optional stand-in chord in small type underneath.
private struct ChordCell: View {
    let triad: Triad
    let alternative: Triad?
    let isHome: Bool
    let metrics: ChordTableMetrics

    var body: some View {
        VStack(spacing: 0) {
            Text(triad.name)
                .font(
                    .system(metrics.chordStyle.textStyle, design: .rounded)
                        .weight(isHome ? .heavy : .semibold)
                )
                .foregroundStyle(triad.quality.tint)
                .accessibilityLabel(triad.spokenName)
            if let alternative {
                Text("or \(alternative.name)")
                    .font(.system(metrics.smallStyle.textStyle, design: .rounded).weight(.semibold))
                    .foregroundStyle(alternative.quality.tint)
                    .accessibilityLabel("or \(alternative.spokenName)")
            }
        }
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
