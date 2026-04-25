package com.pocketsarkar.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.ui.screens.GuideStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val gemmaEngine: GemmaEngine
) : ViewModel() {

    private val _steps = MutableStateFlow<List<GuideStep>>(emptyList())
    val steps: StateFlow<List<GuideStep>> = _steps

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun generateGuide(schemeName: String, language: String) {
        if (_steps.value.isNotEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val prompt = """
                    Act as Pocket Sarkar AI. Generate a professional 5-step roadmap for: $schemeName.
                    Format each step on a new line as: StepTitle | StepDescription
                    Keep it short and practical. Language: $language.
                """.trimIndent()

                var fullResponse = ""
                gemmaEngine.generateStreaming(prompt).collect { token ->
                    fullResponse += token
                    if (token.contains("\n")) {
                        _steps.value = parseAiResponse(fullResponse)
                    }
                }
                _steps.value = parseAiResponse(fullResponse)
            } catch (e: Exception) {
                _steps.value = listOf(
                    GuideStep("AI Notice", "The local model is busy. Please try again in a moment.")
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseAiResponse(response: String): List<GuideStep> {
        val lines = response.lines().filter { it.contains("|") }
        return if (lines.isNotEmpty()) {
            lines.take(5).map { line ->
                val parts = line.split("|")
                GuideStep(parts.getOrNull(0)?.trim() ?: "Step", parts.getOrNull(1)?.trim() ?: "Follow official guidance.")
            }
        } else {
            emptyList()
        }
    }
    
    fun clear() {
        _steps.value = emptyList()
    }
}
