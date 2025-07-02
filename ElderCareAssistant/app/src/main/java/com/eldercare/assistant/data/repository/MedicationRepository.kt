package com.eldercare.assistant.data.repository

import com.eldercare.assistant.data.dao.MedicationDao
import com.eldercare.assistant.data.dao.MedicationLogDao
import com.eldercare.assistant.data.dao.AdherenceStats
import com.eldercare.assistant.data.entity.Medication
import com.eldercare.assistant.data.entity.MedicationLog
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for handling medication data operations
 */
@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao
) {
    
    // Medication operations
    fun getAllActiveMedications(): Flow<List<Medication>> = medicationDao.getAllActiveMedications()
    
    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAllMedications()
    
    suspend fun getMedicationById(id: Long): Medication? = medicationDao.getMedicationById(id)
    
    suspend fun insertMedication(medication: Medication): Long = medicationDao.insertMedication(medication)
    
    suspend fun updateMedication(medication: Medication) = medicationDao.updateMedication(medication)
    
    suspend fun deleteMedication(medication: Medication) = medicationDao.deleteMedication(medication)
    
    suspend fun updateMedicationActiveStatus(id: Long, isActive: Boolean) = 
        medicationDao.updateMedicationActiveStatus(id, isActive)
    
    suspend fun getMedicationsForDay(dayOfWeek: DayOfWeek): List<Medication> {
        val dayPattern = "%${dayOfWeek.name}%"
        return medicationDao.getMedicationsForDay(dayPattern)
    }
    
    suspend fun getTodaysMedications(): List<Medication> {
        val today = LocalDate.now().dayOfWeek
        return getMedicationsForDay(today)
    }
    
    // Medication log operations
    fun getLogsForMedication(medicationId: Long): Flow<List<MedicationLog>> = 
        medicationLogDao.getLogsForMedication(medicationId)
    
    fun getLogsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MedicationLog>> {
        val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val endTime = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        return medicationLogDao.getLogsForDateRange(startTime, endTime)
    }
    
    suspend fun getPendingLogs(): List<MedicationLog> = medicationLogDao.getPendingLogs()
    
    suspend fun insertLog(log: MedicationLog): Long = medicationLogDao.insertLog(log)
    
    suspend fun confirmMedication(logId: Long, confirmedTime: Long = System.currentTimeMillis()) = 
        medicationLogDao.confirmMedication(logId, confirmedTime)
    
    suspend fun markAsMissed(logId: Long) = medicationLogDao.markAsMissed(logId)
    
    // Statistics
    suspend fun getAdherenceStats(medicationId: Long, startDate: LocalDate, endDate: LocalDate): AdherenceStats {
        val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val endTime = endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
        return medicationLogDao.getAdherenceStats(medicationId, startTime, endTime)
    }
    
    suspend fun getWeeklyAdherenceStats(medicationId: Long): AdherenceStats {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(7)
        return getAdherenceStats(medicationId, startDate, endDate)
    }
    
    suspend fun getMonthlyAdherenceStats(medicationId: Long): AdherenceStats {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(30)
        return getAdherenceStats(medicationId, startDate, endDate)
    }
}
