package com.eldercare.assistant.data.dao

import androidx.room.*
import com.eldercare.assistant.data.entity.ExerciseProgress
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ExerciseProgressDao {
    
    @Query("SELECT * FROM exercise_progress WHERE exerciseId = :exerciseId")
    suspend fun getProgressByExerciseId(exerciseId: Int): ExerciseProgress?
    
    @Query("SELECT * FROM exercise_progress")
    fun getAllProgress(): Flow<List<ExerciseProgress>>
    
    @Query("SELECT * FROM exercise_progress WHERE completionCount > 0 ORDER BY lastCompleted DESC")
    fun getCompletedExercises(): Flow<List<ExerciseProgress>>
    
    @Query("SELECT COUNT(*) FROM exercise_progress WHERE completionCount > 0")
    suspend fun getTotalCompletedExercises(): Int
    
    @Query("SELECT SUM(completionCount) FROM exercise_progress")
    suspend fun getTotalExerciseCompletions(): Int
    
    @Query("""
        SELECT COUNT(DISTINCT DATE(lastCompleted)) 
        FROM exercise_progress 
        WHERE lastCompleted IS NOT NULL 
        AND lastCompleted >= :fromDate
    """)
    suspend fun getStreakDays(fromDate: LocalDateTime): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ExerciseProgress)
    
    @Update
    suspend fun updateProgress(progress: ExerciseProgress)
    
    @Delete
    suspend fun deleteProgress(progress: ExerciseProgress)
    
    @Query("DELETE FROM exercise_progress")
    suspend fun deleteAllProgress()
}
