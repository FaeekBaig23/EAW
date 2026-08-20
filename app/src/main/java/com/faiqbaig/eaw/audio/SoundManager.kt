package com.faiqbaig.eaw.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.faiqbaig.eaw.R

class SoundManager(context: Context) {

    init {
        instance = this
    }

    companion object {
        var instance: SoundManager? = null
    }
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

    private val volleySounds: List<Pair<Int, Float>> = listOf(
        soundPool.load(context, R.raw.volley1, 1) to 1.0f,
        soundPool.load(context, R.raw.volley2, 1) to 1.0f,
        soundPool.load(context, R.raw.volley3, 1) to 0.67f
    )

    // Load Artillery Fire SFX
    private val artillerySoundId: Int = soundPool.load(context, R.raw.artillery_fire, 1)

    fun playRandomVolley() {
        if (!isSfxEnabled) return
        val (soundId, volume) = volleySounds.random()
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }

    fun playArtilleryFire() {
        if (!isSfxEnabled) return
        soundPool.play(artillerySoundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}