package com.example.core.sensory

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build

enum class SoundEffectType {
    GLASS_TAP,
    LIQUID_MORPH,
    VAULT_LOCK,
    VAULT_UNLOCK,
    SYNC_CHIME,
    ERROR_THUD
}

/**
 * Spatial Micro-Sound Engine for AetherVault
 * Low-latency audio feedback inspired by iOS & VisionOS
 */
object SpatialSoundEngine {
    private var soundPool: SoundPool? = null
    private var toneGen: ToneGenerator? = null
    private var isMuted: Boolean = false

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, 25)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build()
        } catch (e: Exception) {
            // Audio synthesizer fallback
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun play(context: Context, type: SoundEffectType) {
        if (isMuted) return

        try {
            when (type) {
                SoundEffectType.GLASS_TAP -> {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 15)
                }
                SoundEffectType.LIQUID_MORPH -> {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 30)
                }
                SoundEffectType.VAULT_LOCK -> {
                    toneGen?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 20)
                }
                SoundEffectType.VAULT_UNLOCK -> {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 25)
                }
                SoundEffectType.SYNC_CHIME -> {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
                }
                SoundEffectType.ERROR_THUD -> {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_NACK, 50)
                }
            }
        } catch (e: Exception) {
            // Ignore audio synthesis errors
        }
    }
}
