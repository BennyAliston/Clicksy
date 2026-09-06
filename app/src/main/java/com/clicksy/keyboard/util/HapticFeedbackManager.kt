package com.clicksy.keyboard.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages haptic feedback for key presses.
 * Uses modern VibrationEffect API on Android 8.0+.
 */
class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        null
    }

    private val hasVibrator: Boolean = try {
        vibrator?.hasVibrator() == true
    } catch (e: Exception) {
        false
    }

    private val tickEffect: VibrationEffect? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    } catch (e: Exception) {
        null
    }

    private val clickEffect: VibrationEffect? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    } catch (e: Exception) {
        null
    }

    /**
     * Light haptic tick for standard key presses.
     */
    fun performKeyTick() {
        if (!hasVibrator) return
        val vib = vibrator ?: return
        val effect = tickEffect ?: return
        try {
            vib.vibrate(effect)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Medium haptic click for special keys (shift, enter, mode switch).
     */
    fun performKeyClick() {
        if (!hasVibrator) return
        val vib = vibrator ?: return
        val effect = clickEffect ?: return
        try {
            vib.vibrate(effect)
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
