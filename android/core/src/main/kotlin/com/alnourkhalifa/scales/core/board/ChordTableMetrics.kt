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
 * The television set ([tv]) is Android's own, since iOS draws no chord table on the TV. The
 * last two members exist for it alone; the phone and tablet sets keep the iOS values through
 * their defaults.
 *
 * @property contentMaxWidth caps the switch and footnote at the table's width so all three
 *   line up as one column; null on the phone, where the table already fills the screen.
 * @property rowPadding the space above and below each key's row. iOS pads every row by 8;
 *   on a television the twelve rows must share one screen, so the TV set nearly drops it.
 * @property isSignatureBesideRoot puts the key signature after the key's root on one line
 *   instead of under it. A stacked key cell is taller than a chord, so on a television the
 *   key column, not the chord names, would decide how large the table can be drawn.
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
    val rowPadding: Double = 8.0,
    val isSignatureBesideRoot: Boolean = false,
) {
    /**
     * The metric sets and the gap between columns they share. `columnSpacing` keeps
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

        /**
         * The screen height, in dp, the [tv] set is drawn for. The TV screen sets its density
         * so the screen is this many dp tall on every television, which turns each length and
         * text size in [tv] into a fixed share of the screen height: the table fills a 720p
         * and a 1080p set alike.
         *
         * Why 495 and not Android TV's usual 540: at 495 the chord names (title2, 22 sp) come
         * out at 4.4 % of the screen height, which is what 24 sp is on a standard 540 dp
         * television, large enough to read across a room.
         */
        val tvDesignHeight: Double = 495.0

        /**
         * Fills a television screen with all twelve keys at once, read from the sofa. Sizes are
         * dp of a screen [tvDesignHeight] tall: each row is about 29 of 495 (5.9 %), so the
         * twelve rows, the header, the Major/Minor tabs and the footnote fit inside the 90 % of
         * the height the overscan margins leave. Nothing in the table can take focus, and
         * Compose has no documented way to scroll focus-free content with a D-pad, so the
         * table has to fit without scrolling.
         */
        val tv: ChordTableMetrics =
            ChordTableMetrics(
                keyColumnWidth = 84.0,
                chordColumnWidth = 96.0,
                cellHeight = 27.0,
                keyStyle = ChordTableTextStyle.TITLE3,
                chordStyle = ChordTableTextStyle.TITLE2,
                smallStyle = ChordTableTextStyle.FOOTNOTE,
                headerStyle = ChordTableTextStyle.SUBHEADLINE,
                footnoteStyle = ChordTableTextStyle.FOOTNOTE,
                contentMaxWidth = tableWidth(keyColumn = 84.0, chordColumn = 96.0),
                rowPadding = 1.0,
                isSignatureBesideRoot = true,
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
