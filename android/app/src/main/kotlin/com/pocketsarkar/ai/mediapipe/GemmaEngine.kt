package com.pocketsarkar.ai.mediapipe

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import com.google.mediapipe.tasks.core.OutputHandler
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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

    // Base engine for sync inference (no result listener)
    private var llmInference: LlmInference? = null
    private var modelPath: String? = null
    private val initMutex = Mutex()

    val isLoaded: Boolean get() = llmInference != null

    /**
     * Returns true if the model file exists on disk and can be loaded.
     * Does NOT load the model — use ensureLoaded() for that.
     * AiRouter calls this to decide routing without triggering a load.
     */
    fun isModelAvailable(): Boolean {
        // Check external files dir (preferred — large file, pushed via ADB)
        val externalModel = File(context.getExternalFilesDir(null), "models/$MODEL_FILE")
        if (externalModel.exists()) return true
        // Check internal files dir (copied from assets)
        val internalModel = File(context.filesDir, MODEL_FILE)
        if (internalModel.exists()) return true
        // Check assets (bundled — only feasible for CI/testing, not production)
        return try {
            context.assets.open("models/$MODEL_FILE").use { true }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
        if (llmInference != null) return@withContext
        initMutex.withLock {
        if (llmInference != null) return@withLock

        val path = getModelPath()
            ?: error(
                "Gemma 4 E4B model not found at assets/models/$MODEL_FILE. " +
                "Run: python scripts/download_model/download_model.py --model e4b-int4"
            )
        modelPath = path

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(path)
            .setMaxTokens(MAX_TOKENS)
            .build()

        llmInference = LlmInference.createFromOptions(context, options)
        } // end initMutex.withLock
    }

    private fun getModelPath(): String? {
        val externalModel = File(context.getExternalFilesDir(null), "models/$MODEL_FILE")
        if (externalModel.exists()) return externalModel.absolutePath
        return try {
            // Copy asset to filesDir so MediaPipe gets a real filesystem path
            val outFile = File(context.filesDir, MODEL_FILE)
            if (!outFile.exists()) {
                context.assets.open("models/$MODEL_FILE").use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract model asset models/$MODEL_FILE", e)
            null
        }
    }

    private fun createSession(engine: LlmInference): LlmInferenceSession {
        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(TOP_K)
            .setTemperature(TEMPERATURE)
            .build()
        return LlmInferenceSession.createFromOptions(engine, sessionOptions)
    }

    /**
     * Streaming inference. Creates a temporary engine with the result listener baked in,
     * since LlmInferenceOptions.setResultListener is the only way to receive async tokens
     * in MediaPipe 0.10.20.
     */
    fun generateStream(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String> = callbackFlow {
        val path = modelPath
            ?: error("GemmaEngine not initialised. Call ensureLoaded() first.")

        val listener = OutputHandler.ProgressListener<String> { partialResult, done ->
            if (partialResult != null) trySend(partialResult)
            if (done) close()
        }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(path)
            .setMaxTokens(MAX_TOKENS)
            .setResultListener(listener)
            .build()

        val streamEngine = LlmInference.createFromOptions(context, options)
        val session = createSession(streamEngine)

        session.addQueryChunk(buildPrompt(systemPrompt, conversationHistory, userPrompt))
        session.generateResponseAsync()

        awaitClose {
            session.close()
            streamEngine.close()
        }
    }

    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val engine = llmInference
            ?: error("GemmaEngine not initialised. Call ensureLoaded() first.")
        // LlmInferenceSession.generateResponse() does NOT exist in MediaPipe 0.10.20.
        // The sync API lives on the base LlmInference engine directly.
        engine.generateResponse(buildPrompt(systemPrompt, conversationHistory, userPrompt))
    }

    // NOTE: image param is currently unused — MediaPipe 0.10.20 vision API
    // requires MPImage + session.addImage() which needs GraphOptions.setEnableVisionModality(true).
    // Wire this properly in Phase 4 when DocumentDecoder is fully implemented.
    // For now falls back to text-only inference so the build compiles.
    @Suppress("UNUSED_PARAMETER")
    suspend fun generateWithImage(
        systemPrompt: String,
        userPrompt: String,
        image: Bitmap
    ): Flow<String> = callbackFlow {
        val path = modelPath
            ?: error("GemmaEngine not initialised. Call ensureLoaded() first.")

        val listener = OutputHandler.ProgressListener<String> { partialResult, done ->
            if (partialResult != null) trySend(partialResult)
            if (done) close()
        }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(path)
            .setMaxTokens(MAX_TOKENS)
            .setResultListener(listener)
            .build()

        val streamEngine = LlmInference.createFromOptions(context, options)
        val session = createSession(streamEngine)

        session.addQueryChunk(buildPrompt(systemPrompt, emptyList(), userPrompt))
        session.generateResponseAsync()

        awaitClose {
            session.close()
            streamEngine.close()
        }
    }

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

    fun release() {
        llmInference?.close()
        llmInference = null
    }
}

data class ChatTurn(val role: ChatRole, val content: String)
enum class ChatRole { USER, ASSISTANT }




