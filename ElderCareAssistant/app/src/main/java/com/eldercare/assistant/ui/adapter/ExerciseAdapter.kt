package com.eldercare.assistant.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.Exercise
import com.eldercare.assistant.data.entity.ExerciseProgress

class ExerciseAdapter(
    private val onExerciseClick: (Exercise) -> Unit
) : ListAdapter<ExerciseWithProgress, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(view, onExerciseClick)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExerciseViewHolder(
        itemView: View,
        private val onExerciseClick: (Exercise) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val exerciseAnimation: LottieAnimationView = itemView.findViewById(R.id.exerciseAnimation)
        private val exerciseTitle: TextView = itemView.findViewById(R.id.exerciseTitle)
        private val exerciseDuration: TextView = itemView.findViewById(R.id.exerciseDuration)
        private val exerciseStatus: TextView = itemView.findViewById(R.id.exerciseStatus)
        private val startButton: ImageButton = itemView.findViewById(R.id.startButton)

        fun bind(exerciseWithProgress: ExerciseWithProgress) {
            val exercise = exerciseWithProgress.exercise
            val progress = exerciseWithProgress.progress

            exerciseTitle.text = exercise.title
            exerciseDuration.text = "Duration: ${exercise.durationSeconds} seconds"

            // Set up Lottie animation
            try {
                exerciseAnimation.setAnimation("exercises/${exercise.lottieFile}")
                exerciseAnimation.playAnimation()
            } catch (e: Exception) {
                // Fallback to default animation or hide
                exerciseAnimation.visibility = View.GONE
            }

            // Show progress information
            if (progress != null && progress.completionCount > 0) {
                exerciseStatus.text = "Completed ${progress.completionCount} times"
                exerciseStatus.visibility = View.VISIBLE
            } else {
                exerciseStatus.visibility = View.GONE
            }

            // Handle both card click and button click
            val clickListener = View.OnClickListener {
                onExerciseClick(exercise)
            }
            
            itemView.setOnClickListener(clickListener)
            startButton.setOnClickListener(clickListener)
        }
    }

    private class ExerciseDiffCallback : DiffUtil.ItemCallback<ExerciseWithProgress>() {
        override fun areItemsTheSame(
            oldItem: ExerciseWithProgress,
            newItem: ExerciseWithProgress
        ): Boolean {
            return oldItem.exercise.id == newItem.exercise.id
        }

        override fun areContentsTheSame(
            oldItem: ExerciseWithProgress,
            newItem: ExerciseWithProgress
        ): Boolean {
            return oldItem == newItem
        }
    }
}

data class ExerciseWithProgress(
    val exercise: Exercise,
    val progress: ExerciseProgress?
)
