package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.settings.ApiKeyStore
import com.example.ui.components.ApiKeyDialog
import com.example.ui.screens.StudioMainScreen
import com.example.ui.theme.SinCcTheme
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StudioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // STEP 1: Mandatory App Check setup before any Firebase/AI Logic or Compose UI
        configureAppCheck(this, intent)
        super.onCreate(savedInstanceState)

        // STEP 2: Edge-to-edge support
        enableEdgeToEdge()

        // STEP 3: Load the user's saved Gemini API key (entered on first launch)
        ApiKeyStore.load(this)

        // STEP 4: SinCC Studio UI
        setContent {
            SinCcTheme {
                // Ask for the key only when the user has not entered one and the build has no key.
                var needsKey by remember {
                    mutableStateOf(!ApiKeyStore.hasUserKey() && BuildConfig.GEMINI_API_KEY.isBlank())
                }
                StudioMainScreen(viewModel = viewModel)
                if (needsKey) {
                    ApiKeyDialog(onSaved = { needsKey = false })
                }
            }
        }
    }
}
