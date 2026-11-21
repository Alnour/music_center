package com.alnourkhalifa.scales.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alnourkhalifa.scales.core.board.Size
import com.alnourkhalifa.scales.core.board.TVLayoutMetrics
import com.alnourkhalifa.scales.core.music.BassTuning
import com.alnourkhalifa.scales.core.music.FretboardLayout
import com.alnourkhalifa.scales.core.music.Instrument
import com.alnourkhalifa.scales.core.music.KeyboardLayout
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.Tuning
import com.alnourkhalifa.scales.core.session.NoteLabelStyle
import com.alnourkhalifa.scales.core.session.PracticeSettings
import kotlin.math.roundToInt

/**
 * The large layout shown on the TV: scale name, spelled notes and the full board. Twin of
 * `Scales/Views/TVDisplayView.swift`.
 *
 * Sizes are fractions of the screen height (`TVLayoutMetrics`) so the same layout fills a
 * 720p, 1080p or 4K picture, and generous margins keep everything clear of TV overscan, where
 * some sets crop the outer edge of the picture.
 *
 * What differs: Swift reads the practice session from the environment; this view takes the
 * board's settings as parameters (defaults are the app's defaults), so the phone's full-screen
 * board, the TV screen and previews pass what they hold; [capoFret] is the settings'
 * `boardCapoFret`, already 0 for the keyboard. The margins can be overridden as
 * shares of the screen ([horizontalMarginShare], [verticalMarginShare]), and an optional
 * [bottomContent] sits under the board (the TV screen's controls). SF Symbols' close button
 * is drawn by hand, as the ui module has no icon set.
 */
@Composable
fun TVDisplayView(
    scale: Scale,
    palette: FretboardPalette,
    modifier: Modifier = Modifier,
    instrument: Instrument = Instrument.GUITAR,
    tuning: Tuning = Tuning.STANDARD,
    bassTuning: BassTuning = BassTuning.STANDARD,
    fretCount: Int = DEFAULT_FRET_COUNT,
    keyboardOctaves: Int = DEFAULT_KEYBOARD_OCTAVES,
    labelStyle: NoteLabelStyle = NoteLabelStyle.NOTE_NAMES,
    thinStringOnTop: Boolean = true,
    isLeftHanded: Boolean = false,
    capoFret: Int = 0,
    ringedNotes: Set<NoteName> = emptySet(),
    horizontalMarginShare: Double? = null,
    verticalMarginShare: Double? = null,
    onClose: (() -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val board =
        TVBoardChoice(
            instrument = instrument,
            tuning = tuning,
            bassTuning = bassTuning,
            fretCount = fretCount,
            keyboardOctaves = keyboardOctaves,
            labelStyle = labelStyle,
            thinStringOnTop = thinStringOnTop,
            isLeftHanded = isLeftHanded,
            capoFret = capoFret,
        )
    val fixedFontDensity = Density(LocalDensity.current.density, fontScale = 1f)
    CompositionLocalProvider(LocalDensity provides fixedFontDensity) {
        BoxWithConstraints(modifier = modifier.background(palette.background)) {
            val screen = Size(maxWidth.value.toDouble(), maxHeight.value.toDouble())
            val metrics = TVLayoutMetrics(screen)
            val horizontalMargin =
                marginOrDefault(screen.width, horizontalMarginShare, metrics.horizontalMargin)
            val verticalMargin =
                marginOrDefault(screen.height, verticalMarginShare, metrics.verticalMargin)
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalMargin.dp, vertical = verticalMargin.dp),
                verticalArrangement = Arrangement.spacedBy(metrics.spacing.dp),
            ) {
                TitleRow(scale.title, board.caption, metrics, palette, onClose)
                ScaleNotesRow(
                    scale = scale,
                    noteSize = metrics.notesFontSize.dp,
                    palette = palette,
                    highlightedNotes = ringedNotes,
                )
                val boardModifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                TVBoard(board, scale, palette, ringedNotes, boardModifier)
                bottomContent?.invoke()
            }
        }
    }
}

/** The app's default fret count, as in `PracticeSettings`. */
private const val DEFAULT_FRET_COUNT = 15

/** The app's default keyboard, two octaves, as in `PracticeSettings`. */
private const val DEFAULT_KEYBOARD_OCTAVES = 2

/**
 * A margin of [share] of the screen's [length] when the caller names a share, otherwise
 * `TVLayoutMetrics`' own margin, the one the iPhone's TV layout uses.
 */
private fun marginOrDefault(
    length: Double,
    share: Double?,
    metricsMargin: Double,
): Double {
    if (share == null) {
        return metricsMargin
    }
    return length * share
}

/** The board settings the TV layout draws with, gathered so they travel as one value. */
private data class TVBoardChoice(
    val instrument: Instrument,
    val tuning: Tuning,
    val bassTuning: BassTuning,
    val fretCount: Int,
    val keyboardOctaves: Int,
    val labelStyle: NoteLabelStyle,
    val thinStringOnTop: Boolean,
    val isLeftHanded: Boolean,
    val capoFret: Int,
) {
    /** The small caption beside the title: the tuning, and the capo's fret when one is on.
     *  The words come from `PracticeSettings.boardCaption`, as on iOS, so the TV, the phone's
     *  full-screen board and the iPhone never word it differently. */
    val caption: String
        get() {
            return asSettings.boardCaption
        }

    /** The open strings of the neck, thickest first: the bass guitar's or the guitar's. */
    val openStrings: List<NoteName>
        get() = asSettings.boardOpenStrings

    /** These choices as the settings they came from, so the caption and the strings follow
     *  the same rules as everywhere else. */
    private val asSettings: PracticeSettings
        get() {
            val tuned = PracticeSettings(tuning = tuning)
            val withInstrument = tuned.withSelectedInstrument(instrument)
            val withBassTuning = withInstrument.withSelectedBassTuning(bassTuning)
            return withBassTuning.withSelectedCapoFret(capoFret)
        }
}

