package com.example.data.ai

import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.FactCheckResult
import com.example.data.model.InlineAction
import com.example.data.model.OutputLanguage
import com.example.data.model.WritingMode

interface AiService {
    suspend fun analyzeDocument(
        text: String,
        imagesBase64: List<String> = emptyList()
    ): Result<DocumentAnalysisResult>

    suspend fun generateReply(
        sourceText: String,
        analysis: DocumentAnalysisResult?,
        mode: WritingMode,
        language: OutputLanguage,
        userSpecialInstructions: String? = null
    ): Result<String>

    suspend fun improveDraft(
        draftText: String,
        mode: WritingMode,
        language: OutputLanguage,
        focusInstruction: String? = null
    ): Result<String>

    suspend fun createNewLetter(
        instructions: String,
        mode: WritingMode,
        language: OutputLanguage
    ): Result<String>

    suspend fun factCheck(
        sourceText: String,
        replyText: String
    ): Result<FactCheckResult>

    suspend fun applyInlineAction(
        selectedText: String,
        fullDocument: String,
        action: InlineAction,
        language: OutputLanguage
    ): Result<String>
}
