package com.alnourkhalifa.scales.core.board

import com.alnourkhalifa.scales.core.Answers
import com.alnourkhalifa.scales.core.array
import com.alnourkhalifa.scales.core.assertClose
import com.alnourkhalifa.scales.core.assertSame
import com.alnourkhalifa.scales.core.bool
import com.alnourkhalifa.scales.core.double
import com.alnourkhalifa.scales.core.int
import com.alnourkhalifa.scales.core.music.DrumGroove
import com.alnourkhalifa.scales.core.music.DrumVoice
import com.alnourkhalifa.scales.core.obj
import com.alnourkhalifa.scales.core.string
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

/**
 * Reads the `geometry` section of `expected/answers.json`, written by the Swift generator,
 * and checks every board, keyboard, drum staff, TV and chord-table size against it.
 *
 * These numbers are where a drawing bug shows first: if the Android fretboard put fret 12 a
 * pixel off, the TV picture would no longer match the iPhone's, so every named position is
 * compared to six decimals rather than eyeballed on a screenshot.
 */
class AnswersGeometryTest {
    private val geometry: JsonObject = Answers.section("geometry").jsonObject

    // feature: board
    @Test
    fun fretboardConstantsMatchSwift() {
        val answer = geometry.obj("fretboardConstants")
        val case = "fretboard constants"
        assertClose(
            "$case tuningColumnShare",
            answer.double("tuningColumnShare"),
            FretboardGeometry.tuningColumnShare,
        )
        assertClose(
            "$case numberBandShare",
            answer.double("numberBandShare"),
            FretboardGeometry.numberBandShare,
        )
        assertClose(
            "$case inlayBandShare",
            answer.double("inlayBandShare"),
            FretboardGeometry.inlayBandShare,
        )
        assertSame(
            "$case singleInlayFrets",
            intsOf(answer.array("singleInlayFrets")),
            FretboardGeometry.singleInlayFrets.sorted(),
        )
        assertSame(
            "$case doubleInlayFrets",
            intsOf(answer.array("doubleInlayFrets")),
            FretboardGeometry.doubleInlayFrets.sorted(),
        )
    }

    // feature: board
    @Test
    fun rightHandedFretboardsMatchSwift() {
        val boards = fretboardAnswers(isLeftHanded = false)
        assertSame("right-handed fretboard count", 4, boards.size)
        for (answer in boards) {
            checkFretboard(answer)
        }
    }

    // feature: left-handed-thin-top
    @Test
    fun leftHandedFretboardsMatchSwift() {
        val boards = fretboardAnswers(isLeftHanded = true)
        assertSame("left-handed fretboard count", 4, boards.size)
        for (answer in boards) {
            checkFretboard(answer)
        }
    }

    // feature: left-handed-thin-top
    @Test
    fun stringOrderMatchesSwift() {
        for (element in geometry.array("fretboards")) {
            val answer = element.jsonObject
            val case = fretboardCase(answer)
            val stringCount = answer.int("stringCount")
            val thinOnTop = mutableListOf<Int>()
            val thickOnTop = mutableListOf<Int>()
            for (row in 0 until stringCount) {
                val thinIndex =
                    FretboardGeometry.stringIndex(
                        forRow = row,
                        stringCount = stringCount,
                        thinStringOnTop = true,
                    )
                val thickIndex =
                    FretboardGeometry.stringIndex(
                        forRow = row,
                        stringCount = stringCount,
                        thinStringOnTop = false,
                    )
                thinOnTop.add(thinIndex)
                thickOnTop.add(thickIndex)
            }
            assertSame("$case thin on top", intsOf(answer.array("stringIndexThinOnTop")), thinOnTop)
            val expectedThick = intsOf(answer.array("stringIndexThickOnTop"))
            assertSame("$case thick on top", expectedThick, thickOnTop)
        }
    }

