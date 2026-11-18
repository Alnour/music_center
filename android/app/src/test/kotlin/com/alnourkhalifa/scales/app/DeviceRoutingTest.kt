package com.alnourkhalifa.scales.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The phone screen hands over to the TV screen on any TV, Fire OS included. */
class DeviceRoutingTest {
    // feature: tv-routing
    @Test
    fun phoneWithoutTvFeaturesStaysOnThePhoneScreen() {
        assertFalse(shouldOpenTvActivity(hasLeanback = false, hasFireTv = false))
    }

    // feature: tv-routing
    @Test
    fun androidTvOpensTheTvScreen() {
        assertTrue(shouldOpenTvActivity(hasLeanback = true, hasFireTv = false))
    }

    // feature: tv-routing
    @Test
    fun fireTvWithoutLeanbackStillOpensTheTvScreen() {
        assertTrue(shouldOpenTvActivity(hasLeanback = false, hasFireTv = true))
    }
}
