package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

/**
 * Data model for mood tracking entries
 * Stores user's daily mood, energy level and optional notes
 */
@Entity(tableName = "mood_entries")
@TypeConverters(LocalDateConverter::class)
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    val date: LocalDate = LocalDate.now(),
    val moodType: MoodType,
    val energyLevel: Int, // 1-5 scale
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enum representing different mood types with emoji representations
 * Designed to be easily recognizable for elderly users
 */
enum class MoodType(val emoji: String, val displayName: String) {
    HAPPY("😊", "Счастливый"),
    SAD("😔", "Грустный"),
    CALM("😌", "Спокойный"),
    EXCITED("🤩", "Взволнованный"),
    ANGRY("😠", "Сердитый"),
    TIRED("😴", "Усталый");
    
    companion object {
        /**
         * Get mood type from string representation
         */
        fun fromString(value: String): MoodType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
        
        /**
         * Get all moods as a list for UI display
         */
        fun getAllMoods(): List<MoodType> {
            return values().toList()
        }
    }
}

/**
 * Type converter for LocalDate to work with Room database
 */
class LocalDateConverter {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }
}
