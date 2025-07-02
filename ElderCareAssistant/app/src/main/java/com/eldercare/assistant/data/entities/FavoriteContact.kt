package com.eldercare.assistant.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_contacts")
data class FavoriteContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val photoUri: String? = null,
    val order: Int = 0  // For custom sorting
)
