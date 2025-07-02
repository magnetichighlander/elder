package com.eldercare.assistant.data.dao

import androidx.room.*
import com.eldercare.assistant.data.entity.RecentContact
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface RecentContactDao {
    
    @Query("SELECT * FROM recent_contacts ORDER BY last_contacted DESC")
    fun getAllContacts(): Flow<List<RecentContact>>
    
    @Query("SELECT * FROM recent_contacts ORDER BY last_contacted DESC LIMIT :limit")
    fun getRecentContacts(limit: Int = 10): Flow<List<RecentContact>>
    
    @Query("SELECT * FROM recent_contacts ORDER BY contact_count DESC LIMIT :limit")
    fun getFrequentContacts(limit: Int = 10): Flow<List<RecentContact>>
    
    @Query("SELECT * FROM recent_contacts WHERE id = :id")
    suspend fun getContactById(id: Long): RecentContact?
    
    @Query("SELECT * FROM recent_contacts WHERE phone_number = :phoneNumber")
    suspend fun getContactByPhoneNumber(phoneNumber: String): RecentContact?
    
    @Query("SELECT * FROM recent_contacts WHERE name LIKE '%' || :searchQuery || '%' OR phone_number LIKE '%' || :searchQuery || '%' ORDER BY contact_count DESC")
    fun searchContacts(searchQuery: String): Flow<List<RecentContact>>
    
    @Query("SELECT * FROM recent_contacts WHERE is_favorite = 1 ORDER BY name")
    fun getFavoriteContacts(): Flow<List<RecentContact>>
    
    @Query("SELECT * FROM recent_contacts WHERE is_emergency_contact = 1 ORDER BY name")
    fun getEmergencyContacts(): Flow<List<RecentContact>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: RecentContact): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<RecentContact>): List<Long>
    
    @Update
    suspend fun updateContact(contact: RecentContact)
    
    @Delete
    suspend fun deleteContact(contact: RecentContact)
    
    @Query("DELETE FROM recent_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
    
    @Query("DELETE FROM recent_contacts WHERE last_contacted < :cutoffDate AND is_favorite = 0 AND is_emergency_contact = 0")
    suspend fun deleteOldContacts(cutoffDate: LocalDateTime)
    
    @Query("UPDATE recent_contacts SET contact_count = contact_count + 1, last_contacted = :lastContacted WHERE id = :id")
    suspend fun incrementContactCount(id: Long, lastContacted: LocalDateTime)
    
    @Query("UPDATE recent_contacts SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    
    @Query("UPDATE recent_contacts SET is_emergency_contact = :isEmergency WHERE id = :id")
    suspend fun updateEmergencyStatus(id: Long, isEmergency: Boolean)
    
    @Query("SELECT COUNT(*) FROM recent_contacts")
    suspend fun getContactCount(): Int
    
    @Query("SELECT COUNT(*) FROM recent_contacts WHERE is_favorite = 1")
    suspend fun getFavoriteContactCount(): Int
    
    @Query("SELECT COUNT(*) FROM recent_contacts WHERE is_emergency_contact = 1")
    suspend fun getEmergencyContactCount(): Int
    
    // Method to add or update contact when messaging
    @Transaction
    suspend fun addOrUpdateContact(name: String, phoneNumber: String, avatarPath: String? = null) {
        val existingContact = getContactByPhoneNumber(phoneNumber)
        val currentTime = LocalDateTime.now()
        
        if (existingContact != null) {
            incrementContactCount(existingContact.id, currentTime)
            // Update name and avatar if provided
            if (name != existingContact.name || avatarPath != existingContact.avatarPath) {
                updateContact(existingContact.copy(
                    name = name,
                    avatarPath = avatarPath ?: existingContact.avatarPath,
                    lastContacted = currentTime,
                    contactCount = existingContact.contactCount + 1
                ))
            }
        } else {
            insertContact(RecentContact(
                name = name,
                phoneNumber = phoneNumber,
                avatarPath = avatarPath,
                lastContacted = currentTime,
                contactCount = 1,
                isFavorite = false,
                isEmergencyContact = false
            ))
        }
    }
}
