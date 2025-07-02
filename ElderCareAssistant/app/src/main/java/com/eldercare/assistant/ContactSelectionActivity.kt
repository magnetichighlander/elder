package com.eldercare.assistant

import android.os.Bundle
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class ContactSelectionActivity : AppCompatActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple layout programmatically for contact selection
        recyclerView = RecyclerView(this)
        recyclerView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        recyclerView.setPadding(16, 16, 16, 16)
        setContentView(recyclerView)
        
        // Set title
        title = "Select Emergency Contact"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) tts.language = Locale.getDefault()
        }
        
        // Load contacts
        loadContacts()
    }
    
    private fun loadContacts() {
        val contacts = mutableListOf<EmergencyContactActivity.Contact>()
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
                    contacts.add(EmergencyContactActivity.Contact(name, phone))
                }
            }
        }
        
        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ContactSelectionAdapter(contacts) { contact ->
            saveEmergencyContact(contact)
            finish()
        }
    }
    
    private fun saveEmergencyContact(contact: EmergencyContactActivity.Contact) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString("EMERGENCY_CONTACT", contact.phone)
            .putString("EMERGENCY_CONTACT_NAME", contact.name)
            .apply()
        
        // Voice confirmation
        tts.speak("Emergency contact set to ${contact.name}", TextToSpeech.QUEUE_FLUSH, null, "")
        Toast.makeText(this, "Set emergency contact: ${contact.name}", Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    class ContactSelectionAdapter(
        private val contacts: List<EmergencyContactActivity.Contact>,
        private val onClick: (EmergencyContactActivity.Contact) -> Unit
    ) : RecyclerView.Adapter<ContactSelectionAdapter.ViewHolder>() {
        
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
    
    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
