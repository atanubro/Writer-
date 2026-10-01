package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.NavyBlueContainer
import com.example.ui.theme.NavyBluePrimary
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200

/**
 * Help overlay / modal displaying keyboard shortcuts, voice dictation commands,
 * and AI editor navigation guidance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpShortcutsModal(
    onDismiss: () -> Unit,
    initialTab: Int = 0
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("help_shortcuts_modal"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyBlueContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Help",
                                    tint = NavyBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Editor & AI Navigation Help",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NavyBluePrimary
                            )
                            Text(
                                text = "Quick reference for shortcuts, voice commands, and AI features",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_help_modal_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("help_search_input"),
                    placeholder = { Text("Search shortcuts, voice commands, or actions...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Keyboard Shortcuts | Voice Commands | AI Tools
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Keyboard Shortcuts")
                            }
                        },
                        modifier = Modifier.testTag("help_tab_shortcuts")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Voice Commands")
                            }
                        },
                        modifier = Modifier.testTag("help_tab_voice")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI & Tools")
                            }
                        },
                        modifier = Modifier.testTag("help_tab_ai")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> KeyboardShortcutsSection(searchQuery)
                        1 -> VoiceCommandsSection(searchQuery)
                        2 -> AiToolsSection(searchQuery)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyBluePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close Reference")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardShortcutsSection(query: String) {
    val items = listOf(
        ShortcutItem("Ctrl + S", "Save Draft", "Saves current letter to local correspondence database"),
        ShortcutItem("Ctrl + I", "Improve Draft", "Initiates AI tone and grammar improvement or formal rewrite"),
        ShortcutItem("Ctrl + T / \\", "Toggle Split / Tab View", "Switches between side-by-side split screen and tabbed layout on desktop"),
        ShortcutItem("F10", "Toggle View Mode", "Quick key to alternate desktop layout mode"),
        ShortcutItem("F1 / Ctrl + H", "Open Help Reference", "Displays this keyboard shortcuts and voice guide"),
        ShortcutItem("Ctrl + Z", "Undo Action", "Reverts previous change in document editor"),
        ShortcutItem("Ctrl + Y", "Redo Action", "Restores next change from edit history stack"),
        ShortcutItem("Ctrl + B", "Bold Selection", "Wraps selected text in **bold** markdown tags"),
        ShortcutItem("Ctrl + U", "Underline Selection", "Wraps selected text in <u>underline</u> tags"),
        ShortcutItem("Tab / Shift+Tab", "Indent / Outdent", "Adds or removes 4-space paragraph indentation")
    ).filter {
        query.isBlank() ||
        it.keys.contains(query, ignoreCase = true) ||
        it.action.contains(query, ignoreCase = true) ||
        it.description.contains(query, ignoreCase = true)
    }

    if (items.isEmpty()) {
        Text("No shortcuts matching \"$query\"", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(8.dp))
    } else {
        items.forEach { item ->
            ShortcutCard(item)
        }
    }
}

@Composable
private fun VoiceCommandsSection(query: String) {
    val commands = listOf(
        VoiceCommandItem(
            pattern = "“Write a letter requesting replacement of [Item]...”",
            intent = "Requisition Letter",
            notes = "Dictates a formal request note with quantities, asset IDs, and urgency."
        ),
        VoiceCommandItem(
            pattern = "“Draft an urgent reminder for non-submission of [Report]...”",
            intent = "Reminder Dispatch",
            notes = "Creates a strict follow-up letter referencing prior communication."
        ),
        VoiceCommandItem(
            pattern = "“Formal request for sanction of estimate for [Project]...”",
            intent = "Financial Sanction",
            notes = "Formats financial estimate approval letters with monetary figures."
        ),
        VoiceCommandItem(
            pattern = "“Notice to contractor regarding delay in work progress...”",
            intent = "Contractor Notice",
            notes = "Drafts show-cause/liquidated damages notice citing clause numbers."
        ),
        VoiceCommandItem(
            pattern = "“Forwarding audit compliance along with verified vouchers...”",
            intent = "Audit Forwarding",
            notes = "Structures audit response and list of enclosure references."
        ),
        VoiceCommandItem(
            pattern = "“Tap Microphone Button”",
            intent = "Start Dictation",
            notes = "Streams speech in real-time. Supports English, Hindi, and Bengali."
        ),
        VoiceCommandItem(
            pattern = "“Tap Done / Stop Button”",
            intent = "Finish Voice Input",
            notes = "Appends recognized transcription into your instructions field."
        )
    ).filter {
        query.isBlank() ||
        it.pattern.contains(query, ignoreCase = true) ||
        it.intent.contains(query, ignoreCase = true) ||
        it.notes.contains(query, ignoreCase = true)
    }

    if (commands.isEmpty()) {
        Text("No voice commands matching \"$query\"", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(8.dp))
    } else {
        commands.forEach { cmd ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate100),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cmd.pattern, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyBluePrimary)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NavyBlueContainer
                        ) {
                            Text(
                                text = cmd.intent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyBluePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(cmd.notes, fontSize = 11.sp, color = Navy700)
                }
            }
        }
    }
}

@Composable
private fun AiToolsSection(query: String) {
    val aiFeatures = listOf(
        AiFeatureItem(
            name = "Document Analysis",
            description = "Extracts Letter No., Date, From, To, Subject, Deadlines, Quantities, and Financial Figures."
        ),
        AiFeatureItem(
            name = "Factual Inconsistencies Check",
            description = "Compares source letter against generated reply to detect mismatched dates, references, or amounts."
        ),
        AiFeatureItem(
            name = "Writing Modes",
            description = "Select between Official, Formal, Very Formal, Concise, Polite, or Urgent tones."
        ),
        AiFeatureItem(
            name = "Multi-Format Export",
            description = "Export to Word (.docx with OOXML), Adobe PDF (paginated), or Plain Text (.txt) via system picker or native share sheet."
        ),
        AiFeatureItem(
            name = "Camera OCR Scanner",
            description = "Capture physical correspondence photos using CameraX and extract letterheads with ML Kit text recognition."
        )
    ).filter {
        query.isBlank() ||
        it.name.contains(query, ignoreCase = true) ||
        it.description.contains(query, ignoreCase = true)
    }

    aiFeatures.forEach { feat ->
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate100),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(feat.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyBluePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(feat.description, fontSize = 11.sp, color = Navy800)
            }
        }
    }
}

private data class ShortcutItem(val keys: String, val action: String, val description: String)
private data class VoiceCommandItem(val pattern: String, val intent: String, val notes: String)
private data class AiFeatureItem(val name: String, val description: String)

@Composable
private fun ShortcutCard(item: ShortcutItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate100),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy800)
                Text(item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
            ) {
                Text(
                    text = item.keys,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = NavyBluePrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
