package com.faiqbaig.eaw.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.faiqbaig.eaw.R

class SoundManager(context: Context) {
    var isSfxEnabled: Boolean = true
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val volleySoundIds: List<Int> = listOf(
        soundPool.load(context, R.raw.volley1, 1),
        soundPool.load(context, R.raw.volley2, 1),
        soundPool.load(context, R.raw.volley3, 1)
    )

    fun playRandomVolley() {
        if (!isSfxEnabled) return
        val soundId = volleySoundIds.random()
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}