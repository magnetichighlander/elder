package com.eldercare.assistant.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.Achievement
import com.eldercare.assistant.viewmodel.AchievementWithStatus
import com.airbnb.lottie.LottieAnimationView

class AchievementAdapter(
    private val onAchievementClick: ((Int) -> Unit)? = null
) : ListAdapter<AchievementWithStatus, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view, onAchievementClick)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AchievementViewHolder(
        itemView: View,
        private val onAchievementClick: ((Int) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val imageIcon: ImageView = itemView.findViewById(R.id.imageIcon)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textDescription: TextView = itemView.findViewById(R.id.textDescription)
        private val celebrationAnimation: LottieAnimationView? = itemView.findViewById(R.id.celebrationAnimation)

        fun bind(achievementWithStatus: AchievementWithStatus) {
            val achievement = achievementWithStatus.achievement
            
            textTitle.text = achievement.title
            textDescription.text = achievement.description
            imageIcon.setImageResource(achievement.iconResId)

            // Handle earned vs unearned state
            if (achievementWithStatus.isEarned) {
                // Earned achievement - full color and animation
                imageIcon.alpha = 1.0f
                textTitle.alpha = 1.0f
                textDescription.alpha = 1.0f
                itemView.isEnabled = true
                
                // Play celebration animation if available
                celebrationAnimation?.let { animation ->
                    animation.visibility = View.VISIBLE
                    try {
                        animation.setAnimation(R.raw.celebration_animation)
                        animation.playAnimation()
                    } catch (e: Exception) {
                        // Hide if animation not found
                        animation.visibility = View.GONE
                    }
                }
            } else {
                // Unearned achievement - grayed out
                imageIcon.alpha = 0.3f
                textTitle.alpha = 0.5f
                textDescription.alpha = 0.5f
                itemView.isEnabled = false
                
                // Hide celebration animation
                celebrationAnimation?.visibility = View.GONE
            }

            itemView.setOnClickListener {
                if (achievementWithStatus.isEarned) {
                    onAchievementClick?.invoke(achievement.id)
                }
            }
        }
    }

    private class AchievementDiffCallback : DiffUtil.ItemCallback<AchievementWithStatus>() {
        override fun areItemsTheSame(oldItem: AchievementWithStatus, newItem: AchievementWithStatus): Boolean {
            return oldItem.achievement.id == newItem.achievement.id
        }

        override fun areContentsTheSame(oldItem: AchievementWithStatus, newItem: AchievementWithStatus): Boolean {
            return oldItem == newItem
        }
    }
}
