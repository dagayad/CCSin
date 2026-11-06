package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class SinCcApplication : Application() {

    companion object {
        private const val TAG = "SinCcApplication"
        lateinit var instance: SinCcApplication
            private set

        fun ensureFirebaseInitialized(context: Context): FirebaseApp {
            val existingApps = FirebaseApp.getApps(context)
            if (existingApps.isNotEmpty()) {
                val defaultApp = runCatching { FirebaseApp.getInstance() }.getOrNull()
                if (defaultApp != null) return defaultApp
                return existingApps.first()
            }

            // 1. Initialize directly with verified FirebaseOptions (bypasses any OEM resource reader quirks)
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:729010840009:android:3555d91384eaf739148818")
                .setApiKey("AIzaSyAitz0Nkp0OdsJsrd4PDlPI9MHo0TV8CY4")
                .setProjectId("cccc-5d0b3")
                .setStorageBucket("cccc-5d0b3.firebasestorage.app")
                .setGcmSenderId("729010840009")
                .build()

            val appWithOptions = runCatching { FirebaseApp.initializeApp(context, options) }.getOrNull()
            if (appWithOptions != null) {
                Log.d(TAG, "FirebaseApp initialized directly with verified FirebaseOptions.")
                return appWithOptions
            }

            // 2. Standard resource fallback if direct initialization was already processed
            val standardApp = runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
            if (standardApp != null) {
                return standardApp
            }

            return FirebaseApp.getInstance()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            ensureFirebaseInitialized(this)
            Log.d(TAG, "FirebaseApp successfully initialized in Application.onCreate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseApp in Application.onCreate", e)
        }
    }
}
