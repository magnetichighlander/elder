package com.eldercare.assistant.data.dao

import androidx.room.*
import com.eldercare.assistant.data.entity.MessageTemplate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MessageTemplateDao {
    
    @Query("SELECT * FROM message_templates ORDER BY category, title")
    fun getAllTemplates(): Flow<List<MessageTemplate>>
    
    @Query("SELECT * FROM message_templates WHERE category = :category ORDER BY use_count DESC, title")
    fun getTemplatesByCategory(category: String): Flow<List<MessageTemplate>>
    
    @Query("SELECT * FROM message_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): MessageTemplate?
    
    @Query("SELECT * FROM message_templates ORDER BY use_count DESC LIMIT :limit")
    fun getMostUsedTemplates(limit: Int = 10): Flow<List<MessageTemplate>>
    
    @Query("SELECT * FROM message_templates ORDER BY last_used DESC LIMIT :limit")
    fun getRecentlyUsedTemplates(limit: Int = 10): Flow<List<MessageTemplate>>
    
    @Query("SELECT DISTINCT category FROM message_templates ORDER BY category")
    fun getAllCategories(): Flow<List<String>>
    
    @Query("SELECT * FROM message_templates WHERE title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%' ORDER BY use_count DESC")
    fun searchTemplates(searchQuery: String): Flow<List<MessageTemplate>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MessageTemplate): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<MessageTemplate>): List<Long>
    
    @Update
    suspend fun updateTemplate(template: MessageTemplate)
    
    @Delete
    suspend fun deleteTemplate(template: MessageTemplate)
    
    @Query("DELETE FROM message_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)
    
    @Query("DELETE FROM message_templates WHERE category = :category")
    suspend fun deleteTemplatesByCategory(category: String)
    
    @Query("UPDATE message_templates SET use_count = use_count + 1, last_used = :lastUsed WHERE id = :id")
    suspend fun incrementUseCount(id: Long, lastUsed: LocalDateTime)
    
    @Query("UPDATE message_templates SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    
    @Query("SELECT * FROM message_templates WHERE is_favorite = 1 ORDER BY title")
    fun getFavoriteTemplates(): Flow<List<MessageTemplate>>
    
    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun getTemplateCount(): Int
    
    @Query("SELECT COUNT(*) FROM message_templates WHERE category = :category")
    suspend fun getTemplateCountByCategory(category: String): Int
}
