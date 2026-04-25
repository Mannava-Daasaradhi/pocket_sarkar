package com.pocketsarkar.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,      // 0.0–1.0
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
        private const val MIN_VALID_BYTES = 1_000_000_000L // 1 GB sanity check
    }

    val modelFile: File
        get() = File(context.getExternalFilesDir(null), "models/$MODEL_FILENAME")

    fun isModelDownloaded(): Boolean =
        modelFile.exists() && modelFile.length() > MIN_VALID_BYTES

    /**
     * Downloads the model with resume support.
     * Partial downloads are saved to .tmp — on retry, continues from where it left off.
     * Emits DownloadState every 10 MB. flowOn(IO) so caller can collect on Main.
     */
    fun downloadModel(): Flow<DownloadState> = flow {
        val destFile = modelFile
        destFile.parentFile?.mkdirs()
        val tempFile = File(destFile.parent, "$MODEL_FILENAME.tmp")

        try {
            val resumeFrom = if (tempFile.exists()) tempFile.length() else 0L

            val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 120_000
                setRequestProperty("User-Agent", "PocketSarkar/1.0")
                if (resumeFrom > 0) setRequestProperty("Range", "bytes=$resumeFrom-")
                connect()
            }

            val contentLength = connection.contentLengthLong
            val totalBytes = if (resumeFrom > 0) resumeFrom + contentLength else contentLength
            var downloadedBytes = resumeFrom
            var lastEmittedAt = resumeFrom

            emit(DownloadState.Downloading(
                progress = if (totalBytes > 0) resumeFrom.toFloat() / totalBytes else 0f,
                downloadedMB = resumeFrom / 1_048_576f,
                totalMB = totalBytes / 1_048_576f
            ))

            connection.inputStream.use { input ->
                FileOutputStream(tempFile, resumeFrom > 0).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        // Emit every 10 MB
                        if (downloadedBytes - lastEmittedAt >= 10 * 1_048_576) {
                            lastEmittedAt = downloadedBytes
                            emit(DownloadState.Downloading(
                                progress = downloadedBytes.toFloat() / totalBytes,
                                downloadedMB = downloadedBytes / 1_048_576f,
                                totalMB = totalBytes / 1_048_576f
                            ))
                        }
                    }
                }
            }

            tempFile.renameTo(destFile)
            emit(DownloadState.Complete)

        } catch (e: Exception) {
            tempFile.delete()
            emit(DownloadState.Error(e.message ?: "Download failed. Check your connection."))
        }
    }.flowOn(Dispatchers.IO)

    fun deleteModel() {
        modelFile.delete()
        File(modelFile.parent, "$MODEL_FILENAME.tmp").delete()
    }
}