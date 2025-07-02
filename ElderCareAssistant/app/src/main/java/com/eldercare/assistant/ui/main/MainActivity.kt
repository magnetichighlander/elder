package com.eldercare.assistant.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibrationEffect
import android.preference.PreferenceManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView
import com.eldercare.assistant.databinding.ActivityMainBinding
import com.eldercare.assistant.utils.AccessibilityUtils
import com.eldercare.assistant.utils.AccessibilityConfig
import com.eldercare.assistant.utils.Constants
import com.eldercare.assistant.R
import com.eldercare.assistant.service.EmergencyService
import com.eldercare.assistant.service.TTSHelper
import com.eldercare.assistant.service.VoiceAssistant
import com.eldercare.assistant.ui.EmergencyContactActivity
import com.eldercare.assistant.ui.exercise.ExerciseActivity
import com.eldercare.assistant.backup.BackupManager
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var safetyOverlay: View
    private lateinit var sosButton: LottieAnimationView
    private var emergencyContact = "112"
    private val PERMISSION_REQUEST_CODE = 101
    private lateinit var gestureDetector: GestureDetector
    private lateinit var ttsHelper: TTSHelper
    private lateinit var voiceAssistant: VoiceAssistant
    private var isListening = false
    
    @Inject
    lateinit var accessibilityConfig: AccessibilityConfig
    
    @Inject
    lateinit var emergencyService: EmergencyService
    
    @Inject
    lateinit var backupManager: BackupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply accessibility font scale settings
        applyFontScaleSettings()
        
        // Enable view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Increase font size for accessibility
        resources.configuration.fontScale = 1.2f
        
        // Initialize UI elements
        sosButton = binding.sosButton
        safetyOverlay = binding.safetyOverlay
        
        // Load emergency contact
        emergencyContact = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("EMERGENCY_CONTACT", "112") ?: "112"
            
