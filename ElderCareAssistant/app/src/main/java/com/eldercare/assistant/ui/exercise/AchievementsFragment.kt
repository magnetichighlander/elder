package com.eldercare.assistant.ui.exercise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.eldercare.assistant.databinding.FragmentAchievementsBinding
import com.eldercare.assistant.ui.adapter.AchievementAdapter
import com.eldercare.assistant.viewmodel.AchievementViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AchievementsFragment : Fragment() {
    private lateinit var binding: FragmentAchievementsBinding
    private val viewModel: AchievementViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupRecyclerView() {
        val adapter = AchievementAdapter { achievementId ->
            // Handle achievement click - mark as viewed
            viewModel.markAchievementAsViewed(achievementId)
            Toast.makeText(requireContext(), "Achievement details viewed!", Toast.LENGTH_SHORT).show()
        }
        
        binding.achievementsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = adapter
        }
    }
    
    private fun observeViewModel() {
        // Observe achievements list
        viewModel.achievements.observe(viewLifecycleOwner) { achievements ->
            val adapter = binding.achievementsRecyclerView.adapter as AchievementAdapter
            adapter.submitList(achievements)
        }
        
        // Observe achievement progress
        viewModel.getAchievementProgress().observe(viewLifecycleOwner) { progress ->
            updateProgressOverview(progress)
        }
        
        // Observe newly unlocked achievements for celebration
        viewModel.newlyUnlockedAchievement.observe(viewLifecycleOwner) { achievement ->
            achievement?.let {
                showAchievementUnlockedCelebration(it)
                viewModel.clearNewlyUnlockedAchievement()
            }
        }
        
        // Load initial data
        viewModel.loadAchievements()
    }
    
    private fun updateProgressOverview(progress: com.eldercare.assistant.viewmodel.AchievementProgress) {
        binding.textProgressDetails.text = "${progress.unlockedAchievements} of ${progress.totalAchievements} achievements unlocked"
        binding.textProgressPercentage.text = "${progress.progressPercentage}%"
    }
    
    private fun showAchievementUnlockedCelebration(achievement: com.eldercare.assistant.data.entity.Achievement) {
        // Show a celebration toast or dialog
        Toast.makeText(
            requireContext(), 
            "🎉 Achievement Unlocked: ${achievement.title}!", 
            Toast.LENGTH_LONG
        ).show()
        
        // Could also show a celebration dialog here
    }
}

