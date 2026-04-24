package com.pocketsarkar.ai.ollama

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP client for the Ollama server bridge.
 *
 * Used when:
 * - Running the web demo (localhost:8080 via demo_server.py)
 * - Raspberry Pi CSC deployment (Gemma 4 26B MoE)
 * - Testing on emulator where MediaPipe model isn't loaded
 *
 * The Android app uses GemmaEngine (on-device) by default.
 * OllamaClient is a fallback — same interface, different backend.
 */
@Singleton
class OllamaClient @Inject constructor() {

    companion object {
        private const val DEFAULT_HOST = "http://10.0.2.2:11434"  // Android emulator → localhost
        private const val MODEL_NAME = "gemma4:e4b"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)     // Long reads for slow Raspberry Pi
        .build()

    var baseUrl: String = DEFAULT_HOST

    /**
     * Streaming inference via Ollama /api/generate endpoint.
     * Each chunk is emitted as it arrives (newline-delimited JSON).
     */
    fun generateStream(
        prompt: String,
        systemPrompt: String = ""
    ): Flow<String> = flow {

        val body = JSONObject().apply {
            put("model", MODEL_NAME)
            put("prompt", prompt)
            if (systemPrompt.isNotEmpty()) put("system", systemPrompt)
            put("stream", true)
        }.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(body)
            .build()

        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit("[Ollama error: ${response.code}]")
                    return@withContext
                }

                response.body?.source()?.let { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank()) continue

                        runCatching {
                            val json = JSONObject(line)
                            val token = json.optString("response", "")
                            if (token.isNotEmpty()) emit(token)
                        }
                    }
                }
            }
        }
    }

    /**
     * Blocking (non-streaming) inference via Ollama /api/generate.
     * Used by AiRouter for simple single-turn queries.
     */
    suspend fun generate(
        prompt: String,
        systemPrompt: String = "",
        serverUrl: String = baseUrl
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", MODEL_NAME)
            put("prompt", prompt)
            if (systemPrompt.isNotEmpty()) put("system", systemPrompt)
            put("stream", false)
        }.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$serverUrl/api/generate")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext "[Ollama error: ${response.code}]"
            }
            val json = JSONObject(response.body?.string() ?: "{}")
            json.optString("response", "[Empty response from Ollama]")
        }
    }

    /** Check if Ollama server is reachable */
    suspend fun isReachable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$baseUrl/api/tags").get().build()
            client.newCall(request).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }
}
