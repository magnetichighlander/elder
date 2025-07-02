package com.eldercare.assistant.utils

import android.content.Context
import com.eldercare.assistant.data.database.AppDatabase
import com.eldercare.assistant.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object DemoDataPopulator {
    
    suspend fun populateAll(context: Context) {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            
            // Populate all data types if they are empty
            populateMedications(database)
            populateFavoriteContacts(database)
            populateMoodEntries(database)
            populateExercises(database)
            populateExerciseProgress(database)
            populateMessageTemplates(database)
            populateAchievements(database)
        }
    }

    private suspend fun populateMedications(database: AppDatabase) {
        val dao = database.medicationDao()
        if (dao.getMedicationCount() == 0) {
            val medications = listOf(
                Medication(
                    name = "Aspirin",
                    dosage = "1 tablet",
                    instruction = "Take with food",
                    time = LocalTime.of(8, 0),
                    days = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                ),
                Medication(
                    name = "Metformin",
                    dosage = "500mg",
                    instruction = "Before breakfast",
                    time = LocalTime.of(7, 30),
                    days = DayOfWeek.values().toList() // Daily
                ),
                Medication(
                    name = "Lisinopril",
                    dosage = "10mg",
                    instruction = "Take in the morning",
                    time = LocalTime.of(9, 0),
                    days = DayOfWeek.values().toList()
                )
            )
            medications.forEach { dao.insertMedication(it) }
        }
    }

    private suspend fun populateFavoriteContacts(database: AppDatabase) {
        val dao = database.emergencyContactDao()
        if (dao.getContactCount() == 0) {
            val contacts = listOf(
                EmergencyContact(
                    name = "Doctor Smith",
                    phone = "+1234567890",
                    photoUri = null,
                    isEmergency = true
                ),
                EmergencyContact(
                    name = "Jane Doe",
                    phone = "+0987654321",
                    photoUri = null,
                    isEmergency = false
                ),
                EmergencyContact(
                    name = "Local Pharmacy",
                    phone = "+1122334455",
                    photoUri = null,
                    isEmergency = false
                )
            )
            contacts.forEach { dao.insertContact(it) }
        }
    }

    private suspend fun populateMoodEntries(database: AppDatabase) {
        val dao = database.moodDao()
        if (dao.getMoodCount() == 0) {
            val entries = listOf(
                MoodEntry(mood = "Happy", date = LocalDate.now().minusDays(2), notes = "Feeling great today!"),
                MoodEntry(mood = "Calm", date = LocalDate.now().minusDays(1), notes = "Relaxing afternoon"),
                MoodEntry(mood = "Sad", date = LocalDate.now(), notes = "Feeling a bit down")
            )
            entries.forEach { dao.insertMood(it) }
        }
    }

    private suspend fun populateExercises(database: AppDatabase) {
        val dao = database.exerciseDao()
        if (dao.getExerciseCount() == 0) {
            val exercises = listOf(
                Exercise(1, "Neck Stretch", "Gentle neck stretching", 30, "neck_stretch.json", Difficulty.BEGINNER),
                Exercise(2, "Shoulder Rolls", "Improve shoulder mobility", 45, "shoulder_rolls.json", Difficulty.BEGINNER),
                Exercise(3, "Seated Twist", "Gentle spinal twist", 90, "seated_twist.json", Difficulty.INTERMEDIATE)
            )
            dao.insertExercises(exercises)
        }
    }

    private suspend fun populateExerciseProgress(database: AppDatabase) {
        val dao = database.exerciseProgressDao()
        if (dao.getTotalCompletedExercises() == 0) {
            val progress = listOf(
                ExerciseProgress(exerciseId = 1, lastCompleted = LocalDateTime.now().minusDays(1), completionCount = 5, bestDuration = 28),
                ExerciseProgress(exerciseId = 2, lastCompleted = LocalDateTime.now(), completionCount = 3, bestDuration = 42)
            )
            progress.forEach { dao.insertProgress(it) }
        }
    }

    private suspend fun populateMessageTemplates(database: AppDatabase) {
        val dao = database.messageTemplateDao()
        if (dao.getTemplateCount() == 0) {
            val templates = listOf(
                MessageTemplate(title = "Birthday Wishes", text = "Happy birthday!", category = "Greetings"),
                MessageTemplate(title = "Get Well Soon", text = "Wishing you a speedy recovery!", category = "Greetings"),
                MessageTemplate(title = "Thank You", text = "Thanks for your help!", category = "Gratitude"),
                MessageTemplate(title = "Running Late", text = "I'm running a bit late.", category = "Practical")
            )
            dao.insertTemplates(templates)
        }
    }
    
    private suspend fun populateAchievements(database: AppDatabase) {
        val dao = database.achievementDao()
        if (dao.getAchievementCount() == 0) {
            val achievements = listOf(
                Achievement(1, "First Steps", "Complete your first exercise", R.drawable.ic_trophy, AchievementCondition.COMPLETE_5_EXERCISES),
                Achievement(2, "Getting Started", "Complete 5 exercises", R.drawable.ic_trophy, AchievementCondition.COMPLETE_5_EXERCISES),
                Achievement(3, "Dedicated", "Complete 10 exercises", R.drawable.ic_trophy, AchievementCondition.COMPLETE_10_EXERCISES),
                Achievement(4, "Three Day Streak", "Exercise for 3 consecutive days", R.drawable.ic_trophy, AchievementCondition.STREAK_3_DAYS),
                Achievement(5, "Week Warrior", "Exercise for 7 consecutive days", R.drawable.ic_trophy, AchievementCondition.STREAK_7_DAYS)
            )
            dao.insertAchievements(achievements)
        }
    }
}
