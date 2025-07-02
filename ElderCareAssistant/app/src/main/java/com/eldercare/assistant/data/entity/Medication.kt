package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Medication entity for storing medication reminders
 */
@Entity(tableName = "medications")
@TypeConverters(LocalTimeConverter::class, DaysConverter::class)
data class Medication(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val name: String,           // Medication name
    val dosage: String,         // e.g., "1 tablet"
    val instruction: String,    // e.g., "after meal"
    val time: LocalTime,        // Time of day
    val days: List<DayOfWeek>,  // Days of week (e.g., [MONDAY, WEDNESDAY])
    val isActive: Boolean = true // To enable/disable reminder
)

/**
 * Medication log entry for tracking confirmation
 */
@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val scheduledTime: Long,    // Timestamp when reminder was scheduled
    val confirmedTime: Long?,   // Timestamp when user confirmed taking medication
    val isConfirmed: Boolean = false,
    val isMissed: Boolean = false,
    val notes: String? = null
)

/**
 * Type converter for LocalTime
 */
class LocalTimeConverter {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }
    
    @TypeConverter
    fun timeToTimestamp(time: LocalTime?): String? {
        return time?.toString()
    }
}

/**
 * Type converter for List<DayOfWeek>
 */
class DaysConverter {
    @TypeConverter
    fun fromString(value: String?): List<DayOfWeek>? {
        return value?.split(",")?.mapNotNull { dayName ->
            try {
                DayOfWeek.valueOf(dayName.trim())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    
    @TypeConverter
    fun toString(days: List<DayOfWeek>?): String? {
        return days?.joinToString(",") { it.name }
    }
}
