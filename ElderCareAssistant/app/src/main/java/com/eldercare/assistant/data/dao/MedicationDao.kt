package com.eldercare.assistant.data.dao

import androidx.room.*
import com.eldercare.assistant.data.entity.Medication
import com.eldercare.assistant.data.entity.MedicationLog
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

/**
 * Data Access Object for medication operations
 */
@Dao
interface MedicationDao {
    
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY time ASC")
    fun getAllActiveMedications(): Flow<List<Medication>>
    
    @Query("SELECT * FROM medications ORDER BY time ASC")
    fun getAllMedications(): Flow<List<Medication>>
    
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): Medication?
    
    @Insert
    suspend fun insertMedication(medication: Medication): Long
    
    @Update
    suspend fun updateMedication(medication: Medication)
    
    @Delete
    suspend fun deleteMedication(medication: Medication)
    
    @Query("UPDATE medications SET isActive = :isActive WHERE id = :id")
    suspend fun updateMedicationActiveStatus(id: Long, isActive: Boolean)
    
    // Custom query to get medications for specific day
    @Query("SELECT * FROM medications WHERE isActive = 1 AND days LIKE :dayPattern ORDER BY time ASC")
    suspend fun getMedicationsForDay(dayPattern: String): List<Medication>
}

/**
 * Data Access Object for medication log operations
 */
@Dao
interface MedicationLogDao {
    
    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY scheduledTime DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<MedicationLog>>
    
    @Query("SELECT * FROM medication_logs WHERE scheduledTime BETWEEN :startTime AND :endTime ORDER BY scheduledTime DESC")
    fun getLogsForDateRange(startTime: Long, endTime: Long): Flow<List<MedicationLog>>
    
    @Query("SELECT * FROM medication_logs WHERE isConfirmed = 0 AND isMissed = 0 ORDER BY scheduledTime ASC")
    suspend fun getPendingLogs(): List<MedicationLog>
    
    @Insert
    suspend fun insertLog(log: MedicationLog): Long
    
    @Update
    suspend fun updateLog(log: MedicationLog)
    
    @Query("UPDATE medication_logs SET isConfirmed = 1, confirmedTime = :confirmedTime WHERE id = :logId")
    suspend fun confirmMedication(logId: Long, confirmedTime: Long)
    
    @Query("UPDATE medication_logs SET isMissed = 1 WHERE id = :logId")
    suspend fun markAsMissed(logId: Long)
    
    // Get adherence statistics
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN isConfirmed = 1 THEN 1 ELSE 0 END) as confirmed,
            SUM(CASE WHEN isMissed = 1 THEN 1 ELSE 0 END) as missed
        FROM medication_logs 
        WHERE medicationId = :medicationId 
        AND scheduledTime BETWEEN :startTime AND :endTime
    """)
    suspend fun getAdherenceStats(medicationId: Long, startTime: Long, endTime: Long): AdherenceStats
}

/**
 * Data class for adherence statistics
 */
data class AdherenceStats(
    val total: Int,
    val confirmed: Int,
    val missed: Int
) {
    val adherencePercentage: Float
        get() = if (total > 0) (confirmed.toFloat() / total.toFloat()) * 100f else 0f
}
