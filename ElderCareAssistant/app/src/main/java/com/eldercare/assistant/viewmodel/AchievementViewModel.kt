package com.eldercare.assistant.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eldercare.assistant.data.entity.Achievement
import com.eldercare.assistant.data.entity.UserAchievement
import com.eldercare.assistant.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _achievements = MutableLiveData<List<AchievementWithStatus>>()
    val achievements: LiveData<List<AchievementWithStatus>> = _achievements

    private val _unlockedAchievements = MutableLiveData<List<Achievement>>()
    val unlockedAchievements: LiveData<List<Achievement>> = _unlockedAchievements

    private val _newlyUnlockedAchievement = MutableLiveData<Achievement?>()
    val newlyUnlockedAchievement: LiveData<Achievement?> = _newlyUnlockedAchievement

    fun loadAchievements() {
        viewModelScope.launch {
            // Combine all achievements with user achievement status
            combine(
                exerciseRepository.getAllAchievements(),
                exerciseRepository.getUnlockedAchievements()
            ) { allAchievements, unlockedAchievements ->
                allAchievements.map { achievement ->
                    val isUnlocked = unlockedAchievements.any { it.id == achievement.id }
                    AchievementWithStatus(
                        achievement = achievement,
                        isEarned = isUnlocked,
                        isNewlyUnlocked = false // Will be set separately for new achievements
                    )
                }
            }.collect { achievementsWithStatus ->
                _achievements.value = achievementsWithStatus
            }
        }
    }

    fun loadUnlockedAchievements() {
        viewModelScope.launch {
            exerciseRepository.getUnlockedAchievements().collect { achievements ->
                _unlockedAchievements.value = achievements
            }
        }
    }

    fun markAchievementAsViewed(achievementId: Int) {
        viewModelScope.launch {
            exerciseRepository.markAchievementAsViewed(achievementId)
            // Refresh achievements to update viewed status
            loadAchievements()
        }
    }

    fun showNewlyUnlockedAchievement(achievement: Achievement) {
        _newlyUnlockedAchievement.value = achievement
    }

    fun clearNewlyUnlockedAchievement() {
        _newlyUnlockedAchievement.value = null
    }

    fun getAchievementProgress(): LiveData<AchievementProgress> {
        val progressLiveData = MutableLiveData<AchievementProgress>()
        
        viewModelScope.launch {
            combine(
                exerciseRepository.getAllAchievements(),
                exerciseRepository.getUnlockedAchievements()
            ) { allAchievements, unlockedAchievements ->
                AchievementProgress(
                    totalAchievements = allAchievements.size,
                    unlockedAchievements = unlockedAchievements.size,
                    progressPercentage = if (allAchievements.isNotEmpty()) {
                        (unlockedAchievements.size * 100) / allAchievements.size
                    } else 0
                )
            }.collect { progress ->
                progressLiveData.value = progress
            }
        }
        
        return progressLiveData
    }
}

data class AchievementWithStatus(
    val achievement: Achievement,
    val isEarned: Boolean,
    val isNewlyUnlocked: Boolean
)

data class AchievementProgress(
    val totalAchievements: Int,
    val unlockedAchievements: Int,
    val progressPercentage: Int
)
