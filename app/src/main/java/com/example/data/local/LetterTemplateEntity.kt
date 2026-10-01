package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "letter_templates")
data class LetterTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String, // "Official Reply", "Request Letter", "Reminder", "Deficiency Report", etc.
    val description: String,
    val subjectTemplate: String,
    val bodyTemplate: String,
    val placeholders: String = "", // Comma-separated placeholder tokens e.g. "[Letter No.],[Date]"
    val isCustom: Boolean = false
)
