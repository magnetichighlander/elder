package com.eldercare.assistant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.eldercare.assistant.data.entities.FavoriteContact
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteContactDao {
    @Insert
    suspend fun insert(contact: FavoriteContact)

    @Delete
    suspend fun delete(contact: FavoriteContact)
    
    @Update
    suspend fun update(contact: FavoriteContact)

    @Query("SELECT * FROM favorite_contacts ORDER BY `order` ASC")
    fun getAll(): Flow<List<FavoriteContact>>
    
    @Query("SELECT * FROM favorite_contacts WHERE id = :id")
    suspend fun getById(id: Int): FavoriteContact?
    
    @Query("SELECT COUNT(*) FROM favorite_contacts")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM favorite_contacts")
    suspend fun deleteAll()
}
