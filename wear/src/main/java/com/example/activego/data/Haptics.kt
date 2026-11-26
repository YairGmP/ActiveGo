package com.example.activego.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Vibration feedback. The ActiveGo FRD requires the watch to confirm every action
 * with a vibration (feature F-01 and the usability note in section 5), because the
 * user is exercising and is not necessarily looking at the screen.
 */
object Haptics {

    const val SHORT = 60L
    const val MEDIUM = 150L
    const val LONG = 300L

    @Suppress("DEPRECATION")
    fun buzz(context: Context, millis: Long = SHORT) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator?.hasVibrator() != true) return
        vibrator.vibrate(
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }
}
