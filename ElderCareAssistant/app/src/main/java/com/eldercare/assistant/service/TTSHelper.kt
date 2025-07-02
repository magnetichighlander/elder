package com.eldercare.assistant.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Text-To-Speech Helper for announcements and feedback
 * Handles simple speech synthesis tasks
 */
class TTSHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var isReady = false
    private val pendingMessages = mutableListOf<String>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            isReady = true
            pendingMessages.forEach { speak(it) }
            pendingMessages.clear()
        }
    }

    fun speak(message: String) {
        if (isReady) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
        } else {
            pendingMessages.add(message)
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
