package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "exercise_progress")
data class ExerciseProgress(
    @PrimaryKey val exerciseId: Int,
    val lastCompleted: LocalDateTime? = null,
    val completionCount: Int = 0,
    val bestDuration: Int = Int.MAX_VALUE
)
