package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiProviderManager
import com.example.data.local.AppDatabase
import com.example.data.local.LetterDocumentEntity
import com.example.data.local.LetterTemplateEntity
import com.example.data.model.AiProviderConfig
import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.FactCheckResult
import com.example.data.model.InlineAction
import com.example.data.model.OutputLanguage
import com.example.data.model.WritingMode
import com.example.data.parser.DocumentParser
import com.example.data.repository.LetterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveLetterState(
    val currentLetterId: Long = 0L,
    val sourceFileName: String = "",
    val sourceFileType: String = "",
    val sourceRawText: String = "",
    val imageBase64List: List<String> = emptyList(),
    val hasUnclearOcrWarning: Boolean = false,
    val ocrWarningMessage: String? = null,
    val analysisResult: DocumentAnalysisResult? = null,
    val replyText: String = "",
    val editedReplyText: String = "",
    val writingMode: WritingMode = WritingMode.OFFICIAL,
    val outputLanguage: OutputLanguage = OutputLanguage.ENGLISH,
    val specialUserInstructions: String = "",
    val isAnalyzing: Boolean = false,
    val isGenerating: Boolean = false,
    val isFactChecking: Boolean = false,
    val progressMessage: String? = null,
    val factCheckResult: FactCheckResult? = null,
    val errorMessage: String? = null
)

class LetterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = LetterRepository(db.letterDao(), db.templateDao())
    val aiProviderManager = AiProviderManager()

    private val _activeState = MutableStateFlow(ActiveLetterState())
    val activeState: StateFlow<ActiveLetterState> = _activeState.asStateFlow()

    private val _aiConfig = MutableStateFlow(aiProviderManager.getConfig())
    val aiConfig: StateFlow<AiProviderConfig> = _aiConfig.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val savedLetters: StateFlow<List<LetterDocumentEntity>> = repository.allLetters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<LetterTemplateEntity>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultTemplatesIfNeeded()
        }
    }

    fun updateAiConfig(newConfig: AiProviderConfig) {
        aiProviderManager.updateConfig(newConfig)
        _aiConfig.value = newConfig
    }

    fun setWritingMode(mode: WritingMode) {
        _activeState.value = _activeState.value.copy(writingMode = mode)
    }

    fun setOutputLanguage(language: OutputLanguage) {
        _activeState.value = _activeState.value.copy(outputLanguage = language)
    }

    fun setSpecialInstructions(instructions: String) {
        _activeState.value = _activeState.value.copy(specialUserInstructions = instructions)
    }

    fun updateEditedReplyText(newText: String) {
        _activeState.value = _activeState.value.copy(editedReplyText = newText)
    }

    fun updateSourceRawText(newText: String) {
        _activeState.value = _activeState.value.copy(sourceRawText = newText)
    }

    fun cancelActiveOperation() {
        activeJob?.cancel()
        activeJob = null
        _activeState.value = _activeState.value.copy(
            isAnalyzing = false,
            isGenerating = false,
            isFactChecking = false,
            progressMessage = null
        )
    }

    fun loadDocumentFromUri(uri: Uri, fileName: String, autoAnalyze: Boolean = true) {
        activeJob?.cancel()
        _activeState.value = _activeState.value.copy(
            progressMessage = "Reading document...",
            isAnalyzing = true,
            errorMessage = null
        )

        activeJob = viewModelScope.launch {
            try {
                _activeState.value = _activeState.value.copy(progressMessage = "Extracting text...")
                val extracted = DocumentParser.parseUri(getApplication(), uri, fileName)

                _activeState.value = _activeState.value.copy(
                    currentLetterId = 0L,
                    sourceFileName = extracted.fileName,
                    sourceFileType = extracted.fileType,
                    sourceRawText = extracted.rawText,
                    imageBase64List = extracted.imageBase64List,
                    hasUnclearOcrWarning = extracted.hasUnclearWarning,
                    ocrWarningMessage = extracted.warningMessage,
                    progressMessage = if (autoAnalyze) "Analysing letter..." else null,
                    isAnalyzing = autoAnalyze
                )

                if (autoAnalyze) {
                    analyzeCurrentDocument()
                }
            } catch (e: Exception) {
                _activeState.value = _activeState.value.copy(
                    isAnalyzing = false,
                    progressMessage = null,
                    errorMessage = "Failed to load document: ${e.message}"
                )
            }
        }
    }

    fun loadDocumentFromBitmap(bitmap: Bitmap) {
        activeJob?.cancel()
        _activeState.value = _activeState.value.copy(
            progressMessage = "Scanning letter via ML Kit OCR...",
            isAnalyzing = true,
            errorMessage = null
        )

        activeJob = viewModelScope.launch {
            try {
                // Run through OcrService for ML Kit OCR, document segmentation, and error checking
                val ocrResult = com.example.data.ocr.OcrService.extractFromBitmap(bitmap).getOrNull()
                val parsed = DocumentParser.parseBitmap(bitmap)
                val finalRawText = ocrResult?.cleanedText?.takeIf { it.isNotBlank() } ?: parsed.rawText

                _activeState.value = _activeState.value.copy(
                    currentLetterId = 0L,
                    sourceFileName = "Camera_Letter_Scan.jpg",
                    sourceFileType = "IMG",
                    sourceRawText = finalRawText,
                    imageBase64List = parsed.imageBase64List,
                    hasUnclearOcrWarning = ocrResult?.hasWarnings ?: false,
                    ocrWarningMessage = ocrResult?.warnings?.joinToString("\n"),
                    progressMessage = "Analysing letter structure & metadata...",
                    isAnalyzing = true
                )
                analyzeCurrentDocument()
            } catch (e: Exception) {
                _activeState.value = _activeState.value.copy(
                    isAnalyzing = false,
                    progressMessage = null,
                    errorMessage = "Error processing image: ${e.message}"
                )
            }
        }
    }

    fun clearActiveDocument() {
        _activeState.value = ActiveLetterState()
    }

    fun loadSampleLetter() {
        val sampleText = """
            OFFICE OF THE EXECUTIVE ENGINEER
            DEPARTMENT OF IRRIGATION AND PUBLIC WORKS
            Letter No: WRD/DIV-IV/2026/842
            Dated: 24th October 2026

            To,
            The Superintending Engineer,
            Civil Construction Circle,
            Administrative Complex, Sector 5

            Subject: Urgent requisition for replacement of 2 nos unserviceable de-watering pump batteries at Barrage Site B.
            Reference: Prior inspection report No. IR/2026/19 dated 12-10-2026.

            Sir,
            1. With reference to the subject cited above, it is brought to your kind notice that during emergency flood containment rehearsals, 2 nos heavy-duty 12V batteries installed in de-watering pumps (Equipment Model: PMP-400-K, Sr. Nos. 8831 and 8832) were found completely dead and beyond economical repair.

            2. In view of upcoming monsoon operational readiness, it is requested that replacement of 2 nos batteries may kindly be sanctioned and supplied within 10 days to avert any operational stoppage.

            Yours faithfully,
            A. K. Sharma
            Executive Engineer, Division IV
        """.trimIndent()

        _activeState.value = ActiveLetterState(
            sourceFileName = "Sample_Departmental_Letter.pdf",
            sourceFileType = "PDF",
            sourceRawText = sampleText,
            writingMode = WritingMode.OFFICIAL,
            outputLanguage = _activeState.value.outputLanguage
        )
        analyzeCurrentDocument()
    }

    fun analyzeCurrentDocument() {
        val current = _activeState.value
        val textToAnalyze = current.sourceRawText
        if (textToAnalyze.isBlank() && current.imageBase64List.isEmpty()) {
            _activeState.value = _activeState.value.copy(
                isAnalyzing = false,
                progressMessage = null,
                errorMessage = "No document content to analyze."
            )
            return
        }

        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _activeState.value = _activeState.value.copy(
                isAnalyzing = true,
                progressMessage = "Analysing letter..."
            )
            val service = aiProviderManager.getService()
            val result = service.analyzeDocument(textToAnalyze, current.imageBase64List)

            result.onSuccess { analysis ->
                _activeState.value = _activeState.value.copy(
                    analysisResult = analysis,
                    isAnalyzing = false,
                    progressMessage = null,
                    hasUnclearOcrWarning = analysis.hasUnclearOcrWarning || current.hasUnclearOcrWarning,
                    ocrWarningMessage = analysis.ocrWarningMessage ?: current.ocrWarningMessage
                )
            }.onFailure { err ->
                _activeState.value = _activeState.value.copy(
                    isAnalyzing = false,
                    progressMessage = null,
                    errorMessage = "Analysis error: ${err.message}"
                )
            }
        }
    }

    fun generateReply() {
        val current = _activeState.value
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _activeState.value = _activeState.value.copy(
                isGenerating = true,
                progressMessage = "Generating reply..."
            )

            val service = aiProviderManager.getService()
            val result = service.generateReply(
                sourceText = current.sourceRawText,
                analysis = current.analysisResult,
                mode = current.writingMode,
                language = current.outputLanguage,
                userSpecialInstructions = current.specialUserInstructions.takeIf { it.isNotBlank() }
            )

            result.onSuccess { reply ->
                _activeState.value = _activeState.value.copy(
                    replyText = reply,
                    editedReplyText = reply,
                    isGenerating = false,
                    progressMessage = null
                )
                // Automatically run fact check
                performFactCheck(current.sourceRawText, reply)
                // Auto save draft to history
                saveCurrentLetterToHistory()
            }.onFailure { err ->
                _activeState.value = _activeState.value.copy(
                    isGenerating = false,
                    progressMessage = null,
                    errorMessage = "Failed to generate reply: ${err.message}"
                )
            }
        }
    }

    fun improveDraft(draftInput: String, focus: String? = null) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _activeState.value = _activeState.value.copy(
                sourceFileName = "User_Draft.txt",
                sourceFileType = "DRAFT",
                sourceRawText = draftInput,
                isGenerating = true,
                progressMessage = "Refining and improving draft..."
            )

            val service = aiProviderManager.getService()
            val result = service.improveDraft(
                draftText = draftInput,
                mode = _activeState.value.writingMode,
                language = _activeState.value.outputLanguage,
                focusInstruction = focus
            )

            result.onSuccess { improved ->
                _activeState.value = _activeState.value.copy(
                    replyText = improved,
                    editedReplyText = improved,
                    isGenerating = false,
                    progressMessage = null
                )
                saveCurrentLetterToHistory()
            }.onFailure { err ->
                _activeState.value = _activeState.value.copy(
                    isGenerating = false,
                    progressMessage = null,
                    errorMessage = "Error improving draft: ${err.message}"
                )
            }
        }
    }

    fun createNewLetterFromInstructions(instructions: String) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _activeState.value = _activeState.value.copy(
                sourceFileName = "Custom_Instruction.txt",
                sourceFileType = "MANUAL",
                sourceRawText = instructions,
                specialUserInstructions = instructions,
                isGenerating = true,
                progressMessage = "Drafting new official letter..."
            )

            val service = aiProviderManager.getService()
            val result = service.createNewLetter(
                instructions = instructions,
                mode = _activeState.value.writingMode,
                language = _activeState.value.outputLanguage
            )

            result.onSuccess { newLetter ->
                _activeState.value = _activeState.value.copy(
                    replyText = newLetter,
                    editedReplyText = newLetter,
                    isGenerating = false,
                    progressMessage = null
                )
                saveCurrentLetterToHistory()
            }.onFailure { err ->
                _activeState.value = _activeState.value.copy(
                    isGenerating = false,
                    progressMessage = null,
                    errorMessage = "Failed to draft letter: ${err.message}"
                )
            }
        }
    }

    fun performFactCheck(source: String? = null, reply: String? = null) {
        val src = source ?: _activeState.value.sourceRawText
        val rep = reply ?: _activeState.value.editedReplyText
        if (src.isBlank() || rep.isBlank()) return

        viewModelScope.launch {
            _activeState.value = _activeState.value.copy(isFactChecking = true)
            val service = aiProviderManager.getService()
            val res = service.factCheck(src, rep)
            res.onSuccess { checkResult ->
                _activeState.value = _activeState.value.copy(
                    factCheckResult = checkResult,
                    isFactChecking = false
                )
            }.onFailure {
                _activeState.value = _activeState.value.copy(isFactChecking = false)
            }
        }
    }

    fun applyInlineActionToSelection(
        selectedText: String,
        action: InlineAction,
        onReplacementReady: (String) -> Unit
    ) {
        if (selectedText.isBlank()) return
        viewModelScope.launch {
            val service = aiProviderManager.getService()
            val res = service.applyInlineAction(
                selectedText = selectedText,
                fullDocument = _activeState.value.editedReplyText,
                action = action,
                language = _activeState.value.outputLanguage
            )
            res.onSuccess { replacement ->
                onReplacementReady(replacement)
            }
        }
    }

    fun loadFromHistory(letter: LetterDocumentEntity) {
        _activeState.value = ActiveLetterState(
            currentLetterId = letter.id,
            sourceFileName = letter.sourceFileName ?: "Untitled_Letter",
            sourceFileType = letter.sourceFileType ?: "DOC",
            sourceRawText = letter.sourceRawText,
            analysisResult = if (letter.analysisSummary.isNotBlank()) {
                DocumentAnalysisResult(
                    subject = letter.title,
                    rawSummary = letter.analysisSummary
                )
            } else null,
            replyText = letter.generatedReply,
            editedReplyText = letter.editedReplyText.ifBlank { letter.generatedReply },
            writingMode = WritingMode.entries.find { it.displayName.equals(letter.writingMode, true) } ?: WritingMode.OFFICIAL,
            outputLanguage = OutputLanguage.entries.find { it.displayName.startsWith(letter.language, true) } ?: OutputLanguage.ENGLISH
        )
    }

    fun loadFromTemplate(template: LetterTemplateEntity) {
        _activeState.value = ActiveLetterState(
            sourceFileName = "${template.name}.tpl",
            sourceFileType = "TEMPLATE",
            sourceRawText = "Template: ${template.name}\n${template.description}",
            replyText = template.bodyTemplate,
            editedReplyText = template.bodyTemplate,
            writingMode = WritingMode.OFFICIAL
        )
    }

    fun saveCurrentLetterToHistory() {
        val current = _activeState.value
        val title = current.analysisResult?.subject?.takeIf { it.isNotBlank() }
            ?: current.sourceFileName.takeIf { it.isNotBlank() }
            ?: "Official Letter ${System.currentTimeMillis() % 10000}"

        viewModelScope.launch {
            val entity = LetterDocumentEntity(
                id = current.currentLetterId,
                title = title,
                sourceFileName = current.sourceFileName,
                sourceFileType = current.sourceFileType,
                sourceRawText = current.sourceRawText,
                analysisSummary = current.analysisResult?.toFormattedSummary() ?: "",
                generatedReply = current.replyText,
                editedReplyText = current.editedReplyText,
                writingMode = current.writingMode.displayName,
                language = current.outputLanguage.displayName,
                status = if (current.editedReplyText.isNotBlank()) "Finalized" else "Draft"
            )
            val newId = repository.saveLetter(entity)
            if (current.currentLetterId == 0L) {
                _activeState.value = _activeState.value.copy(currentLetterId = newId)
            }
        }
    }

    fun deleteLetter(id: Long) {
        viewModelScope.launch {
            repository.deleteLetter(id)
            if (_activeState.value.currentLetterId == id) {
                _activeState.value = ActiveLetterState()
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllLetters()
            _activeState.value = ActiveLetterState()
        }
    }

    fun saveCustomTemplate(name: String, category: String, subject: String, body: String) {
        viewModelScope.launch {
            val entity = LetterTemplateEntity(
                name = name,
                category = category,
                description = "Custom user saved template",
                subjectTemplate = subject,
                bodyTemplate = body,
                isCustom = true
            )
            repository.saveTemplate(entity)
        }
    }
}
