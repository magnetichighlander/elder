package com.eldercare.assistant.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling voice operations including TTS and speech recognition
 */
@Singleton
class VoiceService @Inject constructor(
    private val context: Context
) : TextToSpeech.OnInitListener {
    
    companion object {
        private const val TAG = "VoiceService"
        private const val TTS_UTTERANCE_ID = "medication_reminder"
    }
    
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isInitialized = false
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking
    
    private val _speechRecognitionResult = MutableStateFlow<String?>(null)
    val speechRecognitionResult: StateFlow<String?> = _speechRecognitionResult
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    init {
        initializeTextToSpeech()
        initializeSpeechRecognizer()
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
    }
    
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    Log.d(TAG, "Ready for speech")
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Beginning of speech")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Voice level changes
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }
                
                override fun onEndOfSpeech() {
                    Log.d(TAG, "End of speech")
                    _isListening.value = false
                }
                
                override fun onError(error: Int) {
                    Log.e(TAG, "Speech recognition error: $error")
                    _isListening.value = false
                    _speechRecognitionResult.value = null
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = matches?.firstOrNull()
                    Log.d(TAG, "Speech recognition result: $result")
                    _speechRecognitionResult.value = result
                    _isListening.value = false
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // Handle partial results if needed
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Handle events if needed
                }
            })
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported")
                // Fallback to English
                textToSpeech?.setLanguage(Locale.ENGLISH)
            }
            
            // Configure TTS for elderly-friendly speech
            textToSpeech?.setSpeechRate(0.8f) // Slower speech rate
            textToSpeech?.setPitch(1.1f) // Slightly higher pitch for clarity
            
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }
                
                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }
                
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    Log.e(TAG, "TTS error for utterance: $utteranceId")
                }
            })
            
            isInitialized = true
            Log.d(TAG, "TextToSpeech initialized successfully")
        } else {
            Log.e(TAG, "TextToSpeech initialization failed")
        }
    }
    
    /**
     * Speaks the given text with medication reminder optimizations
     */
    fun speakMedicationReminder(medicationName: String, dosage: String, instruction: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized")
            return
        }
        
        val reminderText = buildString {
            append("Time for your medication. ")
            append("Please take $dosage of $medicationName")
            if (instruction.isNotBlank()) {
                append(", $instruction")
            }
            append(". ")
            append("Press the large button to confirm you have taken your medication, ")
            append("or say 'taken' to confirm by voice.")
        }
        
        speak(reminderText)
    }
    
    /**
     * Speaks the given text
     */
    fun speak(text: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized")
            return
        }
        
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
    }
    
    /**
     * Stops current speech
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }
    
    /**
     * Starts listening for voice commands
     */
    fun startListening() {
        if (!hasRecordAudioPermission()) {
            Log.w(TAG, "Missing RECORD_AUDIO permission")
            return
        }
        
        if (speechRecognizer == null) {
            Log.w(TAG, "Speech recognizer not available")
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'taken' to confirm medication")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _isListening.value = false
        }
    }

    /**
     * Checks if RECORD_AUDIO permission is granted
     */
    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Stops listening for voice commands
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }
    
    /**
     * Checks if the speech result indicates medication confirmation
     */
    fun isMedicationConfirmed(speechResult: String?): Boolean {
        if (speechResult.isNullOrBlank()) return false
        
        val positivePatterns = listOf(
            "^(yes|taken|done|finished|completed)$",
            "^(i )?((have|already) )?(took|taken) (it|them|medication)$",
            "^(i )?finished (taking )?medication$",
            "^confirmed$"
        )
        
        val negativePatterns = listOf(
            "no", "not", "haven't", "didn't", "won't", "can't", "refuse"
        )
        
        val lowerResult = speechResult.lowercase().trim()
        
        // Check for negative words first
        if (negativePatterns.any { lowerResult.contains(it) }) {
            return false
        }
        
        // Check for positive patterns
        return positivePatterns.any { pattern ->
            lowerResult.matches(Regex(pattern))
        }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}
