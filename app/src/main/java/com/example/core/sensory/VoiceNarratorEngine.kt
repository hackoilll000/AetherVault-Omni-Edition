package com.example.core.sensory

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Text-To-Speech Audio Narration Engine for AetherVault
 * Provides Hindi and English spoken voice walkthroughs for onboarding and setup
 */
class VoiceNarratorEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    var isInitialized by mutableStateOf(false)
        private set

    var isSpeaking by mutableStateOf(false)
        private set

    var isMuted by mutableStateOf(false)

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            // TTS might fail on some platforms
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                }
            })
        }
    }

    fun speak(text: String, isHindi: Boolean = false) {
        if (isMuted || !isInitialized || tts == null) return
        try {
            val locale = if (isHindi) Locale("hi", "IN") else Locale.US
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AETHER_VOICE_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            // graceful fallback
        }
    }

    fun stop() {
        try {
            tts?.stop()
            isSpeaking = false
        } catch (e: Exception) {
            // ignore
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
