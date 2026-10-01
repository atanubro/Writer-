package com.example.ui.home

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LetterDocumentEntity
import com.example.data.model.DocumentAnalysisResult
import com.example.data.model.OutputLanguage
import com.example.data.model.WritingMode
import com.example.ui.camera.CameraScanDialog
import com.example.ui.components.HelpShortcutsModal
import com.example.ui.components.PrivacyBadge
import com.example.ui.components.ProgressModal
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LetterViewModel,
    onNavigateToWorkspace: () -> Unit,
    onNavigateToDraftImprovement: () -> Unit,
    onNavigateToCreateLetter: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val activeState by viewModel.activeState.collectAsState()
    val savedLetters by viewModel.savedLetters.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showUploadSourceDialog by remember { mutableStateOf(false) }
    var showCameraScanDialog by remember { mutableStateOf(false) }
    var showHelpModal by remember { mutableStateOf(false) }

    // File Picker for PDF, DOCX, XLSX, TXT, Images
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.loadDocumentFromUri(uri, fileName, autoAnalyze = true)
            onNavigateToWorkspace()
        }
    }

    // Camera Launcher for capturing photo of official letter
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.loadDocumentFromBitmap(bitmap)
            onNavigateToWorkspace()
        }
    }

    val filteredLetters = remember(savedLetters, searchQuery) {
        if (searchQuery.isBlank()) savedLetters
        else savedLetters.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.sourceRawText.contains(searchQuery, ignoreCase = true) ||
            it.editedReplyText.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Office Letter AI",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Executive Correspondence & Reply Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showUploadSourceDialog = true },
                        modifier = Modifier.testTag("top_attach_file_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Attach File", tint = NavyBluePrimary)
                    }
                    PrivacyBadge(
                        isLocal = viewModel.aiProviderManager.isCurrentProviderLocal,
                        badgeText = viewModel.aiProviderManager.providerBadgeText,
                        onClick = onNavigateToSettings
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showHelpModal = true },
                        modifier = Modifier.testTag("home_help_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help & Shortcuts")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Document Summary Widget (Processes From, To, Date, Subject, Key Points)
                item {
                    DocumentSummaryWidget(
                        analysis = activeState.analysisResult,
                        sourceFileName = activeState.sourceFileName,
                        sourceFileType = activeState.sourceFileType,
                        sourceRawText = activeState.sourceRawText,
                        hasReply = activeState.editedReplyText.isNotBlank(),
                        onGenerateReply = {
                            viewModel.generateReply()
                            onNavigateToWorkspace()
                        },
                        onOpenInWorkspace = onNavigateToWorkspace,
                        onUploadClick = { showUploadSourceDialog = true },
                        onLoadSample = { viewModel.loadSampleLetter() },
                        onClear = { viewModel.clearActiveDocument() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Four Primary Actions Grid (Responsive for Mobile & Desktop)
                item {
                    Text(
                        text = "PRIMARY ACTIONS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 700.dp
                        if (isWide) {
                            // 4 columns in a single row on desktop/wide tablet
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PrimaryActionCard(
                                    title = "Upload Letter",
                                    subtitle = "Import & Parse",
                                    tags = listOf("PDF", "DOCX", "XLSX", "IMG", "TXT"),
                                    icon = Icons.Default.UploadFile,
                                    accentColor = NavyBluePrimary,
                                    containerColor = NavyBlueContainer,
                                    modifier = Modifier.weight(1f).testTag("action_upload_letter"),
                                    onClick = { showUploadSourceDialog = true }
                                )

                                PrimaryActionCard(
                                    title = "Generate Reply",
                                    subtitle = "Formal response",
                                    tags = listOf("Analysis", "Reply", "Facts"),
                                    icon = Icons.Default.AutoAwesome,
                                    accentColor = GoldAccent,
                                    containerColor = Color(0xFFFEF3C7),
                                    modifier = Modifier.weight(1f).testTag("action_generate_reply"),
                                    onClick = {
                                        if (activeState.sourceRawText.isNotBlank()) {
                                            viewModel.generateReply()
                                            onNavigateToWorkspace()
                                        } else {
                                            showUploadSourceDialog = true
                                        }
                                    }
                                )

                                PrimaryActionCard(
                                    title = "Improve Draft",
                                    subtitle = "Tone & grammar",
                                    tags = listOf("Grammar", "Clarity", "Tone"),
                                    icon = Icons.Default.EditNote,
                                    accentColor = Color(0xFF0D9488),
                                    containerColor = Color(0xFFCCFBF1),
                                    modifier = Modifier.weight(1f).testTag("action_improve_draft"),
                                    onClick = onNavigateToDraftImprovement
                                )

                                PrimaryActionCard(
                                    title = "Create Letter",
                                    subtitle = "From instruction",
                                    tags = listOf("Departmental", "Templates"),
                                    icon = Icons.Default.Add,
                                    accentColor = Color(0xFF4338CA),
                                    containerColor = Color(0xFFE0E7FF),
                                    modifier = Modifier.weight(1f).testTag("action_create_letter"),
                                    onClick = onNavigateToCreateLetter
                                )
                            }
                        } else {
                            // 2x2 grid on mobile and portrait tablets
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PrimaryActionCard(
                                        title = "Upload Letter",
                                        subtitle = "Import & Parse",
                                        tags = listOf("PDF", "DOCX", "XLSX", "IMG", "TXT"),
                                        icon = Icons.Default.UploadFile,
                                        accentColor = NavyBluePrimary,
                                        containerColor = NavyBlueContainer,
                                        modifier = Modifier.weight(1f).testTag("action_upload_letter"),
                                        onClick = { showUploadSourceDialog = true }
                                    )

                                    PrimaryActionCard(
                                        title = "Generate Reply",
                                        subtitle = "Formal response",
                                        tags = listOf("Analysis", "Reply", "Facts"),
                                        icon = Icons.Default.AutoAwesome,
                                        accentColor = GoldAccent,
                                        containerColor = Color(0xFFFEF3C7),
                                        modifier = Modifier.weight(1f).testTag("action_generate_reply"),
                                        onClick = {
                                            if (activeState.sourceRawText.isNotBlank()) {
                                                viewModel.generateReply()
                                                onNavigateToWorkspace()
                                            } else {
                                                showUploadSourceDialog = true
                                            }
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PrimaryActionCard(
                                        title = "Improve Draft",
                                        subtitle = "Tone & grammar",
                                        tags = listOf("Grammar", "Clarity", "Tone"),
                                        icon = Icons.Default.EditNote,
                                        accentColor = Color(0xFF0D9488),
                                        containerColor = Color(0xFFCCFBF1),
                                        modifier = Modifier.weight(1f).testTag("action_improve_draft"),
                                        onClick = onNavigateToDraftImprovement
                                    )

                                    PrimaryActionCard(
                                        title = "Create Letter",
                                        subtitle = "From instruction",
                                        tags = listOf("Official", "Placeholders"),
                                        icon = Icons.Default.Add,
                                        accentColor = Color(0xFF4338CA),
                                        containerColor = Color(0xFFE0E7FF),
                                        modifier = Modifier.weight(1f).testTag("action_create_letter"),
                                        onClick = onNavigateToCreateLetter
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Mode and Output Language Selection Controls
                item {
                    ModeAndLanguageBar(
                        selectedMode = activeState.writingMode,
                        selectedLanguage = activeState.outputLanguage,
                        onModeSelected = { viewModel.setWritingMode(it) },
                        onLanguageSelected = { viewModel.setOutputLanguage(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Templates & Reference Row
                item {
                    Card(
                        onClick = onNavigateToTemplates,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("templates_entry_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Templates",
                                tint = NavyBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Official Departmental Templates",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Government replies, reminders, requisitions, deficiency reports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "View Templates",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Recent Documents Header & Search
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Recent Documents",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RECENT DOCUMENTS",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "${savedLetters.size} in Room DB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by subject, letter no., or text...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Recent Documents List
                if (filteredLetters.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Empty",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "No correspondence history yet" else "No matches found",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Uploaded letters and generated replies will be saved locally.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(filteredLetters) { letter ->
                        RecentLetterItem(
                            letter = letter,
                            onOpen = {
                                viewModel.loadFromHistory(letter)
                                onNavigateToWorkspace()
                            },
                            onDelete = {
                                viewModel.deleteLetter(letter.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Progress Modal during processing
            ProgressModal(
                message = activeState.progressMessage,
                onCancel = { viewModel.cancelActiveOperation() }
            )
        }
    }

    // Upload Source Dialog (File vs Camera)
    if (showUploadSourceDialog) {
        AlertDialog(
            onDismissRequest = { showUploadSourceDialog = false },
            title = {
                Text("Attach Official Letter", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Select incoming official letter source. Accepted formats: PDF, Word (.docx), Excel (.xlsx), Images (.jpg, .png), Text (.txt).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        onClick = {
                            showUploadSourceDialog = false
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "text/plain",
                                    "image/*",
                                    "*/*"
                                )
                            )
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("pick_file_option")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Browse Files", tint = NavyBluePrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Browse Documents / Files", fontWeight = FontWeight.SemiBold)
                                Text("PDF, DOCX, XLSX, TXT, Photos", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        onClick = {
                            showUploadSourceDialog = false
                            showCameraScanDialog = true
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("scan_from_camera_option")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = GoldAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Scan from Camera", fontWeight = FontWeight.SemiBold)
                                Text("Take photo with CameraX & extract text via ML Kit OCR", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUploadSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CameraX Document Scanning Dialog
    if (showCameraScanDialog) {
        CameraScanDialog(
            onDismissRequest = { showCameraScanDialog = false },
            onPhotoCaptured = { bitmap ->
                showCameraScanDialog = false
                viewModel.loadDocumentFromBitmap(bitmap)
                onNavigateToWorkspace()
            }
        )
    }

    if (showHelpModal) {
        HelpShortcutsModal(
            onDismiss = { showHelpModal = false }
        )
    }
}

@Composable
private fun PrimaryActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    tags: List<String> = emptyList(),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        shadowElevation = 1.dp,
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (tags.isNotEmpty()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tags.take(3).joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Navy800
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Navy800.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActiveDocumentBanner(
    fileName: String,
    hasReply: Boolean,
    onResume: () -> Unit,
    onGenerate: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .testTag("active_doc_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Active Doc",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasReply) "Reply draft prepared & ready for editing" else "Document loaded & parsed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = if (hasReply) onResume else onGenerate,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (hasReply) "Open Editor" else "Generate Reply", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ModeAndLanguageBar(
    selectedMode: WritingMode,
    selectedLanguage: OutputLanguage,
    onModeSelected: (WritingMode) -> Unit,
    onLanguageSelected: (OutputLanguage) -> Unit
) {
    var languageDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Output Language Dropdown Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Output Language",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Navy800
                    )
                    Text(
                        text = "Active language for AI generation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Dropdown Menu for Output Language
                Box {
                    Surface(
                        onClick = { languageDropdownExpanded = true },
                        color = NavyBlueContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("output_language_dropdown_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = NavyBluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedLanguage.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = NavyBluePrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = NavyBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false }
                    ) {
                        OutputLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = lang.displayName,
                                            fontWeight = if (selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedLanguage == lang) NavyBluePrimary else Navy800
                                        )
                                        if (selectedLanguage == lang) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = NavyBluePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onLanguageSelected(lang)
                                    languageDropdownExpanded = false
                                },
                                modifier = Modifier.testTag("language_option_${lang.name}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Writing Mode selector
            Text(
                text = "Writing Mode: ${selectedMode.displayName}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(WritingMode.entries) { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.displayName, fontSize = 11.sp) },
                        modifier = Modifier.testTag("mode_chip_${mode.name}")
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentSummaryWidget(
    analysis: DocumentAnalysisResult?,
    sourceFileName: String,
    sourceFileType: String,
    sourceRawText: String,
    hasReply: Boolean,
    onGenerateReply: () -> Unit,
    onOpenInWorkspace: () -> Unit,
    onUploadClick: () -> Unit,
    onLoadSample: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(14.dp))
            .testTag("document_summary_widget")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .background(NavyBlueContainer, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Document Summary",
                            tint = NavyBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DOCUMENT SUMMARY",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = NavyBluePrimary
                        )
                        if (sourceFileName.isNotBlank()) {
                            Text(
                                text = sourceFileName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (sourceRawText.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = NavyBlueContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = sourceFileType.ifBlank { "DOC" },
                                color = NavyBluePrimary,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.size(28.dp).testTag("clear_summary_button")
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sourceRawText.isNotBlank() && analysis != null) {
                // Metadata Table: From, To, Date, Subject
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetadataRow(label = "From", value = analysis.sender.ifBlank { "The Submitting Authority / Office" })
                        MetadataRow(label = "To", value = analysis.recipient.ifBlank { "Competent Authority" })
                        MetadataRow(label = "Date", value = analysis.date.ifBlank { "Not specified" })
                        MetadataRow(label = "Subject", value = analysis.subject.ifBlank { "Departmental correspondence" }, isBold = true)
                    }
                }

                // Key Points Section
                val keyPoints = buildList {
                    if (analysis.mainIssue.isNotBlank()) add(analysis.mainIssue)
                    addAll(analysis.requestsOrInstructions)
                    if (analysis.deadlines.isNotEmpty()) add("Deadline: ${analysis.deadlines.joinToString("; ")}")
                    if (analysis.quantities.isNotEmpty()) add("Quantities / Units: ${analysis.quantities.joinToString(", ")}")
                    if (analysis.importantFacts.isNotEmpty() && size < 4) addAll(analysis.importantFacts.take(2))
                }

                if (keyPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "KEY POINTS & ACTIONS:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = Navy700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        keyPoints.take(4).forEach { point ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("• ", fontWeight = FontWeight.Bold, color = NavyBluePrimary, fontSize = 13.sp)
                                Text(
                                    text = point,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Navy800,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onGenerateReply,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier.weight(1f).testTag("summary_generate_reply_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Reply", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (hasReply) "Regenerate Reply" else "Generate Reply", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenInWorkspace,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("summary_open_workspace_button")
                    ) {
                        Text(if (hasReply) "Open Editor" else "View Workspace", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (sourceRawText.isNotBlank()) {
                // Text loaded but analyzing
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Document text loaded. Generating structured metadata summary...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenInWorkspace,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open in Workspace")
                    }
                }
            } else {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No letter uploaded yet. Upload an incoming official document to automatically extract From, To, Date, Subject, and Key Points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onUploadClick,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("summary_upload_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Letter")
                        }

                        Button(
                            onClick = onLoadSample,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary),
                            modifier = Modifier.testTag("summary_sample_button")
                        ) {
                            Text("Try Sample Letter")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Navy700,
            modifier = Modifier.width(68.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = Navy800,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RecentLetterItem(
    letter: LetterDocumentEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(letter.timestamp) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(letter.timestamp))
    }
    val snippet = remember(letter.editedReplyText, letter.sourceRawText) {
        val raw = letter.editedReplyText.ifBlank { letter.sourceRawText }
        raw.lines().firstOrNull { it.isNotBlank() && !it.startsWith("To", ignoreCase = true) }?.take(80) ?: ""
    }

    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            .testTag("recent_item_${letter.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .background(NavyBlueContainer, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Document",
                        tint = NavyBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = letter.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Navy800
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("delete_recent_${letter.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete from Room DB",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // File Type Pill
                    val fileType = letter.sourceFileType ?: "DOC"
                    Surface(
                        color = NavyBlueContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = fileType,
                            color = NavyBluePrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Mode Pill
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = letter.writingMode,
                            color = Navy700,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Status Pill
                    Surface(
                        color = if (letter.status == "Finalized") VerifiedGreenContainer else Slate100,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = letter.status,
                            color = if (letter.status == "Finalized") VerifiedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Open in Editor →",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = NavyBluePrimary,
                    modifier = Modifier.clickable { onOpen() }
                )
            }
        }
    }
}
