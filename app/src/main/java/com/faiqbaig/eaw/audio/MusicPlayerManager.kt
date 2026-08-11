package com.faiqbaig.eaw.audio // Adjust package name as needed

import android.content.Context
import android.media.MediaPlayer
import com.faiqbaig.eaw.R // Ensure this imports your app's R class
import kotlinx.coroutines.*

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var playlist = listOf<Int>()
    private var currentIndex = 0
    private var isPlaying = false
    private var delayJob: Job? = null

    // List of all your march resources
    private val allMarches = listOf(
        R.raw.la_marseillaise,
        R.raw.radetzky_march,
        R.raw.marengo_march,
        R.raw.konig_march,
        R.raw.wagram_march,
        R.raw.prussia_gloria,
        R.raw.british_grenadiers,
        R.raw.la_grenadiere
    )

    fun startMusic(context: Context) {
        if (isPlaying) return
        isPlaying = true

        // Shuffle the playlist every time a new game/session starts
        playlist = allMarches.shuffled()
        currentIndex = 0

        playNextTrack(context)
    }

    private fun playNextTrack(context: Context) {
        if (playlist.isEmpty() || !isPlaying) return

        // Release any existing player before creating a new one
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(context, playlist[currentIndex])

        mediaPlayer?.setOnCompletionListener {
            // When a track finishes, wait 2 seconds then play the next one
            delayJob?.cancel()
            delayJob = CoroutineScope(Dispatchers.Main).launch {
                delay(2000L) // 2-second delay
                if (isPlaying) {
                    currentIndex = (currentIndex + 1) % playlist.size
                    playNextTrack(context)
                }
            }
        }

        mediaPlayer?.start()
    }

    fun stopMusic() {
        isPlaying = false
        delayJob?.cancel()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
    }

    fun resumeMusic() {
        mediaPlayer?.start()
    }
}