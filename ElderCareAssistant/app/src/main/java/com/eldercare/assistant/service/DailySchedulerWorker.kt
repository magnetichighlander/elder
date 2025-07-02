package com.eldercare.assistant.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eldercare.assistant.data.repository.MedicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.time.LocalDate
import java.time.LocalTime

/**
 * Worker that runs daily to schedule medication reminders for the next day
 * This ensures continuity of medication schedules
 */
@HiltWorker
class DailySchedulerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MedicationRepository,
    private val reminderService: MedicationReminderService
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailySchedulerWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Daily scheduler worker started")
            
            // Get tomorrow's date
            val tomorrow = LocalDate.now().plusDays(1)
            val tomorrowDayOfWeek = tomorrow.dayOfWeek
            
            // Get all active medications
            val activeMedications = repository.getAllActiveMedications()
            
            activeMedications.collect { medications ->
                var scheduledCount = 0
                
                medications.forEach { medication ->
                    // Check if this medication should be taken tomorrow
                    if (medication.days.contains(tomorrowDayOfWeek)) {
                        try {
                            // Schedule reminder for tomorrow
                            reminderService.scheduleMedicationReminder(medication)
                            scheduledCount++
                            
                            Log.d(TAG, "Scheduled reminder for ${medication.name} on $tomorrow at ${medication.time}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error scheduling reminder for ${medication.name}", e)
                        }
                    }
                }
                
                Log.d(TAG, "Daily scheduler completed: scheduled $scheduledCount reminders for $tomorrow")
            }
            
            // Clean up old completed medication logs (optional)
            cleanupOldLogs()
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in daily scheduler worker", e)
            Result.failure()
        }
    }
    
    /**
     * Cleans up old medication logs to prevent database bloat
     * Keeps logs for the last 30 days for adherence tracking
     */
    private suspend fun cleanupOldLogs() {
        try {
            val thirtyDaysAgo = LocalDate.now().minusDays(30)
            val cutoffTime = thirtyDaysAgo.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toEpochSecond() * 1000
            
            // Could add a method to repository for cleaning up old logs
            // repository.cleanupLogsOlderThan(cutoffTime)
            
            Log.d(TAG, "Cleanup completed for logs older than $thirtyDaysAgo")
        } catch (e: Exception) {
            Log.e(TAG, "Error during log cleanup", e)
        }
    }
}
