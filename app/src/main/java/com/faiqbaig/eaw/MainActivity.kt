package com.faiqbaig.eaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent the UI from fitting system windows, allowing it to draw edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Retrieve the insets controller
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Hide system bars and enable transient swipe behavior
        windowInsetsController.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Placeholder for your Milestone 2 Sandbox rendering
                    Text(text = "Corps: Europe at War - Sandbox Environment")
                }
            }
        }
    }
}