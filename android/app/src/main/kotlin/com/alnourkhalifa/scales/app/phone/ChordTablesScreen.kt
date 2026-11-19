package com.alnourkhalifa.scales.app.phone

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.board.ChordTableMetrics
import com.alnourkhalifa.scales.core.board.ChordTableTextStyle
import com.alnourkhalifa.scales.core.music.KeyChordRow
import com.alnourkhalifa.scales.core.music.KeyChordTable
import com.alnourkhalifa.scales.core.music.TriadQuality
import com.alnourkhalifa.scales.ui.ChordTableColors
import com.alnourkhalifa.scales.ui.ChordTablesView
import com.alnourkhalifa.scales.ui.tint

/**
 * The two key families the switch at the top picks between, labelled as on the iPhone.
 * Twin of `ChordTablesView.KeyFamily` in `Scales/Views/ChordTablesView.swift`.
 */
private enum class KeyFamily(
    val label: String,
    val testTag: String,
) {
    MAJOR("Major keys", "majorKeys"),
    MINOR("Minor keys", "minorKeys"),
    ;

    /**
     * The line under the table that explains how to read it. The minor one names the natural
     * minor scale because the table lists its chords only, with no harmonic-minor stand-ins.
     */
    val footnote: String
        get() =
            when (this) {
                MAJOR -> {
                    "Each chord stacks every other note of the key's scale. Upper-case " +
                        "numerals are major chords, lower case minor, and ° diminished."
                }

                MINOR -> {
                    "The seven chords come from the natural minor scale. Upper-case " +
                        "numerals are major chords, lower case minor, and ° diminished."
                }
            }
}

/** iOS's `secondaryLabel` (60 % of a dark grey) in light mode. */
private val SecondaryLabelLight = Color(0x993C3C43)

/** iOS's `secondaryLabel` (60 % of a pale grey) in dark mode. */
private val SecondaryLabelDark = Color(0x99EBEBF5)

/**
 * A window counts as wide, the iPad's regular horizontal size class, from this width. The
 * height floor keeps a phone turned sideways on the compact set: it is wide but too short
 * for the roomy rows to show more than two keys at once.
 */
private val WideWindowMinWidth: Dp = 600.dp
private val WideWindowMinHeight: Dp = 480.dp

/** The space around the switch, table and footnote, SwiftUI's default `padding()`. */
private val ContentPadding: Dp = 16.dp

/** The gap between the switch, the table and the footnote, as the Swift `VStack` spaces them. */
private val SectionSpacing: Dp = 16.dp

/**
 * Every major and minor key with the chords that belong to it, behind a "Major keys" /
 * "Minor keys" switch. Twin of `ChordTablesView` in `Scales/Views/ChordTablesView.swift`.
 *
 * What differs from Swift: the title is centred in a Material top bar instead of the iPhone's
 * large left-aligned title, and the table is painted by the shared `ChordTablesView` of the ui
 * module, which the TV screen uses too. The picked family survives rotation
 * (`rememberSaveable`), as the iPhone keeps it while the tab stays alive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordTablesScreen(modifier: Modifier = Modifier) {
    var family by rememberSaveable { mutableStateOf(KeyFamily.MAJOR) }
    val majorRows = remember { KeyChordTable.majorRows() }
    val minorRows = remember { KeyChordTable.minorRows() }
    val rows = if (family == KeyFamily.MAJOR) majorRows else minorRows
    val metrics = if (isWideWindow()) ChordTableMetrics.roomy else ChordTableMetrics.compact
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) Color.Black else Color.White
    val colors = chordTableColors(isDark)
    Scaffold(
        modifier = modifier,
        containerColor = background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Chords", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
            )
        },
    ) { innerPadding ->
        ChordTablesContent(
            family = family,
            onFamilyChange = { picked -> family = picked },
            rows = rows,
            metrics = metrics,
            colors = colors,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The switch, the table and the footnote in one column that scrolls up and down. On a wide
 * window the column is capped at the table's width and centred, so the switch and footnote
 * line up with the table, as Swift's two `frame(maxWidth:)` calls do.
 */
@Composable
private fun ChordTablesContent(
    family: KeyFamily,
    onFamilyChange: (KeyFamily) -> Unit,
    rows: List<KeyChordRow>,
    metrics: ChordTableMetrics,
    colors: ChordTableColors,
    modifier: Modifier,
) {
    val maxWidth = metrics.contentMaxWidth?.dp ?: Dp.Unspecified
    val scrollState = rememberScrollState()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(ContentPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            KeyFamilySwitch(
                family = family,
                onFamilyChange = onFamilyChange,
            )
            ChordTablesView(rows = rows, metrics = metrics, colors = colors)
            Text(
                text = family.footnote,
                color = colors.secondary,
                style = MaterialTheme.typography.bodySmall,
                fontSize = metrics.footnoteStyle.footnoteFontSize,
            )
        }
    }
}

/**
 * Swift's segmented `Picker`: one segment per key family, painted the iOS way by the shared
 * [IosSegmentedControl] so it matches the switches on the Scales screen.
 */
@Composable
private fun KeyFamilySwitch(
    family: KeyFamily,
    onFamilyChange: (KeyFamily) -> Unit,
) {
    IosSegmentedControl(
        options = KeyFamily.entries,
        selected = family,
        label = { option -> option.label },
        onSelect = onFamilyChange,
        modifier = Modifier.fillMaxWidth(),
        optionTag = { option -> option.testTag },
    )
}

/**
 * The table's colours as SwiftUI paints them: the theme's text colour for key names and
 * major chords, iOS's secondary grey for numerals and signatures, the system blue accent
 * for the home chord's tile, and one tint per chord quality.
 */
@Composable
private fun chordTableColors(isDark: Boolean): ChordTableColors {
    val scheme = MaterialTheme.colorScheme
    val onSurface = scheme.onSurface
    val secondary = if (isDark) SecondaryLabelDark else SecondaryLabelLight
    val tints = mutableMapOf<TriadQuality, Color>()
    for (quality in TriadQuality.entries) {
        tints[quality] = quality.tint(primary = onSurface, isDark = isDark)
    }
    return ChordTableColors(
        onSurface = onSurface,
        secondary = secondary,
        accent = scheme.primary,
        chordTints = tints,
    )
}

/**
 * Whether the window is wide enough for the roomy table, Android's stand-in for SwiftUI's
 * regular horizontal size class. It reads the window rather than the screen, so a tablet app
 * sharing the screen with another one falls back to the phone sizes, as on the iPad.
 */
@Composable
private fun isWideWindow(): Boolean {
    val windowSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val width = with(density) { windowSize.width.toDp() }
    val height = with(density) { windowSize.height.toDp() }
    val isWideEnough = width >= WideWindowMinWidth
    val isTallEnough = height >= WideWindowMinHeight
    return isWideEnough && isTallEnough
}

/**
 * The footnote's size in the iOS text style the metrics name: footnote on the phone, callout
 * on a wide window. The ui module keeps its own style table private, and the footnote only
 * ever uses these two.
 */
private val ChordTableTextStyle.footnoteFontSize: TextUnit
    get() =
        when (this) {
            ChordTableTextStyle.CALLOUT -> 16.sp
            else -> 13.sp
        }
