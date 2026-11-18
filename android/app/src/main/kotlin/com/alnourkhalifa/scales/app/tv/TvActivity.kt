package com.alnourkhalifa.scales.app.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.alnourkhalifa.scales.ui.MusicFontFamily

/**
 * The television screen (TV Material, D-pad only), opened by Android TV, Google TV and Fire OS
 * launchers and by PhoneActivity on TV hardware. For now it shows one line of text set in the
 * music font, which proves the bundled double sharp renders on a TV system image.
 */
class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // TV Material defaults to a light scheme; TV launchers and the window theme are dark.
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Scales — TV: C𝄪 Lydian ♭7",
                            fontFamily = MusicFontFamily,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            }
        }
    }
}
