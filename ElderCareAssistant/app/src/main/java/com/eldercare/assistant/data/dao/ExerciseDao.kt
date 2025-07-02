package com.eldercare.assistant.data.dao

import androidx.room.*
import com.eldercare.assistant.data.entity.Exercise
import com.eldercare.assistant.data.entity.Difficulty
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    
    @Query("SELECT * FROM exercises ORDER BY difficulty, title")
    fun getAllExercises(): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE difficulty = :difficulty ORDER BY title")
    fun getExercisesByDifficulty(difficulty: Difficulty): Flow<List<Exercise>>
    
    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Int): Exercise?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)
    
    @Update
    suspend fun updateExercise(exercise: Exercise)
    
    @Delete
    suspend fun deleteExercise(exercise: Exercise)
    
    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()
}
