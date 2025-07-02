package com.eldercare.assistant.data.repository

import com.eldercare.assistant.data.dao.ExerciseDao
import com.eldercare.assistant.data.dao.ExerciseProgressDao
import com.eldercare.assistant.data.dao.AchievementDao
import com.eldercare.assistant.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepository @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseProgressDao: ExerciseProgressDao,
    private val achievementDao: AchievementDao
) {
    
    // Exercise operations
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()
    
    fun getExercisesByDifficulty(difficulty: Difficulty): Flow<List<Exercise>> = 
        exerciseDao.getExercisesByDifficulty(difficulty)
    
    suspend fun getExerciseById(id: Int): Exercise? = exerciseDao.getExerciseById(id)
    
    suspend fun insertExercises(exercises: List<Exercise>) = exerciseDao.insertExercises(exercises)
    
    // Progress operations
    fun getAllProgress(): Flow<List<ExerciseProgress>> = exerciseProgressDao.getAllProgress()
    
    fun getCompletedExercises(): Flow<List<ExerciseProgress>> = exerciseProgressDao.getCompletedExercises()
    
    suspend fun getProgressByExerciseId(exerciseId: Int): ExerciseProgress? = 
        exerciseProgressDao.getProgressByExerciseId(exerciseId)
    
    suspend fun completeExercise(exerciseId: Int, durationSeconds: Int) {
        val existingProgress = exerciseProgressDao.getProgressByExerciseId(exerciseId)
        val now = LocalDateTime.now()
        
        val updatedProgress = if (existingProgress != null) {
            existingProgress.copy(
                lastCompleted = now,
                completionCount = existingProgress.completionCount + 1,
                bestDuration = minOf(existingProgress.bestDuration, durationSeconds)
            )
        } else {
            ExerciseProgress(
                exerciseId = exerciseId,
                lastCompleted = now,
                completionCount = 1,
                bestDuration = durationSeconds
            )
        }
        
        exerciseProgressDao.insertProgress(updatedProgress)
        checkAndUnlockAchievements()
    }
    
    // Achievement operations
    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()
    
    fun getUnlockedAchievements(): Flow<List<Achievement>> = achievementDao.getUnlockedAchievements()
    
    fun getUnviewedAchievements(): Flow<List<UserAchievement>> = achievementDao.getUnviewedAchievements()
    
    suspend fun markAchievementAsViewed(achievementId: Int) = 
        achievementDao.markAchievementAsViewed(achievementId)
    
    suspend fun initializeAchievements() {
        val achievements = listOf(
            Achievement(1, "First Steps", "Complete your first exercise", android.R.drawable.star_on, AchievementCondition.COMPLETE_5_EXERCISES),
            Achievement(2, "Getting Started", "Complete 5 exercises", android.R.drawable.star_on, AchievementCondition.COMPLETE_5_EXERCISES),
            Achievement(3, "Dedicated", "Complete 10 exercises", android.R.drawable.star_on, AchievementCondition.COMPLETE_10_EXERCISES),
            Achievement(4, "Three Day Streak", "Exercise for 3 consecutive days", android.R.drawable.star_on, AchievementCondition.STREAK_3_DAYS),
            Achievement(5, "Week Warrior", "Exercise for 7 consecutive days", android.R.drawable.star_on, AchievementCondition.STREAK_7_DAYS)
        )
        achievementDao.insertAchievements(achievements)
    }
    
    suspend fun initializeExercises() {
        val exercises = listOf(
            Exercise(1, "Neck Stretch", "Gentle neck stretching to reduce tension", 30, "neck_stretch.json", Difficulty.BEGINNER),
            Exercise(2, "Shoulder Rolls", "Roll your shoulders to improve mobility", 45, "shoulder_rolls.json", Difficulty.BEGINNER),
            Exercise(3, "Ankle Circles", "Rotate your ankles to improve circulation", 60, "ankle_circles.json", Difficulty.BEGINNER),
            Exercise(4, "Deep Breathing", "Practice deep breathing for relaxation", 120, "deep_breathing.json", Difficulty.BEGINNER),
            Exercise(5, "Seated Twist", "Gentle spinal twist while seated", 90, "seated_twist.json", Difficulty.INTERMEDIATE),
            Exercise(6, "Arm Raises", "Raise your arms to strengthen shoulders", 75, "arm_raises.json", Difficulty.INTERMEDIATE),
            Exercise(7, "Leg Extensions", "Extend your legs to strengthen thighs", 120, "leg_extensions.json", Difficulty.ADVANCED),
            Exercise(8, "Balance Practice", "Practice standing balance", 180, "balance_practice.json", Difficulty.ADVANCED)
        )
        exerciseDao.insertExercises(exercises)
    }
    
    private suspend fun checkAndUnlockAchievements() {
        val totalCompletions = getTotalCompletions()
        val totalCompletedExercises = getTotalCompletedExercises()
        val currentStreak = calculateStreak()
        
        val achievementsToUnlock = mutableListOf<UserAchievement>()
        val now = LocalDateTime.now()
        
        // Check completion-based achievements
        if (totalCompletions >= 1 && achievementDao.getUserAchievement(1) == null) {
            achievementsToUnlock.add(UserAchievement(1, now))
        }
        if (totalCompletions >= 5 && achievementDao.getUserAchievement(2) == null) {
            achievementsToUnlock.add(UserAchievement(2, now))
        }
        if (totalCompletions >= 10 && achievementDao.getUserAchievement(3) == null) {
            achievementsToUnlock.add(UserAchievement(3, now))
        }
        
        // Check streak-based achievements
        if (currentStreak >= 3 && achievementDao.getUserAchievement(4) == null) {
            achievementsToUnlock.add(UserAchievement(4, now))
        }
        if (currentStreak >= 7 && achievementDao.getUserAchievement(5) == null) {
            achievementsToUnlock.add(UserAchievement(5, now))
        }
        
        // Unlock new achievements
        achievementsToUnlock.forEach { userAchievement ->
            achievementDao.insertUserAchievement(userAchievement)
        }
    }
    
    // Additional progress tracking methods
    suspend fun getTotalCompletions(): Int {
        return exerciseProgressDao.getTotalExerciseCompletions()
    }
    
    suspend fun getTotalCompletedExercises(): Int {
        return exerciseProgressDao.getTotalCompletedExercises()
    }
    
    suspend fun calculateStreak(): Int {
        val progressList = exerciseProgressDao.getCompletedExercises().first()
        
        if (progressList.isEmpty()) return 0
        
        // Sort by last completed date in descending order
        val sortedProgress = progressList
            .filter { it.lastCompleted != null }
            .sortedByDescending { it.lastCompleted }
        
        if (sortedProgress.isEmpty()) return 0
        
        var streak = 0
        var currentDate = LocalDateTime.now().toLocalDate()
        
        // Get unique completion dates
        val completionDates = sortedProgress
            .mapNotNull { it.lastCompleted?.toLocalDate() }
            .distinct()
            .sortedDescending()
        
        // Check if there's activity today or yesterday
        val latestDate = completionDates.firstOrNull()
        if (latestDate == null) return 0
        
        val daysSinceLatest = java.time.temporal.ChronoUnit.DAYS.between(latestDate, currentDate)
        if (daysSinceLatest > 1) return 0 // Streak broken if more than 1 day gap
        
        // Count consecutive days
        for (i in completionDates.indices) {
            val date = completionDates[i]
            val expectedDate = currentDate.minusDays(i.toLong())
            
            if (date == expectedDate || (i == 0 && date == currentDate.minusDays(1))) {
                streak++
                if (i == 0 && date == currentDate.minusDays(1)) {
                    currentDate = currentDate.minusDays(1)
                }
            } else {
                break
            }
        }
        
        return streak
    }
    
    suspend fun getExerciseStats(): ExerciseStats {
        val totalCompletions = getTotalCompletions()
        val totalExercises = getTotalCompletedExercises()
        val currentStreak = calculateStreak()
        val allProgress = exerciseProgressDao.getAllProgress().first()
        
        val bestDuration = allProgress
            .filter { it.bestDuration != Int.MAX_VALUE }
            .minByOrNull { it.bestDuration }?.bestDuration ?: 0
            
        return ExerciseStats(
            totalCompletions = totalCompletions,
            totalExercises = totalExercises,
            currentStreak = currentStreak,
            bestDuration = bestDuration,
            averageCompletionsPerExercise = if (totalExercises > 0) totalCompletions.toFloat() / totalExercises else 0f
        )
    }
}
