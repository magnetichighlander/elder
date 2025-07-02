package com.eldercare.assistant.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * Theme utilities for elderly-friendly application design
 * Provides methods for managing theme settings and accessibility features
 */
object Theme {

    /**
     * Applies elderly-friendly theme settings
     * Forces light theme for better readability and accessibility
     */
    fun applyElderlyFriendlyTheme() {
        // Force light theme for better contrast and readability
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    /**
     * Checks if the device is in dark mode
     * @param context Application context
     * @return true if device is in dark mode, false otherwise
     */
    fun isDarkTheme(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Gets the recommended text size multiplier for elderly users
     * @param context Application context
     * @return multiplier for text sizes (1.0 = normal, > 1.0 = larger)
     */
    fun getTextSizeMultiplier(context: Context): Float {
        val fontScale = context.resources.configuration.fontScale
        // If system font scale is already large, don't multiply further
        return if (fontScale >= 1.3f) 1.0f else 1.2f
    }

    /**
     * Checks if high contrast should be enabled
     * @param context Application context
     * @return true if high contrast should be enabled
     */
    fun shouldUseHighContrast(context: Context): Boolean {
        // Always use high contrast for elderly users
        return true
    }

    /**
     * Gets the recommended touch target size multiplier
     * @return multiplier for touch targets (1.0 = normal, > 1.0 = larger)
     */
    fun getTouchTargetMultiplier(): Float {
        return 1.5f // 50% larger touch targets for elderly users
    }
}
