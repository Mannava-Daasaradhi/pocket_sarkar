package com.pocketsarkar.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: ModelDownloadManager
) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var downloadJob: Job? = null

    fun isModelReady(): Boolean = downloadManager.isModelDownloaded()

    fun startDownload() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            downloadManager.downloadModel().collect { state ->
                _downloadState.value = state
            }
        }
    }

    fun retryDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Idle
        startDownload()
    }
}