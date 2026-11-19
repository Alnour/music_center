package com.alnourkhalifa.scales.app.phone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alnourkhalifa.scales.app.ScalesTheme
import com.alnourkhalifa.scales.app.shouldOpenTvActivity
import com.alnourkhalifa.scales.app.tv.TvActivity

/** Amazon's feature name for Fire TV devices; PackageManager has no constant for it. */
private const val FIRE_TV_FEATURE = "amazon.hardware.fire_tv"

/**
 * The phone and tablet screen (Material 3, touch): the four tabs of [RootView].
 *
 * On a TV it opens TvActivity and closes itself: Fire OS launchers may list this
 * CATEGORY_LAUNCHER entry as well as the TV one, and the touch layout is unusable with a
 * remote. The manifest lets it handle rotation and window resizes itself (`configChanges`):
 * Compose relayouts in place, so a turn of the phone keeps menus, scroll positions and the
 * drum loop instead of rebuilding the screen.
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
            ScalesTheme {
                RootView(
                    scales = { ScalesTabStub() },
                    chords = { ChordsTabStub() },
                    transform = { TransformTabStub() },
                    drums = { DrumsTabStub() },
                )
            }
        }
    }

    private fun isTelevision(): Boolean {
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasFireTv = packageManager.hasSystemFeature(FIRE_TV_FEATURE)
        return shouldOpenTvActivity(hasLeanback = hasLeanback, hasFireTv = hasFireTv)
    }
}