    // feature: keyboard
    @Test
    fun keyboardConstantsMatchSwift() {
        val answer = geometry.obj("keyboardConstants")
        val case = "keyboard constants"
        assertClose(
            "$case whiteKeyHeightToWidth",
            answer.double("whiteKeyHeightToWidth"),
            KeyboardGeometry.whiteKeyHeightToWidth,
        )
        assertClose(
            "$case blackKeyWidthRatio",
            answer.double("blackKeyWidthRatio"),
            KeyboardGeometry.blackKeyWidthRatio,
        )
        assertClose(
            "$case blackKeyHeightRatio",
            answer.double("blackKeyHeightRatio"),
            KeyboardGeometry.blackKeyHeightRatio,
        )
        assertClose(
            "$case whiteKeyCircleShare",
            answer.double("whiteKeyCircleShare"),
            KeyboardGeometry.whiteKeyCircleShare,
        )
        assertClose(
            "$case blackKeyCircleShare",
            answer.double("blackKeyCircleShare"),
            KeyboardGeometry.blackKeyCircleShare,
        )
    }

    // feature: keyboard
    @Test
    fun keyboardsMatchSwift() {
        val keyboards = geometry.array("keyboards")
        assertSame("keyboard count", 2, keyboards.size)
        for (element in keyboards) {
            checkKeyboard(element.jsonObject)
        }
    }

    // feature: drum-trainer
    @Test
    fun drumStaffsMatchSwift() {
        val staffs = geometry.array("drumStaffs")
        assertSame("drum staff count", 2, staffs.size)
        for (element in staffs) {
            checkDrumStaff(element.jsonObject)
        }
    }

    // feature: board
    @Test
    fun tvLayoutsMatchSwift() {
        val layouts = geometry.array("tvLayouts")
        assertSame("TV layout count", 2, layouts.size)
        for (element in layouts) {
            val answer = element.jsonObject
            val metrics = TVLayoutMetrics(sizeOf(answer))
            val case = "TV ${answer.double("width")}x${answer.double("height")}"
            assertClose("$case unit", answer.double("unit"), metrics.unit)
            assertClose("$case spacing", answer.double("spacing"), metrics.spacing)
            assertClose("$case title", answer.double("titleFontSize"), metrics.titleFontSize)
            assertClose("$case caption", answer.double("captionFontSize"), metrics.captionFontSize)
            assertClose("$case notes", answer.double("notesFontSize"), metrics.notesFontSize)
            assertClose("$case close icon", answer.double("closeIconSize"), metrics.closeIconSize)
            assertClose(
                "$case close padding",
                answer.double("closeLeadingPadding"),
                metrics.closeLeadingPadding,
            )
            assertClose(
                "$case horizontal margin",
                answer.double("horizontalMargin"),
                metrics.horizontalMargin,
            )
            assertClose(
                "$case vertical margin",
                answer.double("verticalMargin"),
                metrics.verticalMargin,
            )
        }
    }

    // feature: chord-tables
    @Test
    fun chordTableMetricsMatchSwift() {
        val metricSets = geometry.array("chordTableMetrics")
        assertSame("chord table metric count", 2, metricSets.size)
        val byName =
            mapOf(
                "compact" to ChordTableMetrics.compact,
                "roomy" to ChordTableMetrics.roomy,
            )
        for (element in metricSets) {
            val answer = element.jsonObject
            val name = answer.string("name")
            val metrics = byName[name]
            requireNotNull(metrics) { "no Kotlin chord table metrics named '$name'" }
            checkChordTableMetrics(answer, metrics)
        }
    }

    private fun fretboardAnswers(isLeftHanded: Boolean): List<JsonObject> {
        val boards = mutableListOf<JsonObject>()
        for (element in geometry.array("fretboards")) {
            val answer = element.jsonObject
            if (answer.bool("isLeftHanded") == isLeftHanded) {
                boards.add(answer)
            }
        }
        return boards
    }
}

// The checks below compare one answer object with the Kotlin geometry; they sit outside the
// test class so the class stays a list of the cases it proves.

private fun checkFretboard(answer: JsonObject) {
    val case = fretboardCase(answer)
    val fretCount = answer.int("fretCount")
    val stringCount = answer.int("stringCount")
    val board =
        FretboardGeometry(
            size = sizeOf(answer),
            fretCount = fretCount,
            stringCount = stringCount,
            isLeftHanded = answer.bool("isLeftHanded"),
        )
    assertClose(
        "$case columnUnits",
        answer.double("columnUnits"),
        FretboardGeometry.columnUnits(fretCount),
    )
    checkFretboardPositions(case, answer, board)
    checkFretboardColumnsAndRows(case, answer, board)
    checkFretboardDrawingSizes(case, answer, board)
}

