package com.alnourkhalifa.scales.app.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.alnourkhalifa.scales.app.rememberScalesApplication
import com.alnourkhalifa.scales.ui.FretboardPalette
import com.alnourkhalifa.scales.ui.TVDisplayView

/** Margins on every side, as a share of the screen: clear of the overscan some TVs crop. */
private const val TV_MARGIN_SHARE = 0.05

/**
 * The television screen (TV Material, D-pad only), opened by Android TV, Google TV and Fire OS
 * launchers and by PhoneActivity on TV hardware. Until the TV screens are ported it shows the
 * saved board full screen with 5% margins, in the dark or light TV theme the player chose.
 * It reads the same session snapshot as the phone, so a change made there redraws it.
 */
class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = rememberScalesApplication()
            val state = app.sessionState.value
            val settings = state.settings
            TVDisplayView(
                scale = state.scale,
                palette = FretboardPalette.matching(settings.tvUsesDarkTheme),
                modifier = Modifier.fillMaxSize(),
                instrument = settings.selectedInstrument,
                tuning = settings.tuning,
                fretCount = settings.fretCount,
                keyboardOctaves = settings.selectedKeyboardOctaves,
                labelStyle = settings.labelStyle,
                thinStringOnTop = settings.thinStringOnTop,
                isLeftHanded = settings.isLeftHanded,
                ringedNotes = state.ringedNotes,
                horizontalMarginShare = TV_MARGIN_SHARE,
                verticalMarginShare = TV_MARGIN_SHARE,
            )
        }
    }
}
