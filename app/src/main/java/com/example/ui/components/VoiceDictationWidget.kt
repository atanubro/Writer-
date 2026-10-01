package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.speech.SpeechToTextManager
import com.example.ui.theme.DiscrepancyRed
import com.example.ui.theme.DiscrepancyRedContainer
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.NavyBlueContainer
import com.example.ui.theme.NavyBluePrimary
import com.example.ui.theme.Slate200

@Composable
fun VoiceDictationWidget(
    onTextDictated: (String) -> Unit,
    languageCode: String = "en-US",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val speechManager = remember { SpeechToTextManager(context) }

    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var rmsValue by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Fallback Speech Intent for devices where SpeechRecognizer direct binding is restricted
    val speechIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                onTextDictated(spoken)
                Toast.makeText(context, "Voice dictation captured", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startListeningWithManager() {
        isListening = true
        partialText = ""
        speechManager.startListening(
            languageTag = languageCode,
            onPartialResult = { text ->
                partialText = text
            },
            onFinalResult = { text ->
                isListening = false
                partialText = ""
                onTextDictated(text)
                Toast.makeText(context, "Voice input added", Toast.LENGTH_SHORT).show()
            },
            onError = { errMsg ->
                isListening = false
                partialText = ""
                // If service is unavailable or had client error, try the system dialog intent
                try {
                    val intent = SpeechToTextManager.createSpeechIntent(
                        prompt = "Speak letter instructions...",
                        languageTag = languageCode
                    )
                    speechIntentLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                }
            },
            onRmsChanged = { rms ->
                rmsValue = rms
            }
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListeningWithManager()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice dictation", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMicClick() {
        if (isListening) {
            speechManager.stopListening()
            isListening = false
            if (partialText.isNotBlank()) {
                onTextDictated(partialText)
                partialText = ""
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startListeningWithManager()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.stopListening()
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isListening) DiscrepancyRedContainer.copy(alpha = 0.5f) else NavyBlueContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isListening) DiscrepancyRed else Slate200,
                RoundedCornerShape(12.dp)
            )
            .testTag("voice_dictation_widget")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(pulseScale)
                                    .background(DiscrepancyRed.copy(alpha = 0.25f), CircleShape)
                            )
                        }
                        Surface(
                            onClick = { handleMicClick() },
                            shape = CircleShape,
                            color = if (isListening) DiscrepancyRed else NavyBluePrimary,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("dictate_microphone_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Stop Recording" else "Start Microphone Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isListening) "Listening for dictation..." else "Voice Dictation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isListening) DiscrepancyRed else Navy800
                        )
                        Text(
                            text = if (isListening) "Speak instructions clearly into microphone" else "Tap mic to dictate letter instructions",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isListening) {
                    OutlinedButton(
                        onClick = {
                            speechManager.stopListening()
                            isListening = false
                            if (partialText.isNotBlank()) {
                                onTextDictated(partialText)
                                partialText = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("stop_dictation_button")
                    ) {
                        Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DiscrepancyRed)
                    }
                } else {
                    // Demo voice sample button for emulators or testing
                    OutlinedButton(
                        onClick = {
                            val demoText = "Draft an official letter to the Director requesting approval for replacement of 2 unserviceable office printers within 10 days."
                            onTextDictated(demoText)
                            Toast.makeText(context, "Demo voice dictation inserted", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("demo_voice_dictation_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Demo Voice", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Try Sample", fontSize = 11.sp)
                    }
                }
            }

            AnimatedVisibility(visible = isListening && partialText.isNotBlank()) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Heard so far: \"$partialText\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Navy700,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