private fun checkFretboardPositions(
    case: String,
    answer: JsonObject,
    board: FretboardGeometry,
) {
    val stringCount = answer.int("stringCount")
    assertClose("$case cellWidth", answer.double("cellWidth"), board.cellWidth)
    assertClose("$case rowHeight", answer.double("rowHeight"), board.rowHeight)
    assertClose("$case noteRadius", answer.double("noteRadius"), board.noteRadius)
    assertClose("$case nutX", answer.double("nutX"), board.nutX)
    assertClose("$case tuningLabelX", answer.double("tuningLabelX"), board.tuningLabelX)
    assertClose("$case fretNumberY", answer.double("fretNumberY"), board.fretNumberY)
    assertClose("$case inlayY", answer.double("inlayY"), board.inlayY(stringCount))
}

private fun checkFretboardColumnsAndRows(
    case: String,
    answer: JsonObject,
    board: FretboardGeometry,
) {
    val fretCount = answer.int("fretCount")
    val stringCount = answer.int("stringCount")
    val noteX = doublesOf(answer.array("noteX"))
    val fretWireX = doublesOf(answer.array("fretWireX"))
    assertSame("$case noteX count", fretCount + 1, noteX.size)
    assertSame("$case fretWireX count", fretCount + 1, fretWireX.size)
    for (fret in 0..fretCount) {
        assertClose("$case noteX fret $fret", noteX[fret], board.noteX(fret))
        assertClose("$case fretWireX fret $fret", fretWireX[fret], board.fretWireX(fret))
    }
    val stringY = doublesOf(answer.array("stringY"))
    val strokeWidths = doublesOf(answer.array("stringStrokeWidths"))
    assertSame("$case stringY count", stringCount, stringY.size)
    assertSame("$case stroke width count", stringCount, strokeWidths.size)
    for (row in 0 until stringCount) {
        assertClose("$case stringY row $row", stringY[row], board.stringY(row))
        val strokeWidth = board.stringStrokeWidth(stringIndex = row, stringCount = stringCount)
        assertClose("$case stroke width string $row", strokeWidths[row], strokeWidth)
    }
}

private fun checkFretboardDrawingSizes(
    case: String,
    answer: JsonObject,
    board: FretboardGeometry,
) {
    assertClose("$case inlayRadius", answer.double("inlayRadius"), board.inlayRadius)
    assertClose(
        "$case doubleInlayOffset",
        answer.double("doubleInlayOffset"),
        board.doubleInlayOffset,
    )
    assertClose("$case fretWireWidth", answer.double("fretWireWidth"), board.fretWireWidth)
    assertClose("$case nutWidth", answer.double("nutWidth"), board.nutWidth)
    assertClose(
        "$case fretNumberFontSize",
        answer.double("fretNumberFontSize"),
        board.fretNumberFontSize,
    )
    assertClose(
        "$case openStringLabelFontSize",
        answer.double("openStringLabelFontSize"),
        board.openStringLabelFontSize,
    )
    // One- and two-character labels, measured as the board measures them.
    val shortLabel = FretboardGeometry.labelLength("G")
    val longLabel = FretboardGeometry.labelLength("F𝄪")
    assertSame("$case short label length", 1, shortLabel)
    assertSame("$case long label length", 2, longLabel)
    assertClose(
        "$case noteLabelFontSizeShort",
        answer.double("noteLabelFontSizeShort"),
        FretboardGeometry.noteLabelFontSize(board.noteRadius, shortLabel),
    )
    assertClose(
        "$case noteLabelFontSizeLong",
        answer.double("noteLabelFontSizeLong"),
        FretboardGeometry.noteLabelFontSize(board.noteRadius, longLabel),
    )
    val diameter = board.noteRadius * 2
    assertClose(
        "$case chordRingLineWidth",
        answer.double("chordRingLineWidth"),
        FretboardGeometry.chordRingLineWidth(diameter),
    )
    assertClose(
        "$case chordRingOutset",
        answer.double("chordRingOutset"),
        FretboardGeometry.chordRingOutset(diameter),
    )
}

