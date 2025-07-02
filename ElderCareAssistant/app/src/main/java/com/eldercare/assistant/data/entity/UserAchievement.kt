package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "user_achievements")
data class UserAchievement(
    @PrimaryKey val achievementId: Int,
    val unlockedAt: LocalDateTime,
    val isViewed: Boolean = false
)
