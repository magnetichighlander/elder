package com.eldercare.assistant.service

import android.content.Context
import android.util.Log
import com.eldercare.assistant.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * Voice Command Processor for parsing vocabulary and matching spoken commands
 * Loads commands from voice_commands.txt and provides intelligent matching
 */
class VoiceCommandProcessor(private val context: Context) {
    
    private val commandMap = mutableMapOf<String, MutableList<String>>()
    private val TAG = "VoiceCommandProcessor"
    
    init {
        loadVocabulary()
    }
    
    /**
     * Load voice commands vocabulary from raw resource file
     */
    private fun loadVocabulary() {
        try {
            val inputStream = context.resources.openRawResource(R.raw.voice_commands)
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            
            var currentCategory: String? = null
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmedLine = line.trim()
                    
                    when {
                        // Category header [CategoryName]
                        trimmedLine.startsWith("[") && trimmedLine.endsWith("]") -> {
                            currentCategory = trimmedLine.substring(1, trimmedLine.length - 1)
                            commandMap[currentCategory!!] = mutableListOf()
                        }
                        // Command phrase
                        trimmedLine.isNotEmpty() && currentCategory != null -> {
                            commandMap[currentCategory]?.add(trimmedLine.lowercase(Locale.getDefault()))
                        }
                    }
                }
            }
            
            Log.d(TAG, "Loaded ${commandMap.size} command categories")
            commandMap.forEach { (category, commands) ->
                Log.d(TAG, "Category '$category': ${commands.size} commands")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading voice commands vocabulary: ${e.message}")
            // Fallback to basic commands if vocabulary file fails to load
            loadDefaultCommands()
        }
    }
    
    /**
     * Fallback method to load basic commands if vocabulary file is not available
     */
    private fun loadDefaultCommands() {
        commandMap["OpenReminders"] = mutableListOf(
            "открой напоминания", "напомни о лекарстве", "таблетки", "лекарства"
        )
        commandMap["Emergency"] = mutableListOf(
            "sos", "помогите", "экстренный вызов", "скорая"
        )
        commandMap["CallMom"] = mutableListOf(
            "позвони маме", "свяжись с мамой", "звонок маме"
        )
        commandMap["Stop"] = mutableListOf(
            "стоп", "остановись", "хватит", "отмена"
        )
    }
    
    /**
     * Process a spoken command and return the matched category
     * Uses fuzzy matching to handle speech recognition variations
     */
    fun processCommand(spokenText: String): String? {
        val normalizedInput = spokenText.lowercase(Locale.getDefault()).trim()
        
        Log.d(TAG, "Processing command: '$normalizedInput'")
        
        // First try exact matching
        commandMap.forEach { (category, commands) ->
            commands.forEach { command ->
                if (normalizedInput == command) {
                    Log.d(TAG, "Exact match found: '$command' -> $category")
                    return mapCategoryToAction(category)
                }
            }
        }
        
        // Then try substring matching
        commandMap.forEach { (category, commands) ->
            commands.forEach { command ->
                if (normalizedInput.contains(command) || command.contains(normalizedInput)) {
                    Log.d(TAG, "Substring match found: '$command' -> $category")
                    return mapCategoryToAction(category)
                }
            }
        }
        
        // Finally try fuzzy matching for individual words
        val inputWords = normalizedInput.split("\\s+".toRegex())
        commandMap.forEach { (category, commands) ->
            commands.forEach { command ->
                val commandWords = command.split("\\s+".toRegex())
                val matchScore = calculateMatchScore(inputWords, commandWords)
                if (matchScore > 0.6) { // 60% similarity threshold
                    Log.d(TAG, "Fuzzy match found: '$command' -> $category (score: $matchScore)")
                    return mapCategoryToAction(category)
                }
            }
        }
        
        Log.d(TAG, "No match found for: '$normalizedInput'")
        return null
    }
    
    /**
     * Calculate similarity score between two word lists
     */
    private fun calculateMatchScore(words1: List<String>, words2: List<String>): Double {
        val matchingWords = words1.intersect(words2.toSet()).size
        val totalWords = maxOf(words1.size, words2.size)
        return if (totalWords > 0) matchingWords.toDouble() / totalWords else 0.0
    }
    
    /**
     * Map vocabulary categories to action commands
     */
    private fun mapCategoryToAction(category: String): String {
        return when (category) {
            "OpenReminders" -> "open_medication"
            "CallMom" -> "call_mom"
            "CallDad" -> "call_dad"
            "CallDoctor" -> "call_doctor"
            "Emergency" -> "sos"
            "Mood" -> "open_mood"
            "Exercises" -> "open_exercises"
            "Contacts" -> "open_contacts"
            "Settings" -> "open_settings"
            "Stop" -> "stop"
            "Repeat" -> "repeat"
            else -> "unrecognized"
        }
    }
    
    /**
     * Get all available commands for a specific category
     */
    fun getCommandsForCategory(category: String): List<String> {
        return commandMap[category] ?: emptyList()
    }
    
    /**
     * Get all available categories
     */
    fun getAllCategories(): Set<String> {
        return commandMap.keys
    }
    
    /**
     * Get a summary of all loaded commands (for debugging)
     */
    fun getCommandsSummary(): String {
        val summary = StringBuilder()
        commandMap.forEach { (category, commands) ->
            summary.append("$category: ${commands.joinToString(", ")}\n")
        }
        return summary.toString()
    }
}
