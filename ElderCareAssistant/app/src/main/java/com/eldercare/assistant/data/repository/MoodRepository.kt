package com.eldercare.assistant.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.eldercare.assistant.data.dao.MoodDao
import com.eldercare.assistant.data.entity.MoodEntry
import java.time.LocalDate

class MoodRepository(private val dao: MoodDao) {
    fun getLastWeekEntries(): LiveData<List<MoodEntry>> {
        val startDate = LocalDate.now().minusDays(7)
        return dao.getEntriesSince(startDate).asLiveData()
    }
    
    suspend fun insert(entry: MoodEntry) {
        dao.insert(entry)
    }
}

