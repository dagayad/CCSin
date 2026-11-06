package com.example.data.settings

import android.content.Context

/**
 * Keeps the Gemini API key the user typed in on first launch.
 * Stored only in this app's private storage on the phone. Never sent anywhere except Google's Gemini API.
 */
object ApiKeyStore {

    private const val PREFS_NAME = "sincc_settings"
    private const val KEY_GEMINI = "gemini_api_key"

    @Volatile
    private var cachedKey: String = ""

    /** Call once at startup, before any AI request. */
    fun load(context: Context) {
        cachedKey = try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_GEMINI, "")
                .orEmpty()
                .trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun hasUserKey(): Boolean = cachedKey.isNotBlank()

    fun userKey(): String = cachedKey

    fun save(context: Context, key: String) {
        val clean = key.trim()
        cachedKey = clean
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_GEMINI, clean)
                .apply()
        } catch (_: Exception) {
            // Key still works for this session even if saving to disk fails.
        }
    }
}
