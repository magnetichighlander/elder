package com.eldercare.assistant.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.eldercare.assistant.data.dao.FavoriteContactDao
import com.eldercare.assistant.data.entities.FavoriteContact

class ContactsRepository(private val contactDao: FavoriteContactDao) {
    val allContacts: LiveData<List<FavoriteContact>> = contactDao.getAll().asLiveData()
    
    suspend fun insert(contact: FavoriteContact) {
        contactDao.insert(contact)
    }
    
    suspend fun update(contact: FavoriteContact) {
        // For Room's update to work, add @Update annotation to DAO
        contactDao.update(contact)
    }
    
    suspend fun delete(contact: FavoriteContact) {
        contactDao.delete(contact)
    }
}
