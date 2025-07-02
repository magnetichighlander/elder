package com.eldercare.assistant.data.repository

import com.eldercare.assistant.data.dao.MessageTemplateDao
import com.eldercare.assistant.data.dao.RecentContactDao
import com.eldercare.assistant.data.entity.MessageTemplate
import com.eldercare.assistant.data.entity.RecentContact
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageTemplateRepository @Inject constructor(
    private val messageTemplateDao: MessageTemplateDao,
    private val recentContactDao: RecentContactDao
) {
    
    // Message Template operations
    fun getAllTemplates(): Flow<List<MessageTemplate>> = messageTemplateDao.getAllTemplates()
    
    fun getTemplatesByCategory(category: String): Flow<List<MessageTemplate>> = 
        messageTemplateDao.getTemplatesByCategory(category)
    
    fun getRecentlyUsedTemplates(): Flow<List<MessageTemplate>> = 
        messageTemplateDao.getRecentlyUsedTemplates()
    
    fun getFrequentlyUsedTemplates(): Flow<List<MessageTemplate>> = 
        messageTemplateDao.getFrequentlyUsedTemplates()
    
    suspend fun getTemplateById(id: Int): MessageTemplate? = 
        messageTemplateDao.getTemplateById(id)
    
    suspend fun insertTemplate(template: MessageTemplate): Long = 
        messageTemplateDao.insertTemplate(template)
    
    suspend fun updateTemplate(template: MessageTemplate) = 
        messageTemplateDao.updateTemplate(template)
    
    suspend fun deleteTemplate(template: MessageTemplate) = 
        messageTemplateDao.deleteTemplate(template)
    
    suspend fun markTemplateAsUsed(templateId: Int) {
        val template = messageTemplateDao.getTemplateById(templateId)
        template?.let {
            val updatedTemplate = it.copy(
                lastUsed = LocalDateTime.now(),
                usageCount = it.usageCount + 1
            )
            messageTemplateDao.updateTemplate(updatedTemplate)
        }
    }
    
    // Recent Contact operations
    fun getAllRecentContacts(): Flow<List<RecentContact>> = 
        recentContactDao.getAllRecentContacts()
    
    fun getRecentContactsByLimit(limit: Int): Flow<List<RecentContact>> = 
        recentContactDao.getRecentContactsByLimit(limit)
    
    suspend fun getRecentContactByNumber(phoneNumber: String): RecentContact? = 
        recentContactDao.getRecentContactByNumber(phoneNumber)
    
    suspend fun insertRecentContact(contact: RecentContact) = 
        recentContactDao.insertRecentContact(contact)
    
    suspend fun updateRecentContact(contact: RecentContact) = 
        recentContactDao.updateRecentContact(contact)
    
    suspend fun deleteRecentContact(contact: RecentContact) = 
        recentContactDao.deleteRecentContact(contact)
    
    suspend fun clearOldRecentContacts() = 
        recentContactDao.clearOldRecentContacts()
    
    suspend fun updateContactLastContacted(phoneNumber: String, name: String) {
        val existingContact = recentContactDao.getRecentContactByNumber(phoneNumber)
        val now = LocalDateTime.now()
        
        if (existingContact != null) {
            val updatedContact = existingContact.copy(
                lastContacted = now,
                contactCount = existingContact.contactCount + 1
            )
            recentContactDao.updateRecentContact(updatedContact)
        } else {
            val newContact = RecentContact(
                phoneNumber = phoneNumber,
                name = name,
                lastContacted = now,
                contactCount = 1
            )
            recentContactDao.insertRecentContact(newContact)
        }
    }
    
    // Combined operations for message sending
    suspend fun sendMessageWithTemplate(templateId: Int, phoneNumber: String, contactName: String) {
        markTemplateAsUsed(templateId)
        updateContactLastContacted(phoneNumber, contactName)
    }
    
    // Initialize default templates
    suspend fun initializeDefaultTemplates() {
        val defaultTemplates = listOf(
            MessageTemplate(
                id = 1,
                title = "Emergency Help",
                content = "I need help immediately. Please call me or come to my location.",
                category = "Emergency",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 2,
                title = "Feeling Unwell",
                content = "I'm not feeling well today. Could you please check on me?",
                category = "Health",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 3,
                title = "Medication Reminder",
                content = "Please remind me to take my medication at the scheduled time.",
                category = "Health",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 4,
                title = "Appointment Reminder",
                content = "I have a doctor's appointment tomorrow. Please remind me.",
                category = "Health",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 5,
                title = "Good Morning",
                content = "Good morning! Hope you have a wonderful day.",
                category = "Social",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 6,
                title = "Checking In",
                content = "Hi! Just wanted to check in and see how you're doing today.",
                category = "Social",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 7,
                title = "Thank You",
                content = "Thank you so much for your help and kindness.",
                category = "Social",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 8,
                title = "Running Late",
                content = "I'm running a bit late. I'll be there as soon as possible.",
                category = "General",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 9,
                title = "Call Me",
                content = "Please call me when you get a chance. Thanks!",
                category = "General",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            ),
            MessageTemplate(
                id = 10,
                title = "Safe Arrival",
                content = "I've arrived safely at my destination. Thank you for checking.",
                category = "General",
                isDefault = true,
                lastUsed = null,
                usageCount = 0
            )
        )
        
        // Only insert if no templates exist
        val existingTemplates = messageTemplateDao.getAllTemplatesSync()
        if (existingTemplates.isEmpty()) {
            messageTemplateDao.insertTemplates(defaultTemplates)
        }
    }
    
    // Statistics
    suspend fun getMessageStats(): MessageStats {
        val allTemplates = messageTemplateDao.getAllTemplatesSync()
        val totalTemplates = allTemplates.size
        val totalUsage = allTemplates.sumOf { it.usageCount }
        val mostUsedTemplate = allTemplates.maxByOrNull { it.usageCount }
        val recentContacts = recentContactDao.getAllRecentContactsSync()
        
        return MessageStats(
            totalTemplates = totalTemplates,
            totalUsage = totalUsage,
            mostUsedTemplate = mostUsedTemplate?.title ?: "None",
            recentContactsCount = recentContacts.size,
            averageUsagePerTemplate = if (totalTemplates > 0) totalUsage.toFloat() / totalTemplates else 0f
        )
    }
}

// Data class for message statistics
data class MessageStats(
    val totalTemplates: Int,
    val totalUsage: Int,
    val mostUsedTemplate: String,
    val recentContactsCount: Int,
    val averageUsagePerTemplate: Float
)
