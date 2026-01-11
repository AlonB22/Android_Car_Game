package com.example.hw1.services

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.hw1.R

class AndroidSfx(context: Context) : Sfx {
    private val soundPool: SoundPool
    private val crashSoundId: Int
    private var loaded = false
    private var pendingCrash = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        crashSoundId = soundPool.load(context, R.raw.crash, 1)
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == crashSoundId && status == 0) {
                loaded = true
                if (pendingCrash) {
                    pendingCrash = false
                    playCrashInternal()
                }
            }
        }
    }

    override fun playCrash() {
        if (!loaded) {
            pendingCrash = true
            return
        }
        playCrashInternal()
    }

    private fun playCrashInternal() {
        soundPool.play(crashSoundId, 1f, 1f, 1, 0, 0.7f)
        soundPool.play(crashSoundId, 0.6f, 0.6f, 0, 0, 0.55f)
    }

    override fun release() {
        soundPool.release()
    }
}
