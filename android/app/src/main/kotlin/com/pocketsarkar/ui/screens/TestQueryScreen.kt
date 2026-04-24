package com.pocketsarkar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketsarkar.ai.AiRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class TestQueryViewModel @Inject constructor(
    private val aiRouter: AiRouter
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestQueryUiState())
    val uiState: StateFlow<TestQueryUiState> = _uiState

    private var streamJob: Job? = null

    init {
        // Detect initial source label
        viewModelScope.launch {
            val source = aiRouter.detectSource()
            _uiState.value = _uiState.value.copy(modelSource = source.label())
        }
    }

    fun send(prompt: String) {
        if (prompt.isBlank()) return
        streamJob?.cancel()

        _uiState.value = _uiState.value.copy(
            response = "",
            isGenerating = true,
            error = null
        )

        streamJob = viewModelScope.launch {
            val source = aiRouter.detectSource()
            _uiState.value = _uiState.value.copy(modelSource = source.label())

            try {
                aiRouter.generateStream(
                    userPrompt = prompt,
                    systemPrompt = "You are Pocket Sarkar, a helpful AI assistant for Indian citizens. Be concise and helpful."
                ).collect { token ->
                    _uiState.value = _uiState.value.copy(
                        response = _uiState.value.response + token
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isGenerating = false)
            }
        }
    }

    fun cancelGeneration() {
        streamJob?.cancel()
        _uiState.value = _uiState.value.copy(isGenerating = false)
    }

    private fun AiRouter.Source.label(): String = when (this) {
        AiRouter.Source.ON_DEVICE -> "Model: on-device (Gemma)"
        AiRouter.Source.OLLAMA    -> "Model: Ollama (network)"
        AiRouter.Source.OFFLINE   -> "Model: offline — no model found"
    }
}

data class TestQueryUiState(
    val response: String = "",
    val isGenerating: Boolean = false,
    val modelSource: String = "Detecting…",
    val error: String? = null
)

// ── UI ────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestQueryScreen(
    onBack: () -> Unit,
    viewModel: TestQueryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom as tokens arrive
    LaunchedEffect(uiState.response) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AI Test Console",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            uiState.modelSource,
                            style = MaterialTheme.typography.labelSmall,
                            color = sourceColor(uiState.modelSource)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // ── Response area ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    if (uiState.response.isBlank() && !uiState.isGenerating) {
                        Text(
                            "Ask anything about Indian government schemes,\nyour rights, or how to decode a document.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    if (uiState.response.isNotBlank()) {
                        Text(
                            text = uiState.response,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Default,
                                lineHeight = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    uiState.error?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Blinking cursor while generating
                    AnimatedVisibility(visible = uiState.isGenerating) {
                        Text(
                            "▌",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Loading indicator ─────────────────────────────────────────────
            AnimatedVisibility(visible = uiState.isGenerating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Generating…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.cancelGeneration() }) {
                        Text("Stop", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ── Input row ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("PM Kisan kya hai? / What is Aadhaar?") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isGenerating
                )

                Spacer(Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        viewModel.send(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank() && !uiState.isGenerating,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun sourceColor(label: String): Color = when {
    "on-device" in label -> Color(0xFF2E7D32)   // IndiaGreen — good
    "Ollama"    in label -> Color(0xFF00838F)   // PeacockBlue — ok
    else                 -> MaterialTheme.colorScheme.error
}
