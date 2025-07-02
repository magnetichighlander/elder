package com.eldercare.assistant.ui.exercise

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.eldercare.assistant.databinding.ActivityAchievementBinding
import com.eldercare.assistant.ui.adapter.AchievementAdapter
import com.eldercare.assistant.viewmodel.AchievementViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AchievementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementBinding
    private val viewModel: AchievementViewModel by viewModels()
    private lateinit var achievementAdapter: AchievementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Achievements"
    }

    private fun setupRecyclerView() {
        achievementAdapter = AchievementAdapter { achievementId ->
            viewModel.markAchievementAsViewed(achievementId)
        }
        
        binding.recyclerViewAchievements.apply {
            layoutManager = GridLayoutManager(this@AchievementActivity, 2)
            adapter = achievementAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.achievements.observe(this) { achievements ->
            achievementAdapter.submitList(achievements)
        }
        
        viewModel.loadAchievements()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
