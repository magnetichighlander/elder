package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.eldercare.assistant.data.converter.DateConverter
import java.time.LocalDateTime

@Entity(tableName = "recent_contacts")
@TypeConverters(DateConverter::class)
data class RecentContact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val lastContactTime: LocalDateTime = LocalDateTime.now(),
    val contactCount: Int = 0
)
