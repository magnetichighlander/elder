package com.eldercare.assistant.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.ExerciseProgress
import java.time.format.DateTimeFormatter

class ExerciseProgressAdapter : ListAdapter<ExerciseProgress, ExerciseProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise_progress, parent, false)
        return ProgressViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textExerciseId: TextView = itemView.findViewById(R.id.textExerciseId)
        private val textCompletionCount: TextView = itemView.findViewById(R.id.textCompletionCount)
        private val textBestDuration: TextView = itemView.findViewById(R.id.textBestDuration)
        private val textLastCompleted: TextView = itemView.findViewById(R.id.textLastCompleted)

        fun bind(progress: ExerciseProgress) {
            textExerciseId.text = "Exercise #${progress.exerciseId}"
            textCompletionCount.text = "Completed: ${progress.completionCount} times"
            
            if (progress.bestDuration != Int.MAX_VALUE) {
                val minutes = progress.bestDuration / 60
                val seconds = progress.bestDuration % 60
                textBestDuration.text = "Best Time: ${minutes}m ${seconds}s"
            } else {
                textBestDuration.text = "Best Time: --"
            }
            
            progress.lastCompleted?.let { dateTime ->
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                textLastCompleted.text = "Last: ${dateTime.format(formatter)}"
            } ?: run {
                textLastCompleted.text = "Last: Never"
            }
        }
    }

    private class ProgressDiffCallback : DiffUtil.ItemCallback<ExerciseProgress>() {
        override fun areItemsTheSame(oldItem: ExerciseProgress, newItem: ExerciseProgress): Boolean {
            return oldItem.exerciseId == newItem.exerciseId
        }

        override fun areContentsTheSame(oldItem: ExerciseProgress, newItem: ExerciseProgress): Boolean {
            return oldItem == newItem
        }
    }
}
