package com.eldercare.assistant.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * Worker for handling snoozed medication reminders
 */
@HiltWorker
class SnoozeReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationService: MedicationNotificationService
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SnoozeReminderWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Snooze reminder worker started")
            
            val medicationId = inputData.getLong(MedicationReminderWorker.KEY_MEDICATION_ID, -1L)
            val medicationName = inputData.getString(MedicationReminderWorker.KEY_MEDICATION_NAME) ?: ""
            val dosage = inputData.getString(MedicationReminderWorker.KEY_MEDICATION_DOSAGE) ?: ""
            val instruction = inputData.getString(MedicationReminderWorker.KEY_MEDICATION_INSTRUCTION) ?: ""
            
            if (medicationId == -1L) {
                Log.e(TAG, "Invalid medication ID for snooze")
                return@withContext Result.failure()
            }
            
            // Create a new log entry for the snoozed reminder
            // We'll use a temporary log ID since this is a snooze
            val tempLogId = System.currentTimeMillis()
            
            // Show snooze notification
            notificationService.showMedicationReminder(
                medicationId = medicationId,
                logId = tempLogId,
                medicationName = medicationName,
                dosage = dosage,
                instruction = "⏰ Snoozed Reminder • $instruction"
            )
            
            Log.d(TAG, "Snooze reminder notification sent for: $medicationName")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in snooze reminder worker", e)
            Result.failure()
        }
    }
}
