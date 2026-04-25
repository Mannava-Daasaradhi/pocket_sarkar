package com.pocketsarkar.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketsarkar.modules.schemes.ChatMessage
import com.pocketsarkar.modules.schemes.FakeDetectionResult
import com.pocketsarkar.modules.schemes.FeedbackRating
import com.pocketsarkar.modules.schemes.SchemeExplainerViewModel
import com.pocketsarkar.ui.theme.PSNavy
import com.pocketsarkar.ui.theme.PSSaffron
import com.pocketsarkar.ui.theme.PSCream

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────
private val FakeWarningColor  = Color(0xFFFFF3CD)
private val FakeWarningBorder = Color(0xFFFF8800)
private val UserBubbleColor   = PSNavy
private val BotBubbleColor    = Color(0xFFF0F4FF)

// ─────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeExplainerScreen(
    userPrefs: com.pocketsarkar.data.UserPreferences,
    onBack: () -> Unit,
    viewModel: SchemeExplainerViewModel = hiltViewModel(),
) {
    val strings = com.pocketsarkar.ui.theme.Localization.getStrings(userPrefs.userLanguage)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val keyboard  = LocalSoftwareKeyboardController.current
    val context   = LocalContext.current

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Voice input launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!text.isNullOrBlank()) {
            viewModel.onInputChanged(text)
        }
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye...")
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.navSchemes, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.messages.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text(strings.cancel, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = PSNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(PSCream),
        ) {
            // Loading bar
            if (uiState.isLoading || uiState.isStreaming) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color    = PSSaffron,
                )
            }

            // Fake warning banner (shown BEFORE the response)
            AnimatedVisibility(
                visible = uiState.showFakeWarning,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut(),
            ) {
                uiState.fakeDetectionResult?.let { fake ->
                    FakeWarningBanner(result = fake)
                }
            }

            // Query info chip
            uiState.currentQueryInfo?.let { info ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 1.dp,
                    ) {
                        Text(
                            text     = "🔍 $info",
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Chat messages
            LazyColumn(
                state    = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                if (uiState.messages.isEmpty()) {
                    item { EmptyStateHint(strings) }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message   = message,
                        onFeedback = { rating -> viewModel.submitFeedback(message.id, rating) },
                        onLinkClick = { url ->
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Input row
            ChatInputRow(
                strings        = strings,
                text           = uiState.inputText,
                isLoading      = uiState.isLoading || uiState.isStreaming,
                onTextChanged  = { viewModel.onInputChanged(it) },
                onSend         = {
                    keyboard?.hide()
                    viewModel.sendQuery(uiState.inputText)
                },
                onVoiceInput   = {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fake warning banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FakeWarningBanner(result: FakeDetectionResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .border(1.5.dp, FakeWarningBorder, RoundedCornerShape(12.dp)),
        colors   = CardDefaults.cardColors(containerColor = FakeWarningColor),
        shape    = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint               = FakeWarningBorder,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "⚠️ FAKE SCHEME WARNING",
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color(0xFF8B4500),
                    fontSize   = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                val confidencePct = (result.confidence * 100).toInt()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FakeWarningBorder.copy(alpha = 0.2f),
                ) {
                    Text(
                        text     = "$confidencePct% confidence",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color    = Color(0xFF8B4500),
                    )
                }
            }
            if (result.reasons.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                result.reasons.forEach { reason ->
                    Text(
                        text     = "• $reason",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color(0xFF8B4500),
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onFeedback: (FeedbackRating) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (isUser) {
            UserBubble(text = message.content)
        } else {
            AssistantBubble(
                message     = message,
                onFeedback  = onFeedback,
                onLinkClick = onLinkClick,
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
            .background(UserBubbleColor)
            .padding(12.dp, 10.dp),
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AssistantBubble(
    message: ChatMessage,
    onFeedback: (FeedbackRating) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    Column(modifier = Modifier.widthIn(max = 300.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(BotBubbleColor)
                .border(0.5.dp, PSNavy.copy(alpha = 0.12f), RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .padding(12.dp, 10.dp),
        ) {
            if (message.isStreaming && message.content.isEmpty()) {
                TypingIndicator()
            } else {
                Text(
                    text  = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PSNavy,
                )
            }
        }

        // Feedback row (only on complete assistant messages)
        if (!message.isStreaming && message.content.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "Helpful?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(Modifier.width(4.dp))
                FilledTonalIconButton(
                    onClick = { onFeedback(FeedbackRating.THUMBS_UP) },
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (message.feedback == FeedbackRating.THUMBS_UP)
                            Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = if (message.feedback == FeedbackRating.THUMBS_UP)
                            Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                ) {
                    Icon(Icons.Default.ThumbUp, "Helpful", modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(4.dp))
                FilledTonalIconButton(
                    onClick = { onFeedback(FeedbackRating.THUMBS_DOWN) },
                    modifier = Modifier.size(28.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (message.feedback == FeedbackRating.THUMBS_DOWN)
                            Color(0xFFF44336) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = if (message.feedback == FeedbackRating.THUMBS_DOWN)
                            Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                ) {
                    Icon(Icons.Default.ThumbDown, "Not helpful", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Typing indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "dot",
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PSNavy.copy(alpha = if (i == 0) alpha else alpha * 0.6f)),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Input row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputRow(
    strings: com.pocketsarkar.ui.theme.AppStrings,
    text: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 8.dp, 8.dp, 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value         = text,
                onValueChange = onTextChanged,
                modifier      = Modifier.weight(1f),
                placeholder   = { Text(strings.placeholderNote.take(20) + "...") },
                maxLines      = 4,
                // Keep field enabled while loading so the user can see/edit their next question
                enabled       = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank() && !isLoading) onSend() }),
                trailingIcon  = {
                    if (text.isNotBlank()) {
                        IconButton(onClick = { onTextChanged("") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
            )
            Spacer(Modifier.width(6.dp))

            // Voice button — disabled while a response is streaming
            FilledTonalIconButton(
                onClick  = onVoiceInput,
                enabled  = !isLoading,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.Mic, "Voice input")
            }
            Spacer(Modifier.width(6.dp))

            // Send button
            Button(
                onClick  = { if (text.isNotBlank() && !isLoading) onSend() },
                enabled  = text.isNotBlank() && !isLoading,
                modifier = Modifier.size(48.dp),
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = PSNavy),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state hint
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateHint(strings: com.pocketsarkar.ui.theme.AppStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🏛️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text  = "सरकारी योजनाएं खोजें",
            style = MaterialTheme.typography.titleMedium,
            color = PSNavy,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text  = "Ask about any government scheme",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(20.dp))

        val suggestions = listOf(
            "PM Kisan mein kitna milta hai?",
            "Ladki ke liye scholarship kaunsi hai?",
            "Pradhan Mantri Awas Yojana kya hai?",
        )
        suggestions.forEach { suggestion ->
            Surface(
                modifier  = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape     = RoundedCornerShape(20.dp),
                color     = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
            ) {
                Text(
                    text     = "💬 $suggestion",
                    modifier = Modifier.padding(14.dp, 10.dp),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = PSNavy,
                )
            }
        }
    }
}