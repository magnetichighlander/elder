package com.eldercare.assistant

import android.app.Application
import com.eldercare.assistant.ui.theme.Theme
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Elder Care Assistant
 * Initializes Hilt dependency injection and applies elderly-friendly configurations
 */
@HiltAndroidApp
class ElderCareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Apply elderly-friendly theme settings globally
        Theme.applyElderlyFriendlyTheme()
    }
}
