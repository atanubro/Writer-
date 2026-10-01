package com.example.ui.newletter

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OutputLanguage
import com.example.ui.components.HelpShortcutsModal
import com.example.ui.components.ProgressModal
import com.example.ui.components.VoiceDictationWidget
import com.example.ui.home.ModeAndLanguageBar
import com.example.ui.theme.Navy800
import com.example.ui.theme.NavyBluePrimary
import com.example.ui.theme.Slate200
import com.example.ui.viewmodel.LetterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLetterScreen(
    viewModel: LetterViewModel,
    onNavigateBack: () -> Unit,
    onLetterCreated: () -> Unit
) {
    val activeState by viewModel.activeState.collectAsState()
    var instructionText by remember { mutableStateOf("") }
    var showHelpModal by remember { mutableStateOf(false) }

    val samplePrompts = listOf(
        "Write a letter requesting replacement of two unserviceable batteries for departmental vehicle.",
        "Draft an urgent reminder for non-submission of monthly progress report.",
        "Formal request for sanction of estimate for annual maintenance of office premises.",
        "Notice to contractor regarding delay in work progress and imposition of liquidated damages.",
        "Forwarding audit reply along with verified compliance vouchers."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Create New Official Letter", fontWeight = FontWeight.Bold)
                        Text(
                            "Generate structured correspondence from concise instructions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("create_letter_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHelpModal = true },
                        modifier = Modifier.testTag("create_letter_help_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Voice & Shortcut Help")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "INSTRUCTIONS & CONTEXT",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = NavyBluePrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Microphone Recording & Voice Dictation Widget
                    VoiceDictationWidget(
                        onTextDictated = { dictated ->
                            instructionText = if (instructionText.isBlank()) {
                                dictated
                            } else {
                                "${instructionText.trim()}\n$dictated"
                            }
                        },
                        languageCode = when (activeState.outputLanguage) {
                            OutputLanguage.BENGALI -> "bn-IN"
                            OutputLanguage.HINDI -> "hi-IN"
                            else -> "en-US"
                        },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = instructionText,
                        onValueChange = { instructionText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("new_letter_instructions_input"),
                        placeholder = {
                            Text("Describe the letter you need drafted...\n\nExample: 'Write a letter requesting replacement of two unserviceable batteries for inspection vehicle WB-02-1234.'")
                        },
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Quick Sample Instructions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(samplePrompts) { prompt ->
                            SuggestionChip(
                                onClick = { instructionText = prompt },
                                label = { Text(prompt.take(34) + "...", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Mode and Language selector
            ModeAndLanguageBar(
                selectedMode = activeState.writingMode,
                selectedLanguage = activeState.outputLanguage,
                onModeSelected = { viewModel.setWritingMode(it) },
                onLanguageSelected = { viewModel.setOutputLanguage(it) }
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Placeholder Rule: Before inventing missing official details, editable placeholders such as [Letter No.], [Date], [Vehicle No.], [Quantity], and [Authority] will be inserted for review.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Navy800,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Button(
                onClick = {
                    if (instructionText.isNotBlank()) {
                        viewModel.createNewLetterFromInstructions(instructionText)
                        onLetterCreated()
                    }
                },
                enabled = instructionText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_create_letter_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Draft")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Complete Letter", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        ProgressModal(
            message = activeState.progressMessage,
            onCancel = { viewModel.cancelActiveOperation() }
        )

        if (showHelpModal) {
            HelpShortcutsModal(
                onDismiss = { showHelpModal = false },
                initialTab = 1
            )
        }
    }
}
