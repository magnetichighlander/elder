package com.eldercare.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eldercare.assistant.data.entities.FavoriteContact
import com.eldercare.assistant.databinding.ActivityContactsBinding
import com.eldercare.assistant.viewmodel.ContactsViewModel

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: FavoriteContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Increase font size for accessibility
        resources.configuration.fontScale = 1.2f
        
        // Setup RecyclerView
        adapter = FavoriteContactsAdapter(
            contacts = emptyList(),
            onCallClick = { contact -> makeCall(contact.phone) },
            onMessageClick = { contact -> sendMessage(contact.phone) },
            onEditClick = { contact -> showEditDialog(contact) }
        )
        
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = adapter
        
        // Add contact FAB
        binding.addContactFab.setOnClickListener {
            showAddContactDialog()
        }
        
        // Observe contacts changes
        viewModel.favoriteContacts.observe(this) { contacts ->
            adapter.contacts = contacts
            adapter.notifyDataSetChanged()
        }
    }
    
    private fun makeCall(phoneNumber: String) {
        try {
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(callIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Calling not supported", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun sendMessage(phoneNumber: String) {
        try {
            val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("smsto:$phoneNumber")
            }
            startActivity(smsIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Messaging not supported", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAddContactDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Favorite Contact")
            .setView(R.layout.dialog_add_contact)
            .setPositiveButton("Add", null) // Set later to prevent auto-dismiss
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val nameInput = dialog.findViewById<EditText>(R.id.etContactName)
            val phoneInput = dialog.findViewById<EditText>(R.id.etContactPhone)
            
            positiveButton.setOnClickListener {
                val name = nameInput?.text?.toString()?.trim()
                val phone = phoneInput?.text?.toString()?.filter { it.isDigit() }
                
                if (name.isNullOrEmpty() || phone.isNullOrEmpty()) {
                    Toast.makeText(this@ContactsActivity, "Name and phone required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                viewModel.addFavoriteContact(
                    FavoriteContact(name = name, phone = phone)
                )
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
    
    private fun showEditDialog(contact: FavoriteContact) {
        val options = arrayOf("Edit", "Delete", "Cancel")
        
        AlertDialog.Builder(this)
            .setTitle("Contact Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditContactDialog(contact)
                    1 -> viewModel.deleteFavoriteContact(contact)
                }
            }
            .show()
    }
    
    private fun showEditContactDialog(contact: FavoriteContact) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Contact")
            .setView(R.layout.dialog_add_contact)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.setOnShowListener {
            val nameInput = dialog.findViewById<EditText>(R.id.etContactName)
            val phoneInput = dialog.findViewById<EditText>(R.id.etContactPhone)
            
            nameInput?.setText(contact.name)
            phoneInput?.setText(contact.phone)
            
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newName = nameInput?.text?.toString()?.trim()
                val newPhone = phoneInput?.text?.toString()?.filter { it.isDigit() }
                
                if (newName.isNullOrEmpty() || newPhone.isNullOrEmpty()) {
                    Toast.makeText(this@ContactsActivity, "Name and phone required", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                viewModel.updateFavoriteContact(
                    contact.copy(name = newName, phone = newPhone)
                )
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
}
