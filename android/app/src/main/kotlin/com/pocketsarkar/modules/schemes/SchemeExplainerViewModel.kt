package com.pocketsarkar.modules.schemes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketsarkar.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

enum class FeedbackRating { THUMBS_UP, THUMBS_DOWN, NONE }

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,           // "user" | "assistant"
    val content: String,
    val isStreaming: Boolean = false,
    val schemeCards: List<SchemeCardData> = emptyList(),
    val fakeDetection: FakeDetectionResult? = null,
    val feedback: FeedbackRating = FeedbackRating.NONE,
)

data class SchemeCardData(
    val id: String,
    val nameEn: String,
    val nameHi: String,
    val benefitAmount: String?,
    val targetCategory: String,
    val portalUrl: String?,
)

data class SchemeUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val showFakeWarning: Boolean = false,
    val fakeDetectionResult: FakeDetectionResult? = null,
    val currentQueryInfo: String? = null,  // e.g. "Searching: PM Kisan..."
    val error: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class SchemeExplainerViewModel @Inject constructor(
    private val schemeExplainer: SchemeExplainer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchemeUiState())
    val uiState: StateFlow<SchemeUiState> = _uiState.asStateFlow()

    // ── Query ─────────────────────────────────────────────────────────────────

    fun sendQuery(
        userMessage: String,
        userProfile: UserProfile? = null,
        userLanguage: String = "hi",
    ) {
        if (userMessage.isBlank()) return

        val userMsg = ChatMessage(role = "user", content = userMessage.trim())
        val assistantPlaceholder = ChatMessage(
            role = "assistant",
            content = "",
            isStreaming = true,
        )

        _uiState.update { state ->
            state.copy(
                messages     = state.messages + userMsg + assistantPlaceholder,
                inputText    = "",
                isLoading    = true,
                isStreaming   = true,
                showFakeWarning = false,
                fakeDetectionResult = null,
                currentQueryInfo = null,
                error        = null,
            )
        }

        viewModelScope.launch {
            val streamedContent = StringBuilder()

            schemeExplainer.query(
                userMessage  = userMessage.trim(),
                userProfile  = userProfile,
                userLanguage = userLanguage,
            )
            .catch { e ->
                emit(SchemeStreamEvent.Error(e.message ?: "Unknown error"))
            }
            .collect { event ->
                when (event) {
                    is SchemeStreamEvent.FakeDetectionComplete -> {
                        val fake = event.result
                        _uiState.update { s ->
                            s.copy(
                                showFakeWarning     = fake.isFake,
                                fakeDetectionResult = fake,
                                isLoading           = false,
                                isStreaming         = true,
                            )
                        }
                        // Attach fake detection to the pending assistant message
                        if (fake.isFake) {
                            updateLastAssistantMessage { msg ->
                                msg.copy(fakeDetection = fake)
                            }
                        }
                    }

                    is SchemeStreamEvent.FunctionCallExecuted -> {
                        _uiState.update { s ->
                            s.copy(currentQueryInfo = "Searching: ${event.query} (${event.resultCount} results)")
                        }
                    }

                    is SchemeStreamEvent.Token -> {
                        streamedContent.append(event.text)
                        updateLastAssistantMessage { msg ->
                            msg.copy(content = streamedContent.toString(), isStreaming = true)
                        }
                    }

                    is SchemeStreamEvent.Complete -> {
                        val finalText = event.fullText.ifBlank { streamedContent.toString() }
                        updateLastAssistantMessage { msg ->
                            msg.copy(content = finalText, isStreaming = false)
                        }
                        _uiState.update { s ->
                            s.copy(isLoading = false, isStreaming = false, currentQueryInfo = null)
                        }
                    }

                    is SchemeStreamEvent.Error -> {
                        updateLastAssistantMessage { msg ->
                            msg.copy(
                                content = "Kuch gadbad ho gayi: ${event.message}",
                                isStreaming = false,
                            )
                        }
                        _uiState.update { s ->
                            s.copy(isLoading = false, isStreaming = false, error = event.message)
                        }
                    }
                }
            }
        }
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    fun submitFeedback(messageId: String, rating: FeedbackRating) {
        _uiState.update { s ->
            s.copy(messages = s.messages.map { msg ->
                if (msg.id == messageId) msg.copy(feedback = rating) else msg
            })
        }
        // Log locally for eval pipeline — no network call
        android.util.Log.d("SchemeEval", "Feedback: $rating for message $messageId")
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            schemeExplainer.clearHistory()
            _uiState.value = SchemeUiState()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun updateLastAssistantMessage(transform: (ChatMessage) -> ChatMessage) {
        _uiState.update { s ->
            val messages = s.messages.toMutableList()
            val lastIdx  = messages.indexOfLast { it.role == "assistant" }
            if (lastIdx >= 0) messages[lastIdx] = transform(messages[lastIdx])
            s.copy(messages = messages)
        }
    }
}