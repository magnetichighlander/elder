package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val iconResId: Int,
    val condition: AchievementCondition
)

enum class AchievementCondition(val value: Int) {
    COMPLETE_5_EXERCISES(5),
    COMPLETE_10_EXERCISES(10),
    STREAK_3_DAYS(3),
    STREAK_7_DAYS(7)
}
