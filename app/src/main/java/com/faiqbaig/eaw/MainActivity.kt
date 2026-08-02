package com.faiqbaig.eaw

import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.faiqbaig.eaw.ui.MainMenuScreen
import com.faiqbaig.eaw.ui.ModeSelectScreen
import com.faiqbaig.eaw.ui.SandboxScreen

// Define our simple routing states
enum class ScreenState {
    MainMenu, ModeSelect, Sandbox
}

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

                    // State to control which screen is visible
                    var currentScreen by remember { mutableStateOf(ScreenState.MainMenu) }

                    // Only play the out-of-game music when NOT in the Sandbox
                    if (currentScreen != ScreenState.Sandbox) {
                        DisposableEffect(Unit) {
                            val mediaPlayer = MediaPlayer.create(context, R.raw.bgm_main).apply {
                                isLooping = true
                                setVolume(1.0f, 1.0f)
                                start()
                            }

                            val enhancer = try {
                                LoudnessEnhancer(mediaPlayer.audioSessionId).apply {
                                    setTargetGain(350)
                                    enabled = true
                                }
                            } catch (e: Exception) { null }

                            onDispose {
                                try { enhancer?.release() } catch (e: Exception) { }
                                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                mediaPlayer.release()
                            }
                        }
                    }

                    // Handle the smooth transitions between screens
                    Crossfade(targetState = currentScreen, label = "Screen Navigation") { screen ->
                        when (screen) {
                            ScreenState.MainMenu -> {
                                MainMenuScreen(
                                    onStartNewClick = { currentScreen = ScreenState.ModeSelect },
                                    onLoadGameClick = { /* TODO */ },
                                    onExitClick = { finish() },
                                    onSettingsClick = { /* TODO */ }
                                )
                            }
                            ScreenState.ModeSelect -> {
                                ModeSelectScreen(
                                    onSandboxClick = { currentScreen = ScreenState.Sandbox },
                                    onConquestClick = { /* TODO */ },
                                    onBackClick = { currentScreen = ScreenState.MainMenu }
                                )
                            }
                            ScreenState.Sandbox -> {
                                SandboxScreen(
                                    onExitToMenu = { currentScreen = ScreenState.MainMenu }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}