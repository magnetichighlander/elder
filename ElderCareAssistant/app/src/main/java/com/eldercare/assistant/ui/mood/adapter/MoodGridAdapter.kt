package com.eldercare.assistant.ui.mood.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.MoodType

class MoodGridAdapter(
    private val context: Context,
    private val moodTypes: List<MoodType>,
    private val onMoodSelected: (MoodType) -> Unit
) : BaseAdapter() {

    private var selectedPosition: Int = -1

    override fun getCount(): Int = moodTypes.size

    override fun getItem(position: Int): MoodType = moodTypes[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_mood_grid, parent, false)

        val moodType = moodTypes[position]
        val emojiTextView = view.findViewById<TextView>(R.id.emoji)
        val nameTextView = view.findViewById<TextView>(R.id.label)

        emojiTextView.text = moodType.emoji
        nameTextView.text = moodType.displayName

        // Set selection state
        view.isSelected = position == selectedPosition
        view.setBackgroundResource(
            if (position == selectedPosition) 
                R.drawable.button_primary 
            else 
                android.R.color.transparent
        )

        view.setOnClickListener {
            val previousSelection = selectedPosition
            selectedPosition = position
            onMoodSelected(moodType)
            
            // Refresh the views to update selection state
            notifyDataSetChanged()
        }

        return view
    }

    fun setSelectedMood(moodType: MoodType?) {
        selectedPosition = if (moodType != null) {
            moodTypes.indexOf(moodType)
        } else {
            -1
        }
        notifyDataSetChanged()
    }
}
