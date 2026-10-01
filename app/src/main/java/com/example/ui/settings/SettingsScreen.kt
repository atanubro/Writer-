package com.example.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiProviderConfig
import com.example.data.model.AiProviderType
import com.example.ui.theme.DiscrepancyRed
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
fun SettingsScreen(
    viewModel: LetterViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val config by viewModel.aiConfig.collectAsState()
    val savedLetters by viewModel.savedLetters.collectAsState()

    // Processing mode: Cloud (false) vs Local (true)
    var isLocalMode by remember(config) { mutableStateOf(config.providerType.isLocal) }

    // Cloud models
    var selectedCloudModel by remember(config) { mutableStateOf(config.geminiModel) }
    var customApiKey by remember(config) { mutableStateOf(config.geminiApiKey) }

    // Local models
    var localEngineType by remember(config) {
        mutableStateOf(
            if (config.providerType == AiProviderType.OLLAMA) "OLLAMA" else "OFFLINE"
        )
    }
    var ollamaUrl by remember(config) { mutableStateOf(config.ollamaEndpoint) }
    var ollamaModelName by remember(config) { mutableStateOf(config.ollamaModel) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI & Privacy Settings", fontWeight = FontWeight.Bold)
                        Text(
                            "Select AI models and toggle Cloud vs Local modes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Processing Mode Toggle Section (Cloud vs Local)
            Text(
                text = "PROCESSING MODE (PRIVACY SELECTION)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = NavyBluePrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cloud Mode Card
                ModeSelectionCard(
                    title = "Cloud Mode",
                    subtitle = "High Accuracy & Vision",
                    icon = Icons.Default.Cloud,
                    isSelected = !isLocalMode,
                    activeColor = NavyBluePrimary,
                    containerColor = if (!isLocalMode) NavyBlueContainer else Slate100,
                    modifier = Modifier.weight(1f).testTag("mode_toggle_cloud"),
                    onClick = { isLocalMode = false }
                )

                // Local / Private Mode Card
                ModeSelectionCard(
                    title = "Local Mode",
                    subtitle = "100% On-Device / Private",
                    icon = Icons.Default.Lock,
                    isSelected = isLocalMode,
                    activeColor = VerifiedGreen,
                    containerColor = if (isLocalMode) VerifiedGreenContainer else Slate100,
                    modifier = Modifier.weight(1f).testTag("mode_toggle_local"),
                    onClick = { isLocalMode = true }
                )
            }

            // Privacy Confirmation Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocalMode) VerifiedGreenContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = if (isLocalMode) Icons.Default.Lock else Icons.Default.CloudQueue,
                        contentDescription = "Privacy Status",
                        tint = if (isLocalMode) VerifiedGreen else NavyBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isLocalMode) "Local Confidentiality Tier Active" else "Cloud Intelligence Tier Active",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isLocalMode) VerifiedGreen else NavyBluePrimary
                        )
                        Text(
                            text = if (isLocalMode)
                                "Zero document content leaves your device. Fully compliant with government non-disclosure and departmental privacy protocols."
                            else
                                "Letters are processed via Google Gemini with SSL encryption. No document text is permanently cached remotely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Navy800
                        )
                    }
                }
            }

            // AI Model Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (!isLocalMode) "SELECT CLOUD AI MODEL" else "SELECT LOCAL AI ENGINE",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = NavyBluePrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isLocalMode) {
                        // Cloud Models List
                        val cloudModels = listOf(
                            Triple("gemini-3.5-flash", "Gemini 3.5 Flash (Recommended)", "Fast, high accuracy for office correspondence & proofreading"),
                            Triple("gemini-3.1-pro-preview", "Gemini 3.1 Pro Preview", "Advanced reasoning for complex departmental issues & technical letters"),
                            Triple("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash-Lite Preview", "Ultra-low latency for quick drafts and summaries")
                        )

                        cloudModels.forEach { (modelId, label, desc) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCloudModel = modelId }
                                    .padding(vertical = 6.dp)
                                    .testTag("model_option_$modelId")
                            ) {
                                RadioButton(
                                    selected = selectedCloudModel == modelId,
                                    onClick = { selectedCloudModel = modelId }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            label = { Text("Custom Gemini API Key (Optional)") },
                            placeholder = { Text("Leave blank to use preconfigured studio key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_custom_key")
                        )
                    } else {
                        // Local Engines Selection
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { localEngineType = "OFFLINE" }
                                .padding(vertical = 6.dp)
                                .testTag("engine_option_offline")
                        ) {
                            RadioButton(
                                selected = localEngineType == "OFFLINE",
                                onClick = { localEngineType = "OFFLINE" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Built-in Office Intelligence (On-Device)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("100% offline, zero setup, deterministic departmental letter drafting", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { localEngineType = "OLLAMA" }
                                .padding(vertical = 6.dp)
                                .testTag("engine_option_ollama")
                        ) {
                            RadioButton(
                                selected = localEngineType == "OLLAMA",
                                onClick = { localEngineType = "OLLAMA" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Ollama Local Model Server", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Run local LLM (llama3, mistral, qwen) via HTTP server", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        AnimatedVisibility(visible = localEngineType == "OLLAMA") {
                            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ollamaUrl,
                                    onValueChange = { ollamaUrl = it },
                                    label = { Text("Ollama Server Endpoint URL") },
                                    placeholder = { Text("http://10.0.2.2:11434") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ollama_endpoint_input")
                                )
                                OutlinedTextField(
                                    value = ollamaModelName,
                                    onValueChange = { ollamaModelName = it },
                                    label = { Text("Model Name") },
                                    placeholder = { Text("llama3, mistral, or qwen") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ollama_model_input")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val newProviderType = if (!isLocalMode) {
                                AiProviderType.GEMINI
                            } else {
                                if (localEngineType == "OLLAMA") AiProviderType.OLLAMA else AiProviderType.OFFLINE_ENGINE
                            }

                            val newConfig = config.copy(
                                providerType = newProviderType,
                                geminiModel = selectedCloudModel,
                                geminiApiKey = customApiKey,
                                ollamaEndpoint = ollamaUrl,
                                ollamaModel = ollamaModelName
                            )
                            viewModel.updateAiConfig(newConfig)
                            Toast.makeText(context, "Settings saved: ${viewModel.aiProviderManager.providerBadgeText}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("apply_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply & Save AI Configuration")
                    }
                }
            }

            // Local Room Database & Storage Management
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = "Storage", tint = NavyBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROOM DATABASE PERSISTENCE",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = NavyBluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Correspondence Records:", style = MaterialTheme.typography.bodyMedium)
                        Text("${savedLetters.size} letters", fontWeight = FontWeight.Bold, color = Navy800)
                    }

                    Text(
                        text = "All recent correspondence, extracted document texts, summaries, and finalized drafts are preserved locally using SQLite via Android Room.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DiscrepancyRed),
                        modifier = Modifier.fillMaxWidth().testTag("delete_all_data_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete All Stored Documents & History")
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Correspondence Data?") },
            text = {
                Text("This action will irreversibly remove all saved letters, extracted text, and document history from your device's Room database.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "All local document data deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiscrepancyRed)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModeSelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
            .height(95.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) activeColor else Slate200,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = activeColor,
                    modifier = Modifier.size(22.dp)
                )

                if (isSelected) {
                    Surface(
                        color = activeColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Navy800
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Navy700
                )
            }
        }
    }
}
