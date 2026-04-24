package com.pocketsarkar.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    private val downloadManager: ModelDownloadManager
) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun isModelReady(): Boolean = downloadManager.isModelDownloaded()

    fun startDownload() {
        if (_downloadState.value is DownloadState.Downloading) return
        viewModelScope.launch {
            downloadManager.downloadModel().collect { state ->
                _downloadState.value = state
            }
        }
    }

    fun retryDownload() {
        _downloadState.value = DownloadState.Idle
        startDownload()
    }
}