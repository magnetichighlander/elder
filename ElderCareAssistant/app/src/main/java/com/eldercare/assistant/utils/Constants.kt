package com.eldercare.assistant.utils

/**
 * Application-wide constants for the Elder Care Assistant
 */
object Constants {

    // Accessibility Constants
    const val MIN_TOUCH_TARGET_SIZE_DP = 64
    const val HAPTIC_FEEDBACK_DURATION_MS = 100L
    const val SCREEN_BRIGHTNESS_LEVEL = 0.8f // 80% brightness
    
    // Text Size Multipliers
    const val TEXT_SIZE_MULTIPLIER_ELDERLY = 1.2f
    const val LARGE_TEXT_THRESHOLD = 1.3f
    
    // UI Constants
    const val BUTTON_CORNER_RADIUS_DP = 12
    const val STROKE_WIDTH_DP = 2
    const val ELEVATION_DP = 4
    
    // Layout Constants
    const val SCREEN_PADDING_HORIZONTAL_DP = 24
    const val SCREEN_PADDING_VERTICAL_DP = 16
    const val CONTENT_MARGIN_LARGE_DP = 32
    const val CONTENT_MARGIN_MEDIUM_DP = 24
    const val CONTENT_MARGIN_SMALL_DP = 16
    
    // Animation Constants
    const val FOCUS_ANIMATION_DURATION_MS = 200L
    const val TOUCH_FEEDBACK_ALPHA = 0.8f
    
    // Emergency Constants
    const val EMERGENCY_VIBRATION_DURATION_MS = 500L
    const val EMERGENCY_BUTTON_HOLD_TIME_MS = 2000L
    
    // Preferences Keys
    const val PREF_FONT_SIZE_MULTIPLIER = "font_size_multiplier"
    const val PREF_HIGH_CONTRAST_MODE = "high_contrast_mode"
    const val PREF_HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"
    const val PREF_SCREEN_BRIGHTNESS = "screen_brightness"
    const val PREF_KEEP_SCREEN_ON = "keep_screen_on"
    
    // Intent Actions
    const val ACTION_EMERGENCY_CALL = "com.eldercare.assistant.EMERGENCY_CALL"
    const val ACTION_MAIN_FEATURE = "com.eldercare.assistant.MAIN_FEATURE"
    
    // Permission Request Codes
    const val REQUEST_CALL_PERMISSION = 100
    const val REQUEST_SMS_PERMISSION = 101
    const val REQUEST_LOCATION_PERMISSION = 102
    
    // Color Contrast Ratios (WCAG AA compliance)
    const val MIN_CONTRAST_RATIO_NORMAL = 4.5f
    const val MIN_CONTRAST_RATIO_LARGE = 3.0f
    
    // Touch Target Guidelines
    const val TOUCH_TARGET_MIN_SIZE_DP = 48 // Material Design minimum
    const val TOUCH_TARGET_ELDERLY_SIZE_DP = 64 // Recommended for elderly
    const val TOUCH_TARGET_SPACING_DP = 8
    
    // Font Sizes (in SP)
    const val FONT_SIZE_CAPTION = 16
    const val FONT_SIZE_BODY = 18
    const val FONT_SIZE_BUTTON = 20
    const val FONT_SIZE_TITLE = 24
    const val FONT_SIZE_HEADING = 28
    const val FONT_SIZE_DISPLAY = 32
    
    // Error Messages
    const val ERROR_GENERIC = "Something went wrong. Please try again."
    const val ERROR_NETWORK = "Network connection error. Please check your internet connection."
    const val ERROR_PERMISSION_DENIED = "Permission denied. Please grant the required permissions."
    
    // Success Messages
    const val SUCCESS_EMERGENCY_CALLED = "Emergency services have been contacted."
    const val SUCCESS_SETTINGS_SAVED = "Settings have been saved successfully."
}
