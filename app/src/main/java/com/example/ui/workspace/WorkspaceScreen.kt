package com.example.ui.workspace

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportManager
import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.FactCheckResult
import com.example.data.model.InlineAction
import com.example.data.model.WritingMode
import com.example.ui.components.FactCheckChip
import com.example.ui.components.FactualInconsistenciesWidget
import com.example.ui.components.HelpShortcutsModal
import com.example.ui.components.OcrWarningBanner
import com.example.ui.components.ProgressModal
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.DiscrepancyRed
import com.example.ui.theme.DiscrepancyRedContainer
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.NavyBlueContainer
import com.example.ui.theme.NavyBluePrimary
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenContainer
import com.example.ui.viewmodel.LetterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: LetterViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDraftImprovement: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activeState by viewModel.activeState.collectAsState()

    var selectedMobileTab by remember { mutableIntStateOf(if (activeState.replyText.isNotBlank()) 2 else 0) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showFactCheckModal by remember { mutableStateOf(false) }
    var showAiActionsMenu by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }
    var forceTabViewOnDesktop by remember { mutableStateOf(false) }

    val rootFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        rootFocusRequester.requestFocus()
    }

    // Editor text with undo/redo stack
    var editorFieldValue by remember(activeState.editedReplyText) {
        mutableStateOf(TextFieldValue(activeState.editedReplyText))
    }
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    fun updateEditorText(newVal: TextFieldValue, addToUndo: Boolean = true) {
        if (addToUndo && newVal.text != editorFieldValue.text) {
            undoStack.add(editorFieldValue.text)
            redoStack.clear()
        }
        editorFieldValue = newVal
        viewModel.updateEditedReplyText(newVal.text)
    }

    fun handleGlobalShortcut(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown) {
            val isCtrl = event.isCtrlPressed || event.isMetaPressed
            return when {
                isCtrl && event.key == Key.S -> {
                    viewModel.saveCurrentLetterToHistory()
                    Toast.makeText(context, "Draft Saved to History (Ctrl+S)", Toast.LENGTH_SHORT).show()
                    true
                }
                isCtrl && event.key == Key.I -> {
                    Toast.makeText(context, "Initiating 'Improve Draft' (Ctrl+I)", Toast.LENGTH_SHORT).show()
                    if (onNavigateToDraftImprovement != null) {
                        onNavigateToDraftImprovement()
                    } else {
                        viewModel.improveDraft(editorFieldValue.text, "Formal official tone, clarity, and precision")
                    }
                    true
                }
                (isCtrl && (event.key == Key.T || event.key == Key.Backslash || event.key == Key.M)) || event.key == Key.F10 -> {
                    forceTabViewOnDesktop = !forceTabViewOnDesktop
                    val modeLabel = if (forceTabViewOnDesktop) "Tabbed View" else "Split-Screen View"
                    Toast.makeText(context, "Desktop View: $modeLabel (Ctrl+T)", Toast.LENGTH_SHORT).show()
                    true
                }
                (isCtrl && (event.key == Key.H || event.key == Key.Slash)) || event.key == Key.F1 -> {
                    showHelpModal = true
                    true
                }
                else -> false
            }
        }
        return false
    }

    var pendingExportFormat by remember { mutableStateOf<String?>(null) }
    var pendingExportTitle by remember { mutableStateOf("") }
    var pendingExportContent by remember { mutableStateOf("") }

    // System File Save Picker (Storage Access Framework)
    val fileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri: Uri? ->
        if (destinationUri != null && pendingExportFormat != null) {
            try {
                ExportManager.saveContentToUri(
                    context = context,
                    destinationUri = destinationUri,
                    format = pendingExportFormat!!,
                    title = pendingExportTitle,
                    content = pendingExportContent
                )
                Toast.makeText(context, "Saved file to storage successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { handleGlobalShortcut(it) }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = activeState.sourceFileName.ifBlank { "Official Correspondence" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "${activeState.writingMode.displayName} • ${activeState.outputLanguage.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("workspace_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Toggle Split-Screen / Tab views on desktop
                        IconButton(
                            onClick = {
                                forceTabViewOnDesktop = !forceTabViewOnDesktop
                                val modeLabel = if (forceTabViewOnDesktop) "Tabbed View" else "Split-Screen View"
                                Toast.makeText(context, "Desktop View: $modeLabel (Ctrl+T)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("toggle_desktop_layout_button")
                        ) {
                            Icon(
                                imageVector = if (forceTabViewOnDesktop) Icons.Default.Splitscreen else Icons.Default.Tab,
                                contentDescription = if (forceTabViewOnDesktop) "Switch to Split-Screen (Ctrl+T)" else "Switch to Tab View (Ctrl+T)",
                                tint = NavyBluePrimary
                            )
                        }

                        // Help & Shortcuts modal (F1 / Ctrl+H)
                        IconButton(
                            onClick = { showHelpModal = true },
                            modifier = Modifier.testTag("workspace_help_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help & Shortcuts (F1)")
                        }

                        // Fact Check Chip
                        activeState.factCheckResult?.let { factResult ->
                            FactCheckChip(
                                scorePercent = factResult.scorePercent,
                                isConsistent = factResult.isConsistent,
                                onClick = { showFactCheckModal = true }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Save Letter
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentLetterToHistory()
                                Toast.makeText(context, "Saved to Local Correspondence History (Ctrl+S)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("save_letter_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save (Ctrl+S)")
                        }

                        // Export Letter
                        IconButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.testTag("export_letter_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isDesktopOrTablet = maxWidth >= 700.dp
                val useSplitScreen = isDesktopOrTablet && !forceTabViewOnDesktop

                if (useSplitScreen) {
                    // Desktop / Tablet Split-Screen View
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT PANE: Original Document & Analysis Summary
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    ) {
                        LeftSourcePane(
                            state = activeState,
                            onSourceTextChanged = { viewModel.updateSourceRawText(it) },
                            onReanalyze = { viewModel.analyzeCurrentDocument() },
                            onGenerateReply = { viewModel.generateReply() },
                            onRunFactCheck = { viewModel.performFactCheck() },
                            onApplyFix = { fixValue ->
                                updateEditorText(TextFieldValue(editorFieldValue.text + "\n" + fixValue))
                                Toast.makeText(context, "Applied '$fixValue' to draft", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // RIGHT PANE: AI Editor & Fact Verification
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    ) {
                        RightEditorPane(
                            editorValue = editorFieldValue,
                            onEditorValueChange = { updateEditorText(it) },
                            selectedMode = activeState.writingMode,
                            onModeSelected = { mode ->
                                viewModel.setWritingMode(mode)
                                Toast.makeText(context, "Writing mode set to ${mode.displayName}. Prompt instructions updated.", Toast.LENGTH_SHORT).show()
                            },
                            undoStack = undoStack,
                            redoStack = redoStack,
                            onUndo = {
                                if (undoStack.isNotEmpty()) {
                                    val last = undoStack.removeLast()
                                    redoStack.add(editorFieldValue.text)
                                    editorFieldValue = TextFieldValue(last)
                                    viewModel.updateEditedReplyText(last)
                                }
                            },
                            onRedo = {
                                if (redoStack.isNotEmpty()) {
                                    val next = redoStack.removeLast()
                                    undoStack.add(editorFieldValue.text)
                                    editorFieldValue = TextFieldValue(next)
                                    viewModel.updateEditedReplyText(next)
                                }
                            },
                            onAiAction = { action ->
                                val sel = editorFieldValue.getSelectedText().ifBlank { editorFieldValue.text }
                                viewModel.applyInlineActionToSelection(sel, action) { replacement ->
                                    if (editorFieldValue.selection.length > 0) {
                                        val newText = editorFieldValue.text.replaceRange(
                                            editorFieldValue.selection.start,
                                            editorFieldValue.selection.end,
                                            replacement
                                        )
                                        updateEditorText(TextFieldValue(newText))
                                    } else {
                                        updateEditorText(TextFieldValue(replacement))
                                    }
                                }
                            },
                            onFactCheckClick = {
                                viewModel.performFactCheck()
                                showFactCheckModal = true
                            },
                            onRegenerate = { viewModel.generateReply() },
                            onImproveDraft = {
                                if (onNavigateToDraftImprovement != null) {
                                    onNavigateToDraftImprovement()
                                } else {
                                    viewModel.improveDraft(editorFieldValue.text, "Formal official tone, clarity, and precision")
                                }
                            },
                            onKeyShortcut = { handleGlobalShortcut(it) }
                        )
                    }
                }
            } else {
                // Mobile Tabbed View (Source | Analysis | Reply)
                Column(modifier = Modifier.fillMaxSize()) {
                    PrimaryTabRow(selectedTabIndex = selectedMobileTab) {
                        Tab(
                            selected = selectedMobileTab == 0,
                            onClick = { selectedMobileTab = 0 },
                            text = { Text("Source Letter") },
                            modifier = Modifier.testTag("tab_source")
                        )
                        Tab(
                            selected = selectedMobileTab == 1,
                            onClick = { selectedMobileTab = 1 },
                            text = { Text("Analysis") },
                            modifier = Modifier.testTag("tab_analysis")
                        )
                        Tab(
                            selected = selectedMobileTab == 2,
                            onClick = { selectedMobileTab = 2 },
                            text = { Text("Reply & Editor") },
                            modifier = Modifier.testTag("tab_reply")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        when (selectedMobileTab) {
                            0 -> SourceLetterTab(
                                state = activeState,
                                onSourceTextChanged = { viewModel.updateSourceRawText(it) },
                                onGenerateReply = {
                                    viewModel.generateReply()
                                    selectedMobileTab = 2
                                }
                            )
                            1 -> AnalysisTab(
                                state = activeState,
                                onGenerateReply = {
                                    viewModel.generateReply()
                                    selectedMobileTab = 2
                                },
                                onRunFactCheck = { viewModel.performFactCheck() },
                                onApplyFix = { fixValue ->
                                    updateEditorText(TextFieldValue(editorFieldValue.text + "\n" + fixValue))
                                    Toast.makeText(context, "Applied to draft", Toast.LENGTH_SHORT).show()
                                }
                            )
                            2 -> RightEditorPane(
                                editorValue = editorFieldValue,
                                onEditorValueChange = { updateEditorText(it) },
                                selectedMode = activeState.writingMode,
                                onModeSelected = { mode ->
                                    viewModel.setWritingMode(mode)
                                    Toast.makeText(context, "Writing mode set to ${mode.displayName}. Prompt instructions updated.", Toast.LENGTH_SHORT).show()
                                },
                                undoStack = undoStack,
                                redoStack = redoStack,
                                onUndo = {
                                    if (undoStack.isNotEmpty()) {
                                        val last = undoStack.removeLast()
                                        redoStack.add(editorFieldValue.text)
                                        editorFieldValue = TextFieldValue(last)
                                        viewModel.updateEditedReplyText(last)
                                    }
                                },
                                onRedo = {
                                    if (redoStack.isNotEmpty()) {
                                        val next = redoStack.removeLast()
                                        undoStack.add(editorFieldValue.text)
                                        editorFieldValue = TextFieldValue(next)
                                        viewModel.updateEditedReplyText(next)
                                    }
                                },
                                onAiAction = { action ->
                                    val sel = editorFieldValue.getSelectedText().ifBlank { editorFieldValue.text }
                                    viewModel.applyInlineActionToSelection(sel, action) { replacement ->
                                        if (editorFieldValue.selection.length > 0) {
                                            val newText = editorFieldValue.text.replaceRange(
                                                editorFieldValue.selection.start,
                                                editorFieldValue.selection.end,
                                                replacement
                                            )
                                            updateEditorText(TextFieldValue(newText))
                                        } else {
                                            updateEditorText(TextFieldValue(replacement))
                                        }
                                    }
                                },
                                onFactCheckClick = {
                                    viewModel.performFactCheck()
                                    showFactCheckModal = true
                                },
                                onRegenerate = { viewModel.generateReply() },
                                onImproveDraft = {
                                    if (onNavigateToDraftImprovement != null) {
                                        onNavigateToDraftImprovement()
                                    } else {
                                        viewModel.improveDraft(editorFieldValue.text, "Formal official tone, clarity, and precision")
                                    }
                                },
                                onKeyShortcut = { handleGlobalShortcut(it) }
                            )
                        }
                    }
                }
            }

            // Progress Overlay Modal
            ProgressModal(
                message = activeState.progressMessage,
                onCancel = { viewModel.cancelActiveOperation() }
            )
        }
    }

    // Fact Check Report Dialog
    if (showFactCheckModal && activeState.factCheckResult != null) {
        val fc = activeState.factCheckResult!!
        AlertDialog(
            onDismissRequest = { showFactCheckModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (fc.isConsistent) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Fact Check",
                        tint = if (fc.isConsistent) VerifiedGreen else DiscrepancyRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Source vs Reply Verification", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Consistency Score: ${fc.scorePercent}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (fc.isConsistent) VerifiedGreen else DiscrepancyRed
                    )
                    Text(
                        text = fc.summaryReport,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Checked Fact Anchors:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    fc.discrepancies.forEach { disc ->
                        Surface(
                            color = if (disc.isVerified) VerifiedGreenContainer.copy(alpha = 0.5f) else DiscrepancyRedContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(disc.category, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        if (disc.isVerified) "VERIFIED" else "CHECK",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (disc.isVerified) VerifiedGreen else DiscrepancyRed
                                    )
                                }
                                Text("Source: ${disc.sourceValue}", fontSize = 12.sp, color = Navy700)
                                Text("Reply: ${disc.replyValue}", fontSize = 12.sp, color = Navy800)
                                Text("Remarks: ${disc.remarks}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFactCheckModal = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Export Format Selector Dialog
    if (showExportDialog) {
        val title = activeState.analysisResult?.subject?.takeIf { it.isNotBlank() } ?: "Official_Letter"
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val content = editorFieldValue.text.ifBlank { activeState.replyText }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export / Share Correspondence", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "SAVE TO STORAGE (FILE PICKER)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = NavyBluePrimary
                    )

                    // Save as Word .docx
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            pendingExportFormat = "docx"
                            pendingExportTitle = title
                            pendingExportContent = content
                            fileSaveLauncher.launch("${cleanTitle}.docx")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_docx_button")
                    ) {
                        Text("Save as Word Document (.docx)")
                    }

                    // Save as PDF .pdf
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            pendingExportFormat = "pdf"
                            pendingExportTitle = title
                            pendingExportContent = content
                            fileSaveLauncher.launch("${cleanTitle}.pdf")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_pdf_button")
                    ) {
                        Text("Save as Adobe PDF (.pdf)")
                    }

                    // Save as Text .txt
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            pendingExportFormat = "txt"
                            pendingExportTitle = title
                            pendingExportContent = content
                            fileSaveLauncher.launch("${cleanTitle}.txt")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_txt_button")
                    ) {
                        Text("Save as Plain Text (.txt)")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "SHARE VIA APPS (NATIVE SHARE DIALOG)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = NavyBluePrimary
                    )

                    // Share Word .docx
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            try {
                                val file = ExportManager.exportToDocx(context, title, content)
                                ExportManager.shareExportedFile(context, file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_docx_button")
                    ) {
                        Text("Share via Word (.docx)")
                    }

                    // Share PDF .pdf
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            try {
                                val file = ExportManager.exportToPdf(context, title, content)
                                ExportManager.shareExportedFile(context, file, "application/pdf")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_pdf_button")
                    ) {
                        Text("Share via PDF (.pdf)")
                    }

                    // Share Plain Text .txt
                    OutlinedButton(
                        onClick = {
                            showExportDialog = false
                            val file = ExportManager.exportToTxt(context, title, content)
                            ExportManager.shareExportedFile(context, file, "text/plain")
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_txt_button")
                    ) {
                        Text("Share via Text (.txt)")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Copy to Clipboard
                    Button(
                        onClick = {
                            showExportDialog = false
                            ExportManager.copyToClipboard(context, content, title)
                            Toast.makeText(context, "Copied letter text to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary),
                        modifier = Modifier.fillMaxWidth().testTag("export_copy_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy to Clipboard")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showHelpModal) {
        HelpShortcutsModal(
            onDismiss = { showHelpModal = false }
        )
    }
    }
}

@Composable
private fun LeftSourcePane(
    state: com.example.ui.viewmodel.ActiveLetterState,
    onSourceTextChanged: (String) -> Unit,
    onReanalyze: () -> Unit,
    onGenerateReply: () -> Unit,
    onRunFactCheck: () -> Unit,
    onApplyFix: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "SOURCE DOCUMENT & ANALYSIS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = NavyBluePrimary
            )
            Button(
                onClick = onGenerateReply,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Generate", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate Reply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (state.hasUnclearOcrWarning) {
            OcrWarningBanner(
                message = state.ocrWarningMessage ?: "Some parts of the document could not be read clearly.",
                onReviewClick = {}
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Factual Inconsistencies Widget in Editor Side Panel
        FactualInconsistenciesWidget(
            factCheckResult = state.factCheckResult,
            isChecking = state.isFactChecking,
            onRunFactCheck = onRunFactCheck,
            onApplyFix = onApplyFix
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Card
        state.analysisResult?.let { analysis ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("DOCUMENT SUMMARY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Navy700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(analysis.toFormattedSummary(), style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                }
            }
        }

        Text("Extracted Source Text (Editable):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = state.sourceRawText,
            onValueChange = onSourceTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .testTag("source_text_editor"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            placeholder = { Text("Upload or paste official incoming letter text here...") },
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun RightEditorPane(
    editorValue: TextFieldValue,
    onEditorValueChange: (TextFieldValue) -> Unit,
    selectedMode: WritingMode,
    onModeSelected: (WritingMode) -> Unit,
    undoStack: List<String>,
    redoStack: List<String>,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onAiAction: (InlineAction) -> Unit,
    onFactCheckClick: () -> Unit,
    onRegenerate: () -> Unit,
    onImproveDraft: (() -> Unit)? = null,
    onKeyShortcut: ((KeyEvent) -> Boolean)? = null
) {
    var showAiMenu by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Toolbar: Undo/Redo, Formatting, AI Mode & Actions
        Surface(
            color = Slate100,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onUndo,
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.size(34.dp).testTag("editor_undo")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onRedo,
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.size(34.dp).testTag("editor_redo")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
                    }

                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Slate200))

                    // Bold formatting
                    IconButton(
                        onClick = {
                            val sel = editorValue.getSelectedText()
                            val rep = if (sel.isNotBlank()) "**$sel**" else "**Bold Text**"
                            onEditorValueChange(editorValue.replaceSelectedText(rep))
                        },
                        modifier = Modifier.size(32.dp).testTag("format_bold")
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(18.dp))
                    }

                    // Italic formatting
                    IconButton(
                        onClick = {
                            val sel = editorValue.getSelectedText()
                            val rep = if (sel.isNotBlank()) "*$sel*" else "*Italic Text*"
                            onEditorValueChange(editorValue.replaceSelectedText(rep))
                        },
                        modifier = Modifier.size(32.dp).testTag("format_italic")
                    ) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(18.dp))
                    }

                    // Underline formatting
                    IconButton(
                        onClick = {
                            val sel = editorValue.getSelectedText()
                            val rep = if (sel.isNotBlank()) "__${sel}__" else "__Underlined Text__"
                            onEditorValueChange(editorValue.replaceSelectedText(rep))
                        },
                        modifier = Modifier.size(32.dp).testTag("format_underline")
                    ) {
                        Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", modifier = Modifier.size(18.dp))
                    }

                    // Indent formatting
                    IconButton(
                        onClick = {
                            onEditorValueChange(editorValue.indent())
                        },
                        modifier = Modifier.size(32.dp).testTag("format_indent")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, contentDescription = "Indent", modifier = Modifier.size(18.dp))
                    }

                    // Outdent formatting
                    IconButton(
                        onClick = {
                            onEditorValueChange(editorValue.outdent())
                        },
                        modifier = Modifier.size(32.dp).testTag("format_outdent")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, contentDescription = "Outdent", modifier = Modifier.size(18.dp))
                    }

                    // Numbered paragraph
                    IconButton(
                        onClick = {
                            val rep = "\n1. "
                            onEditorValueChange(editorValue.replaceSelectedText(rep))
                        },
                        modifier = Modifier.size(32.dp).testTag("format_numbered")
                    ) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", modifier = Modifier.size(18.dp))
                    }

                    // Placeholder shortcut
                    IconButton(
                        onClick = {
                            onEditorValueChange(editorValue.replaceSelectedText("[PlaceHolder]"))
                        },
                        modifier = Modifier.size(32.dp).testTag("format_placeholder")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = "Insert Placeholder", modifier = Modifier.size(18.dp))
                    }
                }

                // AI Writing Mode & Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Writing Mode Dropdown Menu
                    Box {
                        Surface(
                            onClick = { showModeMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            color = NavyBlueContainer,
                            modifier = Modifier.height(32.dp).testTag("editor_writing_mode_menu_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Mode", tint = NavyBluePrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedMode.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyBluePrimary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NavyBluePrimary, modifier = Modifier.size(14.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showModeMenu,
                            onDismissRequest = { showModeMenu = false }
                        ) {
                            WritingMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = mode.displayName,
                                                fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedMode == mode) NavyBluePrimary else Navy800
                                            )
                                            Text(
                                                text = mode.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (selectedMode == mode) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = NavyBluePrimary, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        showModeMenu = false
                                        onModeSelected(mode)
                                    },
                                    modifier = Modifier.testTag("editor_mode_option_${mode.name}")
                                )
                            }
                        }
                    }

                    // AI Commands Dropdown
                    Box {
                        Button(
                            onClick = { showAiMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary),
                            modifier = Modifier.height(32.dp).testTag("ai_commands_menu_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Actions", fontSize = 11.sp)
                        }

                        DropdownMenu(
                            expanded = showAiMenu,
                            onDismissRequest = { showAiMenu = false }
                        ) {
                            InlineAction.entries.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.label) },
                                    onClick = {
                                        showAiMenu = false
                                        onAiAction(action)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Document Editor
        OutlinedTextField(
            value = editorValue,
            onValueChange = onEditorValueChange,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    onKeyShortcut?.invoke(event) ?: false
                }
                .testTag("reply_editor_field"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                lineHeight = 22.sp
            ),
            placeholder = { Text("AI generated reply will appear here. You can freely edit, format, or apply AI rewrite actions.") },
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom Action Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onFactCheckClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("run_fact_check_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = "Fact Check", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verify Facts vs Source", fontSize = 12.sp)
                }

                if (onImproveDraft != null) {
                    OutlinedButton(
                        onClick = onImproveDraft,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("improve_draft_editor_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Improve Draft", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Improve (Ctrl+I)", fontSize = 12.sp)
                    }
                }
            }

            OutlinedButton(
                onClick = onRegenerate,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("regenerate_reply_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Regenerate", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SourceLetterTab(
    state: com.example.ui.viewmodel.ActiveLetterState,
    onSourceTextChanged: (String) -> Unit,
    onGenerateReply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (state.hasUnclearOcrWarning) {
            OcrWarningBanner(
                message = state.ocrWarningMessage ?: "Some parts of the document could not be read clearly.",
                onReviewClick = {}
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Extracted Letter Text",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Button(
                onClick = onGenerateReply,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Generate", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate Reply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.sourceRawText,
            onValueChange = onSourceTextChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .testTag("mobile_source_text"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            placeholder = { Text("Upload or paste incoming letter text here...") }
        )
    }
}

@Composable
private fun AnalysisTab(
    state: com.example.ui.viewmodel.ActiveLetterState,
    onGenerateReply: () -> Unit,
    onRunFactCheck: () -> Unit,
    onApplyFix: (String) -> Unit
) {
    val analysis = state.analysisResult
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Factual Inconsistencies Widget in Mobile Analysis tab
        FactualInconsistenciesWidget(
            factCheckResult = state.factCheckResult,
            isChecking = state.isFactChecking,
            onRunFactCheck = onRunFactCheck,
            onApplyFix = onApplyFix
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (analysis == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text("No document analysis available yet.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onGenerateReply) {
                        Text("Analyse & Generate Reply")
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("OFFICIAL DOCUMENT SUMMARY", fontWeight = FontWeight.Bold, color = NavyBluePrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    SummaryRow("From", analysis.sender)
                    SummaryRow("To", analysis.recipient)
                    SummaryRow("Letter / Ref No", analysis.letterNumber)
                    SummaryRow("Date", analysis.date)
                    SummaryRow("Subject", analysis.subject)

                    if (analysis.previousReferences.isNotEmpty()) {
                        SummaryRow("References", analysis.previousReferences.joinToString(", "))
                    }
                    if (analysis.mainIssue.isNotBlank()) {
                        SummaryRow("Main Issue", analysis.mainIssue)
                    }
                    if (analysis.actionRequiredFromRecipient.isNotBlank()) {
                        SummaryRow("Action Required", analysis.actionRequiredFromRecipient)
                    }
                    if (analysis.deadlines.isNotEmpty()) {
                        SummaryRow("Deadlines", analysis.deadlines.joinToString("; "))
                    }
                    if (analysis.quantities.isNotEmpty()) {
                        SummaryRow("Quantities", analysis.quantities.joinToString(", "))
                    }
                    if (analysis.financialFigures.isNotEmpty()) {
                        SummaryRow("Financials", analysis.financialFigures.joinToString(", "))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGenerateReply,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Reply")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Official Reply from Analysis", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Navy700,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Navy800
        )
    }
}

private fun TextFieldValue.getSelectedText(): String {
    val start = selection.min
    val end = selection.max
    return if (start < end && end <= text.length) {
        text.substring(start, end)
    } else ""
}

private fun TextFieldValue.replaceSelectedText(replacement: String): TextFieldValue {
    val start = selection.min
    val end = selection.max
    return if (start <= end && end <= text.length) {
        val newText = text.replaceRange(start, end, replacement)
        TextFieldValue(
            text = newText,
            selection = TextRange(start + replacement.length)
        )
    } else {
        TextFieldValue(text = text + replacement)
    }
}

private fun TextFieldValue.indent(): TextFieldValue {
    val start = selection.min
    val end = selection.max
    return if (start == end) {
        replaceSelectedText("    ")
    } else {
        val selected = text.substring(start, end)
        val indented = selected.lines().joinToString("\n") { "    $it" }
        replaceSelectedText(indented)
    }
}

private fun TextFieldValue.outdent(): TextFieldValue {
    val start = selection.min
    val end = selection.max
    return if (start == end) {
        val before = text.substring(0, start)
        val after = text.substring(end)
        val spacesToRemove = before.takeLast(4).takeWhile { it == ' ' }.length
        if (spacesToRemove > 0) {
            val newBefore = before.dropLast(spacesToRemove)
            TextFieldValue(text = newBefore + after, selection = TextRange(newBefore.length))
        } else this
    } else {
        val selected = text.substring(start, end)
        val outdented = selected.lines().joinToString("\n") { line ->
            when {
                line.startsWith("    ") -> line.substring(4)
                line.startsWith("\t") -> line.substring(1)
                line.startsWith("  ") -> line.substring(2)
                line.startsWith(" ") -> line.substring(1)
                else -> line
            }
        }
        replaceSelectedText(outdented)
    }
}
