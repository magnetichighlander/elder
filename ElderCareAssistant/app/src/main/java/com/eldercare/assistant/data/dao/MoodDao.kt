package com.eldercare.assistant.data.dao

import androidx.room.*
import com.eldercare.assistant.data.entity.MoodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MoodDao {
    @Insert
    suspend fun insert(entry: MoodEntry)
    
    @Query("SELECT * FROM mood_entries WHERE date >= :startDate ORDER BY date DESC")
    fun getEntriesSince(startDate: LocalDate): Flow<List<MoodEntry>>
}
