package com.spark.app.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Plain (non-secret) settings: session limits + a random per-install device id.
 * Limits apply from the next session — the current feed was already sized from
 * them when it loaded.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("spark_settings", Context.MODE_PRIVATE)

    var cardLimit: Int
        get() = clampCards(prefs.getInt(KEY_CARD_LIMIT, DEFAULT_CARD_LIMIT))
        set(value) = prefs.edit().putInt(KEY_CARD_LIMIT, clampCards(value)).apply()

    var minuteLimit: Int
        get() = clampMinutes(prefs.getInt(KEY_MINUTE_LIMIT, DEFAULT_MINUTE_LIMIT))
        set(value) = prefs.edit().putInt(KEY_MINUTE_LIMIT, clampMinutes(value)).apply()

    /** Stable random id for this install; the server groups sessions by it. */
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { generated ->
                prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            }

    companion object {
        const val MIN_CARD_LIMIT = 5
        const val MAX_CARD_LIMIT = 100
        const val MIN_MINUTE_LIMIT = 5
        const val MAX_MINUTE_LIMIT = 120
        const val DEFAULT_CARD_LIMIT = SESSION_CARD_LIMIT
        const val DEFAULT_MINUTE_LIMIT = 15

        private const val KEY_CARD_LIMIT = "card_limit"
        private const val KEY_MINUTE_LIMIT = "minute_limit"
        private const val KEY_DEVICE_ID = "device_id"

        /** Pure clamp rules shared with the Me-tab steppers (unit-tested). */
        fun clampCards(value: Int): Int = value.coerceIn(MIN_CARD_LIMIT, MAX_CARD_LIMIT)
        fun clampMinutes(value: Int): Int = value.coerceIn(MIN_MINUTE_LIMIT, MAX_MINUTE_LIMIT)
    }
}
