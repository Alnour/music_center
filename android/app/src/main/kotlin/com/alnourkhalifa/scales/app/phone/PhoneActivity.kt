package com.alnourkhalifa.scales.app.phone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alnourkhalifa.scales.app.shouldOpenTvActivity
import com.alnourkhalifa.scales.app.tv.TvActivity
import com.alnourkhalifa.scales.core.music.FretboardLayout
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.core.music.Tuning
import com.alnourkhalifa.scales.core.session.NoteLabelStyle
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.MusicFontFamily
import com.alnourkhalifa.scales.ui.PhoneFretboard
import com.alnourkhalifa.scales.ui.ScaleNotesRow

/** Amazon's feature name for Fire TV devices; PackageManager has no constant for it. */
private const val FIRE_TV_FEATURE = "amazon.hardware.fire_tv"

/** The app's default board: 15 frets, as in `PracticeSettings`. */
private const val DEFAULT_FRET_COUNT = 15

/**
 * The phone and tablet screen (Material 3, touch).
 *
 * On a TV it opens TvActivity and closes itself: Fire OS launchers may list this
 * CATEGORY_LAUNCHER entry as well as the TV one, and the touch layout is unusable with a
 * remote. Until the Scales screen is ported it shows the default board (G Major, standard
 * tuning, note names, thin string on top) the way the iPhone lays it out: the notes row, then
 * the fretboard, with the lower third left for the chord cards.
 */
class PhoneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTelevision()) {
            startActivity(Intent(this, TvActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val isDark = isSystemInDarkTheme()
            var colorScheme = lightColorScheme()
            if (isDark) {
                colorScheme = darkColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme) {
                DefaultBoardPreview(FretboardPalette.matching(isDark))
            }
        }
    }

    private fun isTelevision(): Boolean {
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasFireTv = packageManager.hasSystemFeature(FIRE_TV_FEATURE)
        return shouldOpenTvActivity(hasLeanback = hasLeanback, hasFireTv = hasFireTv)
    }
}

/** G Major on the guitar, as a new player first sees it. */
@Composable
private fun DefaultBoardPreview(palette: FretboardPalette) {
    val scale = remember { Scale(NoteName(Letter.G), ScaleType.MAJOR) }
    val layout = remember { FretboardLayout(scale, Tuning.STANDARD, DEFAULT_FRET_COUNT) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .safeDrawingPadding()
                .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = scale.title,
            fontFamily = MusicFontFamily,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
            color = palette.title,
        )
        ScaleNotesRow(scale = scale, noteSize = 14.dp, palette = palette)
        PhoneFretboard(
            layout = layout,
            scaleTitle = scale.title,
            labelStyle = NoteLabelStyle.NOTE_NAMES,
            palette = palette,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(2f),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
