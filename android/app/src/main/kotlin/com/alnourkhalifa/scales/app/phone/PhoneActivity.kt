package com.alnourkhalifa.scales.app.phone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.alnourkhalifa.scales.app.shouldOpenTvActivity
import com.alnourkhalifa.scales.app.tv.TvActivity
import com.alnourkhalifa.scales.ui.MusicFontFamily

/** Amazon's feature name for Fire TV devices; PackageManager has no constant for it. */
private const val FIRE_TV_FEATURE = "amazon.hardware.fire_tv"

/**
 * The phone and tablet screen (Material 3, touch).
 *
 * On a TV it opens TvActivity and closes itself: Fire OS launchers may list this
 * CATEGORY_LAUNCHER entry as well as the TV one, and the touch layout is unusable with a
 * remote. For now it shows one line of text set in the music font, which proves the bundled
 * double sharp renders.
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
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Scales — phone: C𝄪 Lydian ♭7",
                            fontFamily = MusicFontFamily,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
        }
    }

    private fun isTelevision(): Boolean {
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasFireTv = packageManager.hasSystemFeature(FIRE_TV_FEATURE)
        return shouldOpenTvActivity(hasLeanback = hasLeanback, hasFireTv = hasFireTv)
    }
}