/** The fretboard or the piano octaves, whichever instrument the player chose. */
@Composable
private fun TVBoard(
    board: TVBoardChoice,
    scale: Scale,
    palette: FretboardPalette,
    ringedNotes: Set<NoteName>,
    modifier: Modifier,
) {
    when (board.instrument) {
        Instrument.GUITAR, Instrument.BASS -> {
            val openStrings = board.openStrings
            val layout =
                remember(scale, openStrings, board.fretCount, board.capoFret) {
                    FretboardLayout(scale, openStrings, board.fretCount, board.capoFret)
                }
            var spokenName = GUITAR_BOARD_SPOKEN_NAME
            if (board.instrument == Instrument.BASS) {
                spokenName = BASS_BOARD_SPOKEN_NAME
            }
            FretboardView(
                layout = layout,
                scaleTitle = scale.title,
                labelStyle = board.labelStyle,
                palette = palette,
                modifier = modifier,
                ringedNotes = ringedNotes,
                thinStringOnTop = board.thinStringOnTop,
                isLeftHanded = board.isLeftHanded,
                spokenName = spokenName,
            )
        }

        Instrument.KEYBOARD -> {
            val layout =
                remember(scale, board.keyboardOctaves) {
                    KeyboardLayout(scale, board.keyboardOctaves)
                }
            KeyboardView(
                layout = layout,
                scaleTitle = scale.title,
                labelStyle = board.labelStyle,
                palette = palette,
                modifier = modifier,
                ringedNotes = ringedNotes,
            )
        }
    }
}

/** The scale's name, the caption at the far end, and the close button when there is one. */
@Composable
private fun TitleRow(
    title: String,
    caption: String,
    metrics: TVLayoutMetrics,
    palette: FretboardPalette,
    onClose: (() -> Unit)?,
) {
    val titleStyle = boardTextStyle(metrics.titleFontSize, FontWeight.Bold, palette.title)
    val captionStyle =
        boardTextStyle(metrics.captionFontSize, FontWeight.Medium, palette.secondaryText)
    Row {
        BasicText(text = title, style = titleStyle, modifier = Modifier.alignByBaseline())
        Spacer(modifier = Modifier.weight(1f))
        BasicText(text = caption, style = captionStyle, modifier = Modifier.alignByBaseline())
        if (onClose != null) {
            CloseButton(metrics, palette, onClose)
        }
    }
}

/**
 * A filled circle with a cross, the look of SF Symbols' `xmark.circle.fill`: the circle in
 * the secondary text colour and the cross cut out in the background colour. Like the symbol
 * in a SwiftUI row aligned on the first text baseline, it sits on the title's baseline, which
 * crosses the symbol near its bottom ([SYMBOL_BASELINE_SHARE] of its height).
 */
@Composable
private fun RowScope.CloseButton(
    metrics: TVLayoutMetrics,
    palette: FretboardPalette,
    onClose: () -> Unit,
) {
    val diameter = metrics.closeIconSize.dp
    Canvas(
        modifier =
            Modifier
                .alignBy(::symbolBaseline)
                .padding(start = metrics.closeLeadingPadding.dp)
                .size(diameter)
                .semantics { contentDescription = "Close" }
                .clickable(role = Role.Button, onClick = onClose),
    ) {
        drawCircle(palette.secondaryText)
        val armLength = size.minDimension * 0.2f
        val strokeWidth = size.minDimension * 0.09f
        drawCross(center, armLength, strokeWidth, palette.background)
    }
}

/** Where an SF Symbol's baseline crosses it, measured on the iPhone TV layout. */
private const val SYMBOL_BASELINE_SHARE = 0.87f

/** The close button's baseline, counted from its top. */
private fun symbolBaseline(button: Measured): Int {
    val baseline = button.measuredHeight * SYMBOL_BASELINE_SHARE
    return baseline.roundToInt()
}

/** Two strokes crossing at [center], each reaching [armLength] from it. */
private fun DrawScope.drawCross(
    center: Offset,
    armLength: Float,
    strokeWidth: Float,
    color: Color,
) {
    val topLeft = Offset(center.x - armLength, center.y - armLength)
    val bottomRight = Offset(center.x + armLength, center.y + armLength)
    val topRight = Offset(center.x + armLength, center.y - armLength)
    val bottomLeft = Offset(center.x - armLength, center.y + armLength)
    drawLine(color, topLeft, bottomRight, strokeWidth, cap = StrokeCap.Round)
    drawLine(color, topRight, bottomLeft, strokeWidth, cap = StrokeCap.Round)
}

/** Text in the music font at a size in dp (the same number of sp at a font scale of 1). */
private fun boardTextStyle(
    sizeDp: Double,
    weight: FontWeight,
    color: Color,
): TextStyle =
    TextStyle(
        color = color,
        fontSize = sizeDp.toFloat().sp,
        fontWeight = weight,
        fontFamily = MusicFontFamily,
    )
