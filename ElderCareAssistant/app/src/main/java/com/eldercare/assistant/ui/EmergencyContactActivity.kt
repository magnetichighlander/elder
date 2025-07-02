package com.eldercare.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eldercare.assistant.R
import com.eldercare.assistant.databinding.ActivityEmergencyContactBinding
import com.eldercare.assistant.ui.adapter.EmergencyContactAdapter
import com.eldercare.assistant.utils.AccessibilityUtils
import com.eldercare.assistant.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

/**
 * Activity for managing emergency contacts
 * Allows elderly users to easily select and manage their emergency contacts
 */
@AndroidEntryPoint
class EmergencyContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactBinding
    private lateinit var contactAdapter: EmergencyContactAdapter
    private val emergencyContacts = mutableListOf<EmergencyContact>()

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                addContactFromUri(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityEmergencyContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupRecyclerView()
        checkContactsPermission()
    }

    private fun setupUI() {
        // Setup accessibility for all UI elements
        AccessibilityUtils.setupElderlyAccessibility(binding.addContactButton, this)
        AccessibilityUtils.setupElderlyAccessibility(binding.backButton, this)
        
        // Setup click listeners
        binding.addContactButton.setOnClickListener {
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
            
            if (hasContactsPermission()) {
                openContactPicker()
            } else {
                requestContactsPermission()
            }
        }
        
        binding.backButton.setOnClickListener {
            AccessibilityUtils.provideHapticFeedback(this, Constants.HAPTIC_FEEDBACK_DURATION_MS)
            finish()
        }
        
        // Setup help text
        binding.helpText.text = getString(R.string.emergency_contacts_help)
        
        // Announce screen for accessibility
        AccessibilityUtils.announceForAccessibility(
            binding.root,
            "Emergency contacts management screen. Add contacts for emergency situations."
        )
    }

    private fun setupRecyclerView() {
        contactAdapter = EmergencyContactAdapter(
            contacts = emergencyContacts,
            onCallContact = { contact ->
                callContact(contact)
            },
            onRemoveContact = { contact ->
                removeContact(contact)
            }
        )
        
        binding.contactsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@EmergencyContactActivity)
            adapter = contactAdapter
        }
        
        updateEmptyState()
    }

    private fun checkContactsPermission() {
        if (!hasContactsPermission()) {
            binding.permissionWarning.visibility = View.VISIBLE
            binding.addContactButton.isEnabled = false
        } else {
            binding.permissionWarning.visibility = View.GONE
            binding.addContactButton.isEnabled = true
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            CONTACTS_PERMISSION_REQUEST_CODE
        )
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    private fun addContactFromUri(uri: Uri) {
        val cursor: Cursor? = contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                
                if (nameIndex >= 0 && idIndex >= 0) {
                    val name = it.getString(nameIndex)
                    val contactId = it.getString(idIndex)
                    
                    val phoneNumber = getPhoneNumber(contactId)
                    
                    if (phoneNumber.isNotEmpty()) {
                        val contact = EmergencyContact(
                            id = contactId,
                            name = name,
                            phoneNumber = phoneNumber
                        )
                        
                        addEmergencyContact(contact)
                    } else {
                        Toast.makeText(
                            this,
                            "Contact has no phone number",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun getPhoneNumber(contactId: String): String {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (phoneIndex >= 0) {
                    return it.getString(phoneIndex)
                }
            }
        }
        
        return ""
    }

    private fun addEmergencyContact(contact: EmergencyContact) {
        // Check if contact already exists
        if (emergencyContacts.any { it.phoneNumber == contact.phoneNumber }) {
            Toast.makeText(
                this,
                "Contact already added",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Limit to maximum 5 emergency contacts
        if (emergencyContacts.size >= 5) {
            Toast.makeText(
                this,
                "Maximum 5 emergency contacts allowed",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        emergencyContacts.add(contact)
        contactAdapter.notifyItemInserted(emergencyContacts.size - 1)
        updateEmptyState()
        
        // Save to preferences or database
        saveEmergencyContacts()
        
        Toast.makeText(
            this,
            "Emergency contact added: ${contact.name}",
            Toast.LENGTH_SHORT
        ).show()
        
        // Announce for accessibility
        AccessibilityUtils.announceForAccessibility(
            binding.root,
            "Emergency contact ${contact.name} has been added."
        )
    }

    private fun removeContact(contact: EmergencyContact) {
        val position = emergencyContacts.indexOf(contact)
        if (position >= 0) {
            emergencyContacts.removeAt(position)
            contactAdapter.notifyItemRemoved(position)
            updateEmptyState()
            
            // Save updated list
            saveEmergencyContacts()
            
            Toast.makeText(
                this,
                "Emergency contact removed: ${contact.name}",
                Toast.LENGTH_SHORT
            ).show()
            
            // Announce for accessibility
            AccessibilityUtils.announceForAccessibility(
                binding.root,
                "Emergency contact ${contact.name} has been removed."
            )
        }
    }

    private fun callContact(contact: EmergencyContact) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            == PackageManager.PERMISSION_GRANTED) {
            
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
            }
            
            try {
                startActivity(callIntent)
            } catch (e: Exception) {
                // Fallback to dialer
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contact.phoneNumber}")
                }
                startActivity(dialIntent)
            }
        } else {
            // Request call permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun updateEmptyState() {
        if (emergencyContacts.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.contactsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.contactsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun saveEmergencyContacts() {
        // TODO: Implement saving to SharedPreferences or Room database
        // This would store the emergency contacts for use by EmergencyService
    }

    private fun loadEmergencyContacts() {
        // TODO: Implement loading from SharedPreferences or Room database
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CONTACTS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkContactsPermission()
                    Toast.makeText(this, "Contacts permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            CALL_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val CONTACTS_PERMISSION_REQUEST_CODE = 200
        private const val CALL_PERMISSION_REQUEST_CODE = 201
    }
}

/**
 * Data class representing an emergency contact
 */
data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String
)
