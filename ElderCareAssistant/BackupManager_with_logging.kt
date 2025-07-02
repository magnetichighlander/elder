package com.eldercare.assistant.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.room.withTransaction
import com.eldercare.assistant.data.database.AppDatabase
import com.eldercare.assistant.data.entities.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.security.KeyStore
import java.time.LocalDate
import java.time.LocalDateTime
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BackupManager(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .setPrettyPrinting()
        .create()

    companion object {
        private const val TAG = "BackupManager"
        private const val BACKUP_FILENAME = "eldercare_backup.enc"
        private const val CURRENT_BACKUP_VERSION = 1
        private const val KEY_ALIAS = "eldercare_backup_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
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

            // Encrypt and save to file
            val backupFile = File(context.filesDir, BACKUP_FILENAME)
            Log.d(TAG, "Backup file path: ${backupFile.absolutePath}")
            
            val encryptedData = encryptData(jsonString)
            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(encryptedData)
                Log.d(TAG, "Encrypted data written to file")
            }

            // Verify file creation and log details
            if (backupFile.exists()) {
                val fileSize = backupFile.length()
                val lastModified = backupFile.lastModified()
                Log.d(TAG, "✅ Backup file created successfully!")
                Log.d(TAG, "File size: $fileSize bytes")
                Log.d(TAG, "Last modified: ${java.util.Date(lastModified)}")
                Log.d(TAG, "File readable: ${backupFile.canRead()}")
                
                // Verification for encrypted file
                Log.d(TAG, "Backup encrypted and saved successfully")
                
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

            // Read, decrypt and parse JSON
            Log.d(TAG, "Reading encrypted backup file...")
            val encryptedData = FileInputStream(backupFile).use { it.readBytes() }
            Log.d(TAG, "Encrypted file read successfully. Size: ${encryptedData.size} bytes")

            Log.d(TAG, "Decrypting backup data...")
            val jsonString = decryptData(encryptedData)
            Log.d(TAG, "File decrypted successfully. Content length: ${jsonString.length} characters")

            Log.d(TAG, "Parsing JSON...")
            val backupData = parseAndValidateBackup(jsonString)
                ?: return false
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

            // Use database transaction for atomic restoration
            Log.d(TAG, "Starting atomic backup restoration...")
            database.withTransaction {
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
            }

            // Restore settings outside of database transaction
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
            val encryptedData = FileInputStream(backupFile).use { it.readBytes() }
            val jsonString = decryptData(encryptedData)
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

    /**
     * Encrypts data using Android Keystore
     */
    private fun encryptData(data: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedData = cipher.doFinal(data.toByteArray())
        val iv = cipher.iv
        
        // Combine IV and encrypted data
        return iv + encryptedData
    }
    
    /**
     * Decrypts data using Android Keystore
     */
    private fun decryptData(encryptedDataWithIv: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        
        // Extract IV and encrypted data
        val iv = encryptedDataWithIv.sliceArray(0..GCM_IV_LENGTH - 1)
        val encryptedData = encryptedDataWithIv.sliceArray(GCM_IV_LENGTH until encryptedDataWithIv.size)
        
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        
        val decryptedData = cipher.doFinal(encryptedData)
        return String(decryptedData)
    }
    
    /**
     * Gets or creates a secret key in Android Keystore
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, 
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    /**
     * Parses and validates backup data
     */
    private fun parseAndValidateBackup(jsonString: String): BackupData? {
        return try {
            val backupData = gson.fromJson(jsonString, BackupData::class.java)
            // Add validation logic here
            if (backupData.version > CURRENT_BACKUP_VERSION) {
                Log.w(TAG, "Backup version ${backupData.version} is newer than supported version $CURRENT_BACKUP_VERSION")
                return null
            }
            backupData
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing backup data", e)
            null
        }
    }

    data class BackupInfo(
        val timestamp: String,
        val version: Int,
        val fileSizeBytes: Long
    )
}
