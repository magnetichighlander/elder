package com.eldercare.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.eldercare.assistant.data.database.AppDatabase
import com.eldercare.assistant.data.entities.FavoriteContact
import com.eldercare.assistant.data.repository.FavoriteContactRepository
import kotlinx.coroutines.launch

class FavoriteContactViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: FavoriteContactRepository
    val allContacts: LiveData<List<FavoriteContact>>
    
    init {
        val database = AppDatabase.getDatabase(application)
        val favoriteContactDao = database.favoriteContactDao()
        repository = FavoriteContactRepository(favoriteContactDao)
        allContacts = repository.getAllContacts().asLiveData()
    }
    
    fun insertContact(contact: FavoriteContact) = viewModelScope.launch {
        repository.insertContact(contact)
    }
    
    fun deleteContact(contact: FavoriteContact) = viewModelScope.launch {
        repository.deleteContact(contact)
    }
    
    fun updateContact(contact: FavoriteContact) = viewModelScope.launch {
        repository.updateContact(contact)
    }
    
    suspend fun getContactById(id: Int): FavoriteContact? {
        return repository.getContactById(id)
    }
    
    suspend fun getContactCount(): Int {
        return repository.getContactCount()
    }
    
    fun deleteAllContacts() = viewModelScope.launch {
        repository.deleteAllContacts()
    }
}
