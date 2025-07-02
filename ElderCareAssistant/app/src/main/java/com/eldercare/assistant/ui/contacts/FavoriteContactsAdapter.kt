package com.eldercare.assistant.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.eldercare.assistant.R
import com.eldercare.assistant.data.entities.FavoriteContact
import com.eldercare.assistant.databinding.ItemFavoriteContactBinding
import com.eldercare.assistant.viewmodels.ContactsViewModel
import java.util.Collections

class FavoriteContactsAdapter(
    private val isSelectionMode: Boolean,
    private val onContactClick: (FavoriteContact) -> Unit,
    private val onContactLongClick: (FavoriteContact) -> Unit,
    private val onDeleteClick: (FavoriteContact) -> Unit
) : ListAdapter<FavoriteContact, FavoriteContactsAdapter.ViewHolder>(FavoriteContactDiffCallback()) {

    private val currentList = mutableListOf<FavoriteContact>()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getItem(position)
        holder.bind(contact)
    }
    
    override fun submitList(list: List<FavoriteContact>?) {
        super.submitList(list)
        currentList.clear()
        list?.let { currentList.addAll(it) }
    }
    
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(currentList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(currentList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }
    
    suspend fun saveReorderedItems(viewModel: ContactsViewModel) {
        // Update the display order for each contact
        currentList.forEachIndexed { index, contact ->
            val updatedContact = contact.copy(displayOrder = index)
            viewModel.updateFavoriteContact(updatedContact)
        }
    }

    inner class ViewHolder(
        private val binding: ItemFavoriteContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: FavoriteContact) {
            binding.apply {
                textContactName.text = contact.name
                textContactPhone.text = contact.phone
                
                // Load contact photo
                if (contact.photoUri != null) {
                    Glide.with(root.context)
                        .load(contact.photoUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(imageContactPhoto)
                } else {
                    imageContactPhoto.setImageResource(R.drawable.ic_person)
                }
                
                // Show/hide drag handle and delete button based on mode
                if (isSelectionMode) {
                    imageDragHandle.visibility = android.view.View.GONE
                    buttonDelete.visibility = android.view.View.GONE
                    // Show selection indicator
                    root.alpha = 1.0f
                } else {
                    imageDragHandle.visibility = android.view.View.VISIBLE
                    buttonDelete.visibility = android.view.View.VISIBLE
                }
                
                // Set click listeners
                root.setOnClickListener {
                    onContactClick(contact)
                }
                
                root.setOnLongClickListener {
                    onContactLongClick(contact)
                    true
                }
                
                buttonDelete.setOnClickListener {
                    onDeleteClick(contact)
                }
                
                // Add accessibility content descriptions
                root.contentDescription = if (isSelectionMode) {
                    root.context.getString(R.string.select_contact_description, contact.name)
                } else {
                    root.context.getString(R.string.favorite_contact_description, contact.name, contact.phone)
                }
                
                buttonDelete.contentDescription = 
                    root.context.getString(R.string.remove_contact_description, contact.name)
            }
        }
    }
}

class FavoriteContactDiffCallback : DiffUtil.ItemCallback<FavoriteContact>() {
    override fun areItemsTheSame(oldItem: FavoriteContact, newItem: FavoriteContact): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FavoriteContact, newItem: FavoriteContact): Boolean {
        return oldItem == newItem
    }
}
