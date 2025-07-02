package com.eldercare.assistant.utils

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

/**
 * Utility class for accessibility features specifically designed for elderly users
 */
object AccessibilityUtils {

    /**
     * Provides haptic feedback for button presses
     * Helps elderly users confirm their interactions
     */
    fun provideHapticFeedback(context: Context, duration: Long = 100L) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    /**
     * Checks if accessibility services are enabled
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isEnabled
    }

    /**
     * Checks if TalkBack (screen reader) is enabled
     */
    fun isTalkBackEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return accessibilityManager.isTouchExplorationEnabled
    }

    /**
     * Sets up a view with elderly-friendly accessibility features
     */
    fun setupAccessibleView(view: View, contentDescription: String) {
        view.contentDescription = contentDescription
        view.isClickable = true
        view.isFocusable = true
        
        // Add visual feedback for focus
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.alpha = 0.8f
            } else {
                v.alpha = 1.0f
            }
        }
    }

    /**
     * Announces text for screen readers
     */
    fun announceForAccessibility(view: View, text: String) {
        view.announceForAccessibility(text)
    }

    /**
     * Checks if the user has enabled large text in system settings
     */
    fun isLargeTextEnabled(context: Context): Boolean {
        val fontScale = context.resources.configuration.fontScale
        return fontScale >= 1.3f
    }

    /**
     * Gets the recommended minimum touch target size for elderly users
     */
    fun getMinimumTouchTargetSize(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (64 * density).toInt() // 64dp minimum for elderly users
    }

    /**
     * Sets up enhanced accessibility for elderly users
     */
    fun setupElderlyAccessibility(view: View, context: Context) {
        // Increase minimum touch target size
        val minSize = getMinimumTouchTargetSize(context)
        view.minimumWidth = minSize
        view.minimumHeight = minSize
        
        // Add haptic feedback on touch
        view.setOnClickListener { v ->
            provideHapticFeedback(context)
            // Continue with original click handling
        }
        
        // Ensure proper focus handling
        view.isFocusable = true
        view.isClickable = true
    }
}
