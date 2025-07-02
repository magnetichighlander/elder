package com.eldercare.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.eldercare.assistant.data.entities.FavoriteContact
import com.eldercare.assistant.data.repository.ContactsRepository
import com.eldercare.assistant.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactsRepository
    
    init {
        val dao = AppDatabase.getDatabase(application).favoriteContactDao()
        repository = ContactsRepository(dao)
    }
    
    val favoriteContacts: LiveData<List<FavoriteContact>> = repository.allContacts
    
    fun addFavoriteContact(contact: FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(contact)
        }
    }
    
    fun updateFavoriteContact(contact: FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(contact)
        }
    }
    
    fun deleteFavoriteContact(contact: FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(contact)
        }
    }
}
