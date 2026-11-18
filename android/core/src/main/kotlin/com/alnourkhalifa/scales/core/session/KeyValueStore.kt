package com.alnourkhalifa.scales.core.session

/**
 * Where saved settings live, as plain text under string keys. No Swift twin: iOS saves the
 * settings through `UserDefaults`. Keeping this an interface lets [SettingsStore] run in JVM
 * tests over [InMemoryKeyValueStore], while the app wraps `SharedPreferences` in a few lines,
 * its only Android-specific storage code.
 */
interface KeyValueStore {
    /** The text stored under the key, or null when nothing is. */
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )

    fun remove(key: String)

    /** Every key holding a value, for tests and diagnosis. */
    fun keys(): Set<String>
}

/** A [KeyValueStore] in memory only, for tests and for a session that must not touch the
 *  player's own settings. */
class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun keys(): Set<String> = values.keys.toSet()
}
