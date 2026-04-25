package com.pocketsarkar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsarkar.ai.AiRouter
import com.pocketsarkar.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestQueryScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverIp by remember { mutableStateOf("192.168.") }
    var showIpField by remember { mutableStateOf(false) }
    var pingStatus by remember { mutableStateOf("") }
    var aiRouter by remember { mutableStateOf(AiRouter(context)) }
    var inputText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var modelSource by remember { mutableStateOf(aiRouter.modelSource()) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Test Console", color = PSWhite, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PSWhite)
                    }
                },
                actions = {
                    TextButton(onClick = { showIpField = !showIpField }) {
                        Text(if (showIpField) "Done" else "Set IP", color = PSSaffron, style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PSNavy)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PSCream)
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showIpField) {
                IpConfigCard(
                    serverIp = serverIp,
                    onIpChange = { serverIp = it },
                    pingStatus = pingStatus,
                    onConnect = {
                        val url = "http://$serverIp:11434"
                        aiRouter = AiRouter(context, ollamaServerUrl = url)
                        modelSource = aiRouter.modelSource()
                        pingStatus = "Pinging…"
                        scope.launch {
                            pingStatus = if (aiRouter.ollamaClient.ping(url)) "✓ Reachable" else "✗ Not reachable"
                        }
                    }
                )
            }

            // Model Source Badge
            Surface(
                color = PSGreen.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, PSGreen.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "Model: $modelSource",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = PSGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Apna sawaal likhein…") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PSNavy,
                    unfocusedBorderColor = PSBorder,
                    focusedContainerColor = PSWhite,
                    unfocusedContainerColor = PSWhite
                ),
                enabled = !isLoading,
                minLines = 3,
            )

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
                enabled = !isLoading && inputText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PSNavy, contentColor = PSWhite)
            ) {
                Text("Poochho", fontWeight = FontWeight.Medium)
            }

            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PSSaffron)

            if (responseText.isNotEmpty() || isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = PSWhite),
                    border = BorderStroke(1.dp, PSBorder)
                ) {
                    Box(modifier = Modifier.padding(16.dp).verticalScroll(scrollState)) {
                        Text(
                            text = if (responseText.isEmpty() && isLoading) "…" else responseText,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
                            color = PSTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IpConfigCard(
    serverIp: String,
    onIpChange: (String) -> Unit,
    pingStatus: String,
    onConnect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        border = BorderStroke(1.dp, PSBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ollama server IP", style = MaterialTheme.typography.labelSmall, color = PSNavy)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = serverIp,
                    onValueChange = onIpChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PSNavy)
                )
                Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = PSNavy)) {
                    Text("Connect")
                }
            }
            if (pingStatus.isNotEmpty()) {
                Text(
                    text = pingStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pingStatus.startsWith("✓")) PSGreen else PSRedFlag
                )
            }
        }
    }
}