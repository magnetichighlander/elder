package com.eldercare.assistant.ui.medication

import android.content.Intent
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
 * Full-screen activity for medication reminders with elderly-friendly UI
 */
@AndroidEntryPoint
class MedicationReminderActivity : AppCompatActivity() {
    
    @Inject
    lateinit var voiceService: VoiceService
    
    private val viewModel: MedicationReminderViewModel by viewModels()
    
    private lateinit var medicationNameText: TextView
    private lateinit var dosageText: TextView
    private lateinit var instructionText: TextView
    private lateinit var timeText: TextView
    private lateinit var confirmButton: Button
    private lateinit var snoozeButton: Button
    private lateinit var voiceButton: Button
    
    private var medicationId: Long = -1L
    private var logId: Long = -1L
    private var medicationName: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medication_reminder)
        
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
        loadMedicationDetails()
        
        // Start voice reminder after a short delay
        lifecycleScope.launch {
            delay(1000)
            startVoiceReminder()
        }
    }
    
    private fun initializeViews() {
        medicationNameText = findViewById(R.id.medicationNameText)
        dosageText = findViewById(R.id.dosageText)
        instructionText = findViewById(R.id.instructionText)
        timeText = findViewById(R.id.timeText)
        confirmButton = findViewById(R.id.confirmButton)
        snoozeButton = findViewById(R.id.snoozeButton)
        voiceButton = findViewById(R.id.voiceButton)
    }
    
    private fun setupClickListeners() {
        confirmButton.setOnClickListener {
            confirmMedication()
        }
        
        snoozeButton.setOnClickListener {
            snoozeMedication()
        }
        
        voiceButton.setOnClickListener {
            startVoiceConfirmation()
        }
    }
    
    private fun loadMedicationDetails() {
        viewModel.loadMedication(medicationId)
        
        viewModel.medication.observe(this) { medication ->
            medication?.let {
                medicationNameText.text = it.name
                dosageText.text = "Take ${it.dosage}"
                instructionText.text = if (it.instruction.isNotBlank()) it.instruction else "No special instructions"
                timeText.text = "Scheduled for ${it.time}"
                
                medicationName = it.name
            }
        }
    }
    
    private fun startVoiceReminder() {
        viewModel.medication.value?.let { medication ->
            voiceService.speakMedicationReminder(
                medicationName = medication.name,
                dosage = medication.dosage,
                instruction = medication.instruction
            )
        }
    }
    
    private fun confirmMedication() {
        lifecycleScope.launch {
            try {
                viewModel.confirmMedication(logId)
                
                // Speak confirmation
                voiceService.speak("Medication confirmed as taken. Well done!")
                
                // Close activity
                finish()
            } catch (e: Exception) {
                // Handle error
                voiceService.speak("Error confirming medication. Please try again.")
            }
        }
    }
    
    private fun snoozeMedication() {
        lifecycleScope.launch {
            try {
                viewModel.snoozeMedication(medicationId, logId, medicationName)
                
                // Speak snooze confirmation
                voiceService.speak("Medication reminder snoozed for 10 minutes")
                
                // Close activity
                finish()
            } catch (e: Exception) {
                voiceService.speak("Error snoozing medication. Please try again.")
            }
        }
    }
    
    private fun startVoiceConfirmation() {
        val intent = Intent(this, MedicationVoiceActivity::class.java).apply {
            putExtra(MedicationNotificationService.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationNotificationService.EXTRA_LOG_ID, logId)
            putExtra(MedicationNotificationService.EXTRA_MEDICATION_NAME, medicationName)
        }
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        voiceService.stopSpeaking()
    }
}