// Initialize TTSHelper
        ttsHelper = TTSHelper(this)

        // Initialize VoiceAssistant
        voiceAssistant = VoiceAssistant(this) { command ->
            runOnUiThread {
                handleVoiceCommand(command)
            }
        }
        
        // Configure gesture detector
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (checkEmergencyPermissions()) {
                    activateEmergencyMode()
                }
            }
        })
        
        // Configure window for elderly-friendly features
        configureAccessibilityFeatures()
        
        // Check accessibility services and prompt if needed
        checkAccessibilityServices()
        
        // Set up edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupUI()
        
        // Provide feedback about accessibility configuration
        provideAccessibilityFeedback()
        
        // Check permissions on startup
        checkEmergencyPermissions()
        
        // Check for app version update and restore if needed
        checkVersionAndRestore()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check accessibility status when returning from settings
        checkAccessibilityStatusOnResume()
    }
    
    /**
     * Applies font scale settings for elderly users
     * Configures the display to use larger text for better readability
     */
    private fun applyFontScaleSettings() {
        // Apply font scale based on accessibility configuration
        val configuration = resources.configuration
        val currentFontScale = configuration.fontScale
        
        // If current font scale is less than optimal for elderly users, apply enhancement
        if (currentFontScale < Constants.TEXT_SIZE_MULTIPLIER_ELDERLY) {
            configuration.fontScale = Constants.TEXT_SIZE_MULTIPLIER_ELDERLY
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }
    
    /**
     * Checks if accessibility services are enabled and prompts user if needed
     * This helps ensure elderly users have proper accessibility support
     */
    private fun checkAccessibilityServices() {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        
        if (!accessibilityManager.isEnabled) {
            AlertDialog.Builder(this)
                .setTitle(R.string.accessibility_title)
                .setMessage(R.string.accessibility_message)
                .setPositiveButton(R.string.enable) { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton(R.string.not_now, null)
                .setCancelable(true)
                .show()
        }
    }
    
    private fun configureAccessibilityFeatures() {
        // Apply elderly-friendly accessibility settings
        accessibilityConfig.applyElderlyFriendlySettings()
        
        // Keep screen on if enabled in settings
        if (accessibilityConfig.isKeepScreenOnEnabled()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        
        // Set brightness to a comfortable level for elderly users
        val layoutParams = window.attributes
        layoutParams.screenBrightness = Constants.SCREEN_BRIGHTNESS_LEVEL
        window.attributes = layoutParams
    }
    
    private fun setupUI() {
        // Setup welcome message
        binding.welcomeText.text = getString(R.string.welcome_message)
        
        // Setup accessibility for welcome text
        AccessibilityUtils.setupAccessibleView(
            binding.welcomeText,
            getString(R.string.welcome_message)
        )
        
        // Setup main action button with enhanced accessibility
        AccessibilityUtils.setupElderlyAccessibility(binding.mainActionButton, this)
        binding.mainActionButton.setOnClickListener {
            // Provide haptic feedback
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
            
            // Announce action for screen readers
            AccessibilityUtils.announceForAccessibility(
                binding.mainActionButton,
                "Main feature activated"
            )
            
            // TODO: Implement main action functionality
            // This could navigate to the main features of the app
        }
        
        // Set up SOS button with enhanced gesture detection
        sosButton.setAnimation(R.raw.sos_animation)
        sosButton.playAnimation()
        sosButton.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> sosButton.speed = 2.0f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> sosButton.speed = 1.0f
            }
            true
        }
        
        // Setup SOS button click with confirmation
        binding.sosButton.setOnClickListener {
            // Prevent accidental taps by enabling the safety overlay
            binding.safetyOverlay.visibility = View.VISIBLE

            // Provide haptic feedback
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)

            // Confirm emergency action
            AlertDialog.Builder(this)
                .setTitle(R.string.sos_emergency_title)
                .setMessage(R.string.sos_confirm_message)
                .setPositiveButton(R.string.call_emergency) { dialog, _ ->
                    dialog.dismiss()
                    activateEmergencyMode()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    // Disable safety overlay when dialog is dismissed
                    binding.safetyOverlay.visibility = View.GONE
                }
                .show()
        }
        
        // Setup emergency button with enhanced accessibility
        AccessibilityUtils.setupElderlyAccessibility(binding.emergencyButton, this)
        binding.emergencyButton.setOnClickListener {
            // Provide stronger haptic feedback for emergency
            AccessibilityUtils.provideHapticFeedback(this, Constants.EMERGENCY_VIBRATION_DURATION_MS)
            
            // Announce emergency action
            AccessibilityUtils.announceForAccessibility(
                binding.emergencyButton,
                "Emergency help activated"
            )
            
            // Launch dedicated SOS Activity
            val sosIntent = Intent(this, com.eldercare.assistant.ui.emergency.SOSActivity::class.java)
            startActivity(sosIntent)
        }
        
        // Add voice button functionality
        binding.voiceButton.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                startListening()
            }
        }
        
        // Exercise button
        binding.exerciseButton.setOnClickListener {
            // Provide haptic feedback
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
            
            // Announce action for screen readers
            AccessibilityUtils.announceForAccessibility(
                binding.exerciseButton,
                "Opening exercise and wellness features"
            )
            
            // Launch exercise activity
            startActivity(Intent(this, ExerciseActivity::class.java))
        }
        
        // Messaging button
        binding.messagingButton.setOnClickListener {
            // Provide haptic feedback
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
            
            // Announce action for screen readers
            AccessibilityUtils.announceForAccessibility(
                binding.messagingButton,
                "Opening messaging features"
            )
            
            // Launch message editor activity
            startActivity(Intent(this, com.eldercare.assistant.ui.messaging.MessageEditorActivity::class.java))
        }
        
        // Settings button to manage emergency contacts
        if (::binding.isInitialized && binding.root.findViewById<View>(R.id.btnSettings) != null) {
            binding.root.findViewById<View>(R.id.btnSettings).setOnClickListener {
                startActivity(Intent(this, EmergencyContactActivity::class.java))
            }
        }
    }
    
    /**
     * Provides feedback about the current accessibility configuration
     * Informs the user about enabled accessibility features
     */
    private fun provideAccessibilityFeedback() {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        
        // Check if accessibility services are enabled and provide feedback
        if (accessibilityManager.isEnabled) {
            if (AccessibilityUtils.isTalkBackEnabled(this)) {
                // Announce accessibility status for screen readers
                AccessibilityUtils.announceForAccessibility(
                    binding.root,
                    getString(R.string.accessibility_services_enabled)
                )
            }
        }
        
        // Provide feedback about font size enhancement
        if (resources.configuration.fontScale >= Constants.TEXT_SIZE_MULTIPLIER_ELDERLY) {
            // Show a brief toast about enhanced text size (not intrusive)
            if (!AccessibilityUtils.isTalkBackEnabled(this)) {
                Toast.makeText(
                    this,
                    getString(R.string.font_size_increased),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Checks accessibility status when user returns from settings
     * Provides positive feedback if accessibility services were enabled
     */
    private fun checkAccessibilityStatusOnResume() {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        
        if (accessibilityManager.isEnabled) {
            // User has enabled accessibility services
            AccessibilityUtils.announceForAccessibility(
                binding.root,
                "Accessibility services are now enabled. The app is optimized for your use."
            )
        }
    }
    
    /**
     * Demonstrates accessibility features for testing
     * This method can be called during development to test accessibility features
     */
    private fun demonstrateAccessibilityFeatures() {
        // Log accessibility configuration for debugging
        android.util.Log.d("Accessibility", accessibilityConfig.getAccessibilitySummary())
        
        // Test haptic feedback
        AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
        
        // Test screen reader announcement
        AccessibilityUtils.announceForAccessibility(
            binding.root,
            "Elder Care Assistant is ready to use with accessibility features enabled."
        )
    }

    /**
     * Check if all emergency permissions are granted
     */
    private fun checkEmergencyPermissions(): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        return if (missingPermissions.isEmpty()) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            false
        }
    }
    
    /**
     * Activate emergency mode with full emergency sequence
     */
    private fun activateEmergencyMode() {
        // Activate safety overlay
        safetyOverlay.visibility = View.VISIBLE
        
        // Execute emergency sequence
        makeEmergencyCall()
        sendLocationSMS()
        startAlarm()
        
        // Voice confirmation
        ttsHelper.speak("Emergency activated! Calling $emergencyContact")
        
        // Auto-disable after 30 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            safetyOverlay.visibility = View.GONE
        }, 30000)
    }
    
    /**
     * Start voice listening
     */
    private fun startListening() {
        if (voiceAssistant.isAvailable()) {
            voiceAssistant.startListening()
            binding.voiceButton.setImageResource(R.drawable.ic_mic_active)
            ttsHelper.speak("Слушаю")
            isListening = true
        } else {
            Toast.makeText(this, "Voice recognition not available", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Stop voice listening
     */
    private fun stopListening() {
        voiceAssistant.stopListening()
        binding.voiceButton.setImageResource(R.drawable.ic_mic)
        isListening = false
    }
    
    /**
     * Handle voice commands from the VoiceAssistant
     */
    private fun handleVoiceCommand(command: String) {
        when (command) {
            "ready" -> {
                // Voice recognition is ready - provide feedback
                ttsHelper.speak("Готов к команде")
            }
            "open_medication" -> {
                ttsHelper.speak("Открываю напоминания о лекарствах")
                startActivity(Intent(this, com.eldercare.assistant.ui.medication.MedicationReminderActivity::class.java))
            }
            "sos" -> {
                ttsHelper.speak("Активирую экстренный вызов")
                activateEmergencyMode()
            }
            "open_mood" -> {
                ttsHelper.speak("Открываю дневник настроения")
                startActivity(Intent(this, com.eldercare.assistant.ui.mood.MoodTrackingActivity::class.java))
            }
            "open_exercises" -> {
                ttsHelper.speak("Открываю упражнения")
                startActivity(Intent(this, ExerciseActivity::class.java))
            }
            "open_contacts" -> {
                ttsHelper.speak("Открываю контакты")
                startActivity(Intent(this, EmergencyContactActivity::class.java))
            }
            "open_settings" -> {
                ttsHelper.speak("Открываю настройки")
                startActivity(Intent(this, EmergencyContactActivity::class.java)) // For now, settings = contacts
            }
            "call_mom", "call_dad", "call_doctor", "call_emergency" -> {
                val contactType = command.substringAfter("call_")
                ttsHelper.speak("Звоню $contactType")
                makeEmergencyCall() // For now, use emergency contact
            }
            "no_match" -> {
                ttsHelper.speak("Не расслышала, повторите пожалуйста")
            }
            "unrecognized" -> {
                ttsHelper.speak("Не поняла команду")
            }
            "stop" -> {
                ttsHelper.speak("Останавливаю")
                voiceAssistant.stopListening()
            }
            "timeout", "network_timeout", "audio_error" -> {
                ttsHelper.speak("Ошибка распознавания речи")
            }
            "permission_error" -> {
                ttsHelper.speak("Нет разрешения на использование микрофона")
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show()
            }
            else -> {
                if (command.startsWith("error_")) {
                    ttsHelper.speak("Произошла ошибка")
                    Log.e("VoiceCommand", "Voice recognition error: $command")
                }
            }
        }
    }
    
    /**
     * Make emergency call
     */
    private fun makeEmergencyCall() {
        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$emergencyContact")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(callIntent)
        } catch (e: SecurityException) {
            Log.e("SOS", "Call permission denied")
            ttsHelper.speak("Cannot make call. Please check permissions.")
        }
    }
    
    /**
     * Send location via SMS
     */
    private fun sendLocationSMS() {
        val locationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    try {
                        val smsManager = SmsManager.getDefault()
                        val message = "EMERGENCY! My location: https://maps.google.com/?q=${it.latitude},${it.longitude}"
                        smsManager.sendTextMessage(emergencyContact, null, message, null, null)
                    } catch (e: Exception) {
                        Log.e("SOS", "SMS failed: ${e.message}")
                        ttsHelper.speak("Failed to send location.")
                    }
                } ?: run {
                    Log.e("SOS", "No location available")
                    ttsHelper.speak("Location not available.")
                }
            }
        }
    }
    
    /**
     * Start emergency alarm with vibration
     */
    private fun startAlarm() {
        try {
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 1000), 0)
            }
        } catch (e: Exception) {
            Log.e("SOS", "Vibration failed: ${e.message}")
        }
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Emergency permissions granted", Toast.LENGTH_SHORT).show()
                ttsHelper.speak("Emergency features activated")
            }
        }
        emergencyService.onPermissionResult(requestCode, permissions, grantResults, this)
    }
    
    /**
     * Clean up resources
     */
    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
        voiceAssistant.destroy()
    }
    
    /**
     * Handle app exit to create backup
     */
    override fun onStop() {
        super.onStop()
        // Create backup when app goes to background
        createBackupOnExit()
    }
    
    /**
     * Check app version and restore data if version updated
     */
    private fun checkVersionAndRestore() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = packageInfo.versionCode
            
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val savedVersion = prefs.getInt("APP_VERSION_CODE", -1)
            
            if (savedVersion != -1 && savedVersion != currentVersion) {
                // App was updated, check if restore is needed
                Log.d("Backup", "App updated from version $savedVersion to $currentVersion")
                
                // Show dialog asking user if they want to restore data
                AlertDialog.Builder(this)
                    .setTitle("Восстановление данных")
                    .setMessage("Приложение было обновлено. Хотите восстановить данные из резервной копии?")
                    .setPositiveButton("Восстановить") { _, _ ->
                        restoreDataFromBackup()
                    }
                    .setNegativeButton("Пропустить") { _, _ ->
                        // Update version code to current
                        prefs.edit().putInt("APP_VERSION_CODE", currentVersion).apply()
                    }
                    .setCancelable(false)
                    .show()
            } else if (savedVersion == -1) {
                // First install, save current version
                prefs.edit().putInt("APP_VERSION_CODE", currentVersion).apply()
                Log.d("Backup", "First install, saved version code: $currentVersion")
            }
        } catch (e: Exception) {
            Log.e("Backup", "Error checking app version: ${e.message}")
        }
    }
    
    /**
     * Restore data from backup
     */
    private fun restoreDataFromBackup() {
        try {
            val success = backupManager.restoreData()
            if (success) {
                ttsHelper.speak("Данные успешно восстановлены")
                Toast.makeText(this, "Данные восстановлены из резервной копии", Toast.LENGTH_LONG).show()
                
                // Update version code after successful restore
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val currentVersion = packageInfo.versionCode
                PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putInt("APP_VERSION_CODE", currentVersion).apply()
                
                Log.d("Backup", "Data restored successfully")
            } else {
                ttsHelper.speak("Не удалось восстановить данные")
                Toast.makeText(this, "Ошибка восстановления данных", Toast.LENGTH_LONG).show()
                Log.e("Backup", "Failed to restore data")
            }
        } catch (e: Exception) {
            Log.e("Backup", "Error during restore: ${e.message}")
            ttsHelper.speak("Ошибка при восстановлении данных")
            Toast.makeText(this, "Ошибка при восстановлении: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Create backup when app exits
     */
    private fun createBackupOnExit() {
        try {
            // Create backup in background thread to avoid blocking UI
            Thread {
                try {
                    val success = backupManager.createBackup()
                    if (success) {
                        Log.d("Backup", "Exit backup created successfully")
                    } else {
                        Log.e("Backup", "Failed to create exit backup")
                    }
                } catch (e: Exception) {
                    Log.e("Backup", "Error creating exit backup: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            Log.e("Backup", "Error initiating exit backup: ${e.message}")
        }
    }
}
