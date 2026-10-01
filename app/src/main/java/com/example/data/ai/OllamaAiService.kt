package com.example.data.ai

import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.FactCheckResult
import com.example.data.model.InlineAction
import com.example.data.model.OutputLanguage
import com.example.data.model.WritingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OllamaAiService(
    private val endpointUrl: String = "http://10.0.2.2:11434",
    private val model: String = "llama3"
) : AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    override suspend fun analyzeDocument(
        text: String,
        imagesBase64: List<String>
    ): Result<DocumentAnalysisResult> = withContext(Dispatchers.IO) {
        // Try calling Ollama, or fallback to Builtin engine
        val prompt = "Analyze this official letter and identify: Sender, Recipient, Ref No, Date, Subject, Key Points, Action Required.\n\nDocument:\n$text"
        try {
            val response = callOllama(prompt)
            // Parse using Builtin engine's deep extraction
            val baseResult = BuiltinOfficeAiEngine.analyzeDocument(text, imagesBase64).getOrNull()
                ?: DocumentAnalysisResult()
            Result.success(baseResult.copy(rawSummary = response))
        } catch (e: Exception) {
            BuiltinOfficeAiEngine.analyzeDocument(text, imagesBase64)
        }
    }

    override suspend fun generateReply(
        sourceText: String,
        analysis: DocumentAnalysisResult?,
        mode: WritingMode,
        language: OutputLanguage,
        userSpecialInstructions: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
You are an expert official correspondence assistant.
Generate an official reply to this incoming letter:
Writing Mode: ${mode.displayName}
Language: ${language.displayName}
Special Instructions: ${userSpecialInstructions ?: "None"}

Letter:
$sourceText
""".trimIndent()
        try {
            val reply = callOllama(prompt)
            Result.success(reply)
        } catch (e: Exception) {
            BuiltinOfficeAiEngine.generateReply(sourceText, analysis, mode, language, userSpecialInstructions)
        }
    }

    override suspend fun improveDraft(
        draftText: String,
        mode: WritingMode,
        language: OutputLanguage,
        focusInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "Improve this office draft in ${mode.displayName} style, language ${language.displayName}. Focus: ${focusInstruction ?: "Professional grammar & flow"}.\n\n$draftText"
        try {
            val reply = callOllama(prompt)
            Result.success(reply)
        } catch (e: Exception) {
            BuiltinOfficeAiEngine.improveDraft(draftText, mode, language, focusInstruction)
        }
    }

    override suspend fun createNewLetter(
        instructions: String,
        mode: WritingMode,
        language: OutputLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "Draft a formal office letter for: $instructions. Mode: ${mode.displayName}, Language: ${language.displayName}. Use placeholders like [Letter No.], [Date] for missing data."
        try {
            val reply = callOllama(prompt)
            Result.success(reply)
        } catch (e: Exception) {
            BuiltinOfficeAiEngine.createNewLetter(instructions, mode, language)
        }
    }

    override suspend fun factCheck(
        sourceText: String,
        replyText: String
    ): Result<FactCheckResult> {
        return BuiltinOfficeAiEngine.factCheck(sourceText, replyText)
    }

    override suspend fun applyInlineAction(
        selectedText: String,
        fullDocument: String,
        action: InlineAction,
        language: OutputLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = "${action.promptInstruction} for text: '$selectedText'"
        try {
            val res = callOllama(prompt)
            Result.success(res.trim().removeSurrounding("\""))
        } catch (e: Exception) {
            BuiltinOfficeAiEngine.applyInlineAction(selectedText, fullDocument, action, language)
        }
    }

    private fun callOllama(prompt: String): String {
        val normalizedEndpoint = if (endpointUrl.endsWith("/")) endpointUrl else "$endpointUrl/"
        val fullUrl = "${normalizedEndpoint}api/generate"

        val body = JSONObject()
            .put("model", model)
            .put("prompt", prompt)
            .put("stream", false)

        val request = Request.Builder()
            .url(fullUrl)
            .post(body.toString().toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Ollama error (${response.code})")
            }
            val res = response.body?.string() ?: ""
            return JSONObject(res).optString("response", "")
        }
    }
}
