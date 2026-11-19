package com.alnourkhalifa.scales.app

import android.content.Context
import android.content.SharedPreferences
import com.alnourkhalifa.scales.core.session.KeyValueStore

/**
 * The saved settings on the device: [KeyValueStore] over one `SharedPreferences` file, the
 * Android counterpart of the `UserDefaults` the iOS app saves into. The app's only
 * Android-specific storage code; `SettingsStore` in core decides what goes under which key.
 *
 * Writes use `commit()`, which returns once the value is on disk, rather than `apply()`,
 * which queues the write. The TV screen must read the saved board before its first frame,
 * and a process that is killed (swiped away, or replaced by a TV launcher opening
 * `TvActivity` afresh) right after a change would otherwise come back with the old board.
 * A value that is already stored is not written again: `SettingsStore` writes every key on
 * each change, and only the one or two that differ need a disk write.
 */
class SharedPreferencesKeyValueStore(
    context: Context,
) : KeyValueStore {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(
        key: String,
        value: String,
    ) {
        val stored = preferences.getString(key, null)
        if (stored == value) {
            return
        }
        preferences
            .edit()
            .putString(key, value)
            .commit()
    }

    override fun remove(key: String) {
        if (!preferences.contains(key)) {
            return
        }
        preferences
            .edit()
            .remove(key)
            .commit()
    }

    override fun keys(): Set<String> =
        preferences.all.keys
            .toSet()

    /** Names shared with the tests. */
    companion object {
        /** The preferences file's name, also used by tests that start from a fresh install. */
        const val PREFERENCES_NAME = "practiceSettings"
    }
}
