package com.pocketsarkar.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.data.AlertType
import com.pocketsarkar.data.SchemeAlert
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BriefingViewModel @Inject constructor(
    private val gemmaEngine: GemmaEngine
) : ViewModel() {

    private val _alerts = MutableStateFlow<List<SchemeAlert>>(emptyList())
    val alerts: StateFlow<List<SchemeAlert>> = _alerts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun generateDailyBriefing(language: String) {
        if (_alerts.value.isNotEmpty()) return 

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val prompt = """
                    Act as Pocket Sarkar AI. Generate 3 short government scheme alerts for today.
                    Each alert MUST be on a new line and follow this format: Title | Description | Type
                    Type must be: DEADLINE, NEW_SCHEME, or UPDATE.
                    Keep it short. Language: $language.
                """.trimIndent()

                var fullResponse = ""
                gemmaEngine.generateStreaming(prompt).collect { token ->
                    fullResponse += token
                    // Partially update UI if a full line is completed
                    if (token.contains("\n")) {
                        _alerts.value = parseAiResponse(fullResponse)
                    }
                }
                // Final update
                _alerts.value = parseAiResponse(fullResponse)
            } catch (e: Exception) {
                _alerts.value = listOf(
                    SchemeAlert("AI Loading...", "The local AI is warming up to give you real-time updates.", "Live", AlertType.UPDATE)
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseAiResponse(response: String): List<SchemeAlert> {
        val lines = response.lines().filter { it.contains("|") }
        return lines.take(3).map { line ->
            val parts = line.split("|")
            val title = parts.getOrNull(0)?.trim() ?: "Scheme Alert"
            val desc = parts.getOrNull(1)?.trim() ?: "Check portal for updates."
            val typeStr = parts.getOrNull(2)?.trim() ?: "UPDATE"
            val type = try { AlertType.valueOf(typeStr) } catch (e: Exception) { AlertType.UPDATE }
            SchemeAlert(title, desc, "Live AI", type)
        }
    }
}
