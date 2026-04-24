package com.pocketsarkar.ai.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.core.OutputHandler
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GemmaEngine — wraps MediaPipe LlmInference for on-device Gemma 4 E4B INT4.
 *
 * CRITICAL DESIGN:
 * - ONE LlmInference sync engine per process lifetime (GPU/NPU resource is exclusive).
 * - Streaming uses a SEPARATE engine with a ResultListener baked in (MediaPipe
 *   0.10.20 requires the listener at construction time, unlike later versions).
 * - streamMutex ensures at most ONE stream engine exists at a time, preventing
 *   the "LlmInference is already in use" GPU crash on the S24 Ultra.
 */
@Singleton
class GemmaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GemmaEngine"
        private const val MODEL_FILE = "gemma4-e4b-it-int4.task"
        private const val MAX_TOKENS = 1024
        private const val TOP_K = 40
        private const val TEMPERATURE = 0.7f
    }

    // Sync engine — no result listener, for generate()
    private var llmInference: LlmInference? = null
    private var modelPath: String? = null

    private val initMutex = Mutex()
    // Only one streaming engine at a time — prevents GPU exclusive-access crash
    private val streamMutex = Mutex()

    val isLoaded: Boolean get() = llmInference != null

    // ── Model availability ────────────────────────────────────────────────────

    fun isModelAvailable(): Boolean {
        val external = File(context.getExternalFilesDir(null), "models/$MODEL_FILE")
        if (external.exists()) return true
        val internal = File(context.filesDir, MODEL_FILE)
        if (internal.exists()) return true
        return try {
            context.assets.open("models/$MODEL_FILE").use { true }
        } catch (e: Exception) {
            false
        }
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (llmInference != null) return@withContext
        initMutex.withLock {
            if (llmInference != null) return@withLock

            val path = getModelPath()
                ?: error(
                    "Gemma 4 E4B model not found. " +
                    "Run: python scripts/download_model/download_model.py --model e4b-int4\n" +
                    "Then: adb push gemma4-e4b-it-int4.task " +
                    "/sdcard/Android/data/com.pocketsarkar/files/models/"
                )
            modelPath = path

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(MAX_TOKENS)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.i(TAG, "LlmInference loaded from: $path")
        }
    }

    private fun getModelPath(): String? {
        val external = File(context.getExternalFilesDir(null), "models/$MODEL_FILE")
        if (external.exists()) return external.absolutePath
        return try {
            val outFile = File(context.filesDir, MODEL_FILE)
            if (!outFile.exists()) {
                context.assets.open("models/$MODEL_FILE").use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract model asset: $MODEL_FILE", e)
            null
        }
    }

    // ── Sync generation ───────────────────────────────────────────────────────

    /**
     * Full blocking response. Caller must have called ensureLoaded() first.
     *
     * CORRECT API for MediaPipe 0.10.20:
     *   engine.generateResponse(inputText: String): String   ← THIS (on base LlmInference)
     *
     * WRONG (does not exist in 0.10.20):
     *   session.generateResponse()    ← LlmInferenceSession has no such method
     */
    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val engine = llmInference
            ?: error("GemmaEngine not initialised. Call ensureLoaded() first.")
        engine.generateResponse(buildPrompt(systemPrompt, conversationHistory, userPrompt))
    }

    // ── Streaming generation ──────────────────────────────────────────────────

    /**
     * Streaming via a separate LlmInference with a ResultListener.
     *
     * Why a separate engine? In MediaPipe 0.10.20, ResultListener must be passed at
     * LlmInferenceOptions construction time. The sync engine has no listener attached.
     *
     * streamMutex guarantees at most one stream engine is alive at a time, preventing
     * the exclusive-GPU-access RuntimeException on real devices (S24 Ultra, Pixel 8).
     *
     * LlmInferenceSession is required to call generateResponseAsync() on a
     * listener-enabled engine. Session options carry topK / temperature.
     */
    fun generateStream(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String> = callbackFlow {
        val path = modelPath
            ?: error("GemmaEngine not initialised. Call ensureLoaded() first.")

        streamMutex.withLock {
            val listener = OutputHandler.ProgressListener<String> { partialResult, done ->
                if (partialResult != null) trySend(partialResult)
                if (done) close()
            }

            val streamOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(MAX_TOKENS)
                .setResultListener(listener)
                .build()

            val streamEngine = LlmInference.createFromOptions(context, streamOptions)

            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(TOP_K)
                .setTemperature(TEMPERATURE)
                .build()
            val session = LlmInferenceSession.createFromOptions(streamEngine, sessionOptions)

            session.addQueryChunk(buildPrompt(systemPrompt, conversationHistory, userPrompt))
            session.generateResponseAsync()

            awaitClose {
                session.close()
                streamEngine.close()
            }
        }
    }

    // ── Vision stub (text-only fallback until Phase 4) ────────────────────────

    @Suppress("UNUSED_PARAMETER")
    suspend fun generateWithImage(
        systemPrompt: String,
        userPrompt: String,
        image: Bitmap
    ): Flow<String> {
        // TODO Phase 4: wire MPImage + GraphOptions.setEnableVisionModality(true)
        // Falls back to text-only so DocumentDecoder compiles and runs without crash.
        return generateStream(systemPrompt, userPrompt)
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    private fun buildPrompt(
        systemPrompt: String,
        history: List<ChatTurn>,
        currentUserMessage: String
    ): String = buildString {
        append("<start_of_turn>user\n")
        append(systemPrompt)
        append("\n<end_of_turn>\n")
        for (turn in history) {
            when (turn.role) {
                ChatRole.USER -> append("<start_of_turn>user\n${turn.content}\n<end_of_turn>\n")
                ChatRole.ASSISTANT -> append("<start_of_turn>model\n${turn.content}\n<end_of_turn>\n")
            }
        }
        append("<start_of_turn>user\n")
        append(currentUserMessage)
        append("\n<end_of_turn>\n")
        append("<start_of_turn>model\n")
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun release() {
        llmInference?.close()
        llmInference = null
        modelPath = null
    }
}

data class ChatTurn(val role: ChatRole, val content: String)
enum class ChatRole { USER, ASSISTANT }