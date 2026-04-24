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
import androidx.compose.ui.unit.dp
import com.pocketsarkar.ai.AiRouter
import kotlinx.coroutines.launch

/**
 * Phase 3 — AI test console.
 * Shows streaming tokens as they arrive, model source badge, and a back button.
 * This screen is temporary; it will be replaced by module-specific UIs in later phases.
 *
 * [onBack] — wired to navController.popBackStack() by PocketSarkarNavHost.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestQueryScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // AiRouter should be provided via ViewModel/Hilt in production;
    // remembered here for the test screen to keep it simple.
    val aiRouter = remember { AiRouter(context) }

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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Model source badge ─────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Model: $modelSource",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium
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
            ) {
                Text("Poochho")
            }

            // ── Loading indicator ──────────────────────────────────────────────
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // ── Streaming response ─────────────────────────────────────────────
            if (responseText.isNotEmpty() || isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            // Show "…" while waiting for the very first token
                            text = if (responseText.isEmpty() && isLoading) "…" else responseText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}