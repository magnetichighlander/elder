package com.eldercare.assistant.data.dao

import androidx.room.*
import com.eldercare.assistant.data.entity.Achievement
import com.eldercare.assistant.data.entity.UserAchievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    
    @Query("SELECT * FROM achievements ORDER BY id")
    fun getAllAchievements(): Flow<List<Achievement>>
    
    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: Int): Achievement?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)
    
    @Query("SELECT * FROM user_achievements")
    fun getUserAchievements(): Flow<List<UserAchievement>>
    
    @Query("SELECT * FROM user_achievements WHERE achievementId = :achievementId")
    suspend fun getUserAchievement(achievementId: Int): UserAchievement?
    
    @Query("SELECT * FROM user_achievements WHERE isViewed = 0")
    fun getUnviewedAchievements(): Flow<List<UserAchievement>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAchievement(userAchievement: UserAchievement)
    
    @Update
    suspend fun updateUserAchievement(userAchievement: UserAchievement)
    
    @Query("UPDATE user_achievements SET isViewed = 1 WHERE achievementId = :achievementId")
    suspend fun markAchievementAsViewed(achievementId: Int)
    
    @Query("""
        SELECT a.* FROM achievements a
        INNER JOIN user_achievements ua ON a.id = ua.achievementId
        ORDER BY ua.unlockedAt DESC
    """)
    fun getUnlockedAchievements(): Flow<List<Achievement>>
}
