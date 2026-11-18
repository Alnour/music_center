package com.alnourkhalifa.scales.app

/**
 * Whether a launch of the phone screen should hand over to the TV screen instead.
 *
 * Android TV and Google TV launchers open TvActivity through LEANBACK_LAUNCHER, but some
 * Fire OS surfaces list the CATEGORY_LAUNCHER entry too, which would show the touch layout on
 * a television with only a remote. Either signal, the leanback feature or Amazon's Fire TV
 * feature, means the device is a TV.
 *
 * Kept as a pure function, apart from PackageManager, so a JVM test can pin the rule.
 */
fun shouldOpenTvActivity(
    hasLeanback: Boolean,
    hasFireTv: Boolean,
): Boolean = hasLeanback || hasFireTv
