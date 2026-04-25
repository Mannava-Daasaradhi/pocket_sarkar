package com.pocketsarkar.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryAiViewModel @Inject constructor(
    private val gemmaEngine: GemmaEngine
) : ViewModel() {

    private val _recommendation = MutableStateFlow("")
    val recommendation: StateFlow<String> = _recommendation

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    fun getRecommendation(category: String, answers: List<String>, language: String) {
        if (_recommendation.value.isNotEmpty()) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val profileContext = when(category) {
                    "Agriculture" -> "State: ${answers.getOrNull(0)}, Land: ${answers.getOrNull(1)} acres, Crop: ${answers.getOrNull(2)}, Income: ${answers.getOrNull(3)}"
                    "Housing" -> "Area: ${answers.getOrNull(0)}, Owns House: ${answers.getOrNull(1)}, Income: ${answers.getOrNull(2)}, Has Aadhaar: ${answers.getOrNull(3)}"
                    "Health" -> "Age: ${answers.getOrNull(0)}, Has Insurance: ${answers.getOrNull(1)}, In SECC List: ${answers.getOrNull(2)}, State: ${answers.getOrNull(3)}"
                    else -> "Details: ${answers.joinToString(", ")}"
                }

                val prompt = if (category == "Office Selection") {
                    """
                    You are Pocket Sarkar Office Assistant.
                    The user needs help with: $profileContext.
                    1. Identify the specific government office/center they should visit (e.g., Tahsildar, MeeSeva, Panchayat).
                    2. List exactly 3-4 documents they must carry for this specific service.
                    Be concise and helpful. Language: $language.
                    """.trimIndent()
                } else {
                    """
                    You are Pocket Sarkar AI Expert. 
                    User Profile for $category: $profileContext.
                    Based on this, recommend the top 2 government schemes they should apply for.
                    Explain WHY they are eligible in 2 short sentences.
                    Language: $language.
                    """.trimIndent()
                }

                gemmaEngine.generateStreaming(prompt).collect { token ->
                    _recommendation.value += token
                }
            } catch (e: Exception) {
                _recommendation.value = "The AI model is still initializing. Please try again in 30 seconds."
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clear() {
        _recommendation.value = ""
        _isAnalyzing.value = false
    }
}
