package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.FactCheckDiscrepancy
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiService(
    private val customApiKey: String? = null,
    private val modelName: String = "gemini-3.5-flash"
) : AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    private fun getActiveApiKey(): String {
        return if (!customApiKey.isNullOrBlank()) {
            customApiKey
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
        }
    }

    private val systemRule22 = """
You are an expert official correspondence and office drafting assistant.
Your job is to analyse incoming correspondence and prepare accurate, professional and concise replies.
Treat the uploaded document as the primary factual source.
Never invent names, dates, reference numbers, quantities, amounts, equipment numbers, decisions or actions.
Preserve factual information exactly unless the user explicitly asks for a change.
When information required to complete a letter is missing, use a placeholder such as [Letter No.], [Date], [Vehicle No.], [Quantity], [Reference], [Authority] or ask for it.
Write in clear professional office language.
Improve grammar and structure without changing the intended meaning.
When replying to an incoming letter, address every material point requiring a response.
Before returning the final draft, internally compare the reply with the source document for factual inconsistencies.
""".trimIndent()

    override suspend fun analyzeDocument(
        text: String,
        imagesBase64: List<String>
    ): Result<DocumentAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback gracefully to BuiltinOfficeAiEngine
            return@withContext BuiltinOfficeAiEngine.analyzeDocument(text, imagesBase64)
        }

        val prompt = """
Analyze the following official document carefully.
Extract the key fields into a valid JSON object matching these keys:
{
  "sender": "string (name/designation/office)",
  "recipient": "string (recipient office/authority)",
  "letterNumber": "string (ref/letter no)",
  "date": "string",
  "subject": "string",
  "previousReferences": ["string"],
  "mainIssue": "string",
  "importantFacts": ["string"],
  "requestsOrInstructions": ["string"],
  "deadlines": ["string"],
  "quantities": ["string"],
  "financialFigures": ["string"],
  "equipmentOrVehicleNumbers": ["string"],
  "workOrderNumbers": ["string"],
  "annexures": ["string"],
  "actionRequiredFromRecipient": "string",
  "hasUnclearOcrWarning": boolean (true if parts were blurry, damaged or unreadable),
  "ocrWarningMessage": "string or null"
}

Document Content:
$text
""".trimIndent()

        try {
            val contentsArray = JSONArray()
            val partsArray = JSONArray()

            partsArray.put(JSONObject().put("text", prompt))

            // Add images if provided for multimodal OCR
            imagesBase64.take(3).forEach { b64 ->
                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", b64)
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            contentsArray.put(JSONObject().put("parts", partsArray))

            val genConfig = JSONObject()
                .put("responseMimeType", "application/json")
                .put("temperature", 0.2)

            val requestJson = JSONObject()
                .put("contents", contentsArray)
                .put("generationConfig", genConfig)
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemRule22))))

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(mediaTypeJson))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    // Fallback to built-in parser if quota or key error
                    val fallback = BuiltinOfficeAiEngine.analyzeDocument(text, imagesBase64).getOrNull()
                    return@withContext if (fallback != null) Result.success(fallback)
                    else Result.failure(Exception("Gemini HTTP ${response.code}: $errBody"))
                }

                val resBody = response.body?.string() ?: ""
                val resObj = JSONObject(resBody)
                val candidates = resObj.optJSONArray("candidates")
                val textResponse = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: ""

                val parsedJson = JSONObject(textResponse.trim())

                val analysis = DocumentAnalysisResult(
                    sender = parsedJson.optString("sender", ""),
                    recipient = parsedJson.optString("recipient", ""),
                    letterNumber = parsedJson.optString("letterNumber", ""),
                    date = parsedJson.optString("date", ""),
                    subject = parsedJson.optString("subject", ""),
                    previousReferences = jsonArrayToList(parsedJson.optJSONArray("previousReferences")),
                    mainIssue = parsedJson.optString("mainIssue", ""),
                    importantFacts = jsonArrayToList(parsedJson.optJSONArray("importantFacts")),
                    requestsOrInstructions = jsonArrayToList(parsedJson.optJSONArray("requestsOrInstructions")),
                    deadlines = jsonArrayToList(parsedJson.optJSONArray("deadlines")),
                    quantities = jsonArrayToList(parsedJson.optJSONArray("quantities")),
                    financialFigures = jsonArrayToList(parsedJson.optJSONArray("financialFigures")),
                    equipmentOrVehicleNumbers = jsonArrayToList(parsedJson.optJSONArray("equipmentOrVehicleNumbers")),
                    workOrderNumbers = jsonArrayToList(parsedJson.optJSONArray("workOrderNumbers")),
                    annexures = jsonArrayToList(parsedJson.optJSONArray("annexures")),
                    actionRequiredFromRecipient = parsedJson.optString("actionRequiredFromRecipient", ""),
                    rawSummary = textResponse,
                    hasUnclearOcrWarning = parsedJson.optBoolean("hasUnclearOcrWarning", false),
                    ocrWarningMessage = parsedJson.optString("ocrWarningMessage").takeIf { it.isNotBlank() }
                )
                Result.success(analysis)
            }
        } catch (e: Exception) {
            // Fallback to offline engine on exception
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
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BuiltinOfficeAiEngine.generateReply(sourceText, analysis, mode, language, userSpecialInstructions)
        }

        val prompt = """
Prepare a professional official reply based strictly on the uploaded source letter.

Writing Mode: ${mode.displayName} (${mode.description})
Output Language: ${language.displayName}
Special Instructions from User: ${userSpecialInstructions ?: "None"}

Document Summary Details:
${analysis?.toFormattedSummary() ?: "Source text: $sourceText"}

Requirements:
- Directly address the subject and purpose.
- Accurately preserve reference numbers, dates, vehicle numbers, quantities, and file IDs.
- Do NOT fabricate unstated facts; use standard brackets like [Letter No.] or [Date] if essential info is missing.
- Maintain numbered paragraphs in the body (1., 2., 3., etc.).
- Use appropriate formal expressions such as "Please refer to...", "It is intimated that...", "In view of the above...", "Therefore, it is requested that...", etc.
- Provide a clean signature block and copy-forwarded section.
""".trimIndent()

        callGeminiText(prompt)
    }

    override suspend fun improveDraft(
        draftText: String,
        mode: WritingMode,
        language: OutputLanguage,
        focusInstruction: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BuiltinOfficeAiEngine.improveDraft(draftText, mode, language, focusInstruction)
        }

        val prompt = """
Improve the following office draft:
Tone / Mode: ${mode.displayName}
Language: ${language.displayName}
Focus: ${focusInstruction ?: "Grammar, official terminology, paragraph numbering, clarity, and conciseness"}

CRITICAL CONSTRAINT: Do not alter names, dates, amounts, quantities, reference numbers, or factual commitments.

Draft:
$draftText
""".trimIndent()

        callGeminiText(prompt)
    }

    override suspend fun createNewLetter(
        instructions: String,
        mode: WritingMode,
        language: OutputLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BuiltinOfficeAiEngine.createNewLetter(instructions, mode, language)
        }

        val prompt = """
Create a complete, professional official office letter based on these instructions:
"$instructions"

Writing Mode: ${mode.displayName}
Output Language: ${language.displayName}

IMPORTANT: Before inventing missing factual details (like specific letter numbers, dates, vehicle IDs, or authority names), use editable placeholders such as [Letter No.], [Date], [Vehicle No.], [Quantity], [Reference], [Authority].
Follow official departmental formatting with numbered paragraphs, subject line, reference, salutation, body, and signature block.
""".trimIndent()

        callGeminiText(prompt)
    }

    override suspend fun factCheck(
        sourceText: String,
        replyText: String
    ): Result<FactCheckResult> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BuiltinOfficeAiEngine.factCheck(sourceText, replyText)
        }

        val prompt = """
Compare the Generated Reply against the Original Source Letter for factual consistency.
Check especially: Dates, Reference numbers, Quantities, Amounts, Names, Designations, Locations, Vehicle/equipment numbers, Work order numbers, and Deadlines.

Return a valid JSON object:
{
  "isConsistent": boolean,
  "scorePercent": integer (0 to 100),
  "summaryReport": "string",
  "discrepancies": [
    {
      "category": "Reference Number | Date | Quantity | Amount | Name | Deadline",
      "sourceValue": "string",
      "replyValue": "string",
      "isVerified": boolean,
      "remarks": "string"
    }
  ]
}

SOURCE DOCUMENT:
$sourceText

GENERATED REPLY:
$replyText
""".trimIndent()

        try {
            val responseText = callGeminiRaw(prompt, isJson = true)
            val json = JSONObject(responseText.trim())
            val list = mutableListOf<FactCheckDiscrepancy>()
            val arr = json.optJSONArray("discrepancies")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    list.add(
                        FactCheckDiscrepancy(
                            category = item.optString("category", "General"),
                            sourceValue = item.optString("sourceValue", ""),
                            replyValue = item.optString("replyValue", ""),
                            isVerified = item.optBoolean("isVerified", true),
                            remarks = item.optString("remarks", "")
                        )
                    )
                }
            }
            Result.success(
                FactCheckResult(
                    isConsistent = json.optBoolean("isConsistent", true),
                    scorePercent = json.optInt("scorePercent", 95),
                    discrepancies = list,
                    summaryReport = json.optString("summaryReport", "Verification complete.")
                )
            )
        } catch (e: Exception) {
            BuiltinOfficeAiEngine.factCheck(sourceText, replyText)
        }
    }

    override suspend fun applyInlineAction(
        selectedText: String,
        fullDocument: String,
        action: InlineAction,
        language: OutputLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getActiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BuiltinOfficeAiEngine.applyInlineAction(selectedText, fullDocument, action, language)
        }

        val prompt = """
Perform the following editing action on the selected text:
Action: ${action.label} (${action.promptInstruction})
Output Language: ${language.displayName}

Selected Text:
"$selectedText"

Return ONLY the replacement text for the selection. Do not include quotes or conversational preamble.
""".trimIndent()

        callGeminiText(prompt)
    }

    private suspend fun callGeminiText(prompt: String): Result<String> {
        return try {
            val text = callGeminiRaw(prompt, isJson = false)
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGeminiRaw(prompt: String, isJson: Boolean): String {
        val apiKey = getActiveApiKey()
        val contentsArray = JSONArray()
        val partsArray = JSONArray()
        partsArray.put(JSONObject().put("text", prompt))
        contentsArray.put(JSONObject().put("parts", partsArray))

        val genConfig = JSONObject()
            .put("temperature", 0.3)
        if (isJson) {
            genConfig.put("responseMimeType", "application/json")
        }

        val requestJson = JSONObject()
            .put("contents", contentsArray)
            .put("generationConfig", genConfig)
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemRule22))))

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw Exception("Gemini API Error (${response.code}): $err")
            }
            val resBody = response.body?.string() ?: ""
            val resObj = JSONObject(resBody)
            return resObj.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""
        }
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i)
            if (s.isNotBlank()) list.add(s)
        }
        return list
    }
}
