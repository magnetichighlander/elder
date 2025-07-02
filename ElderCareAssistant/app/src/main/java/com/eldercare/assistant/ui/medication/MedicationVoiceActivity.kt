package com.eldercare.assistant.ui.medication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eldercare.assistant.R
import com.eldercare.assistant.service.MedicationNotificationService
import com.eldercare.assistant.service.VoiceService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for voice-based medication confirmation
 */
@AndroidEntryPoint
class MedicationVoiceActivity : AppCompatActivity() {
    
    @Inject
    lateinit var voiceService: VoiceService
    
    private val viewModel: MedicationReminderViewModel by viewModels()
    
    private lateinit var instructionText: TextView
    private lateinit var statusText: TextView
    private lateinit var startListeningButton: Button
    private lateinit var backButton: Button
    
    private var medicationId: Long = -1L
    private var logId: Long = -1L
    private var medicationName: String = ""
    private var isListening = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_voice)
        
        // Extract data from intent
        medicationId = intent.getLongExtra(MedicationNotificationService.EXTRA_MEDICATION_ID, -1L)
        logId = intent.getLongExtra(MedicationNotificationService.EXTRA_LOG_ID, -1L)
        medicationName = intent.getStringExtra(MedicationNotificationService.EXTRA_MEDICATION_NAME) ?: ""
        
        if (medicationId == -1L || logId == -1L) {
            finish()
            return
        }
        
        initializeViews()
        setupClickListeners()
        observeVoiceService()
        
        // Start voice prompt
        lifecycleScope.launch {
            delay(500)
            startVoicePrompt()
        }
    }
    
    private fun initializeViews() {
        instructionText = findViewById(R.id.instructionText)
        statusText = findViewById(R.id.statusText)
        startListeningButton = findViewById(R.id.startListeningButton)
        backButton = findViewById(R.id.backButton)
        
        instructionText.text = "Say 'taken' to confirm you have taken $medicationName"
        statusText.text = "Ready to listen..."
    }
    
    private fun setupClickListeners() {
        startListeningButton.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }
        
        backButton.setOnClickListener {
            finish()
        }
    }
    
    private fun observeVoiceService() {
        // Observe listening state
        lifecycleScope.launch {
            voiceService.isListening.collect { listening ->
                isListening = listening
                updateUI()
            }
        }
        
        // Observe speech recognition results
        lifecycleScope.launch {
            voiceService.speechRecognitionResult.collect { result ->
                result?.let { speechResult ->
                    handleSpeechResult(speechResult)
                }
            }
        }
    }
    
    private fun startVoicePrompt() {
        voiceService.speak("Say 'taken' to confirm you have taken your medication $medicationName")
        
        lifecycleScope.launch {
            delay(3000) // Wait for TTS to finish
            startListening()
        }
    }
    
    private fun startListening() {
        statusText.text = "Listening... Say 'taken'"
        voiceService.startListening()
    }
    
    private fun stopListening() {
        statusText.text = "Stopped listening"
        voiceService.stopListening()
    }
    
    private fun updateUI() {
        startListeningButton.text = if (isListening) "Stop Listening" else "Start Listening"
        
        if (isListening) {
            statusText.text = "Listening... Say 'taken'"
            startListeningButton.setBackgroundResource(R.drawable.button_listening) // You'll need to create this
        } else {
            if (statusText.text == "Listening... Say 'taken'") {
                statusText.text = "Tap to start listening again"
            }
            startListeningButton.setBackgroundResource(R.drawable.button_primary) // You'll need to create this
        }
    }
    
    private fun handleSpeechResult(speechResult: String) {
        statusText.text = "You said: \"$speechResult\""
        
        if (voiceService.isMedicationConfirmed(speechResult)) {
            confirmMedication()
        } else {
            // Give feedback and allow retry
            lifecycleScope.launch {
                voiceService.speak("I didn't understand. Please say 'taken' to confirm your medication")
                delay(3000)
                statusText.text = "Say 'taken' to confirm, or tap button to try again"
            }
        }
    }
    
    private fun confirmMedication() {
        lifecycleScope.launch {
            try {
                statusText.text = "Confirming medication..."
                
                viewModel.confirmMedication(logId)
                
                // Speak confirmation
                voiceService.speak("Medication $medicationName confirmed as taken. Well done!")
                
                // Close activity after confirmation
                delay(2000)
                finish()
                
            } catch (e: Exception) {
                statusText.text = "Error confirming medication"
                voiceService.speak("Error confirming medication. Please try again.")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        voiceService.stopListening()
        voiceService.stopSpeaking()
    }
    
    override fun onPause() {
        super.onPause()
        voiceService.stopListening()
    }
}
