package com.eldercare.assistant.ui.exercise

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.Difficulty
import com.eldercare.assistant.data.entity.Exercise
import com.eldercare.assistant.databinding.ActivityExerciseBinding
import com.eldercare.assistant.ui.adapter.ExerciseAdapter
import com.eldercare.assistant.ui.adapter.ExerciseWithProgress
import com.eldercare.assistant.viewmodel.ExerciseViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseBinding
    private val viewModel: ExerciseViewModel by viewModels()
    private lateinit var exerciseAdapter: ExerciseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupDifficultyTabs()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Exercise & Wellness"
    }

    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter { exercise ->
            startExerciseSession(exercise)
        }
        
        binding.exerciseRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExerciseActivity)
            adapter = exerciseAdapter
        }
    }

    private fun setupDifficultyTabs() {
        binding.difficultyTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val difficulty = when (tab?.position) {
                    0 -> Difficulty.BEGINNER
                    1 -> Difficulty.INTERMEDIATE
                    2 -> Difficulty.ADVANCED
                    else -> Difficulty.BEGINNER
                }
                viewModel.selectDifficulty(difficulty)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Select beginner tab by default
        binding.difficultyTabs.selectTab(binding.difficultyTabs.getTabAt(0))
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            // Combine exercises and progress data
            combine(
                viewModel.exercises,
                viewModel.exerciseProgress
            ) { exercises, progressList ->
                exercises.map { exercise ->
                    val progress = progressList.find { it.exerciseId == exercise.id }
                    ExerciseWithProgress(exercise, progress)
                }
            }.collect { exercisesWithProgress ->
                exerciseAdapter.submitList(exercisesWithProgress)
                updateEmptyState(exercisesWithProgress.isEmpty())
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                handleUiState(uiState)
            }
        }

        lifecycleScope.launch {
            viewModel.unviewedAchievements.collect { achievements ->
                if (achievements.isNotEmpty()) {
                    showAchievementNotification(achievements.size)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.achievementsButton.setOnClickListener {
            startActivity(Intent(this, AchievementActivity::class.java))
        }

        binding.buttonProgressStats.setOnClickListener {
            startActivity(Intent(this, ExerciseStatsActivity::class.java))
        }
    }

    private fun startExerciseSession(exercise: Exercise) {
        val intent = Intent(this, ExercisePlayerActivity::class.java).apply {
            putExtra("exercise_id", exercise.id)
        }
        startActivity(intent)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.exerciseRecyclerView.visibility = View.GONE
            binding.textEmptyState.visibility = View.VISIBLE
            binding.textEmptyState.text = "No exercises available for this difficulty level"
        } else {
            binding.exerciseRecyclerView.visibility = View.VISIBLE
            binding.textEmptyState.visibility = View.GONE
        }
    }

    private fun handleUiState(uiState: com.eldercare.assistant.viewmodel.ExerciseUiState) {
        if (uiState.errorMessage != null) {
            Toast.makeText(this, uiState.errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    private fun showAchievementNotification(count: Int) {
        binding.badgeAchievements.apply {
            visibility = View.VISIBLE
            text = count.toString()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
