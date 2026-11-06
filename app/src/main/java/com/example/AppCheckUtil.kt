package com.example

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

// App Check contract keys. These strings are defined by the Firebase SDK and
// the runtime harness, so do not rename them.
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// Debug-only token. Release builds never use it (they use reCAPTCHA).
private const val FIXED_DEBUG_TOKEN = "de64817c-c6d8-48af-992f-6f941c31f91d"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized with guaranteed fallback
        val app = SinCcApplication.ensureFirebaseInitialized(context)
        val appCheck = FirebaseAppCheck.getInstance(app)

        val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
        val siteKey = if (resId != 0) {
            context.getString(resId)
        } else {
            "6LeDPxEuAAAAANesB3NoIU0fMvqgIBoJcYyjSZye"
        }

        if (BuildConfig.DEBUG) {
            // 2. Debug: Read debug token from Intent extra
            // Debug builds: use the token from the intent if given, otherwise this fixed debug token.
            // Register FIXED_DEBUG_TOKEN once in Firebase console > App Check > Apps > Manage debug tokens.
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA) ?: FIXED_DEBUG_TOKEN

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = app.persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: MUST use commit() instead of apply() so the secret is written
                // synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")

                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            } else if (!siteKey.isNullOrBlank()) {
                // Physical phone installation fallback: use provisioned reCAPTCHA key
                try {
                    appCheck.installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed for phone device.")
                } catch (e: Exception) {
                    Log.w(TAG, "RecaptchaAppCheckProviderFactory fallback to debug provider", e)
                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                }
            } else {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            }
        } else {
            // 3. Release: Look up recaptcha_site_key safely without throwing Resources.NotFoundException
            if (!siteKey.isNullOrBlank()) {
                appCheck.installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found or blank.")
            }
        }
    } catch (e: Exception) {
        // Log explicitly to keep App Check attestation failures visible
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
