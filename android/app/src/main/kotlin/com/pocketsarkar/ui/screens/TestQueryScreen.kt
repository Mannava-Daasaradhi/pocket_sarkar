package com.pocketsarkar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsarkar.ai.AiRouter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestQueryScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Real-device dev: user can type their PC's LAN IP here
    var serverIp     by remember { mutableStateOf("192.168.") }
    var showIpField  by remember { mutableStateOf(false) }
    var pingStatus   by remember { mutableStateOf("") }

    // AiRouter is re-created when the server URL changes
    var aiRouter by remember { mutableStateOf(AiRouter(context)) }

    var inputText    by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var modelSource  by remember { mutableStateOf(aiRouter.modelSource()) }
    val scrollState  = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Test Console") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Gear icon to show/hide the IP field (real-device dev mode)
                    TextButton(onClick = { showIpField = !showIpField }) {
                        Text(if (showIpField) "Done" else "Set IP", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Real-device IP config (hidden by default) ──────────────────────
            if (showIpField) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Ollama server IP (real device only)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Emulator uses 10.0.2.2 automatically.\nFor a real device: enter your PC's LAN IP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = serverIp,
                                onValueChange = { serverIp = it },
                                label = { Text("e.g. 192.168.1.42") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {
                                val url = "http://$serverIp:11434"
                                aiRouter = AiRouter(context, ollamaServerUrl = url)
                                modelSource = aiRouter.modelSource()
                                pingStatus = "Pinging…"
                                scope.launch {
                                    pingStatus = if (aiRouter.ollamaClient.ping(url))
                                        "✓ Reachable" else "✗ Not reachable"
                                }
                            }) { Text("Connect") }
                        }
                        if (pingStatus.isNotEmpty()) {
                            Text(pingStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (pingStatus.startsWith("✓"))
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ── Model source badge ─────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Model: $modelSource",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Input ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Apna sawaal likhein…") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                minLines = 2,
            )

            // ── Ask button ─────────────────────────────────────────────────────
            Button(
                onClick = {
                    if (inputText.isBlank()) return@Button
                    isLoading = true
                    responseText = ""
                    modelSource = aiRouter.modelSource()
                    scope.launch {
                        try {
                            aiRouter.generateStreaming(inputText).collect { token ->
                                responseText += token
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        } catch (e: Exception) {
                            responseText = "Error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading && inputText.isNotBlank()
            ) { Text("Poochho") }

            // ── Loading indicator ──────────────────────────────────────────────
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            // ── Streaming response ─────────────────────────────────────────────
            if (responseText.isNotEmpty() || isLoading) {
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Box(modifier = Modifier.padding(12.dp).verticalScroll(scrollState)) {
                        Text(
                            text = if (responseText.isEmpty() && isLoading) "…" else responseText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}