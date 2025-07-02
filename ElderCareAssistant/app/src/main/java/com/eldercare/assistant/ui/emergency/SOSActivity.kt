package com.eldercare.assistant.ui.emergency

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.eldercare.assistant.R
import com.eldercare.assistant.databinding.ActivitySosBinding
import com.eldercare.assistant.service.EmergencyService
import com.eldercare.assistant.utils.AccessibilityUtils
import com.eldercare.assistant.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dedicated SOS Activity for emergency situations
 * This activity can be launched quickly for urgent help
 */
@AndroidEntryPoint
class SOSActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySosBinding
    
    @Inject
    lateinit var emergencyService: EmergencyService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up for emergency situations
        configureEmergencyWindow()
        
        binding = ActivitySosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupEmergencyUI()
    }

    /**
     * Configure window for emergency situations
     */
    private fun configureEmergencyWindow() {
        // Keep screen on and bright for emergency
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        
        // Set maximum brightness for visibility
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f
        window.attributes = layoutParams
    }

    /**
     * Set up the emergency UI elements
     */
    private fun setupEmergencyUI() {
        // Configure the SOS button with large touch target
        binding.emergencySosButton.setOnClickListener {
            // Provide strong haptic feedback
            AccessibilityUtils.provideHapticFeedback(this, Constants.EMERGENCY_VIBRATION_DURATION_MS)
            
            // Show confirmation dialog
            showEmergencyConfirmation()
        }
        
        // Configure cancel button
        binding.cancelButton.setOnClickListener {
            finish()
        }
        
        // Set up accessibility
        AccessibilityUtils.setupElderlyAccessibility(binding.emergencySosButton, this)
        AccessibilityUtils.setupElderlyAccessibility(binding.cancelButton, this)
        
        // Announce emergency screen for accessibility
        AccessibilityUtils.announceForAccessibility(
            binding.root,
            "Emergency SOS screen. Tap the large red button to call for help."
        )
    }

    /**
     * Show emergency confirmation dialog
     */
    private fun showEmergencyConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.sos_emergency_title)
            .setMessage(R.string.sos_confirm_message)
            .setPositiveButton(R.string.call_emergency) { dialog, _ ->
                dialog.dismiss()
                emergencyService.triggerEmergency(this)
                
                // Show progress indicator
                binding.emergencyProgressBar.visibility = View.VISIBLE
                binding.emergencyStatusText.text = getString(R.string.sos_calling_emergency)
                binding.emergencyStatusText.visibility = View.VISIBLE
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        emergencyService.onPermissionResult(requestCode, permissions, grantResults, this)
    }

    /**
     * Prevent back button from closing emergency screen accidentally
     */
    override fun onBackPressed() {
        // Show confirmation before closing emergency screen
        AlertDialog.Builder(this)
            .setTitle("Exit Emergency Screen")
            .setMessage("Are you sure you want to exit the emergency screen?")
            .setPositiveButton("Exit") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Stay", null)
            .show()
    }
}
