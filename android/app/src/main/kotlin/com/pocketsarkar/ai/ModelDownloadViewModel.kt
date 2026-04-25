package com.pocketsarkar.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private var observeJob: Job? = null

    init {
        // Resume observing if a download was already in progress
        val existingId = downloadManager.savedDownloadId
        if (existingId != -1L && !downloadManager.isModelDownloaded()) {
            observeDownload(existingId)
        }
    }

    fun isModelReady(): Boolean = downloadManager.isModelDownloaded()

    fun startDownload() {
        val id = downloadManager.startDownload()
        observeDownload(id)
    }

    fun retryDownload() {
        observeJob?.cancel()
        downloadManager.cancelDownload()
        _downloadState.value = DownloadState.Idle
        startDownload()
    }

    private fun observeDownload(downloadId: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            downloadManager.observeDownload(downloadId).collect { state ->
                _downloadState.value = state
            }
        }
    }
}