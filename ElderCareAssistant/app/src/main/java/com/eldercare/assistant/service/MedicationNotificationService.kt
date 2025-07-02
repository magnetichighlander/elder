package com.eldercare.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eldercare.assistant.R
import com.eldercare.assistant.ui.medication.MedicationReminderActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling medication reminder notifications
 */
@Singleton
class MedicationNotificationService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val CHANNEL_ID = "medication_reminders"
        private const val CHANNEL_NAME = "Medication Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for medication reminders"
        private const val NOTIFICATION_ID_BASE = 1000
        
        // Actions
        const val ACTION_CONFIRM_MEDICATION = "confirm_medication"
        const val ACTION_SNOOZE_MEDICATION = "snooze_medication"
        const val ACTION_VOICE_CONFIRM = "voice_confirm_medication"
        
        // Extra keys
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_LOG_ID = "log_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(
                    0, 1000, 500, 1000, 500, 1000 // Extended vibration for elderly users
                )
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Shows a medication reminder notification
     */
    fun showMedicationReminder(
        medicationId: Long,
        logId: Long,
        medicationName: String,
        dosage: String,
        instruction: String
    ) {
        val notificationId = (NOTIFICATION_ID_BASE + medicationId).toInt()
        
        // Create intent for opening the medication reminder activity
        val openIntent = Intent(context, MedicationReminderActivity::class.java).apply {
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_LOG_ID, logId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create confirm action
        val confirmIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_MEDICATION
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_LOG_ID, logId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create snooze action
        val snoozeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_MEDICATION
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_LOG_ID, logId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create voice confirm action
        val voiceIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_VOICE_CONFIRM
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_LOG_ID, logId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        
        val voicePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification content
        val title = "💊 Time for $medicationName"
        val content = buildString {
            append("Take $dosage")
            if (instruction.isNotBlank()) {
                append(" • $instruction")
            }
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$content\n\nTap to open, or use the buttons below to confirm or snooze.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Don't auto-cancel for elderly users
            .setOngoing(true) // Keep visible until action is taken
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, true) // Show full screen for elderly users
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .addAction(
                R.drawable.ic_check, // You'll need to add this icon
                "✓ TAKEN",
                confirmPendingIntent
            )
            .addAction(
                R.drawable.ic_snooze, // You'll need to add this icon
                "⏰ SNOOZE",
                snoozePendingIntent
            )
            .addAction(
                R.drawable.ic_voice, // You'll need to add this icon
                "🎤 VOICE",
                voicePendingIntent
            )
            .setColor(Color.parseColor("#4CAF50")) // Green color for health
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Handle permission issues on newer Android versions
            android.util.Log.e("MedicationNotificationService", "Permission denied for notification", e)
        }
    }
    
    /**
     * Cancels a medication reminder notification
     */
    fun cancelMedicationReminder(medicationId: Long) {
        val notificationId = (NOTIFICATION_ID_BASE + medicationId).toInt()
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Shows a confirmation notification when medication is taken
     */
    fun showMedicationConfirmed(medicationName: String) {
        val notificationId = NOTIFICATION_ID_BASE + 9999 // Use a different ID for confirmations
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle("✓ Medication Confirmed")
            .setContentText("$medicationName has been marked as taken")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds
            .setColor(Color.parseColor("#4CAF50"))
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("MedicationNotificationService", "Permission denied for confirmation notification", e)
        }
    }
    
    /**
     * Creates a pending intent for medication confirmation
     * Helper method for creating quick action buttons
     */
    private fun createConfirmationPendingIntent(medicationId: Long, logId: Long): PendingIntent {
        val confirmIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_MEDICATION
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_LOG_ID, logId)
        }
        
        return PendingIntent.getBroadcast(
            context,
            (medicationId * 100 + 1).toInt(), // Unique request code
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
