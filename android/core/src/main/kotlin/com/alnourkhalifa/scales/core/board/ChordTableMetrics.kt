package com.alnourkhalifa.scales.core.board

/**
 * The text styles the chord table uses, by name. Twin of `ChordTableTextStyle` in
 * `Scales/Views/ChordTableMetrics.swift`; [rawValue] is Swift's raw string, named after the
 * iOS text styles, which `expected/answers.json` carries and the Android table maps to its
 * own type scale.
 */
enum class ChordTableTextStyle(
    val rawValue: String,
) {
    TITLE("title"),
    TITLE2("title2"),
    TITLE3("title3"),
    CALLOUT("callout"),
    SUBHEADLINE("subheadline"),
    FOOTNOTE("footnote"),
    CAPTION2("caption2"),
}

/**
 * Column sizes and type styles for the chord table. Twin of `ChordTableMetrics` in
 * `Scales/Views/ChordTableMetrics.swift`, with the same members; lengths are `Double`.
 *
 * The compact set fits a key's seven chords across a phone held upright. On a tablet that set
 * would leave the table in one corner of the screen, so a wide window gets the roomy set,
 * sized to fill a small tablet held upright and read from a music stand.
 *
 * @property contentMaxWidth caps the switch and footnote at the table's width so all three
 *   line up as one column; null on the phone, where the table already fills the screen.
 */
data class ChordTableMetrics(
    val keyColumnWidth: Double,
    val chordColumnWidth: Double,
    val cellHeight: Double,
    val keyStyle: ChordTableTextStyle,
    val chordStyle: ChordTableTextStyle,
    val smallStyle: ChordTableTextStyle,
    val headerStyle: ChordTableTextStyle,
    val footnoteStyle: ChordTableTextStyle,
    val contentMaxWidth: Double?,
) {
    /**
     * The two metric sets and the gap between columns they share. `columnSpacing` keeps
     * Swift's camelCase name, the key `expected/answers.json` uses, so it stays a plain val
     * rather than a SCREAMING_CASE const.
     */
    @Suppress("MayBeConstant")
    companion object {
        /** A key has seven chords, one column each, beside the key column. */
        private const val CHORDS_PER_KEY = 7

        val columnSpacing: Double = 2.0

        /** Fits a key's seven chords across a phone held upright. */
        val compact: ChordTableMetrics =
            ChordTableMetrics(
                keyColumnWidth = 46.0,
                chordColumnWidth = 42.0,
                cellHeight = 36.0,
                keyStyle = ChordTableTextStyle.TITLE3,
                chordStyle = ChordTableTextStyle.CALLOUT,
                smallStyle = ChordTableTextStyle.CAPTION2,
                headerStyle = ChordTableTextStyle.FOOTNOTE,
                footnoteStyle = ChordTableTextStyle.FOOTNOTE,
                contentMaxWidth = null,
            )

        /** Fills a wide window, read from a music stand. */
        val roomy: ChordTableMetrics =
            ChordTableMetrics(
                keyColumnWidth = 76.0,
                chordColumnWidth = 86.0,
                cellHeight = 56.0,
                keyStyle = ChordTableTextStyle.TITLE,
                chordStyle = ChordTableTextStyle.TITLE2,
                smallStyle = ChordTableTextStyle.SUBHEADLINE,
                headerStyle = ChordTableTextStyle.TITLE3,
                footnoteStyle = ChordTableTextStyle.CALLOUT,
                contentMaxWidth = tableWidth(keyColumn = 76.0, chordColumn = 86.0),
            )

        /** Width of the key column plus seven chord columns and the gaps between them. */
        private fun tableWidth(
            keyColumn: Double,
            chordColumn: Double,
        ): Double {
            val chordColumns = CHORDS_PER_KEY * chordColumn
            val gaps = CHORDS_PER_KEY * columnSpacing
            return keyColumn + chordColumns + gaps
        }
    }
}
