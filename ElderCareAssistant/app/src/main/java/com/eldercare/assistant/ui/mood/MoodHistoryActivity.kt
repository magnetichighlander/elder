package com.eldercare.assistant.ui.mood

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eldercare.assistant.databinding.ActivityMoodHistoryBinding
import com.eldercare.assistant.ui.mood.adapter.MoodHistoryAdapter
import com.eldercare.assistant.viewmodel.MoodViewModel
import com.eldercare.assistant.viewmodel.TimePeriod
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoodHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoodHistoryBinding
    private val moodViewModel: MoodViewModel by viewModels()
    private lateinit var moodHistoryAdapter: MoodHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoodHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupUI() {
        // Set up time period filter buttons
        binding.btnFilterWeek.setOnClickListener {
            moodViewModel.loadMoodEntriesForPeriod(TimePeriod.WEEK)
            updateFilterButtons(TimePeriod.WEEK)
        }

        binding.btnFilterMonth.setOnClickListener {
            moodViewModel.loadMoodEntriesForPeriod(TimePeriod.MONTH)
            updateFilterButtons(TimePeriod.MONTH)
        }

        binding.btnFilterAll.setOnClickListener {
            moodViewModel.loadMoodEntriesForPeriod(TimePeriod.ALL)
            updateFilterButtons(TimePeriod.ALL)
        }

        // Set default filter
        updateFilterButtons(TimePeriod.ALL)
    }

    private fun setupRecyclerView() {
        moodHistoryAdapter = MoodHistoryAdapter { moodEntry ->
            // Handle item click - could show details or edit
            Toast.makeText(this, "Clicked on ${moodEntry.moodType.displayName}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewMoodHistory.apply {
            layoutManager = LinearLayoutManager(this@MoodHistoryActivity)
            adapter = moodHistoryAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            moodViewModel.moodEntries.collect { entries ->
                moodHistoryAdapter.submitList(entries)
                
                // Show empty state if no entries
                if (entries.isEmpty()) {
                    binding.textViewEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewMoodHistory.visibility = View.GONE
                } else {
                    binding.textViewEmptyState.visibility = View.GONE
                    binding.recyclerViewMoodHistory.visibility = View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            moodViewModel.uiState.collect { uiState ->
                // Show loading indicator
                binding.progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
                
                // Show error message
                if (uiState.error != null) {
                    Toast.makeText(this@MoodHistoryActivity, uiState.error, Toast.LENGTH_LONG).show()
                    moodViewModel.clearError()
                }
            }
        }
    }

    private fun updateFilterButtons(selectedPeriod: TimePeriod) {
        // Reset all buttons
        binding.btnFilterWeek.isSelected = false
        binding.btnFilterMonth.isSelected = false
        binding.btnFilterAll.isSelected = false

        // Set selected button
        when (selectedPeriod) {
            TimePeriod.WEEK -> binding.btnFilterWeek.isSelected = true
            TimePeriod.MONTH -> binding.btnFilterMonth.isSelected = true
            TimePeriod.ALL -> binding.btnFilterAll.isSelected = true
        }
    }
}
