package com.pocketsarkar.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.pocketsarkar.ai.mediapipe.ChatTurn
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.ai.ollama.OllamaClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AiRouter — the single entry point for all AI generation in Pocket Sarkar.
 *
 * Routing priority:
 *   1. On-device GemmaEngine (model file present) — fully offline, fastest
 *   2. OllamaClient (network reachable + Ollama running) — fallback bridge
 *   3. Offline message — informs user to download model or get connectivity
 *
 * NO module should import GemmaEngine or OllamaClient directly.
 * Always inject and use AiRouter.
 */
@Singleton
class AiRouter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemmaEngine: GemmaEngine,
    private val ollamaClient: OllamaClient
) {
    companion object {
        private const val TAG = "AiRouter"

        // Friendly offline message in Hinglish (the app's primary language)
        private const val OFFLINE_MESSAGE =
            "Abhi offline hain aur on-device model maujood nahi hai.\n\n" +
            "Kripya model download karein:\n" +
            "  python scripts/download_model/download_model.py --model e4b-int4\n\n" +
            "Ya phir Wi-Fi/data se jud kar dubara try karein."
    }

    // ── Source enum so the UI can show "Model: on-device" vs "Model: Ollama" ──

    enum class Source { ON_DEVICE, OLLAMA, OFFLINE }

    data class RouteResult(val source: Source, val text: String)

    // ── Public API — sync (full response) ─────────────────────────────────────

    /**
     * Generate a complete response. Suspends until done.
     * Use this for background jobs or when streaming is not needed.
     */
    suspend fun generate(
        userPrompt: String,
        systemPrompt: String = ""
    ): RouteResult {
        return when {
            gemmaEngine.isModelAvailable() -> {
                Log.d(TAG, "Routing to GemmaEngine (on-device)")
                try {
                    gemmaEngine.ensureLoaded()
                    val response = gemmaEngine.generate(
                        systemPrompt = systemPrompt,
                        userPrompt = userPrompt
                    )
                    RouteResult(Source.ON_DEVICE, response)
                } catch (e: Exception) {
                    Log.e(TAG, "GemmaEngine failed, falling back to Ollama", e)
                    tryOllama(userPrompt, systemPrompt)
                        ?: RouteResult(Source.OFFLINE, OFFLINE_MESSAGE)
                }
            }
            networkAvailable() -> {
                Log.d(TAG, "Model not found — routing to Ollama")
                tryOllama(userPrompt, systemPrompt)
                    ?: RouteResult(Source.OFFLINE, OFFLINE_MESSAGE)
            }
            else -> {
                Log.w(TAG, "No model, no network — returning offline message")
                RouteResult(Source.OFFLINE, OFFLINE_MESSAGE)
            }
        }
    }

    // ── Public API — streaming ────────────────────────────────────────────────

    /**
     * Streaming generation. Emits tokens as they arrive.
     * The last emission includes the [Source] via [generateStream] sentinel.
     *
     * Prefer this for UI — perceived latency is much lower.
     */
    fun generateStream(
        userPrompt: String,
        systemPrompt: String = "",
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String> = flow {
        when {
            gemmaEngine.isModelAvailable() -> {
                Log.d(TAG, "Streaming via GemmaEngine (on-device)")
                try {
                    gemmaEngine.ensureLoaded()
                    gemmaEngine.generateStream(
                        systemPrompt = systemPrompt,
                        userPrompt = userPrompt,
                        conversationHistory = conversationHistory
                    ).collect { emit(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "GemmaEngine stream failed, falling back to Ollama", e)
                    ollamaClient.generateStream(userPrompt, systemPrompt).collect { emit(it) }
                }
            }
            networkAvailable() -> {
                Log.d(TAG, "Streaming via Ollama (network fallback)")
                ollamaClient.generateStream(userPrompt, systemPrompt).collect { emit(it) }
            }
            else -> {
                emit(OFFLINE_MESSAGE)
            }
        }
    }

    // ── Source detection helper ───────────────────────────────────────────────

    /**
     * Returns the source that *would* be used for the next generate() call.
     * Useful for UI labels ("Model: on-device" vs "Model: Ollama").
     */
    suspend fun detectSource(): Source = when {
        gemmaEngine.isModelAvailable() -> Source.ON_DEVICE
        networkAvailable() && ollamaClient.isReachable() -> Source.OLLAMA
        else -> Source.OFFLINE
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun tryOllama(
        userPrompt: String,
        systemPrompt: String
    ): RouteResult? {
        return try {
            if (!ollamaClient.isReachable()) return null
            val response = ollamaClient.generate(userPrompt, systemPrompt)
            RouteResult(Source.OLLAMA, response)
        } catch (e: Exception) {
            Log.e(TAG, "OllamaClient failed", e)
            null
        }
    }

    private fun networkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
