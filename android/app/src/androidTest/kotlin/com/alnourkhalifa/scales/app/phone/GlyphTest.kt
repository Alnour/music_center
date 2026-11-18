package com.alnourkhalifa.scales.app.phone

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.alnourkhalifa.scales.ui.MusicFontFamily
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.alnourkhalifa.scales.ui.R as UiR

/**
 * Proves the accidentals render on this system image. Fire OS 7 has no glyph for 𝄪 or 𝄫, so
 * the app bundles them (`ui.MusicFontFamily`); a missing glyph would show a tofu box on every
 * board that spells a double sharp. Runs on whichever emulator `connectedDebugAndroidTest`
 * reaches, the Android 9 floor image included.
 */
@RunWith(AndroidJUnit4::class)
class GlyphTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val accidentals = listOf("♭", "♯", "♮", "𝄪", "𝄫")

    // feature: board
    @Test
    fun bundledTypefaceHasEveryAccidental() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val typeface = context.resources.getFont(UiR.font.noto_music_accidentals)
        val paint = Paint()
        paint.typeface = typeface
        for (accidental in accidentals) {
            assertTrue("no glyph for $accidental", paint.hasGlyph(accidental))
        }
    }

    // feature: board
    @Test
    fun doubleSharpRendersThroughTheMusicFont() {
        val style = TextStyle(color = Color.Black, fontSize = 48.sp, fontFamily = MusicFontFamily)
        composeRule.setContent {
            Column {
                BasicText(
                    text = "C",
                    style = style,
                    modifier = Modifier.testTag("letter"),
                )
                BasicText(
                    text = "C𝄪",
                    style = style,
                    modifier =
                        Modifier
                            .background(Color.White)
                            .testTag("doubleSharp"),
                )
            }
        }
        val letterWidth =
            composeRule
                .onNodeWithTag("letter")
                .captureToImage()
                .width
        val image = composeRule.onNodeWithTag("doubleSharp").captureToImage()
        assertTrue("C𝄪 is no wider than C", image.width > letterWidth)
        // The ink right of the letter is the 𝄪 itself.
        val accidentalInk = countDarkPixels(image.toPixelMap(), fromColumn = letterWidth)
        assertTrue("nothing drawn where the 𝄪 should be", accidentalInk > 0)
    }

    /** Dark pixels from a column to the right edge: the text's ink on the white background. */
    private fun countDarkPixels(
        pixels: PixelMap,
        fromColumn: Int,
    ): Int {
        var count = 0
        for (x in fromColumn until pixels.width) {
            for (y in 0 until pixels.height) {
                if (pixels[x, y].red < 0.5f) {
                    count += 1
                }
            }
        }
        return count
    }
}
