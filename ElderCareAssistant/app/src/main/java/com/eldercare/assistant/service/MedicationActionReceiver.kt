package com.eldercare.assistant.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import com.eldercare.assistant.data.entity.MedicationLog
import com.eldercare.assistant.data.repository.MedicationRepository
import com.eldercare.assistant.ui.medication.MedicationVoiceActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Broadcast receiver for handling medication reminder notification actions
 */
@AndroidEntryPoint
class MedicationActionReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var repository: MedicationRepository
    
    @Inject
    lateinit var notificationService: MedicationNotificationService
    
    @Inject
    lateinit var voiceService: VoiceService
    
    companion object {
        private const val TAG = "MedicationActionReceiver"
        private const val SNOOZE_DURATION_MINUTES = 10L
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received action: ${intent.action}")
        
        val medicationId = intent.getLongExtra(MedicationNotificationService.EXTRA_MEDICATION_ID, -1L)
        val logId = intent.getLongExtra(MedicationNotificationService.EXTRA_LOG_ID, -1L)
        val medicationName = intent.getStringExtra(MedicationNotificationService.EXTRA_MEDICATION_NAME) ?: ""
        
        if (medicationId == -1L || logId == -1L) {
            Log.e(TAG, "Invalid medication or log ID")
            return
        }
        
        when (intent.action) {
            MedicationNotificationService.ACTION_CONFIRM_MEDICATION -> {
                handleConfirmMedication(context, medicationId, logId, medicationName)
            }
            MedicationNotificationService.ACTION_SNOOZE_MEDICATION -> {
                handleSnoozeMedication(context, medicationId, logId, medicationName)
            }
            MedicationNotificationService.ACTION_VOICE_CONFIRM -> {
                handleVoiceConfirm(context, medicationId, logId, medicationName)
            }
        }
    }
    
    private fun handleConfirmMedication(context: Context, medicationId: Long, logId: Long, medicationName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Record confirmation in database with current timestamp
                val confirmationTime = System.currentTimeMillis()
                repository.confirmMedication(logId, confirmationTime)
                
                // Create additional log entry for tracking (if needed)
                val additionalLog = MedicationLog(
                    medicationId = medicationId,
                    scheduledTime = confirmationTime,
                    confirmedTime = confirmationTime,
                    isConfirmed = true,
                    isMissed = false,
                    notes = "Confirmed via notification action"
                )
                
                // Cancel the reminder notification
                notificationService.cancelMedicationReminder(medicationId)
                
                // Show success confirmation notification
                notificationService.showMedicationConfirmed(medicationName)
                
                // Provide voice feedback
                voiceService.speak("Medication $medicationName has been confirmed as taken. Well done!")
                
                // Log the successful confirmation
                Log.d(TAG, "Medication confirmed successfully: $medicationName at ${java.util.Date(confirmationTime)}")
                
                // Optional: Update adherence statistics or trigger follow-up actions
                updateAdherenceStats(medicationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming medication: $medicationName", e)
                
                // Show error feedback to user
                voiceService.speak("Error confirming medication. Please try again or contact your caregiver.")
                
                // Keep notification visible on error
                // Don't cancel notification so user can try again
            }
        }
    }
    
    /**
     * Updates adherence statistics after medication confirmation
     */
    private suspend fun updateAdherenceStats(medicationId: Long) {
        try {
            // Could trigger weekly/monthly adherence report generation
            // Or update dashboard statistics
            val weeklyStats = repository.getWeeklyAdherenceStats(medicationId)
            Log.d(TAG, "Weekly adherence for medication $medicationId: ${weeklyStats.adherencePercentage}%")
            
            // If adherence is low, could trigger caregiver notifications
            if (weeklyStats.adherencePercentage < 80f && weeklyStats.total >= 7) {
                Log.w(TAG, "Low adherence detected for medication $medicationId")
                // TODO: Implement caregiver notification logic
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating adherence stats", e)
        }
    }
    
    private fun handleSnoozeMedication(context: Context, medicationId: Long, logId: Long, medicationName: String) {
        try {
            // Cancel current notification
            notificationService.cancelMedicationReminder(medicationId)
            
            // Schedule snooze reminder
            val snoozeData = Data.Builder()
                .putLong(MedicationReminderWorker.KEY_MEDICATION_ID, medicationId)
                .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medicationName)
                .putString(MedicationReminderWorker.KEY_MEDICATION_DOSAGE, "Snoozed reminder")
                .putString(MedicationReminderWorker.KEY_MEDICATION_INSTRUCTION, "")
                .build()
            
            val snoozeWorkRequest = OneTimeWorkRequestBuilder<SnoozeReminderWorker>()
                .setInitialDelay(SNOOZE_DURATION_MINUTES, TimeUnit.MINUTES)
                .setInputData(snoozeData)
                .addTag("snooze_${medicationId}")
                .build()
            
            WorkManager.getInstance(context).enqueue(snoozeWorkRequest)
            
            // Speak snooze confirmation
            voiceService.speak("Medication reminder snoozed for $SNOOZE_DURATION_MINUTES minutes")
            
            Log.d(TAG, "Medication snoozed: $medicationName for $SNOOZE_DURATION_MINUTES minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error snoozing medication", e)
        }
    }
    
    private fun handleVoiceConfirm(context: Context, medicationId: Long, logId: Long, medicationName: String) {
        try {
            // Start voice recognition activity
            val voiceIntent = Intent(context, MedicationVoiceActivity::class.java).apply {
                putExtra(MedicationNotificationService.EXTRA_MEDICATION_ID, medicationId)
                putExtra(MedicationNotificationService.EXTRA_LOG_ID, logId)
                putExtra(MedicationNotificationService.EXTRA_MEDICATION_NAME, medicationName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            context.startActivity(voiceIntent)
            
            Log.d(TAG, "Starting voice confirmation for: $medicationName")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice confirmation", e)
        }
    }
}
