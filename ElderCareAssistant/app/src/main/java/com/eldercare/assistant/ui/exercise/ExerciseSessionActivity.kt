package com.eldercare.assistant.ui.exercise

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.Exercise
import com.eldercare.assistant.databinding.ActivityExerciseSessionBinding
import com.eldercare.assistant.viewmodel.ExerciseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExerciseSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseSessionBinding
    private val viewModel: ExerciseViewModel by viewModels()
    private var exercise: Exercise? = null
    private var countDownTimer: CountDownTimer? = null
    private var remainingTimeMs: Long = 0
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val exerciseId = intent.getIntExtra("exercise_id", -1)
        if (exerciseId == -1) {
            finish()
            return
        }

        loadExercise(exerciseId)
        setupClickListeners()
        observeViewModel()
    }

    private fun loadExercise(exerciseId: Int) {
        lifecycleScope.launch {
            exercise = viewModel.getExerciseById(exerciseId)
            exercise?.let { setupExerciseSession(it) }
        }
    }

    private fun setupExerciseSession(exercise: Exercise) {
        binding.apply {
            textExerciseTitle.text = exercise.title
            textExerciseDescription.text = exercise.description
            textExerciseDuration.text = "Duration: ${exercise.durationSeconds} seconds"
            
            // Set up Lottie animation
            try {
                lottieExerciseAnimation.setAnimation("exercises/${exercise.lottieFile}")
            } catch (e: Exception) {
                // Hide animation if file not found
                lottieExerciseAnimation.visibility = View.GONE
            }

            // Initialize timer display
            remainingTimeMs = exercise.durationSeconds * 1000L
            updateTimerDisplay()
            updateProgressBar()
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            buttonStartPause.setOnClickListener {
                if (exercise != null) {
                    if (viewModel.uiState.value.isExerciseActive) {
                        if (isPaused) {
                            resumeExercise()
                        } else {
                            pauseExercise()
                        }
                    } else {
                        startExercise()
                    }
                }
            }

            buttonStop.setOnClickListener {
                showStopConfirmation()
            }

            buttonBack.setOnClickListener {
                if (viewModel.uiState.value.isExerciseActive) {
                    showExitConfirmation()
                } else {
                    finish()
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                updateUI(uiState)
            }
        }
    }

    private fun startExercise() {
        exercise?.let { ex ->
            viewModel.startExercise(ex.id)
            remainingTimeMs = ex.durationSeconds * 1000L
            startCountDownTimer()
            
            binding.lottieExerciseAnimation.playAnimation()
            binding.buttonStartPause.text = "Pause"
            binding.buttonStop.isEnabled = true
            
            Toast.makeText(this, "Exercise started! Follow the animation.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pauseExercise() {
        isPaused = true
        countDownTimer?.cancel()
        viewModel.pauseExercise()
        
        binding.lottieExerciseAnimation.pauseAnimation()
        binding.buttonStartPause.text = "Resume"
        
        Toast.makeText(this, "Exercise paused", Toast.LENGTH_SHORT).show()
    }

    private fun resumeExercise() {
        isPaused = false
        startCountDownTimer()
        viewModel.pauseExercise() // This toggles the pause state
        
        binding.lottieExerciseAnimation.resumeAnimation()
        binding.buttonStartPause.text = "Pause"
        
        Toast.makeText(this, "Exercise resumed", Toast.LENGTH_SHORT).show()
    }

    private fun completeExercise() {
        countDownTimer?.cancel()
        viewModel.completeExercise()
        
        binding.lottieExerciseAnimation.pauseAnimation()
        binding.buttonStartPause.text = "Start"
        binding.buttonStartPause.isEnabled = false
        binding.buttonStop.isEnabled = false
        
        showCompletionDialog()
    }

    private fun stopExercise() {
        countDownTimer?.cancel()
        viewModel.stopExercise()
        isPaused = false
        
        exercise?.let { ex ->
            remainingTimeMs = ex.durationSeconds * 1000L
        }
        
        binding.lottieExerciseAnimation.pauseAnimation()
        binding.buttonStartPause.text = "Start"
        binding.buttonStop.isEnabled = false
        
        updateTimerDisplay()
        updateProgressBar()
    }

    private fun startCountDownTimer() {
        countDownTimer = object : CountDownTimer(remainingTimeMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMs = millisUntilFinished
                updateTimerDisplay()
                updateProgressBar()
                
                // Update view model progress
                val totalDuration = exercise?.durationSeconds?.times(1000L) ?: 1L
                val progress = 1f - (remainingTimeMs.toFloat() / totalDuration)
                viewModel.updateExerciseProgress(progress)
            }

            override fun onFinish() {
                remainingTimeMs = 0
                updateTimerDisplay()
                updateProgressBar()
                completeExercise()
            }
        }.start()
    }

    private fun updateTimerDisplay() {
        val seconds = (remainingTimeMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        binding.textTimer.text = String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun updateProgressBar() {
        val totalDuration = exercise?.durationSeconds?.times(1000L) ?: 1L
        val progress = ((totalDuration - remainingTimeMs) * 100 / totalDuration).toInt()
        binding.progressBarExercise.progress = progress
        binding.textProgress.text = "$progress%"
    }

    private fun updateUI(uiState: com.eldercare.assistant.viewmodel.ExerciseUiState) {
        if (uiState.showCompletionDialog) {
            showCompletionDialog()
            viewModel.dismissCompletionDialog()
        }
    }

    private fun showCompletionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exercise Completed!")
            .setMessage("Congratulations! You've completed this exercise. Keep up the great work!")
            .setPositiveButton("Continue") { _, _ ->
                finish()
            }
            .setNegativeButton("Do Again") { _, _ ->
                resetExercise()
            }
            .setCancelable(false)
            .show()
    }

    private fun showStopConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Stop Exercise")
            .setMessage("Are you sure you want to stop the current exercise?")
            .setPositiveButton("Stop") { _, _ ->
                stopExercise()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Exercise")
            .setMessage("You have an active exercise session. Are you sure you want to exit?")
            .setPositiveButton("Exit") { _, _ ->
                stopExercise()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetExercise() {
        stopExercise()
        binding.buttonStartPause.isEnabled = true
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.uiState.value.isExerciseActive && !isPaused) {
            pauseExercise()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onBackPressed() {
        if (viewModel.uiState.value.isExerciseActive) {
            showExitConfirmation()
        } else {
            super.onBackPressed()
        }
    }
}
