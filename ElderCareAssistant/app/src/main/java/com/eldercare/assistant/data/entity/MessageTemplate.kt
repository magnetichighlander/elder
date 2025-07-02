package com.eldercare.assistant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.eldercare.assistant.data.converter.DateConverter
import java.time.LocalDateTime

@Entity(tableName = "message_templates")
@TypeConverters(DateConverter::class)
data class MessageTemplate(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    val title: String,
    val text: String,
    val category: String,
    val lastUsed: LocalDateTime? = null,
    val useCount: Int = 0
)
