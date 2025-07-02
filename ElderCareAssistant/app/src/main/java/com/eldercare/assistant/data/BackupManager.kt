package com.eldercare.assistant.data

import android.content.Context
import android.util.Log
import com.eldercare.assistant.data.database.AppDatabase
import com.eldercare.assistant.data.entities.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDate
import java.time.LocalDateTime

class BackupManager(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .setPrettyPrinting()
        .create()

    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_FILENAME = "eldercare_backup.json"
        private const val CURRENT_BACKUP_VERSION = 1
    }

    data class BackupData(
        val version: Int,
        val timestamp: String,
        val medications: List<Medication>,
        val contacts: List<EmergencyContact>,
        val moods: List<MoodEntry>,
        val exercises: List<Exercise>,
        val exerciseProgress: List<ExerciseProgress>,
        val achievements: List<Achievement>,
        val messageTemplates: List<MessageTemplate>,
        val recentContacts: List<RecentContact>,
        val settings: Map<String, Any>
    )

    suspend fun createBackup(): Boolean {
        Log.d(TAG, "=== Starting backup creation ===")
        
        return try {
            Log.d(TAG, "Fetching data from database...")
            
            // Fetch all data with logging
            val medications = database.medicationDao().getAllMedications().also {
                Log.d(TAG, "Fetched ${it.size} medications")
            }
            
            val contacts = database.emergencyContactDao().getAllContacts().also {
                Log.d(TAG, "Fetched ${it.size} emergency contacts")
            }
            
            val moods = database.moodDao().getAllMoods().also {
                Log.d(TAG, "Fetched ${it.size} mood entries")
            }
            
            val exercises = database.exerciseDao().getAllExercises().also {
                Log.d(TAG, "Fetched ${it.size} exercises")
            }
            
            val exerciseProgress = database.exerciseProgressDao().getAllProgress().also {
                Log.d(TAG, "Fetched ${it.size} exercise progress entries")
            }
            
            val achievements = database.achievementDao().getAllAchievements().also {
                Log.d(TAG, "Fetched ${it.size} achievements")
            }
            
            val messageTemplates = database.messageTemplateDao().getAllTemplates().also {
                Log.d(TAG, "Fetched ${it.size} message templates")
            }
            
            val recentContacts = database.recentContactDao().getAllRecentContacts().also {
                Log.d(TAG, "Fetched ${it.size} recent contacts")
            }

            // Get settings from SharedPreferences
            val sharedPrefs = context.getSharedPreferences("eldercare_settings", Context.MODE_PRIVATE)
            val settings = sharedPrefs.all.also {
                Log.d(TAG, "Fetched ${it.size} settings entries")
            }

            // Create backup data structure
            val backupData = BackupData(
                version = CURRENT_BACKUP_VERSION,
                timestamp = LocalDateTime.now().toString(),
                medications = medications,
                contacts = contacts,
                moods = moods,
                exercises = exercises,
                exerciseProgress = exerciseProgress,
                achievements = achievements,
                messageTemplates = messageTemplates,
                recentContacts = recentContacts,
                settings = settings
            )

            Log.d(TAG, "Backup data structure created with timestamp: ${backupData.timestamp}")

            // Convert to JSON
            Log.d(TAG, "Converting backup data to JSON...")
            val jsonString = gson.toJson(backupData)
            Log.d(TAG, "JSON conversion completed. Size: ${jsonString.length} characters")

            // Save to file
            val backupFile = File(context.filesDir, BACKUP_FILENAME)
            Log.d(TAG, "Backup file path: ${backupFile.absolutePath}")
            
            FileWriter(backupFile).use { writer ->
                writer.write(jsonString)
                Log.d(TAG, "JSON data written to file")
            }

            // Verify file creation and log details
            if (backupFile.exists()) {
                val fileSize = backupFile.length()
                val lastModified = backupFile.lastModified()
                Log.d(TAG, "✅ Backup file created successfully!")
                Log.d(TAG, "File size: $fileSize bytes")
                Log.d(TAG, "Last modified: ${java.util.Date(lastModified)}")
                Log.d(TAG, "File readable: ${backupFile.canRead()}")
                
                // Additional verification - try to read first few characters
                val firstChars = backupFile.readText().take(100)
                Log.d(TAG, "First 100 characters of backup: $firstChars")
                
                true
            } else {
                Log.e(TAG, "❌ Backup file was not created!")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Backup creation failed with exception", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(): Boolean {
        Log.d(TAG, "=== Starting backup restoration ===")
        
        return try {
            val backupFile = File(context.filesDir, BACKUP_FILENAME)
            Log.d(TAG, "Looking for backup file at: ${backupFile.absolutePath}")

            if (!backupFile.exists()) {
                Log.w(TAG, "❌ Backup file does not exist")
                return false
            }

            Log.d(TAG, "Backup file found. Size: ${backupFile.length()} bytes")
            Log.d(TAG, "File last modified: ${java.util.Date(backupFile.lastModified())}")

            // Read and parse JSON
            Log.d(TAG, "Reading backup file...")
            val jsonString = FileReader(backupFile).use { it.readText() }
            Log.d(TAG, "File read successfully. Content length: ${jsonString.length} characters")

            Log.d(TAG, "Parsing JSON...")
            val backupData = gson.fromJson(jsonString, BackupData::class.java)
            Log.d(TAG, "JSON parsed successfully")
            Log.d(TAG, "Backup version: ${backupData.version}")
            Log.d(TAG, "Backup timestamp: ${backupData.timestamp}")

            // Version check
            if (backupData.version > CURRENT_BACKUP_VERSION) {
                Log.w(TAG, "❌ Backup version ${backupData.version} is newer than supported version $CURRENT_BACKUP_VERSION")
                return false
            }

            // Log data counts from backup
            Log.d(TAG, "Backup contains:")
            Log.d(TAG, "  - ${backupData.medications.size} medications")
            Log.d(TAG, "  - ${backupData.contacts.size} emergency contacts") 
            Log.d(TAG, "  - ${backupData.moods.size} mood entries")
            Log.d(TAG, "  - ${backupData.exercises.size} exercises")
            Log.d(TAG, "  - ${backupData.exerciseProgress.size} exercise progress entries")
            Log.d(TAG, "  - ${backupData.achievements.size} achievements")
            Log.d(TAG, "  - ${backupData.messageTemplates.size} message templates")
            Log.d(TAG, "  - ${backupData.recentContacts.size} recent contacts")
            Log.d(TAG, "  - ${backupData.settings.size} settings")

            // Clear existing data
            Log.d(TAG, "Clearing existing database data...")
            clearAllData()
            Log.d(TAG, "Existing data cleared")

            // Restore data with detailed logging
            Log.d(TAG, "Restoring medications...")
            backupData.medications.forEach { 
                database.medicationDao().insertMedication(it)
            }
            Log.d(TAG, "✅ Medications restored: ${backupData.medications.size}")

            Log.d(TAG, "Restoring emergency contacts...")
            backupData.contacts.forEach { 
                database.emergencyContactDao().insertContact(it)
            }
            Log.d(TAG, "✅ Emergency contacts restored: ${backupData.contacts.size}")

            Log.d(TAG, "Restoring mood entries...")
            backupData.moods.forEach { 
                database.moodDao().insertMood(it)
            }
            Log.d(TAG, "✅ Mood entries restored: ${backupData.moods.size}")

            Log.d(TAG, "Restoring exercises...")
            backupData.exercises.forEach { 
                database.exerciseDao().insertExercise(it)
            }
            Log.d(TAG, "✅ Exercises restored: ${backupData.exercises.size}")

            Log.d(TAG, "Restoring exercise progress...")
            backupData.exerciseProgress.forEach { 
                database.exerciseProgressDao().insertProgress(it)
            }
            Log.d(TAG, "✅ Exercise progress restored: ${backupData.exerciseProgress.size}")

            Log.d(TAG, "Restoring achievements...")
            backupData.achievements.forEach { 
                database.achievementDao().insertAchievement(it)
            }
            Log.d(TAG, "✅ Achievements restored: ${backupData.achievements.size}")

            Log.d(TAG, "Restoring message templates...")
            backupData.messageTemplates.forEach { 
                database.messageTemplateDao().insertTemplate(it)
            }
            Log.d(TAG, "✅ Message templates restored: ${backupData.messageTemplates.size}")

            Log.d(TAG, "Restoring recent contacts...")
            backupData.recentContacts.forEach { 
                database.recentContactDao().insertRecentContact(it)
            }
            Log.d(TAG, "✅ Recent contacts restored: ${backupData.recentContacts.size}")

            // Restore settings
            Log.d(TAG, "Restoring settings...")
            val sharedPrefs = context.getSharedPreferences("eldercare_settings", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()
            var settingsCount = 0
            backupData.settings.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Long -> editor.putLong(key, value)
                    else -> Log.w(TAG, "Unknown setting type for key $key: ${value?.javaClass}")
                }
                settingsCount++
            }
            editor.apply()
            Log.d(TAG, "✅ Settings restored: $settingsCount")

            // Verify restoration by checking database counts
            Log.d(TAG, "Verifying restoration...")
            val verificationCounts = mapOf(
                "medications" to database.medicationDao().getAllMedications().size,
                "contacts" to database.emergencyContactDao().getAllContacts().size,
                "moods" to database.moodDao().getAllMoods().size,
                "exercises" to database.exerciseDao().getAllExercises().size,
                "progress" to database.exerciseProgressDao().getAllProgress().size,
                "achievements" to database.achievementDao().getAllAchievements().size,
                "templates" to database.messageTemplateDao().getAllTemplates().size,
                "recent_contacts" to database.recentContactDao().getAllRecentContacts().size
            )

            Log.d(TAG, "Post-restoration database counts:")
            verificationCounts.forEach { (type, count) ->
                Log.d(TAG, "  $type: $count")
            }

            Log.d(TAG, "✅ Backup restoration completed successfully!")
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Backup restoration failed with exception", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private suspend fun clearAllData() {
        Log.d(TAG, "Clearing all database tables...")
        try {
            database.medicationDao().deleteAllMedications().also {
                Log.d(TAG, "Cleared medications table")
            }
            database.emergencyContactDao().deleteAllContacts().also {
                Log.d(TAG, "Cleared emergency contacts table")
            }
            database.moodDao().deleteAllMoods().also {
                Log.d(TAG, "Cleared moods table")
            }
            database.exerciseDao().deleteAllExercises().also {
                Log.d(TAG, "Cleared exercises table")
            }
            database.exerciseProgressDao().deleteAllProgress().also {
                Log.d(TAG, "Cleared exercise progress table")
            }
            database.achievementDao().deleteAllAchievements().also {
                Log.d(TAG, "Cleared achievements table")
            }
            database.messageTemplateDao().deleteAllTemplates().also {
                Log.d(TAG, "Cleared message templates table")
            }
            database.recentContactDao().deleteAllRecentContacts().also {
                Log.d(TAG, "Cleared recent contacts table")
            }
            Log.d(TAG, "All database tables cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing database tables", e)
            throw e
        }
    }

    fun backupFileExists(): Boolean {
        val backupFile = File(context.filesDir, BACKUP_FILENAME)
        val exists = backupFile.exists()
        Log.d(TAG, "Backup file exists check: $exists")
        if (exists) {
            Log.d(TAG, "Backup file details:")
            Log.d(TAG, "  Path: ${backupFile.absolutePath}")
            Log.d(TAG, "  Size: ${backupFile.length()} bytes")
            Log.d(TAG, "  Last modified: ${java.util.Date(backupFile.lastModified())}")
        }
        return exists
    }

    fun getBackupInfo(): BackupInfo? {
        Log.d(TAG, "Getting backup info...")
        val backupFile = File(context.filesDir, BACKUP_FILENAME)
        
        if (!backupFile.exists()) {
            Log.d(TAG, "No backup file found for info retrieval")
            return null
        }

        return try {
            val jsonString = FileReader(backupFile).use { it.readText() }
            val backupData = gson.fromJson(jsonString, BackupData::class.java)
            
            val info = BackupInfo(
                timestamp = backupData.timestamp,
                version = backupData.version,
                fileSizeBytes = backupFile.length()
            )
            
            Log.d(TAG, "Backup info retrieved: $info")
            info
        } catch (e: Exception) {
            Log.e(TAG, "Error reading backup info", e)
            null
        }
    }

    data class BackupInfo(
        val timestamp: String,
        val version: Int,
        val fileSizeBytes: Long
    )
}
