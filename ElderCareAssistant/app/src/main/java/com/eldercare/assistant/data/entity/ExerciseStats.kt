package com.eldercare.assistant.data.entity

/**
 * Data class representing comprehensive exercise statistics
 */
data class ExerciseStats(
    val totalCompletions: Int,
    val totalExercises: Int,
    val currentStreak: Int,
    val bestDuration: Int, // in seconds
    val averageCompletionsPerExercise: Float
) {
    /**
     * Formats the best duration as a human-readable string
     */
    fun getFormattedBestDuration(): String {
        val minutes = bestDuration / 60
        val seconds = bestDuration % 60
        return "${minutes}m ${seconds}s"
    }
    
    /**
     * Gets streak description for UI display
     */
    fun getStreakDescription(): String {
        return when (currentStreak) {
            0 -> "No current streak"
            1 -> "1 day streak"
            else -> "$currentStreak days streak"
        }
    }
    
    /**
     * Gets completion rate as percentage
     */
    fun getCompletionEfficiency(): Float {
        return if (totalExercises > 0) {
            (averageCompletionsPerExercise * 100) / totalExercises
        } else 0f
    }
}
