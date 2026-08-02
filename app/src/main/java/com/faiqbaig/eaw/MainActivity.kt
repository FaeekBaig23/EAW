package com.faiqbaig.eaw

import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.faiqbaig.eaw.ui.MainMenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
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
                    val context = LocalContext.current

                    DisposableEffect(Unit) {
                        val mediaPlayer = MediaPlayer.create(context, R.raw.bgm_main).apply {
                            isLooping = true
                            setVolume(1.0f, 1.0f) // Max native stream volume
                            start()
                        }

                        // Attach LoudnessEnhancer to gain extra volume boost (+200 mB / ~2dB boost)
                        val enhancer = try {
                            LoudnessEnhancer(mediaPlayer.audioSessionId).apply {
                                setTargetGain(350) // Gain in millibels
                                enabled = true
                            }
                        } catch (e: Exception) {
                            null
                        }

                        onDispose {
                            try {
                                enhancer?.release()
                            } catch (e: Exception) { /* Ignored on release */ }

                            if (mediaPlayer.isPlaying) {
                                mediaPlayer.stop()
                            }
                            mediaPlayer.release()
                        }
                    }

                    MainMenuScreen(
                        onStartNewClick = { /* TODO: Navigate to Mode Select */ },
                        onLoadGameClick = { /* TODO: Load Conquest save */ },
                        onExitClick = { finish() },
                        onSettingsClick = { /* TODO: Open Settings */ }
                    )
                }
            }
        }
    }
}