package com.eldercare.assistant.ui.mood.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eldercare.assistant.data.entity.MoodEntry
import com.eldercare.assistant.databinding.ItemMoodHistoryBinding
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MoodHistoryAdapter(
    private val onItemClick: (MoodEntry) -> Unit
) : ListAdapter<MoodEntry, MoodHistoryAdapter.MoodHistoryViewHolder>(MoodEntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodHistoryViewHolder {
        val binding = ItemMoodHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MoodHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MoodHistoryViewHolder(
        private val binding: ItemMoodHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(moodEntry: MoodEntry) {
            binding.apply {
                // Display mood emoji and name
                textViewMoodEmoji.text = moodEntry.moodType.emoji
                textViewMoodName.text = moodEntry.moodType.displayName

                // Display date
                val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                textViewDate.text = moodEntry.date.format(dateFormatter)

                // Display energy level
                textViewEnergyLevel.text = "Энергия: ${getEnergyLevelText(moodEntry.energyLevel)}"

                // Display note if present
                if (!moodEntry.note.isNullOrBlank()) {
                    textViewNote.text = moodEntry.note
                    textViewNote.visibility = android.view.View.VISIBLE
                } else {
                    textViewNote.visibility = android.view.View.GONE
                }

                // Handle item click
                root.setOnClickListener {
                    onItemClick(moodEntry)
                }
            }
        }

        private fun getEnergyLevelText(level: Int): String {
            return when (level) {
                0 -> "Очень низкий"
                1 -> "Низкий"
                2 -> "Ниже среднего"
                3 -> "Средний"
                4 -> "Высокий"
                5 -> "Очень высокий"
                else -> "Средний"
            }
        }
    }

    private class MoodEntryDiffCallback : DiffUtil.ItemCallback<MoodEntry>() {
        override fun areItemsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean {
            return oldItem == newItem
        }
    }
}
