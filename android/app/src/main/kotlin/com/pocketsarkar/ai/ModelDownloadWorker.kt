package com.pocketsarkar.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val downloadManager: ModelDownloadManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME          = "model_download"
        const val KEY_PROGRESS       = "progress"
        const val KEY_DOWNLOADED_MB  = "downloaded_mb"
        const val KEY_TOTAL_MB       = "total_mb"
        const val KEY_ERROR          = "error"
        private const val NOTIF_CHANNEL_ID = "model_download"
        private const val NOTIF_ID         = 1001
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()
        setForeground(buildForegroundInfo(0f, 0f, 0f))

        var lastResult: Result = Result.failure()

        downloadManager.downloadModel().collect { state ->
            when (state) {
                is DownloadState.Downloading -> {
                    setProgress(workDataOf(
                        KEY_PROGRESS      to state.progress,
                        KEY_DOWNLOADED_MB to state.downloadedMB,
                        KEY_TOTAL_MB      to state.totalMB
                    ))
                    setForeground(buildForegroundInfo(
                        state.progress, state.downloadedMB, state.totalMB
                    ))
                    lastResult = Result.success()
                }
                is DownloadState.Complete -> {
                    lastResult = Result.success()
                }
                is DownloadState.Error -> {
                    setProgress(workDataOf(KEY_ERROR to state.message))
                    lastResult = Result.retry()   // WorkManager will retry
                }
                else -> {}
            }
        }

        return lastResult
    }

    private fun buildForegroundInfo(
        progress: Float,
        downloadedMB: Float,
        totalMB: Float
    ): ForegroundInfo {
        val progressInt = (progress * 100).toInt()
        val text = if (totalMB > 0)
            "${downloadedMB.toInt()} MB / ${totalMB.toInt()} MB  ($progressInt%)"
        else "Starting download…"

        val notification = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setContentTitle("Pocket Sarkar — AI Model")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progressInt, progressInt == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIF_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "AI Model Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows progress while downloading the AI model" }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}