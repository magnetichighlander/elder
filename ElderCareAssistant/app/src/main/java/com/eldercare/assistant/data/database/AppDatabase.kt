package com.eldercare.assistant.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.eldercare.assistant.data.dao.MedicationDao
import com.eldercare.assistant.data.dao.MedicationLogDao
import com.eldercare.assistant.data.dao.MoodDao
import com.eldercare.assistant.data.dao.ExerciseDao
import com.eldercare.assistant.data.dao.ExerciseProgressDao
import com.eldercare.assistant.data.dao.AchievementDao
import com.eldercare.assistant.data.entity.Medication
import com.eldercare.assistant.data.entity.MedicationLog
import com.eldercare.assistant.data.entity.MoodEntry
import com.eldercare.assistant.data.entity.Exercise
import com.eldercare.assistant.data.entity.ExerciseProgress
import com.eldercare.assistant.data.entity.Achievement
import com.eldercare.assistant.data.entity.UserAchievement
import com.eldercare.assistant.data.entity.LocalTimeConverter
import com.eldercare.assistant.data.entity.DaysConverter
import com.eldercare.assistant.data.entity.LocalDateConverter
import com.eldercare.assistant.data.entity.LocalDateTimeConverter

/**
 * Room database for Elder Care Assistant
 */
@Database(
entities = [Medication::class, MedicationLog::class, MoodEntry::class, Exercise::class, ExerciseProgress::class, Achievement::class, UserAchievement::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(LocalTimeConverter::class, DaysConverter::class, LocalDateConverter::class, LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun moodDao(): MoodDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseProgressDao(): ExerciseProgressDao
    abstract fun achievementDao(): AchievementDao
    
    companion object {
        const val DATABASE_NAME = "elder_care_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Migration from version 1 to version 2
         * Add proper migrations instead of destructive fallback
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - add reminder_sound column to medications table
                database.execSQL("ALTER TABLE medications ADD COLUMN reminder_sound TEXT")
                
                // Add any other schema changes for version 2
                // This preserves user data instead of deleting it
            }
        }
    }
}
