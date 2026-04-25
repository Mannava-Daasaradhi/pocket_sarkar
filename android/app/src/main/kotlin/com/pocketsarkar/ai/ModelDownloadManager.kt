package com.pocketsarkar.ai

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val downloadedMB: Float,
        val totalMB: Float
    ) : DownloadState()
    object Complete : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
        const val MODEL_FILENAME = "gemma-4-E4B-it.litertlm"
        private const val MIN_VALID_BYTES = 1_000_000_000L
        private const val PREFS_NAME = "model_download_prefs"
        private const val KEY_DOWNLOAD_ID = "download_id"
    }

    val modelFile: File
        get() = File(
            context.getExternalFilesDir(null),
            "models/$MODEL_FILENAME"
        )

    fun isModelDownloaded(): Boolean =
        modelFile.exists() && modelFile.length() > MIN_VALID_BYTES

    private val dm: DownloadManager
        get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    internal var savedDownloadId: Long
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_DOWNLOAD_ID, -1L)
        set(id) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_DOWNLOAD_ID, id).apply()

    fun startDownload(): Long {
        // Cancel any existing stale download
        if (savedDownloadId != -1L) {
            dm.remove(savedDownloadId)
        }

        modelFile.parentFile?.mkdirs()

        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("Pocket Sarkar — AI Model")
            .setDescription("Downloading Gemma 4 (3.65 GB)")
            .setDestinationUri(Uri.fromFile(modelFile))
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val id = dm.enqueue(request)
        savedDownloadId = id
        return id
    }

    /**
     * Polls DownloadManager every second and emits progress.
     * Survives app restart — pass in the saved downloadId.
     */
    fun observeDownload(downloadId: Long): Flow<DownloadState> = flow {
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = dm.query(query)

            if (cursor == null || !cursor.moveToFirst()) {
                cursor?.close()
                emit(DownloadState.Error("Download not found"))
                break
            }

            val status = cursor.getInt(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            )
            val bytesDownloaded = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            )
            val bytesTotal = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            )
            cursor.close()

            when (status) {
                DownloadManager.STATUS_RUNNING,
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_PAUSED -> {
                    val progress = if (bytesTotal > 0)
                        bytesDownloaded.toFloat() / bytesTotal else 0f
                    emit(DownloadState.Downloading(
                        progress = progress,
                        downloadedMB = bytesDownloaded / 1_048_576f,
                        totalMB = bytesTotal / 1_048_576f
                    ))
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    savedDownloadId = -1L
                    emit(DownloadState.Complete)
                    break
                }
                DownloadManager.STATUS_FAILED -> {
                    savedDownloadId = -1L
                    emit(DownloadState.Error("Download failed. Tap retry."))
                    break
                }
            }
            delay(1000)
        }
    }

    fun cancelDownload() {
        if (savedDownloadId != -1L) {
            dm.remove(savedDownloadId)
            savedDownloadId = -1L
        }
        modelFile.delete()
    }

    
}