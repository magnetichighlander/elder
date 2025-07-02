package com.eldercare.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eldercare.assistant.data.entities.FavoriteContact
import com.eldercare.assistant.viewmodel.FavoriteContactViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FavoriteContactsActivity : AppCompatActivity() {
    
    private lateinit var viewModel: FavoriteContactViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var addFab: FloatingActionButton
private var contacts = listOf<FavoriteContact>()
    
    private val CALL_PERMISSION_REQUEST_CODE = 101
    private val SMS_PERMISSION_REQUEST_CODE = 102
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_contacts)
        
        title = "Favorite Contacts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        initViews()
        setupViewModel()
        setupRecyclerView()
        setupAddButton()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.contactsRecyclerView)
        addFab = findViewById(R.id.addContactFab)
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[FavoriteContactViewModel::class.java]
        
        viewModel.allContacts.observe(this) { contactList ->
            contacts = contactList
            recyclerView.adapter = FavoriteContactsAdapter(
                contacts = contacts,
                onCallClick = { contact -> makeCall(contact.phone) },
                onMessageClick = { contact -> sendMessage(contact.phone) },
                onEditClick = { contact -> editContact(contact) }
            )
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupAddButton() {
        addFab.setOnClickListener {
            val intent = Intent(this, ContactSelectionActivity::class.java)
            intent.putExtra("FOR_FAVORITES", true)
            startActivity(intent)
        }
    }
    
    private fun makeCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_REQUEST_CODE
            )
            return
        }
        
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
    }
    
    private fun sendMessage(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
            return
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phoneNumber")
        }
        startActivity(intent)
    }
    
    private fun removeContact(contact: FavoriteContact) {
        viewModel.deleteContact(contact)
        Toast.makeText(this, "${contact.name} removed from favorites", Toast.LENGTH_SHORT).show()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            CALL_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Call permission needed to make calls", Toast.LENGTH_LONG).show()
                }
            }
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "SMS permission needed to send messages", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class FavoriteContactsAdapter(
    var contacts: List<FavoriteContact>,
    private val onCallClick: (FavoriteContact) -> Unit,
    private val onMessageClick: (FavoriteContact) -> Unit,
    private val onEditClick: (FavoriteContact) -> Unit
) : RecyclerView.Adapter<FavoriteContactsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.contactName)
        val phone: TextView = view.findViewById(R.id.contactPhone)
        val image: ImageView = view.findViewById(R.id.contactImage)
        val callButton: ImageButton = view.findViewById(R.id.callButton)
        val messageButton: ImageButton = view.findViewById(R.id.messageButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        
        // Set contact info
        holder.name.text = contact.name
        holder.phone.text = contact.phone
        
        // Load contact image
        contact.photoUri?.let {
            Glide.with(holder.itemView.context)
                .load(it)
                .circleCrop()
                .placeholder(R.drawable.ic_default_avatar)
                .into(holder.image)
        } ?: run {
            holder.image.setImageResource(R.drawable.ic_default_avatar)
        }
        
        // Set click listeners
        holder.callButton.setOnClickListener { onCallClick(contact) }
        holder.messageButton.setOnClickListener { onMessageClick(contact) }
        holder.itemView.setOnLongClickListener {
            onEditClick(contact)
            true
        }
    }

    override fun getItemCount() = contacts.size
}
