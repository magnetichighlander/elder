package com.eldercare.assistant.ui.exercise

import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.Exercise
import com.eldercare.assistant.databinding.ActivityExercisePlayerBinding
import com.eldercare.assistant.utils.AccessibilityUtils
import com.eldercare.assistant.utils.Constants
import com.eldercare.assistant.viewmodel.ExerciseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class ExercisePlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityExercisePlayerBinding
    private val viewModel: ExerciseViewModel by viewModels()
    private lateinit var exercise: Exercise
    private lateinit var timer: CountDownTimer
    private var timeRemaining = 0
    private var isPaused = true
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExercisePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val exerciseId = intent.getIntExtra("exercise_id", -1)
        if (exerciseId == -1) {
            finish()
            return
        }

        loadExercise(exerciseId)
        setupAccessibility()
        initializeTTS()
    }

    private fun loadExercise(exerciseId: Int) {
        lifecycleScope.launch {
            exercise = viewModel.getExerciseById(exerciseId) ?: run {
                finish()
                return@launch
            }
            setupUI()
            prepareExercise()
        }
    }

    private fun setupUI() {
        binding.exerciseTitle.text = exercise.title
        binding.exerciseDescription.text = exercise.description
        
        // Set up Lottie animation
        try {
            binding.animationView.setAnimation("exercises/${exercise.lottieFile}")
            binding.animationView.repeatCount = LottieDrawable.INFINITE
        } catch (e: Exception) {
            // Fallback to default animation
            binding.animationView.setAnimation(R.raw.default_exercise_animation)
        }
        
        binding.progressBar.max = exercise.durationSeconds
        
        // Setup click listeners
        binding.playPauseButton.setOnClickListener {
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
            
            if (isPaused) {
                startExercise()
            } else {
                pauseExercise()
            }
        }
        
        binding.closeButton.setOnClickListener {
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
            
            if (!isPaused) {
                pauseExercise()
            }
            finish()
        }

        // Setup accessibility
        AccessibilityUtils.setupElderlyAccessibility(binding.playPauseButton, this)
        AccessibilityUtils.setupElderlyAccessibility(binding.closeButton, this)
    }

    private fun setupAccessibility() {
        // Announce screen content for screen readers
        AccessibilityUtils.announceForAccessibility(
            binding.root,
            "Exercise player loaded. ${exercise.title} ready to start."
        )
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                // Announce exercise details
                val message = "Ready to start ${exercise.title}. Duration ${exercise.durationSeconds} seconds."
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "exercise_ready")
            }
        }
    }

    private fun prepareExercise() {
        binding.animationView.playAnimation()
        binding.animationView.speed = 0.5f
        timeRemaining = exercise.durationSeconds
        updateTimerDisplay()
        
        // Initial state
        binding.playPauseButton.setImageResource(R.drawable.ic_play)
        binding.completionMessage.visibility = View.GONE
    }

    private fun startExercise() {
        isPaused = false
        binding.playPauseButton.setImageResource(R.drawable.ic_pause)
        binding.playPauseButton.contentDescription = "Pause exercise"
        
        // Announce start
        AccessibilityUtils.announceForAccessibility(
            binding.playPauseButton,
            "Exercise started"
        )
        
        tts?.speak("Exercise started. Follow the animation.", TextToSpeech.QUEUE_FLUSH, null, "exercise_start")
        
        timer = object : CountDownTimer((timeRemaining * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemaining = (millisUntilFinished / 1000).toInt()
                updateTimerDisplay()
                
                // Voice countdown for last 10 seconds
                if (timeRemaining <= 10 && timeRemaining > 0) {
                    tts?.speak(timeRemaining.toString(), TextToSpeech.QUEUE_ADD, null, "countdown_$timeRemaining")
                }
            }
            
            override fun onFinish() {
                exerciseCompleted()
            }
        }.start()
        
        binding.animationView.speed = 1f
        
        // Update ViewModel
        viewModel.startExercise(exercise.id)
    }

    private fun pauseExercise() {
        isPaused = true
        binding.playPauseButton.setImageResource(R.drawable.ic_play)
        binding.playPauseButton.contentDescription = "Resume exercise"
        
        // Announce pause
        AccessibilityUtils.announceForAccessibility(
            binding.playPauseButton,
            "Exercise paused"
        )
        
        tts?.speak("Exercise paused", TextToSpeech.QUEUE_FLUSH, null, "exercise_pause")
        
        if (::timer.isInitialized) {
            timer.cancel()
        }
        binding.animationView.speed = 0.5f
        
        // Update ViewModel
        viewModel.pauseExercise()
    }

    private fun updateTimerDisplay() {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        binding.timerText.text = String.format("%02d:%02d", minutes, seconds)
        binding.progressBar.progress = exercise.durationSeconds - timeRemaining
        
        // Update progress percentage for accessibility
        val progressPercent = ((exercise.durationSeconds - timeRemaining) * 100) / exercise.durationSeconds
        binding.progressBar.contentDescription = "Exercise progress: $progressPercent percent complete"
    }

    private fun exerciseCompleted() {
        if (::timer.isInitialized) {
            timer.cancel()
        }
        
        isPaused = true
        binding.animationView.speed = 0.5f
        
        // Calculate actual duration (for cases where user paused/resumed)
        val actualDuration = exercise.durationSeconds - timeRemaining
        
        // Save progress via ViewModel
        viewModel.completeExercise()
        
        // Show completion animation
        try {
            binding.animationView.setAnimation(R.raw.success_animation)
            binding.animationView.repeatCount = 1
            binding.animationView.playAnimation()
        } catch (e: Exception) {
            // Fallback: just pause current animation
            binding.animationView.pauseAnimation()
        }
        
        // Show congrats message
        binding.completionMessage.visibility = View.VISIBLE
        binding.completionMessage.text = "Great job! You completed ${exercise.title}"
        
        // Update button to show completion
        binding.playPauseButton.setImageResource(R.drawable.ic_check)
        binding.playPauseButton.contentDescription = "Exercise completed"
        binding.playPauseButton.isEnabled = false
        
        // Provide haptic feedback
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            // Vibration not available
        }
        
        // Announce completion
        val congratsMessage = "Congratulations! You completed the ${exercise.title} exercise. Well done!"
        AccessibilityUtils.announceForAccessibility(binding.root, congratsMessage)
        tts?.speak(congratsMessage, TextToSpeech.QUEUE_FLUSH, null, "exercise_complete")
        
        // Auto-close after 3 seconds
        binding.root.postDelayed({
            finish()
        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        if (::timer.isInitialized) {
            timer.cancel()
        }
        
        tts?.let { textToSpeech ->
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    override fun onPause() {
        super.onPause()
        
        // Auto-pause exercise when activity is paused
        if (!isPaused) {
            pauseExercise()
        }
    }
}
