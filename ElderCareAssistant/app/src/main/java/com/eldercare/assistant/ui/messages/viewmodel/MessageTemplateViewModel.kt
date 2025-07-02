package com.eldercare.assistant.ui.messages.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eldercare.assistant.data.entity.MessageTemplate
import com.eldercare.assistant.data.entity.RecentContact
import com.eldercare.assistant.data.repository.MessageTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageTemplateViewModel @Inject constructor(
    private val repository: MessageTemplateRepository
) : ViewModel() {

    private val _templates = MutableLiveData<List<MessageTemplate>>()
    val templates: LiveData<List<MessageTemplate>> = _templates

    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories

    private val _recentContacts = MutableLiveData<List<RecentContact>>()
    val recentContacts: LiveData<List<RecentContact>> = _recentContacts

    private val _selectedTemplate = MutableLiveData<MessageTemplate?>()
    val selectedTemplate: LiveData<MessageTemplate?> = _selectedTemplate

    private val _selectedContact = MutableLiveData<RecentContact?>()
    val selectedContact: LiveData<RecentContact?> = _selectedContact

    init {
        // Load initial data
        loadCategories()
        loadRecentContacts()
        // Load a default category of templates
        loadTemplatesByCategory("All") 
    }

    fun loadTemplatesByCategory(category: String) {
        viewModelScope.launch {
            val templateFlow = if (category == "All") {
                repository.getAllTemplates()
            } else {
                repository.getTemplatesByCategory(category)
            }
            
            templateFlow.collect { templateList ->
                _templates.value = templateList
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            // This assumes your MessageTemplateDao has a method for getting categories.
            // If not, this can be implemented in the repository.
            val hardcodedCategories = listOf("All", "Emergency", "Health", "Social", "General")
            _categories.value = hardcodedCategories
        }
    }

    fun loadRecentContacts(limit: Int = 10) {
        viewModelScope.launch {
            repository.getRecentContactsByLimit(limit).collect { contacts ->
                _recentContacts.value = contacts
            }
        }
    }

    fun addTemplate(title: String, text: String, category: String) {
        viewModelScope.launch {
            val newTemplate = MessageTemplate(title = title, text = text, category = category)
            repository.insertTemplate(newTemplate)
            // Refresh the list for the current category
            loadTemplatesByCategory(category)
        }
    }

    fun updateTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            repository.updateTemplate(template)
        }
    }

    fun deleteTemplate(template: MessageTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    fun onTemplateSelected(template: MessageTemplate) {
        _selectedTemplate.value = template
    }

    fun onContactSelected(contact: RecentContact) {
        _selectedContact.value = contact
    }

    fun markTemplateAsUsed(templateId: Int) {
        viewModelScope.launch {
            repository.markTemplateAsUsed(templateId)
        }
    }
    
    fun updateRecentContact(contact: RecentContact) {
        viewModelScope.launch {
            repository.updateRecentContact(contact)
        }
    }

    fun clearSelections() {
        _selectedTemplate.value = null
        _selectedContact.value = null
    }
}
