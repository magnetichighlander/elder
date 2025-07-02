package com.eldercare.assistant.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.eldercare.assistant.data.entity.Medication
import com.eldercare.assistant.data.entity.MedicationLog
import com.eldercare.assistant.data.repository.MedicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import android.util.Log
import kotlinx.coroutines.flow.first

/**
 * Background worker for medication reminders
 */
@HiltWorker
class MedicationReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MedicationRepository,
    private val notificationService: MedicationNotificationService,
    private val voiceService: VoiceService
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MedicationReminderWorker"
        const val WORK_NAME = "medication_reminder_work"
        
        // Input data keys
        const val KEY_MEDICATION_ID = "medication_id"
        const val KEY_MEDICATION_NAME = "medication_name"
        const val KEY_MEDICATION_DOSAGE = "medication_dosage"
        const val KEY_MEDICATION_INSTRUCTION = "medication_instruction"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Medication reminder worker started")
            
            val medicationId = inputData.getLong(KEY_MEDICATION_ID, -1L)
            val medicationName = inputData.getString(KEY_MEDICATION_NAME) ?: ""
            val dosage = inputData.getString(KEY_MEDICATION_DOSAGE) ?: ""
            val instruction = inputData.getString(KEY_MEDICATION_INSTRUCTION) ?: ""
            
            if (medicationId == -1L) {
                Log.e(TAG, "Invalid medication ID")
                return@withContext Result.failure()
            }
            
            // Get full medication details from database
            val medication = repository.getMedicationById(medicationId)
            if (medication == null) {
                Log.e(TAG, "Medication not found in database: $medicationId")
                return@withContext Result.failure()
            }
            
            // Create medication log entry
            val logEntry = MedicationLog(
                medicationId = medicationId,
                scheduledTime = System.currentTimeMillis(),
                confirmedTime = null,
                isConfirmed = false,
                isMissed = false
            )
            
            val logId = repository.insertLog(logEntry)
            
            // Show enhanced notification with accessibility features
            showEnhancedNotification(medication, logId)
            
            // Voice alert with TTS
            speakMedicationReminder(medication)
            
            Log.d(TAG, "Medication reminder notification and voice alert sent for: $medicationName")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in medication reminder worker", e)
            Result.failure()
        }
    }
    
    /**
     * Shows enhanced notification with elderly-friendly features
     */
    private fun showEnhancedNotification(medication: Medication, logId: Long) {
        try {
            notificationService.showMedicationReminder(
                medicationId = medication.id,
                logId = logId,
                medicationName = medication.name,
                dosage = medication.dosage,
                instruction = medication.instruction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }
    
    /**
     * Speaks medication reminder using TTS with elderly-optimized settings
     */
    private fun speakMedicationReminder(medication: Medication) {
        try {
            voiceService.speakMedicationReminder(
                medicationName = medication.name,
                dosage = medication.dosage,
                instruction = medication.instruction
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error with voice reminder", e)
        }
    }
}

/**
 * Service for managing medication reminder scheduling
 */
class MedicationReminderService @AssistedInject constructor(
    private val context: Context,
    private val repository: MedicationRepository
) {
    
    companion object {
        private const val TAG = "MedicationReminderService"
    }
    
    /**
     * Schedules reminders for all active medications
     */
    suspend fun scheduleAllMedicationReminders() {
        try {
            val medications = repository.getTodaysMedications()
            medications.forEach { medication ->
                scheduleMedicationReminder(medication)
            }
            Log.d(TAG, "Scheduled reminders for ${medications.size} medications")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling medication reminders", e)
        }
    }
    
    /**
     * Schedules a reminder for a specific medication with enhanced time calculations
     */
    suspend fun scheduleMedicationReminder(medication: Medication) {
        try {
            val timeZone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(timeZone)
            val today = now.toLocalDate()
            val todayOfWeek = today.dayOfWeek
            val scheduledTime = medication.time
            
            // Calculate initial delay using ZonedDateTime for proper timezone handling
            val initialDelay = if (medication.days.contains(todayOfWeek)) {
                // Medication is scheduled for today
                val todayScheduledTime = scheduledTime.atDate(today).atZone(timeZone)
                if (todayScheduledTime.isAfter(now)) {
                    // Schedule for today if time hasn't passed
                    Duration.between(now, todayScheduledTime).toMillis()
                } else {
                    // Time has passed today, schedule for next occurrence
                    getNextOccurrenceDelay(medication, today, timeZone)
                }
            } else {
                // Medication is not scheduled for today, find next occurrence
                getNextOccurrenceDelay(medication, today, timeZone)
            }
            
            if (initialDelay > 0) {
                val workData = Data.Builder()
                    .putLong(MedicationReminderWorker.KEY_MEDICATION_ID, medication.id)
                    .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medication.name)
                    .putString(MedicationReminderWorker.KEY_MEDICATION_DOSAGE, medication.dosage)
                    .putString(MedicationReminderWorker.KEY_MEDICATION_INSTRUCTION, medication.instruction)
                    .build()
                
                val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(workData)
                    .addTag("med_reminder_${medication.id}")
                    .build()
                
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "med_reminder_${medication.id}",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                
                val delayHours = initialDelay / (1000 * 60 * 60)
                val delayMinutes = (initialDelay % (1000 * 60 * 60)) / (1000 * 60)
                Log.d(TAG, "Scheduled reminder for ${medication.name} in ${delayHours}h ${delayMinutes}m")
            } else {
                Log.w(TAG, "Invalid delay calculated for medication: ${medication.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder for medication: ${medication.name}", e)
        }
    }
    
    /**
     * Calculates the delay until the next occurrence of a medication reminder
     */
    private fun getNextOccurrenceDelay(medication: Medication, fromDate: LocalDate, timeZone: ZoneId): Long {
        var nextDate = fromDate.plusDays(1) // Start checking from tomorrow
        
        // Find the next day this medication should be taken
        while (!medication.days.contains(nextDate.dayOfWeek)) {
            nextDate = nextDate.plusDays(1)
            
            // Safety check to prevent infinite loop (shouldn't happen with valid data)
            if (nextDate.isAfter(fromDate.plusDays(7))) {
                Log.e(TAG, "Could not find next occurrence within a week for medication: ${medication.name}")
                return -1L
            }
        }
        
        val nextDateTime = LocalDateTime.of(nextDate, medication.time).atZone(timeZone)
        val currentDateTime = ZonedDateTime.now(timeZone)
        
        return Duration.between(currentDateTime, nextDateTime).toMillis()
    }
    
    /**
     * Cancels a scheduled reminder for a medication
     */
    fun cancelMedicationReminder(medicationId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("medication_reminder_$medicationId")
        Log.d(TAG, "Cancelled reminder for medication ID: $medicationId")
    }
    
    /**
     * Cancels all scheduled medication reminders
     */
    fun cancelAllMedicationReminders() {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag("medication_reminder")
        Log.d(TAG, "Cancelled all medication reminders")
    }
    
    /**
     * Gets the next reminder time for a medication
     */
    private fun getNextReminderTime(medication: Medication): LocalDateTime {
        val today = LocalDate.now()
        val todayOfWeek = today.dayOfWeek
        
        // Find the next day this medication should be taken
        var nextDate = today.plusDays(1)
        var nextDayOfWeek = nextDate.dayOfWeek
        
        while (!medication.days.contains(nextDayOfWeek)) {
            nextDate = nextDate.plusDays(1)
            nextDayOfWeek = nextDate.dayOfWeek
        }
        
        return LocalDateTime.of(nextDate, medication.time)
    }
    
    /**
     * Schedules periodic work to check for missed medications
     */
    fun scheduleMissedMedicationCheck() {
        val workRequest = PeriodicWorkRequestBuilder<MissedMedicationWorker>(
            15, TimeUnit.MINUTES // Check every 15 minutes
        ).build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "missed_medication_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
    
    /**
     * Schedules daily scheduler to set up reminders for the next day
     */
    fun scheduleDailyReminderScheduler() {
        val workRequest = PeriodicWorkRequestBuilder<DailySchedulerWorker>(
            24, TimeUnit.HOURS // Run once per day
        ).build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "daily_reminder_scheduler",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
    
    /**
     * Schedules all recurring medication reminders for the week
     */
    suspend fun scheduleRecurringReminders() {
        try {
            // Use first() to get current data without ongoing collection to prevent memory leaks
            val medications = repository.getAllActiveMedications().first()
            medications.forEach { medication ->
                scheduleWeeklyReminders(medication)
            }
            Log.d(TAG, "Scheduled recurring reminders for ${medications.size} medications")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling recurring reminders", e)
        }
    }
    
    /**
     * Schedules reminders for a medication for the entire week
     */
    private suspend fun scheduleWeeklyReminders(medication: Medication) {
        try {
            val timeZone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(timeZone)
            val today = now.toLocalDate()
            
            // Schedule for the next 7 days
            for (dayOffset in 0..6) {
                val targetDate = today.plusDays(dayOffset.toLong())
                val targetDayOfWeek = targetDate.dayOfWeek
                
                if (medication.days.contains(targetDayOfWeek)) {
                    val targetDateTime = LocalDateTime.of(targetDate, medication.time).atZone(timeZone)
                    
                    // Only schedule if the time is in the future
                    if (targetDateTime.isAfter(now)) {
                        val delay = Duration.between(now, targetDateTime).toMillis()
                        
                        val workData = Data.Builder()
                            .putLong(MedicationReminderWorker.KEY_MEDICATION_ID, medication.id)
                            .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medication.name)
                            .putString(MedicationReminderWorker.KEY_MEDICATION_DOSAGE, medication.dosage)
                            .putString(MedicationReminderWorker.KEY_MEDICATION_INSTRUCTION, medication.instruction)
                            .build()
                        
                        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(workData)
                            .addTag("med_reminder_${medication.id}")
                            .build()
                        
                        // Use unique work name for each day
                        val workName = "med_reminder_${medication.id}_${targetDate}"
                        
                        WorkManager.getInstance(context)
                            .enqueueUniqueWork(
                                workName,
                                ExistingWorkPolicy.REPLACE,
                                workRequest
                            )
                        
                        Log.d(TAG, "Scheduled weekly reminder for ${medication.name} on $targetDate")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling weekly reminders for medication: ${medication.name}", e)
        }
    }
}

/**
 * Worker to check for missed medications and mark them accordingly
 */
@HiltWorker
class MissedMedicationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MedicationRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "MissedMedicationWorker"
        private const val MISSED_THRESHOLD_MINUTES = 30L // Consider missed after 30 minutes
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val pendingLogs = repository.getPendingLogs()
            val currentTime = System.currentTimeMillis()
            val missedThreshold = MISSED_THRESHOLD_MINUTES * 60 * 1000L // Convert to milliseconds
            
            pendingLogs.forEach { log ->
                val timeSinceScheduled = currentTime - log.scheduledTime
                if (timeSinceScheduled > missedThreshold) {
                    repository.markAsMissed(log.id)
                    Log.d(TAG, "Marked medication as missed: ${log.medicationId}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for missed medications", e)
            Result.failure()
        }
    }
}
