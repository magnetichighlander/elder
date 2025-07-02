package com.eldercare.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import java.util.Locale

/**
 * Voice Assistant for offline voice recognition and processing
 * Handles Russian language commands for elderly users
 */
class VoiceAssistant(
    private val context: Context,
    private val commandCallback: (command: String) -> Unit
) : RecognitionListener {

    private val commandProcessor = VoiceCommandProcessor(context)

    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@VoiceAssistant)
        }
    }

    /**
     * Start listening for voice commands
     * Configured for offline speech recognition
     */
    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Crucial for offline
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите команду...")
            }
            speechRecognizer.startListening(intent)
        } else {
            Toast.makeText(context, "Голосовое распознавание недоступно", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Stop listening and cleanup resources
     */
    fun stopListening() {
        speechRecognizer.stopListening()
    }

    /**
     * Destroy the speech recognizer
     */
    fun destroy() {
        speechRecognizer.destroy()
    }

    // RecognitionListener callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        commandCallback("ready")
    }

    override fun onBeginningOfSpeech() {
        // Speech input has begun
    }

    override fun onRmsChanged(rmsdB: Float) {
        // RMS value changed - can be used for volume visualization
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }

    override fun onEndOfSpeech() {
        // Speech input ended
    }

    override fun onError(error: Int) {
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> commandCallback("no_match")
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> commandCallback("timeout")
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> commandCallback("network_timeout")
            SpeechRecognizer.ERROR_NETWORK -> commandCallback("network_error")
            SpeechRecognizer.ERROR_AUDIO -> commandCallback("audio_error")
            SpeechRecognizer.ERROR_SERVER -> commandCallback("server_error")
            SpeechRecognizer.ERROR_CLIENT -> commandCallback("client_error")
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> commandCallback("permission_error")
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> commandCallback("busy_error")
            else -> commandCallback("error_$error")
        }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
            processCommand(it)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Partial results received during recognition
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        // Recognition events
    }

    /**
     * Process recognized voice command using the vocabulary processor
     * Supports Russian language commands for elderly users
     */
    private fun processCommand(rawCommand: String) {
        Log.d("VoiceAssistant", "Raw command received: $rawCommand")
        
        // Use the command processor for intelligent matching
        val matchedCommand = commandProcessor.processCommand(rawCommand)
        
        if (matchedCommand != null) {
            Log.d("VoiceAssistant", "Command matched: $matchedCommand")
            commandCallback(matchedCommand)
        } else {
            Log.d("VoiceAssistant", "No match found for: $rawCommand")
            commandCallback("unrecognized")
        }
    }

    /**
     * Extract contact name from voice command
     * Simple extraction - in real app use NLP or predefined list
     */
    private fun extractContact(command: String): String {
        return when {
            command.contains("мама") || command.contains("маме") -> "mom"
            command.contains("папа") || command.contains("папе") -> "dad"
            command.contains("врач") || command.contains("доктор") -> "doctor"
            command.contains("сын") || command.contains("сыну") -> "son"
            command.contains("дочь") || command.contains("дочери") -> "daughter"
            command.contains("скорая") || command.contains("скорую") -> "emergency"
            command.contains("полиция") || command.contains("полицию") -> "police"
            else -> "default"
        }
    }

    /**
     * Check if voice assistant is available on this device
     */
    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
}
