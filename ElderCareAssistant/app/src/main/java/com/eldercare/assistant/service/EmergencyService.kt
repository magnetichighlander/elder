package com.eldercare.assistant.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.eldercare.assistant.utils.Constants
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emergency Service handles SOS functionality including:
 * - Emergency phone calls
 * - GPS location sharing via SMS
 * - Permission management
 */
@Singleton
class EmergencyService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPrefs: SharedPreferences
) {

    companion object {
        private const val TAG = "EmergencyService"
        private const val DEFAULT_EMERGENCY_NUMBER = "911"
        private const val PREF_EMERGENCY_CONTACT = "emergency_contact_number"
    }

    /**
     * Triggers emergency call with location sharing
     */
    fun triggerEmergency(activity: Activity) {
        // Check and request permissions if needed
        if (!hasRequiredPermissions()) {
            requestPermissions(activity)
            return
        }

        // Make emergency call
        makeEmergencyCall(activity)

        // Share location via SMS
        shareLocationViaSMS()
    }

    /**
     * Makes an emergency phone call
     */
    private fun makeEmergencyCall(activity: Activity) {
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$DEFAULT_EMERGENCY_NUMBER")
        }

        if (hasCallPermission()) {
            try {
                activity.startActivity(callIntent)
            } catch (e: Exception) {
                // Fallback to dialer
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$DEFAULT_EMERGENCY_NUMBER")
                }
                activity.startActivity(dialIntent)
            }
        }
    }

    /**
     * Shares current GPS location via SMS
     */
    private fun shareLocationViaSMS() {
        if (!hasLocationPermission() || !hasSMSPermission()) {
            return
        }

        val emergencyContact = getEmergencyContact()
        if (emergencyContact == null) {
            Log.e(TAG, "No emergency contact configured")
            // Could show user prompt to configure emergency contact
            return
        }

        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val locationMessage = "Emergency! I need help. My location: " +
                            "https://maps.google.com/?q=${it.latitude},${it.longitude}"
                    
                    sendSMS(emergencyContact, locationMessage)
                }
            }
            .addOnFailureListener {
                // Send SMS without location if GPS fails
                val emergencyMessage = "Emergency! I need help. Unable to get location."
                sendSMS(emergencyContact, emergencyMessage)
            }
    }

    /**
     * Gets the configured emergency contact number
     */
    private fun getEmergencyContact(): String? {
        return sharedPrefs.getString(PREF_EMERGENCY_CONTACT, null)
    }

    /**
     * Sets the emergency contact number
     */
    fun setEmergencyContact(phoneNumber: String) {
        sharedPrefs.edit()
            .putString(PREF_EMERGENCY_CONTACT, phoneNumber)
            .apply()
    }

    /**
     * Sends SMS message
     */
    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            
            // Split message if it's too long
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }
        } catch (e: Exception) {
            // Fallback to intent-based SMS
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
        }
    }

    /**
     * Checks if all required permissions are granted
     */
    private fun hasRequiredPermissions(): Boolean {
        return hasCallPermission() && hasLocationPermission() && hasSMSPermission()
    }

    /**
     * Checks if phone call permission is granted
     */
    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if SMS permission is granted
     */
    private fun hasSMSPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests all required permissions
     */
    private fun requestPermissions(activity: Activity) {
        val permissions = mutableListOf<String>()

        if (!hasCallPermission()) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }
        if (!hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasSMSPermission()) {
            permissions.add(Manifest.permission.SEND_SMS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                Constants.REQUEST_CALL_PERMISSION
            )
        }
    }

    /**
     * Handles permission request results
     */
    fun onPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        activity: Activity
    ) {
        when (requestCode) {
            Constants.REQUEST_CALL_PERMISSION,
            Constants.REQUEST_SMS_PERMISSION,
            Constants.REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All permissions granted, trigger emergency
                    triggerEmergency(activity)
                }
            }
        }
    }

    /**
     * Gets current location asynchronously
     */
    fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            callback(null)
            return
        }

        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { location ->
                callback(location)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
