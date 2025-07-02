package com.eldercare.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eldercare.assistant.data.database.AppDatabase
import com.eldercare.assistant.data.entity.MoodEntry
import com.eldercare.assistant.data.entity.MoodType
import com.eldercare.assistant.data.repository.MoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for mood tracking functionality
 */
class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MoodRepository
    
    init {
        val dao = AppDatabase.getDatabase(application).moodDao()
        repository = MoodRepository(dao)
    }
    
    val lastWeekEntries: LiveData<List<MoodEntry>> = repository.getLastWeekEntries()
    
    fun insertEntry(entry: MoodEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(entry)
        }
    }
}
