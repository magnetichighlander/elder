package com.eldercare.assistant.ui.exercise

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eldercare.assistant.databinding.ActivityExerciseStatsBinding
import com.eldercare.assistant.ui.adapter.ExerciseProgressAdapter
import com.eldercare.assistant.viewmodel.ExerciseViewModel
import com.eldercare.assistant.data.repository.ExerciseRepository
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseStatsBinding
    private val viewModel: ExerciseViewModel by viewModels()
    private lateinit var progressAdapter: ExerciseProgressAdapter
    
    @Inject
    lateinit var exerciseRepository: ExerciseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadDetailedStats()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Exercise Progress"
    }

    private fun setupRecyclerView() {
        progressAdapter = ExerciseProgressAdapter()
        
        binding.recyclerViewProgress.apply {
            layoutManager = LinearLayoutManager(this@ExerciseStatsActivity)
            adapter = progressAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.exerciseProgress.collect { progressList ->
                progressAdapter.submitList(progressList.filter { it.completionCount > 0 })
                updateStats(progressList)
            }
        }
    }

    private fun updateStats(progressList: List<com.eldercare.assistant.data.entity.ExerciseProgress>) {
        val totalExercises = progressList.count { it.completionCount > 0 }
        val totalCompletions = progressList.sumOf { it.completionCount }
        
        binding.textTotalExercises.text = "Exercises Completed: $totalExercises"
        binding.textTotalCompletions.text = "Total Sessions: $totalCompletions"
        
        if (progressList.isNotEmpty()) {
            val averageCompletions = totalCompletions.toFloat() / totalExercises
            binding.textAverageCompletions.text = "Average per Exercise: %.1f".format(averageCompletions)
        }
    }
    
    private fun loadDetailedStats() {
        lifecycleScope.launch {
            try {
                val stats = exerciseRepository.getExerciseStats()
                updateDetailedStats(stats)
            } catch (e: Exception) {
                // Handle error gracefully
                android.util.Log.e("ExerciseStats", "Error loading detailed stats", e)
            }
        }
    }
    
    private fun updateDetailedStats(stats: com.eldercare.assistant.data.entity.ExerciseStats) {
        // Update existing fields with detailed stats
        binding.textTotalExercises.text = "Exercises Completed: ${stats.totalExercises}"
        binding.textTotalCompletions.text = "Total Sessions: ${stats.totalCompletions}"
        binding.textAverageCompletions.text = "Average per Exercise: %.1f".format(stats.averageCompletionsPerExercise)
        
        // Add additional stats if UI elements exist
        try {
            binding.textCurrentStreak?.text = "Current Streak: ${stats.getStreakDescription()}"
            binding.textBestDuration?.text = "Best Time: ${stats.getFormattedBestDuration()}"
        } catch (e: Exception) {
            // UI elements don't exist, ignore
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
