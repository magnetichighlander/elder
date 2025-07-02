package com.eldercare.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eldercare.assistant.data.entity.*
import com.eldercare.assistant.data.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()
    
    private val _selectedDifficulty = MutableStateFlow(Difficulty.BEGINNER)
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty.asStateFlow()
    
    val exercises: StateFlow<List<Exercise>> = selectedDifficulty
        .flatMapLatest { difficulty ->
            exerciseRepository.getExercisesByDifficulty(difficulty)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val exerciseProgress: StateFlow<List<ExerciseProgress>> = exerciseRepository.getAllProgress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val unlockedAchievements: StateFlow<List<Achievement>> = exerciseRepository.getUnlockedAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val unviewedAchievements: StateFlow<List<UserAchievement>> = exerciseRepository.getUnviewedAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        initializeData()
    }
    
    fun selectDifficulty(difficulty: Difficulty) {
        _selectedDifficulty.value = difficulty
    }
    
    fun startExercise(exerciseId: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                currentExerciseId = exerciseId,
                isExerciseActive = true,
                exerciseStartTime = System.currentTimeMillis(),
                exerciseProgress = 0f
            )
        }
    }
    
    fun updateExerciseProgress(progress: Float) {
        _uiState.update { currentState ->
            currentState.copy(exerciseProgress = progress)
        }
    }
    
    fun completeExercise() {
        val currentState = _uiState.value
        if (currentState.isExerciseActive && currentState.currentExerciseId != null) {
            val durationSeconds = ((System.currentTimeMillis() - currentState.exerciseStartTime) / 1000).toInt()
            
            viewModelScope.launch {
                exerciseRepository.completeExercise(currentState.currentExerciseId, durationSeconds)
                _uiState.update { state ->
                    state.copy(
                        isExerciseActive = false,
                        currentExerciseId = null,
                        exerciseProgress = 0f,
                        exerciseStartTime = 0L,
                        showCompletionDialog = true
                    )
                }
            }
        }
    }
    
    fun pauseExercise() {
        _uiState.update { currentState ->
            currentState.copy(isExercisePaused = !currentState.isExercisePaused)
        }
    }
    
    fun stopExercise() {
        _uiState.update { currentState ->
            currentState.copy(
                isExerciseActive = false,
                isExercisePaused = false,
                currentExerciseId = null,
                exerciseProgress = 0f,
                exerciseStartTime = 0L
            )
        }
    }
    
    fun dismissCompletionDialog() {
        _uiState.update { currentState ->
            currentState.copy(showCompletionDialog = false)
        }
    }
    
    fun markAchievementAsViewed(achievementId: Int) {
        viewModelScope.launch {
            exerciseRepository.markAchievementAsViewed(achievementId)
        }
    }
    
    suspend fun getExerciseById(exerciseId: Int): Exercise? {
        return exerciseRepository.getExerciseById(exerciseId)
    }
    
    suspend fun getProgressForExercise(exerciseId: Int): ExerciseProgress? {
        return exerciseRepository.getProgressByExerciseId(exerciseId)
    }
    
    private fun initializeData() {
        viewModelScope.launch {
            try {
                exerciseRepository.initializeExercises()
                exerciseRepository.initializeAchievements()
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(errorMessage = "Failed to initialize exercise data: ${e.message}")
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }
}

data class ExerciseUiState(
    val isLoading: Boolean = false,
    val isExerciseActive: Boolean = false,
    val isExercisePaused: Boolean = false,
    val currentExerciseId: Int? = null,
    val exerciseProgress: Float = 0f,
    val exerciseStartTime: Long = 0L,
    val showCompletionDialog: Boolean = false,
    val errorMessage: String? = null
)
