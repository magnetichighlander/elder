package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val durationSeconds: Int,
    val lottieFile: String,
    val difficulty: Difficulty = Difficulty.BEGINNER
)

enum class Difficulty {
    BEGINNER, INTERMEDIATE, ADVANCED
}
