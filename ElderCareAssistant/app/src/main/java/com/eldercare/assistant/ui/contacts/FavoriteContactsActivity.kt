package com.eldercare.assistant.ui.contacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entities.FavoriteContact
import com.eldercare.assistant.databinding.ActivityFavoriteContactsBinding
import com.eldercare.assistant.ui.messaging.MessageEditorActivity
import com.eldercare.assistant.viewmodels.ContactsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class FavoriteContactsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFavoriteContactsBinding
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: FavoriteContactsAdapter
    
    companion object {
        const val EXTRA_SELECTION_MODE = "selection_mode"
        const val EXTRA_SELECTED_CONTACT = "selected_contact"
        private const val REQUEST_CONTACTS_PERMISSION = 100
    }
    
    private val isSelectionMode: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_SELECTION_MODE, false)
    }
    
    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { contactUri ->
                addContactFromUri(contactUri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        if (isSelectionMode) {
            title = getString(R.string.select_contact)
            binding.fabAddContact.hide()
        } else {
            title = getString(R.string.favorite_contacts)
        }
    }
    
    private fun setupUI() {
        binding.fabAddContact.setOnClickListener {
            if (checkContactsPermission()) {
                openContactPicker()
            } else {
                requestContactsPermission()
            }
        }
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // Show message when no favorites exist
        binding.emptyStateText.text = if (isSelectionMode) {
            getString(R.string.no_favorite_contacts_selection)
        } else {
            getString(R.string.no_favorite_contacts)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = FavoriteContactsAdapter(
            isSelectionMode = isSelectionMode,
            onContactClick = { contact ->
                if (isSelectionMode) {
                    // Return selected contact to calling activity
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_SELECTED_CONTACT, contact.name)
                        putExtra("phone", contact.phone)
                        putExtra("photoUri", contact.photoUri)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    // Open messaging with this contact
                    startMessageEditor(contact)
                }
            },
            onContactLongClick = { contact ->
                if (!isSelectionMode) {
                    showContactOptionsDialog(contact)
                }
            },
            onDeleteClick = { contact ->
                showDeleteConfirmation(contact)
            }
        )
        
        binding.recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewContacts.adapter = adapter
        
        if (!isSelectionMode) {
            setupItemTouchHelper()
        }
    }
    
    private fun setupItemTouchHelper() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.moveItem(fromPosition, toPosition)
                return true
            }
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not implemented for swipe
            }
            
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }
            
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
                
                // Save new order to database
                lifecycleScope.launch {
                    adapter.saveReorderedItems(viewModel)
                }
            }
        })
        
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewContacts)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.favoriteContacts.collect { contacts ->
                adapter.submitList(contacts)
                
                // Show/hide empty state
                if (contacts.isEmpty()) {
                    binding.emptyStateText.visibility = android.view.View.VISIBLE
                    binding.recyclerViewContacts.visibility = android.view.View.GONE
                } else {
                    binding.emptyStateText.visibility = android.view.View.GONE
                    binding.recyclerViewContacts.visibility = android.view.View.VISIBLE
                }
            }
        }
    }
    
    private fun checkContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            REQUEST_CONTACTS_PERMISSION
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactPicker()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.contacts_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }
    
    private fun addContactFromUri(contactUri: Uri) {
        lifecycleScope.launch {
            try {
                val contact = getContactFromUri(contactUri)
                if (contact != null) {
                    viewModel.addFavoriteContact(contact)
                    Toast.makeText(
                        this@FavoriteContactsActivity,
                        getString(R.string.contact_added_to_favorites),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@FavoriteContactsActivity,
                        getString(R.string.error_adding_contact),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@FavoriteContactsActivity,
                    getString(R.string.error_adding_contact),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun getContactFromUri(contactUri: Uri): FavoriteContact? {
        val cursor = contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.PHOTO_URI
            ),
            null, null, null
        ) ?: return null
        
        return cursor.use {
            if (it.moveToFirst()) {
                val contactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                val hasPhoneNumber = it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
                val photoUri = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                
                if (hasPhoneNumber) {
                    val phoneNumber = getContactPhoneNumber(contactId)
                    if (phoneNumber != null) {
                        FavoriteContact(
                            name = name,
                            phone = phoneNumber,
                            photoUri = photoUri
                        )
                    } else null
                } else null
            } else null
        }
    }
    
    private fun getContactPhoneNumber(contactId: Long): String? {
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        ) ?: return null
        
        return phoneCursor.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            } else null
        }
    }
    
    private fun showContactOptionsDialog(contact: FavoriteContact) {
        val options = arrayOf(
            getString(R.string.send_message),
            getString(R.string.call),
            getString(R.string.edit),
            getString(R.string.remove_from_favorites)
        )
        
        MaterialAlertDialogBuilder(this)
            .setTitle(contact.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startMessageEditor(contact)
                    1 -> callContact(contact)
                    2 -> editContact(contact)
                    3 -> showDeleteConfirmation(contact)
                }
            }
            .show()
    }
    
    private fun startMessageEditor(contact: FavoriteContact) {
        val intent = Intent(this, MessageEditorActivity::class.java).apply {
            putExtra(MessageEditorActivity.EXTRA_RECIPIENT_NAME, contact.name)
            putExtra(MessageEditorActivity.EXTRA_RECIPIENT_PHONE, contact.phone)
        }
        startActivity(intent)
    }
    
    private fun callContact(contact: FavoriteContact) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${contact.phone}")
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        } else {
            // Request call permission or show dial intent
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${contact.phone}")
            }
            startActivity(dialIntent)
        }
    }
    
    private fun editContact(contact: FavoriteContact) {
        // TODO: Implement contact editing dialog
        Toast.makeText(this, "Edit contact feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDeleteConfirmation(contact: FavoriteContact) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.remove_from_favorites))
            .setMessage(getString(R.string.remove_contact_confirmation, contact.name))
            .setPositiveButton(getString(R.string.remove)) { _, _ ->
                lifecycleScope.launch {
                    viewModel.removeFavoriteContact(contact)
                    Toast.makeText(
                        this@FavoriteContactsActivity,
                        getString(R.string.contact_removed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}
