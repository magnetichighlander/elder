package com.eldercare.assistant.ui.mood

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import java.util.Locale
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entity.MoodEntry
import com.eldercare.assistant.data.entity.MoodType
import com.eldercare.assistant.databinding.ActivityMoodTrackingBinding
import com.eldercare.assistant.viewmodel.MoodViewModel
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.utils.ColorTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MoodTrackingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMoodTrackingBinding
    private lateinit var viewModel: MoodViewModel
    private var selectedMood: MoodType = MoodType.CALM
    private lateinit var textToSpeech: TextToSpeech
    private var isTTSReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoodTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[MoodViewModel::class.java]
        
        setupTextToSpeech()
        setupMoodGrid()
        setupChart()
        setupSaveButton()
        setupAccessibility()
        observeEntries()
    }
    
    private fun setupMoodGrid() {
        val moods = MoodType.values()
        val adapter = object : BaseAdapter() {
            override fun getCount() = moods.size
            override fun getItem(position: Int) = moods[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val mood = moods[position]
                val view = convertView ?: layoutInflater.inflate(R.layout.item_mood_grid, parent, false)
                view.findViewById<TextView>(R.id.emoji).text = mood.emoji
                view.findViewById<TextView>(R.id.label).text = mood.displayName
                view.isSelected = (mood == selectedMood)
                
                // Visual feedback for selected item
                if (mood == selectedMood) {
                    view.setBackgroundResource(R.drawable.button_primary)
                } else {
                    view.setBackgroundResource(android.R.color.transparent)
                }
                
                // Set accessibility content description
                view.contentDescription = "${mood.displayName} mood. ${if (mood == selectedMood) "Currently selected" else "Tap to select"}"
                
                return view
            }
        }
        
        binding.moodGrid.adapter = adapter
        binding.moodGrid.setOnItemClickListener { _, _, position, _ ->
            selectedMood = moods[position]
            adapter.notifyDataSetChanged()
            
            // Voice feedback for mood selection
            speakText("Selected ${selectedMood.displayName}")
        }
    }
    
    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val energy = binding.energySeekBar.progress + 1
            val note = binding.etNote.text.toString().takeIf { it.isNotBlank() }
            
            val entry = MoodEntry(
                moodType = selectedMood,
                energyLevel = energy,
                note = note
            )
            
            viewModel.insertEntry(entry)
            Toast.makeText(this, "Mood saved!", Toast.LENGTH_SHORT).show()
            binding.etNote.text.clear()
            
            // Voice feedback for successful save
            speakText("Mood saved for today")
        }
    }
    
    private fun setupChart() {
        with(binding.moodChart) {
            description.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val date = LocalDate.now().minusDays(value.toLong())
                    return date.format(DateTimeFormatter.ofPattern("MMM dd"))
                }
            }
            
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = MoodType.values().size.toFloat()
            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return MoodType.values().getOrNull(value.toInt())?.emoji ?: ""
                }
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }
    
    private fun observeEntries() {
        viewModel.lastWeekEntries.observe(this) { entries ->
            if (entries.isEmpty()) return@observe
            
            val values = entries.takeLast(7).mapIndexed { index, entry ->
                Entry(
                    index.toFloat(), 
                    MoodType.values().indexOf(entry.moodType).toFloat()
                )
            }
            
            val set = LineDataSet(values, "Mood").apply {
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                setDrawCircles(true)
                circleRadius = 6f
                lineWidth = 3f
                color = ContextCompat.getColor(this@MoodTrackingActivity, android.R.color.holo_blue_bright)
                circleColors = listOf(ColorTemplate.COLORFUL_COLORS[0])
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return MoodType.values().getOrNull(value.toInt())?.emoji ?: ""
                    }
                }
            }
            
            binding.moodChart.data = LineData(set)
            binding.moodChart.invalidate()
        }
    }
    
    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.getDefault())
                isTTSReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                
                if (isTTSReady) {
                    // Welcome message
                    speakText("Mood tracking ready. Select your current mood.")
                }
            }
        }
    }
    
    private fun setupAccessibility() {
        // Add content descriptions for accessibility
        binding.moodGrid.contentDescription = "Select your current mood from the available options"
        binding.energySeekBar.contentDescription = "Adjust your energy level from 1 to 5"
        binding.etNote.contentDescription = "Add an optional note about your mood"
        binding.btnSave.contentDescription = "Save your mood entry"
        binding.moodChart.contentDescription = "Your mood history for the last 7 days"
        
        // Add tactile feedback to SeekBar
        binding.energySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // Provide haptic feedback
                    seekBar?.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                    
                    // Voice feedback for energy level
                    val energyLevel = progress + 1
                    val energyText = when (energyLevel) {
                        1 -> "Very low energy"
                        2 -> "Low energy"
                        3 -> "Medium energy"
                        4 -> "High energy"
                        5 -> "Very high energy"
                        else -> "Medium energy"
                    }
                    speakText(energyText)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                speakText("Adjusting energy level")
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Add focus listeners for better navigation
        binding.btnSave.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                speakText("Save button focused. Press to save your mood entry.")
            }
        }
    }
    
    private fun speakText(text: String) {
        if (isTTSReady) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}

