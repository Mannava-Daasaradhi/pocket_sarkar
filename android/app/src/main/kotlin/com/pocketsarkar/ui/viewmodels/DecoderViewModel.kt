package com.pocketsarkar.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketsarkar.modules.decoder.DecoderResponseParser
import com.pocketsarkar.modules.decoder.DecoderResult
import com.pocketsarkar.modules.decoder.DocumentDecoder
import com.pocketsarkar.modules.decoder.DocumentInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DecoderViewModel @Inject constructor(
    private val decoder: DocumentDecoder,
    private val parser: DecoderResponseParser,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DecoderUiState>(DecoderUiState.Idle)
    val uiState: StateFlow<DecoderUiState> = _uiState.asStateFlow()

    fun analyze(input: DocumentInput) {
        viewModelScope.launch {
            _uiState.value = DecoderUiState.Processing

            val rawResponse = StringBuilder()
            decoder.decodeStream(input).collect { token ->
                rawResponse.append(token)
                // Show streaming progress — partial text gives user confidence
                _uiState.value = DecoderUiState.Streaming(rawResponse.toString())
            }

            val result = parser.parse(rawResponse.toString())
            _uiState.value = if (result != null) {
                DecoderUiState.Success(result)
            } else {
                DecoderUiState.Error(
                    "Could not parse document response. " +
                    "Please try again with a clearer image or typed text."
                )
            }
        }
    }

    fun reset() {
        _uiState.value = DecoderUiState.Idle
    }
}

// ─────────────────────────────────────────────────────────────────────────────

sealed class DecoderUiState {
    /** No analysis running — show camera and upload buttons. */
    object Idle : DecoderUiState()

    /** Submitted to AI — show spinner. */
    object Processing : DecoderUiState()

    /** AI is streaming — show progress indicator with partial text. */
    data class Streaming(val partial: String) : DecoderUiState()

    /** Analysis complete — show full results. */
    data class Success(val result: DecoderResult) : DecoderUiState()

    /** Something went wrong — show error card with retry. */
    data class Error(val message: String) : DecoderUiState()
}
