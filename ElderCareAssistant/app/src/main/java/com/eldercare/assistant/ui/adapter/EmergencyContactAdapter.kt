package com.eldercare.assistant.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eldercare.assistant.databinding.ItemEmergencyContactBinding
import com.eldercare.assistant.ui.EmergencyContact
import com.eldercare.assistant.utils.AccessibilityUtils
import com.eldercare.assistant.utils.Constants

/**
 * RecyclerView adapter for displaying emergency contacts
 * Optimized for elderly users with large touch targets and clear text
 */
class EmergencyContactAdapter(
    private val contacts: MutableList<EmergencyContact>,
    private val onCallContact: (EmergencyContact) -> Unit,
    private val onRemoveContact: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemEmergencyContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(
        private val binding: ItemEmergencyContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: EmergencyContact) {
            // Set contact information
            binding.contactName.text = contact.name
            binding.contactPhone.text = contact.phoneNumber
            
            // Setup accessibility
            setupAccessibility(contact)
            
            // Setup click listeners
            binding.callButton.setOnClickListener {
                AccessibilityUtils.provideHapticFeedback(
                    binding.root.context,
                    Constants.HAPTIC_FEEDBACK_DURATION_MS
                )
                onCallContact(contact)
            }
            
            binding.removeButton.setOnClickListener {
                AccessibilityUtils.provideHapticFeedback(
                    binding.root.context,
                    Constants.HAPTIC_FEEDBACK_DURATION_MS
                )
                onRemoveContact(contact)
            }
            
            // Make the entire item clickable for calling
            binding.root.setOnClickListener {
                AccessibilityUtils.provideHapticFeedback(
                    binding.root.context,
                    Constants.HAPTIC_FEEDBACK_DURATION_MS
                )
                onCallContact(contact)
            }
        }
        
        private fun setupAccessibility(contact: EmergencyContact) {
            val context = binding.root.context
            
            // Setup accessibility for buttons
            AccessibilityUtils.setupElderlyAccessibility(binding.callButton, context as android.app.Activity)
            AccessibilityUtils.setupElderlyAccessibility(binding.removeButton, context)
            
            // Set content descriptions
            binding.callButton.contentDescription = "Call ${contact.name} at ${contact.phoneNumber}"
            binding.removeButton.contentDescription = "Remove ${contact.name} from emergency contacts"
            binding.root.contentDescription = "Emergency contact: ${contact.name}, ${contact.phoneNumber}. Tap to call."
            
            // Set accessibility labels for text views
            binding.contactName.contentDescription = "Contact name: ${contact.name}"
            binding.contactPhone.contentDescription = "Phone number: ${contact.phoneNumber}"
        }
    }
}
