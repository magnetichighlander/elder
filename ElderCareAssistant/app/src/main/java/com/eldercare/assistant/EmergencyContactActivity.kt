package com.eldercare.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eldercare.assistant.databinding.ActivityEmergencyContactBinding
import java.util.*

class EmergencyContactActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmergencyContactBinding
    private lateinit var tts: TextToSpeech
    private val CONTACTS_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) tts.language = Locale.getDefault()
        }
        
        // Setup UI
        setupUI()
        
        // Check permissions and load contacts
        checkPermissionsAndLoadContacts()
    }
    
    private fun setupUI() {
        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // Add contact button
        binding.addContactButton.setOnClickListener {
            if (hasContactsPermission()) {
                startContactSelectionActivity()
            } else {
                requestContactsPermission()
            }
        }
    }
    
    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestContactsPermission() {
        binding.permissionWarning.visibility = View.VISIBLE
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            CONTACTS_PERMISSION_REQUEST_CODE
        )
    }
    
    private fun checkPermissionsAndLoadContacts() {
        if (hasContactsPermission()) {
            binding.permissionWarning.visibility = View.GONE
            loadSavedContacts()
        } else {
            binding.permissionWarning.visibility = View.VISIBLE
        }
    }
    
    private fun startContactSelectionActivity() {
        val intent = Intent(this, ContactSelectionActivity::class.java)
        startActivity(intent)
    }
    
    private fun loadSavedContacts() {
        // Load saved emergency contacts from preferences
        val savedContacts = mutableListOf<Contact>()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val emergencyContact = prefs.getString("EMERGENCY_CONTACT", null)
        val emergencyName = prefs.getString("EMERGENCY_CONTACT_NAME", "Emergency Contact")
        
        if (emergencyContact != null) {
            savedContacts.add(Contact(emergencyName ?: "Emergency Contact", emergencyContact))
        }
        
        // Setup RecyclerView
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = SavedContactAdapter(savedContacts) { contact ->
            // Handle contact actions (call, remove)
        }
        
        // Show/hide empty state
        if (savedContacts.isEmpty()) {
            binding.emptyStateText.visibility = View.VISIBLE
            binding.contactsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateText.visibility = View.GONE
            binding.contactsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CONTACTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.permissionWarning.visibility = View.GONE
                loadSavedContacts()
            } else {
                Toast.makeText(this, "Contacts permission is required to add emergency contacts", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload contacts when returning from contact selection
        if (hasContactsPermission()) {
            loadSavedContacts()
        }
    }
    
    private fun loadContacts() {
        val contacts = mutableListOf<Contact>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )
        
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (it.moveToNext()) {
                val name = it.getString(nameIndex)?.takeIf { it.isNotBlank() }
                val phone = it.getString(phoneIndex)?.filter { it.isDigit() }?.takeIf { it.length > 5 }
                
                if (name != null && phone != null) {
                    contacts.add(Contact(name, phone))
                }
            }
        }
        
        // Setup RecyclerView
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = ContactAdapter(contacts) { contact ->
            saveEmergencyContact(contact)
            finish()
        }
    }
    
    private fun saveEmergencyContact(contact: Contact) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString("EMERGENCY_CONTACT", contact.phone)
            .apply()
        
        // Voice confirmation
        tts.speak("Emergency contact set to ${contact.name}", TextToSpeech.QUEUE_FLUSH, null, "")
        Toast.makeText(this, "Set emergency contact: ${contact.name}", Toast.LENGTH_SHORT).show()
    }
    
    data class Contact(val name: String, val phone: String)
    
    class ContactAdapter(
        private val contacts: List<Contact>,
        private val onClick: (Contact) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.contactName)
            val phone: TextView = view.findViewById(R.id.contactPhone)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_contact, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = contacts[position]
            holder.name.text = contact.name
            holder.name.textSize = 22f
            holder.phone.text = contact.phone
            holder.phone.textSize = 18f
            holder.itemView.setOnClickListener { onClick(contact) }
        }
        
        override fun getItemCount() = contacts.size
    }
    
    class SavedContactAdapter(
        private val contacts: MutableList<Contact>,
        private val onAction: (Contact) -> Unit
    ) : RecyclerView.Adapter<SavedContactAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.contactName)
            val phone: TextView = view.findViewById(R.id.contactPhone)
            val callButton: View = view.findViewById(R.id.callButton)
            val removeButton: View = view.findViewById(R.id.removeButton)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emergency_contact, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = contacts[position]
            holder.name.text = contact.name
            holder.phone.text = contact.phone
            
            holder.callButton.setOnClickListener {
                // Handle call action
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${contact.phone}")
                }
                holder.itemView.context.startActivity(intent)
            }
            
            holder.removeButton.setOnClickListener {
                // Remove contact
                contacts.removeAt(position)
                notifyItemRemoved(position)
                
                // Remove from preferences
                PreferenceManager.getDefaultSharedPreferences(holder.itemView.context).edit()
                    .remove("EMERGENCY_CONTACT")
                    .remove("EMERGENCY_CONTACT_NAME")
                    .apply()
                    
                Toast.makeText(holder.itemView.context, "Emergency contact removed", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun getItemCount() = contacts.size
    }
    
    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
