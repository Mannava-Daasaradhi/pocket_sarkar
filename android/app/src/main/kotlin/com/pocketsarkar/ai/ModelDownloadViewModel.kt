package com.pocketsarkar.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: ModelDownloadManager
) : ViewModel() {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun isModelReady(): Boolean = downloadManager.isModelDownloaded()

    fun startDownload() {
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ModelDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,   // don't restart if already running
            request
        )

        observeWorker()
    }

    fun retryDownload() {
        WorkManager.getInstance(context).cancelUniqueWork(ModelDownloadWorker.WORK_NAME)
        _downloadState.value = DownloadState.Idle
        startDownload()
    }

    private fun observeWorker() {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress     = info.progress.getFloat(ModelDownloadWorker.KEY_PROGRESS, 0f)
                            val downloadedMB = info.progress.getFloat(ModelDownloadWorker.KEY_DOWNLOADED_MB, 0f)
                            val totalMB      = info.progress.getFloat(ModelDownloadWorker.KEY_TOTAL_MB, 0f)
                            _downloadState.value = DownloadState.Downloading(progress, downloadedMB, totalMB)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _downloadState.value = DownloadState.Complete
                        }
                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString(ModelDownloadWorker.KEY_ERROR)
                            _downloadState.value = DownloadState.Error(error ?: "Download failed")
                        }
                        WorkInfo.State.ENQUEUED -> {
                            _downloadState.value = DownloadState.Downloading(0f, 0f, 0f)
                        }
                        else -> {}
                    }
                }
        }
    }

    // Called on app start — resumes observing if a download was already running
    init {
        observeWorker()
    }
}