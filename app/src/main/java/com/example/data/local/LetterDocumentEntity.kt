package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "letter_documents")
data class LetterDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceFileName: String? = null,
    val sourceFileType: String? = null, // "PDF", "DOCX", "XLSX", "IMAGE", "TXT", "DRAFT"
    val sourceRawText: String = "",
    val analysisSummary: String = "",
    val generatedReply: String = "",
    val editedReplyText: String = "",
    val writingMode: String = "Official",
    val language: String = "English",
    val status: String = "Draft", // "Uploaded", "Analyzed", "Replied", "Finalized"
    val timestamp: Long = System.currentTimeMillis()
)
