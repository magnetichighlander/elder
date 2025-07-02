package com.eldercare.assistant.utils

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.TypedValue
import androidx.preference.PreferenceManager

/**
 * Accessibility configuration manager for elderly-friendly settings
 * Manages and applies accessibility configurations throughout the app
 */
class AccessibilityConfig(private val context: Context) {

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        // Configuration keys
        private const val KEY_LARGE_TEXT_ENABLED = "large_text_enabled"
        private const val KEY_HIGH_CONTRAST_ENABLED = "high_contrast_enabled"
        private const val KEY_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
        private const val KEY_KEEP_SCREEN_ON_ENABLED = "keep_screen_on_enabled"
        private const val KEY_TOUCH_TARGET_SIZE_MULTIPLIER = "touch_target_size_multiplier"
        
        // Default values for elderly users
        private const val DEFAULT_LARGE_TEXT = true
        private const val DEFAULT_HIGH_CONTRAST = true
        private const val DEFAULT_HAPTIC_FEEDBACK = true
        private const val DEFAULT_KEEP_SCREEN_ON = true
        private const val DEFAULT_TOUCH_TARGET_MULTIPLIER = 1.5f
    }

    /**
     * Checks if large text is enabled
     */
    fun isLargeTextEnabled(): Boolean {
        // Check system setting first
        val systemLargeText = isSystemLargeTextEnabled()
        // Use app preference or system setting
        return preferences.getBoolean(KEY_LARGE_TEXT_ENABLED, systemLargeText || DEFAULT_LARGE_TEXT)
    }

    /**
     * Checks if high contrast mode is enabled
     */
    fun isHighContrastEnabled(): Boolean {
        return preferences.getBoolean(KEY_HIGH_CONTRAST_ENABLED, DEFAULT_HIGH_CONTRAST)
    }

    /**
     * Checks if haptic feedback is enabled
     */
    fun isHapticFeedbackEnabled(): Boolean {
        return preferences.getBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, DEFAULT_HAPTIC_FEEDBACK)
    }

    /**
     * Checks if keep screen on is enabled
     */
    fun isKeepScreenOnEnabled(): Boolean {
        return preferences.getBoolean(KEY_KEEP_SCREEN_ON_ENABLED, DEFAULT_KEEP_SCREEN_ON)
    }

    /**
     * Gets the touch target size multiplier
     */
    fun getTouchTargetSizeMultiplier(): Float {
        return preferences.getFloat(KEY_TOUCH_TARGET_SIZE_MULTIPLIER, DEFAULT_TOUCH_TARGET_MULTIPLIER)
    }

    /**
     * Sets large text preference
     */
    fun setLargeTextEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_LARGE_TEXT_ENABLED, enabled).apply()
    }

    /**
     * Sets high contrast preference
     */
    fun setHighContrastEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HIGH_CONTRAST_ENABLED, enabled).apply()
    }

    /**
     * Sets haptic feedback preference
     */
    fun setHapticFeedbackEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HAPTIC_FEEDBACK_ENABLED, enabled).apply()
    }

    /**
     * Sets keep screen on preference
     */
    fun setKeepScreenOnEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_KEEP_SCREEN_ON_ENABLED, enabled).apply()
    }

    /**
     * Sets touch target size multiplier
     */
    fun setTouchTargetSizeMultiplier(multiplier: Float) {
        preferences.edit().putFloat(KEY_TOUCH_TARGET_SIZE_MULTIPLIER, multiplier).apply()
    }

    /**
     * Applies all elderly-friendly configurations
     */
    fun applyElderlyFriendlySettings() {
        // Set all defaults for elderly users
        if (!hasUserCustomizedSettings()) {
            setLargeTextEnabled(true)
            setHighContrastEnabled(true)
            setHapticFeedbackEnabled(true)
            setKeepScreenOnEnabled(true)
            setTouchTargetSizeMultiplier(1.5f)
            markSettingsAsCustomized()
        }
    }

    /**
     * Gets the scaled text size based on accessibility settings
     */
    fun getScaledTextSize(baseSize: Float): Float {
        val multiplier = if (isLargeTextEnabled()) 1.2f else 1.0f
        return baseSize * multiplier
    }

    /**
     * Gets the scaled touch target size
     */
    fun getScaledTouchTargetSize(baseSize: Int): Int {
        return (baseSize * getTouchTargetSizeMultiplier()).toInt()
    }

    /**
     * Checks if the system has large text enabled
     */
    private fun isSystemLargeTextEnabled(): Boolean {
        val fontScale = context.resources.configuration.fontScale
        return fontScale >= 1.3f
    }

    /**
     * Checks if user has customized accessibility settings
     */
    private fun hasUserCustomizedSettings(): Boolean {
        return preferences.getBoolean("accessibility_settings_customized", false)
    }

    /**
     * Marks that user has customized accessibility settings
     */
    private fun markSettingsAsCustomized() {
        preferences.edit().putBoolean("accessibility_settings_customized", true).apply()
    }

    /**
     * Resets all accessibility settings to defaults
     */
    fun resetToDefaults() {
        preferences.edit().clear().apply()
        applyElderlyFriendlySettings()
    }

    /**
     * Gets accessibility summary for debugging
     */
    fun getAccessibilitySummary(): String {
        return """
            Accessibility Configuration:
            - Large Text: ${isLargeTextEnabled()}
            - High Contrast: ${isHighContrastEnabled()}
            - Haptic Feedback: ${isHapticFeedbackEnabled()}
            - Keep Screen On: ${isKeepScreenOnEnabled()}
            - Touch Target Multiplier: ${getTouchTargetSizeMultiplier()}
            - System Font Scale: ${context.resources.configuration.fontScale}
        """.trimIndent()
    }
}
