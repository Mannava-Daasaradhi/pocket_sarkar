package com.pocketsarkar.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.ai.ollama.OllamaClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiRouter(
    private val context: Context,
    // Overridable at runtime — TestQueryScreen passes the user-typed IP
    private val ollamaServerUrl: String = "http://10.0.2.2:11434"
) {
    val gemmaEngine = GemmaEngine(context)
    val ollamaClient = OllamaClient(defaultServerUrl = ollamaServerUrl)

    suspend fun generate(prompt: String): String {
        return when {
            gemmaEngine.isModelAvailable() -> gemmaEngine.generate(prompt)
            networkAvailable()             -> ollamaClient.generate(prompt)
            else -> "Abhi offline hain. Kripya model download karein ya internet connect karein."
        }
    }

    fun generateStreaming(prompt: String): Flow<String> {
        return if (gemmaEngine.isModelAvailable()) {
            gemmaEngine.generateStreaming(prompt)
        } else {
            flow {
                val response = if (networkAvailable()) {
                    ollamaClient.generate(prompt)
                } else {
                    "Abhi offline hain. Kripya model download karein ya internet connect karein."
                }
                emit(response)
            }
        }
    }

    fun modelSource(): String =
        if (gemmaEngine.isModelAvailable()) "on-device (Gemma 4 E4B)" else "Ollama (server)"

    private fun networkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun release() { gemmaEngine.release() }
}