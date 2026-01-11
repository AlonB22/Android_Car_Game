package com.example.hw1.services

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

class AndroidHaptics(private val context: Context) : Haptics {
    override fun vibrateCrash() = vibrate(60)

    override fun vibrateGameOver() = vibrate(140)

    private fun vibrate(ms: Long) {
        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(ms)
        }
    }
}
