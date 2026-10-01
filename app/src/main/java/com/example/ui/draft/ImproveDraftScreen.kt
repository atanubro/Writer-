package com.example.ui.draft

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ProgressModal
import com.example.ui.home.ModeAndLanguageBar
import com.example.ui.theme.Navy800
import com.example.ui.theme.NavyBluePrimary
import com.example.ui.theme.Slate200
import com.example.ui.viewmodel.LetterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImproveDraftScreen(
    viewModel: LetterViewModel,
    onNavigateBack: () -> Unit,
    onDraftImproved: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val activeState by viewModel.activeState.collectAsState()

    var draftText by remember { mutableStateOf(activeState.sourceRawText.ifBlank { "" }) }
    var specificFocus by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Improve My Draft", fontWeight = FontWeight.Bold)
                        Text(
                            "Official grammar, clarity & protocol enhancement",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("draft_back_button")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DRAFT TEXT",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = NavyBluePrimary
                        )

                        OutlinedButton(
                            onClick = {
                                clipboardManager.getText()?.let {
                                    draftText = it.text
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("paste_draft_button")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste from Clipboard", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .testTag("draft_input_field"),
                        placeholder = {
                            Text("Type, paste, or draft your letter here...\n\nExample:\nSir, regarding the repair of generator at site B, the contractor visited yesterday but spare parts are still not received. Please do the needful.")
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Mode and Language Bar
            ModeAndLanguageBar(
                selectedMode = activeState.writingMode,
                selectedLanguage = activeState.outputLanguage,
                onModeSelected = { viewModel.setWritingMode(it) },
                onLanguageSelected = { viewModel.setOutputLanguage(it) }
            )

            // Optional Focus input
            OutlinedTextField(
                value = specificFocus,
                onValueChange = { specificFocus = it },
                label = { Text("Special Focus Instructions (Optional)") },
                placeholder = { Text("e.g., Make it strictly formal, emphasize compliance deadline") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("draft_focus_field"),
                shape = RoundedCornerShape(8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Important Rule: AI will elevate grammar, paragraph sequence, and tone without arbitrarily modifying names, dates, amounts, quantities, or reference numbers.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Navy800,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Button(
                onClick = {
                    if (draftText.isNotBlank()) {
                        viewModel.improveDraft(draftText, specificFocus.takeIf { it.isNotBlank() })
                        onDraftImproved()
                    }
                },
                enabled = draftText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("execute_improve_draft_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Improve")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Improve & Elevate Draft", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        ProgressModal(
            message = activeState.progressMessage,
            onCancel = { viewModel.cancelActiveOperation() }
        )
    }
}
