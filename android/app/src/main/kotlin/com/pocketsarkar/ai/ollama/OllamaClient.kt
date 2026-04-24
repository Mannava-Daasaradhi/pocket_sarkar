package com.pocketsarkar.ai.ollama

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OllamaClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        prompt: String,
        serverUrl: String = "http://10.0.2.2:11434"
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("model", "pocket-sarkar") // the custom Modelfile name
            put("prompt", prompt)
            put("stream", false)
        }.toString()

        val request = Request.Builder()
            .url("$serverUrl/api/generate")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Ollama error ${response.code}: ${response.body?.string()}")
            }
            val json = JSONObject(response.body?.string() ?: "{}")
            json.optString("response", "")
        }
    }
}