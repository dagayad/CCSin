package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

        // STEP 3: SinCC Studio UI
        setContent {
            SinCcTheme {
                StudioMainScreen(viewModel = viewModel)
            }
        }
    }
}
