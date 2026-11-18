package com.alnourkhalifa.scales.app.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * The television screen (TV Material, D-pad only), opened by Android TV, Google TV and Fire OS
 * launchers and by PhoneActivity on TV hardware. Everything it shows is [TvApp]; it reads the
 * same session snapshot as the phone, so a change made there redraws it.
 */
class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvApp()
        }
    }
}