private fun checkKeyboard(answer: JsonObject) {
    val whiteKeyCount = answer.int("whiteKeyCount")
    val case = "keyboard ${answer.int("octaves")} octave(s)"
    val keyboard = KeyboardGeometry(size = sizeOf(answer), whiteKeyCount = whiteKeyCount)
    assertClose(
        "$case widthToHeight",
        answer.double("widthToHeight"),
        KeyboardGeometry.widthToHeight(whiteKeyCount),
    )
    assertClose("$case whiteKeyWidth", answer.double("whiteKeyWidth"), keyboard.whiteKeyWidth)
    assertClose("$case keyHeight", answer.double("keyHeight"), keyboard.keyHeight)
    assertClose("$case originX", answer.double("originX"), keyboard.originX)
    assertClose("$case originY", answer.double("originY"), keyboard.originY)
    assertClose("$case outlineWidth", answer.double("outlineWidth"), keyboard.outlineWidth)

    // The file's keyboards are G major; the rects depend only on which keys are black.
    val keys = answer.array("keys")
    val isBlackFlags = mutableListOf<Boolean>()
    for (element in keys) {
        isBlackFlags.add(element.jsonObject.bool("isBlack"))
    }
    val rects = keyboard.keyRects(isBlackFlags)
    assertSame("$case key count", keys.size, rects.size)
    for ((index, element) in keys.withIndex()) {
        val keyCase = "$case key $index"
        checkKey(keyCase, element.jsonObject, rects[index])
    }
}

private fun checkKey(
    case: String,
    answer: JsonObject,
    rect: Rect,
) {
    val isBlack = answer.bool("isBlack")
    checkRect("$case rect", answer.obj("rect"), rect)
    val radius = KeyboardGeometry.circleRadius(keyWidth = rect.width, isBlack = isBlack)
    assertClose("$case circleRadius", answer.double("circleRadius"), radius)
    val center = KeyboardGeometry.circleCenter(onKey = rect, radius = radius)
    checkPoint("$case circleCenter", answer.obj("circleCenter"), center)
    val fontSize = KeyboardGeometry.keyNameFontSize(keyWidth = rect.width, isBlack = isBlack)
    assertClose("$case keyNameFontSize", answer.double("keyNameFontSize"), fontSize)
    val nameCenter =
        KeyboardGeometry.keyNameCenter(
            onKey = rect,
            circleTop = center.y - radius,
            fontSize = fontSize,
        )
    checkPoint("$case keyNameCenter", answer.obj("keyNameCenter"), nameCenter)
}

private fun checkDrumStaff(answer: JsonObject) {
    val grooveName = answer.string("groove")
    val groove = DrumGroove.builtIn.firstOrNull { groove -> groove.name == grooveName }
    requireNotNull(groove) { "no Kotlin groove named '$grooveName'" }
    val staff = DrumStaffLayout(groove = groove, size = sizeOf(answer))
    val case = "drum staff $grooveName"
    val sizes =
        mapOf(
            "lineSpacing" to staff.lineSpacing,
            "staffTop" to staff.staffTop,
            "staffBottom" to staff.staffBottom,
            "staffLeft" to staff.staffLeft,
            "staffRight" to staff.staffRight,
            "beamY" to staff.beamY,
            "countY" to staff.countY,
            "ballRestY" to staff.ballRestY,
            "bounceHeight" to staff.bounceHeight,
            "staffLineWidth" to staff.staffLineWidth,
            "barLineWidth" to staff.barLineWidth,
            "stemWidth" to staff.stemWidth,
            "beamWidth" to staff.beamWidth,
            "secondBeamOffset" to staff.secondBeamOffset,
            "headRadius" to staff.headRadius,
            "ballRadius" to staff.ballRadius,
            "countFontSize" to staff.countFontSize,
            "countMarkerWidth" to staff.countMarkerWidth,
            "columnWidth" to staff.columnWidth,
        )
    for ((name, actual) in sizes) {
        assertClose("$case $name", answer.double(name), actual)
    }
    val columnX = doublesOf(answer.array("columnX"))
    val stemX = doublesOf(answer.array("stemX"))
    assertSame("$case column count", groove.stepsPerBar, columnX.size)
    assertSame("$case stem count", groove.stepsPerBar, stemX.size)
    for (step in 0 until groove.stepsPerBar) {
        assertClose("$case columnX step $step", columnX[step], staff.columnX(step))
        assertClose("$case stemX step $step", stemX[step], staff.stemX(step))
    }
    val headY = answer.obj("headY")
    assertSame("$case voice count", DrumVoice.entries.size, headY.size)
    for (voice in DrumVoice.entries) {
        val id = voiceId(voice)
        assertClose("$case headY $id", headY.double(id), staff.headY(voice))
    }
}

