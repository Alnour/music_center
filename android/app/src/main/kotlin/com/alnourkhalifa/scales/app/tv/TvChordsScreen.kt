package com.alnourkhalifa.scales.app.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.alnourkhalifa.scales.core.board.ChordTableMetrics
import com.alnourkhalifa.scales.core.music.KeyChordTable
import com.alnourkhalifa.scales.core.music.TriadQuality
import com.alnourkhalifa.scales.ui.ChordTableColors
import com.alnourkhalifa.scales.ui.ChordTablesView
import com.alnourkhalifa.scales.ui.tint

/**
 * The two key families the tabs switch between, labelled as on the phone and the iPhone.
 * The footnotes are the phone's (`ChordTablesScreen`), so both screens explain the table alike.
 */
private enum class TvKeyFamily(
    val label: String,
    val testTag: String,
    val footnote: String,
) {
    MAJOR(
        label = "Major keys",
        testTag = "majorKeys",
        footnote =
            "Each chord stacks every other note of the key's scale. Upper-case numerals " +
                "are major chords, lower case minor, and ° diminished.",
    ),
    MINOR(
        label = "Minor keys",
        testTag = "minorKeys",
        footnote =
            "The seven chords come from the natural minor scale. Upper-case numerals " +
                "are major chords, lower case minor, and ° diminished.",
    ),
}

/**
 * The margin on every side, as a share of the screen: Google's 48 dp and 27 dp on a 540 dp
 * television, and the 90 % Amazon asks apps to fill, clear of what some sets crop.
 */
private const val OVERSCAN_SHARE = 0.05f

/** The gap between the tabs, the table and the footnote, in dp of the TV design height. */
private val SectionSpacing: Dp = 6.dp

/**
 * The footnote's size, iOS's footnote style, as [ChordTableMetrics.tv] names it. It is set
 * without tv-material's extra letter spacing so each footnote fits on the one line left
 * under the twelfth key.
 */
private val FootnoteSize = 13.sp

/** iOS's system blue in dark mode: the home chord's tile, as on the iPhone in dark mode. */
private val HomeChordAccent = Color(0xFF0A84FF)

/**
 * Every major and minor key with the chords that belong to it, on the television: a "Major
 * keys" / "Minor keys" tab row that Left and Right switch, the table, and its footnote. The TV
 * counterpart of the phone's `ChordTablesScreen`; iOS shows no chord table on the TV.
 *
 * Why the whole screen draws at its own density: the table has nothing focusable, and Compose
 * has no documented way to scroll focus-free content with a D-pad, so all twelve keys must fit
 * at once. The screen sets the density so it is [ChordTableMetrics.tvDesignHeight] dp tall on
 * every television; [ChordTableMetrics.tv] is drawn for that height, which makes each row, gap
 * and text size a fixed share of the screen height on a 720p and a 1080p set alike. The font
 * scale is pinned to 1 for the same reason: a larger system font would push the last keys off
 * the screen with no way to scroll to them.
 *
 * Focus starts on the selected tab. Back is `TvApp`'s and returns to the panel.
 */
@Composable
fun TvChordsScreen(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeightPixels = constraints.maxHeight.toFloat()
        val designHeight = ChordTableMetrics.tvDesignHeight.toFloat()
        val tvDensity = Density(density = screenHeightPixels / designHeight, fontScale = 1f)
        CompositionLocalProvider(LocalDensity provides tvDensity) {
            TvDarkTheme {
                TvChordsContent()
            }
        }
    }
}

/**
 * The tabs, table and footnote inside the overscan margins, one column as wide as the table
 * so the three line up, centred on the screen.
 */
@Composable
private fun TvChordsContent() {
    var family by rememberSaveable { mutableStateOf(TvKeyFamily.MAJOR) }
    val majorRows = remember { KeyChordTable.majorRows() }
    val minorRows = remember { KeyChordTable.minorRows() }
    val rows = if (family == TvKeyFamily.MAJOR) majorRows else minorRows
    val metrics = ChordTableMetrics.tv
    val tableWidth = metrics.contentMaxWidth?.dp ?: Dp.Unspecified
    val background = MaterialTheme.colorScheme.background
    val colors = tvChordTableColors()

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background),
    ) {
        val horizontalMargin = maxWidth * OVERSCAN_SHARE
        val verticalMargin = maxHeight * OVERSCAN_SHARE
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalMargin, vertical = verticalMargin),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = tableWidth)
                        .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SectionSpacing),
            ) {
                KeyFamilyTabs(
                    family = family,
                    onFamilyChange = { picked -> family = picked },
                )
                ChordTablesView(rows = rows, metrics = metrics, colors = colors)
                Text(
                    text = family.footnote,
                    color = colors.secondary,
                    fontSize = FootnoteSize,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The tv-material tab row. A tab is picked as soon as it takes focus, so Left and Right
 * switch the table with no OK press, the usual behaviour of TV tabs. Focus starts on the
 * picked tab, so the first Right press already moves to the other family.
 */
@Composable
private fun KeyFamilyTabs(
    family: TvKeyFamily,
    onFamilyChange: (TvKeyFamily) -> Unit,
) {
    val families = TvKeyFamily.entries
    val selectedIndex = families.indexOf(family)
    val selectedTabFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusOnceAttached(selectedTabFocus)
    }
    TabRow(selectedTabIndex = selectedIndex) {
        for (option in families) {
            var tabModifier = Modifier.testTag(option.testTag)
            if (option == family) {
                tabModifier = tabModifier.focusRequester(selectedTabFocus)
            }
            Tab(
                selected = option == family,
                onFocus = { onFamilyChange(option) },
                onClick = { onFamilyChange(option) },
                modifier = tabModifier,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * The table's colours from the TV theme: the theme's text colour for key names and major
 * chords, its quieter variant for numerals and signatures, iOS's dark-mode blue for the home
 * chord's tile, and the chord-quality tints the phone uses in dark mode.
 */
@Composable
private fun tvChordTableColors(): ChordTableColors {
    val scheme = MaterialTheme.colorScheme
    val onSurface = scheme.onBackground
    val tints = mutableMapOf<TriadQuality, Color>()
    for (quality in TriadQuality.entries) {
        tints[quality] = quality.tint(primary = onSurface, isDark = true)
    }
    return ChordTableColors(
        onSurface = onSurface,
        secondary = scheme.onSurfaceVariant,
        accent = HomeChordAccent,
        chordTints = tints,
    )
}

/**
 * Calls [onBack] for the remote's Back button and reports whether the key was used.
 *
 * Why a key handler rather than `BackHandler`: on a television Back arrives as a key press
 * that Compose sees first, and handling it here works the same in the app and in a Compose
 * test, which sends keys straight to the focused node. The press is taken on the way down
 * and acted on when the button is released, as Android does, so the release never reaches
 * whatever screen appears next.
 */
internal fun handleBackKey(
    key: Key,
    type: KeyEventType,
    onBack: () -> Unit,
): Boolean {
    if (key != Key.Back) {
        return false
    }
    if (type == KeyEventType.KeyUp) {
        onBack()
    }
    return true
}

/**
 * tv-material's dark colours around [content], keeping the type and shapes of whatever theme
 * hosts the screen.
 *
 * Why force dark: a television is watched in a dim room, where a white screen glares, and
 * tv-material's own default scheme is light. The chord tints and the drum staff are drawn
 * in their dark-mode colours to match.
 */
@Composable
internal fun TvDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
