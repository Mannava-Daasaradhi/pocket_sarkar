package com.pocketsarkar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pocketsarkar.ai.AiRouter
import kotlinx.coroutines.launch

@Composable
fun TestQueryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // AiRouter should be provided via ViewModel in production
    val aiRouter = remember { AiRouter(context) }

    var inputText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var modelSource by remember { mutableStateOf(aiRouter.modelSource()) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Pocket Sarkar — Test",
            style = MaterialTheme.typography.headlineSmall
        )

        // Model source indicator
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

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Apna sawaal likhein…") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            minLines = 2,
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

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (responseText.isNotEmpty()) {
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
                        text = responseText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}