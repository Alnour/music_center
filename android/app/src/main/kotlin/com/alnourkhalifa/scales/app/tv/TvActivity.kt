package com.alnourkhalifa.scales.app.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.alnourkhalifa.scales.core.music.Letter
import com.alnourkhalifa.scales.core.music.NoteName
import com.alnourkhalifa.scales.core.music.Scale
import com.alnourkhalifa.scales.core.music.ScaleType
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.TVDisplayView

/** Margins on every side, as a share of the screen: clear of the overscan some TVs crop. */
private const val TV_MARGIN_SHARE = 0.05

/**
 * The television screen (TV Material, D-pad only), opened by Android TV, Google TV and Fire OS
 * launchers and by PhoneActivity on TV hardware. Until the TV screens are ported it shows the
 * default board (G Major, standard tuning, 15 frets, note names) in the dark TV layout, full
 * screen with 5% margins.
 */
class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scale = remember { Scale(NoteName(Letter.G), ScaleType.MAJOR) }
            TVDisplayView(
                scale = scale,
                palette = FretboardPalette.dark,
                modifier = Modifier.fillMaxSize(),
                horizontalMarginShare = TV_MARGIN_SHARE,
                verticalMarginShare = TV_MARGIN_SHARE,
            )
        }
    }
}