private fun checkChordTableMetrics(
    answer: JsonObject,
    metrics: ChordTableMetrics,
) {
    val case = "chord table ${answer.string("name")}"
    assertClose("$case keyColumnWidth", answer.double("keyColumnWidth"), metrics.keyColumnWidth)
    assertClose(
        "$case chordColumnWidth",
        answer.double("chordColumnWidth"),
        metrics.chordColumnWidth,
    )
    assertClose("$case cellHeight", answer.double("cellHeight"), metrics.cellHeight)
    assertClose(
        "$case columnSpacing",
        answer.double("columnSpacing"),
        ChordTableMetrics.columnSpacing,
    )
    assertSame("$case keyStyle", answer.string("keyStyle"), metrics.keyStyle.rawValue)
    assertSame("$case chordStyle", answer.string("chordStyle"), metrics.chordStyle.rawValue)
    assertSame("$case smallStyle", answer.string("smallStyle"), metrics.smallStyle.rawValue)
    assertSame("$case headerStyle", answer.string("headerStyle"), metrics.headerStyle.rawValue)
    assertSame(
        "$case footnoteStyle",
        answer.string("footnoteStyle"),
        metrics.footnoteStyle.rawValue,
    )
    // Swift leaves a nil optional out of the object.
    val expectedMaxWidth = answer["contentMaxWidth"]
    val actualMaxWidth = metrics.contentMaxWidth
    if (expectedMaxWidth == null || expectedMaxWidth is JsonNull) {
        assertSame("$case contentMaxWidth", null, actualMaxWidth)
    } else {
        requireNotNull(actualMaxWidth) { "$case contentMaxWidth is null in Kotlin" }
        assertClose(
            "$case contentMaxWidth",
            expectedMaxWidth.jsonPrimitive.double,
            actualMaxWidth,
        )
    }
}

private fun checkRect(
    case: String,
    answer: JsonObject,
    rect: Rect,
) {
    assertClose("$case x", answer.double("x"), rect.x)
    assertClose("$case y", answer.double("y"), rect.y)
    assertClose("$case width", answer.double("width"), rect.width)
    assertClose("$case height", answer.double("height"), rect.height)
}

private fun checkPoint(
    case: String,
    answer: JsonObject,
    point: Point,
) {
    assertClose("$case x", answer.double("x"), point.x)
    assertClose("$case y", answer.double("y"), point.y)
}

private fun fretboardCase(answer: JsonObject): String {
    val size = "${answer.double("width")}x${answer.double("height")}"
    val hand = if (answer.bool("isLeftHanded")) "left-handed" else "right-handed"
    return "fretboard $size ${answer.int("fretCount")} frets $hand"
}

private fun sizeOf(answer: JsonObject): Size =
    Size(width = answer.double("width"), height = answer.double("height"))

private fun doublesOf(array: JsonArray): List<Double> {
    val values = mutableListOf<Double>()
    for (element in array) {
        values.add(element.jsonPrimitive.double)
    }
    return values
}

private fun intsOf(array: JsonArray): List<Int> {
    val values = mutableListOf<Int>()
    for (element in array) {
        values.add(element.jsonPrimitive.int)
    }
    return values
}

/** The generator writes voices by their Swift case names. */
private fun voiceId(voice: DrumVoice): String =
    when (voice) {
        DrumVoice.HI_HAT -> "hiHat"
        DrumVoice.SNARE -> "snare"
        DrumVoice.KICK -> "kick"
    }
