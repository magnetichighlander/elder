package com.eldercare.assistant.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.work.WorkManager
import com.eldercare.assistant.data.database.AppDatabase
import com.eldercare.assistant.data.dao.MedicationDao
import com.eldercare.assistant.data.dao.MedicationLogDao
import com.eldercare.assistant.data.dao.MoodDao
import com.eldercare.assistant.data.dao.ExerciseDao
import com.eldercare.assistant.data.dao.ExerciseProgressDao
import com.eldercare.assistant.data.dao.AchievementDao
import com.eldercare.assistant.utils.AccessibilityConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for app-wide dependencies
 * This module provides dependencies that are needed across the entire application
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides shared preferences for storing user settings
     * Uses the default shared preferences for the application
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Provides application context
     * This can be useful for cases where you need context in non-Activity classes
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    /**
     * Provides accessibility configuration for elderly-friendly settings
     */
    @Provides
    @Singleton
    fun provideAccessibilityConfig(@ApplicationContext context: Context): AccessibilityConfig {
        return AccessibilityConfig(context)
    }
    
    /**
     * Provides Room database for medication data
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    /**
     * Provides DAO for medication operations
     */
    @Provides
    fun provideMedicationDao(database: AppDatabase): MedicationDao {
        return database.medicationDao()
    }
    
    /**
     * Provides DAO for medication log operations
     */
    @Provides
    fun provideMedicationLogDao(database: AppDatabase): MedicationLogDao {
        return database.medicationLogDao()
    }
    
    /**
     * Provides DAO for mood tracking operations
     */
    @Provides
    fun provideMoodDao(database: AppDatabase): MoodDao {
        return database.moodDao()
    }
    
    /**
     * Provides DAO for exercise operations
     */
    @Provides
    fun provideExerciseDao(database: AppDatabase): ExerciseDao {
        return database.exerciseDao()
    }
    
    /**
     * Provides DAO for exercise progress operations
     */
    @Provides
    fun provideExerciseProgressDao(database: AppDatabase): ExerciseProgressDao {
        return database.exerciseProgressDao()
    }
    
    /**
     * Provides DAO for achievement operations
     */
    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao {
        return database.achievementDao()
    }
    
    /**
     * Provides WorkManager for background task scheduling
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}
