package com.eldercare.assistant.data.repository

import com.eldercare.assistant.data.dao.FavoriteContactDao
import com.eldercare.assistant.data.entities.FavoriteContact
import kotlinx.coroutines.flow.Flow

class FavoriteContactRepository(private val favoriteContactDao: FavoriteContactDao) {
    
    fun getAllContacts(): Flow<List<FavoriteContact>> = favoriteContactDao.getAll()
    
    suspend fun insertContact(contact: FavoriteContact) {
        favoriteContactDao.insert(contact)
    }
    
    suspend fun deleteContact(contact: FavoriteContact) {
        favoriteContactDao.delete(contact)
    }
    
    suspend fun updateContact(contact: FavoriteContact) {
        favoriteContactDao.update(contact)
    }
    
    suspend fun getContactById(id: Int): FavoriteContact? {
        return favoriteContactDao.getById(id)
    }
    
    suspend fun getContactCount(): Int {
        return favoriteContactDao.getCount()
    }
    
    suspend fun deleteAllContacts() {
        favoriteContactDao.deleteAll()
    }
}
