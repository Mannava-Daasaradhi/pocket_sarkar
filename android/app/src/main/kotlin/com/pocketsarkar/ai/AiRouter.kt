package com.pocketsarkar.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.ai.ollama.OllamaClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AiRouter(private val context: Context) {

    val gemmaEngine = GemmaEngine(context)
    private val ollamaClient = OllamaClient()

    // Single entry point for all modules — DO NOT call GemmaEngine/OllamaClient directly
    suspend fun generate(prompt: String): String {
        return when {
            gemmaEngine.isModelAvailable() -> gemmaEngine.generate(prompt)
            networkAvailable() -> ollamaClient.generate(prompt)
            else -> "Abhi offline hain. Kripya model download karein ya internet connect karein."
        }
    }

    // Streaming version — emits partial tokens
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
                emit(response) // Ollama is non-streaming here — emits full response at once
            }
        }
    }

    fun modelSource(): String {
        return if (gemmaEngine.isModelAvailable()) "on-device (Gemma 4 E4B)" else "Ollama (server)"
    }

    private fun networkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun release() {
        gemmaEngine.release()
    }
}