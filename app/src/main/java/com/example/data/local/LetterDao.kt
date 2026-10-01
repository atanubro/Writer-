package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDao {
    @Query("SELECT * FROM letter_documents ORDER BY timestamp DESC")
    fun getAllLetters(): Flow<List<LetterDocumentEntity>>

    @Query("SELECT * FROM letter_documents WHERE id = :id")
    suspend fun getLetterById(id: Long): LetterDocumentEntity?

    @Query("SELECT * FROM letter_documents WHERE title LIKE '%' || :query || '%' OR sourceRawText LIKE '%' || :query || '%' OR editedReplyText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLetters(query: String): Flow<List<LetterDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLetter(letter: LetterDocumentEntity): Long

    @Update
    suspend fun updateLetter(letter: LetterDocumentEntity)

    @Query("DELETE FROM letter_documents WHERE id = :id")
    suspend fun deleteLetterById(id: Long)

    @Query("DELETE FROM letter_documents")
    suspend fun deleteAllLetters()
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM letter_templates ORDER BY isCustom DESC, name ASC")
    fun getAllTemplates(): Flow<List<LetterTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: LetterTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<LetterTemplateEntity>)

    @Query("DELETE FROM letter_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Long)
}
