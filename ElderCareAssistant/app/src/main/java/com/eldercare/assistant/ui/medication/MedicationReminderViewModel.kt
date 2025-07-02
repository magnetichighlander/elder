package com.eldercare.assistant.ui.medication

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import com.eldercare.assistant.data.entity.Medication
import com.eldercare.assistant.data.repository.MedicationRepository
import com.eldercare.assistant.service.MedicationNotificationService
import com.eldercare.assistant.service.MedicationReminderWorker
import com.eldercare.assistant.service.SnoozeReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for medication reminder activities
 */
@HiltViewModel
class MedicationReminderViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val notificationService: MedicationNotificationService,
    private val workManager: WorkManager
) : ViewModel() {
    
    private val _medication = MutableLiveData<Medication?>()
    val medication: LiveData<Medication?> = _medication
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    /**
     * Loads medication details by ID
     */
    fun loadMedication(medicationId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val med = repository.getMedicationById(medicationId)
                _medication.value = med
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error loading medication: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Confirms medication as taken
     */
    suspend fun confirmMedication(logId: Long) {
        try {
            repository.confirmMedication(logId)
            
            // Cancel notification
            _medication.value?.let { med ->
                notificationService.cancelMedicationReminder(med.id)
                notificationService.showMedicationConfirmed(med.name)
            }
            
        } catch (e: Exception) {
            _error.value = "Error confirming medication: ${e.message}"
            throw e
        }
    }
    
    /**
     * Snoozes medication reminder
     */
    suspend fun snoozeMedication(medicationId: Long, logId: Long, medicationName: String) {
        try {
            // Cancel current notification
            notificationService.cancelMedicationReminder(medicationId)
            
            // Schedule snooze reminder
            val snoozeData = Data.Builder()
                .putLong(MedicationReminderWorker.KEY_MEDICATION_ID, medicationId)
                .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medicationName)
                .putString(MedicationReminderWorker.KEY_MEDICATION_DOSAGE, "Snoozed reminder")
                .putString(MedicationReminderWorker.KEY_MEDICATION_INSTRUCTION, "")
                .build()
            
            val snoozeWorkRequest = OneTimeWorkRequestBuilder<SnoozeReminderWorker>()
                .setInitialDelay(10, TimeUnit.MINUTES) // 10 minute snooze
                .setInputData(snoozeData)
                .addTag("snooze_$medicationId")
                .build()
            
            workManager.enqueue(snoozeWorkRequest)
            
        } catch (e: Exception) {
            _error.value = "Error snoozing medication: ${e.message}"
            throw e
        }
    }
    
    /**
     * Clears any error messages
     */
    fun clearError() {
        _error.value = null
    }
}
