package com.example.core.sensory

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticStyle {
    LIGHT_TICK,
    MEDIUM_IMPACT,
    HEAVY_IMPACT,
    SUCCESS_DOUBLE_PULSE,
    WARNING_BUZZ
}

/**
 * Premium Sensory Feedback Engine for AetherVault
 * Provides iOS/Android flagship tactile haptic matrices and subtle audio feedback
 */
object SensoryFeedbackEngine {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 35)
        } catch (e: Exception) {
            // Audio feedback optional
        }
    }

    fun trigger(context: Context, style: HapticStyle, playSound: Boolean = true) {
        // 1. Hardware Haptics
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        when (style) {
                            HapticStyle.LIGHT_TICK -> {
                                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                            }
                            HapticStyle.MEDIUM_IMPACT -> {
                                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            }
                            HapticStyle.HEAVY_IMPACT -> {
                                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                            }
                            HapticStyle.SUCCESS_DOUBLE_PULSE -> {
                                val timings = longArrayOf(0, 30, 60, 45)
                                val amplitudes = intArrayOf(0, 180, 0, 255)
                                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                            }
                            HapticStyle.WARNING_BUZZ -> {
                                val timings = longArrayOf(0, 50, 40, 50)
                                val amplitudes = intArrayOf(0, 200, 0, 200)
                                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        when (style) {
                            HapticStyle.LIGHT_TICK -> vib.vibrate(15)
                            HapticStyle.MEDIUM_IMPACT -> vib.vibrate(35)
                            HapticStyle.HEAVY_IMPACT -> vib.vibrate(60)
                            HapticStyle.SUCCESS_DOUBLE_PULSE -> vib.vibrate(longArrayOf(0, 30, 40, 45), -1)
                            HapticStyle.WARNING_BUZZ -> vib.vibrate(longArrayOf(0, 60, 40, 60), -1)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore haptic failures
        }

        // 2. Subtle Tone Feedback
        if (playSound) {
            try {
                when (style) {
                    HapticStyle.LIGHT_TICK -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 20)
                    HapticStyle.SUCCESS_DOUBLE_PULSE -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
                    HapticStyle.WARNING_BUZZ -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 60)
                    else -> {}
                }
            } catch (e: Exception) {
                // Ignore tone failures
            }
        }
    }
}
