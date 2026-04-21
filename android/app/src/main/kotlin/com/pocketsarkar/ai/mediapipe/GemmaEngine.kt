package com.pocketsarkar.ai.mediapipe

import android.content.Context
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
            context.assets.openFd("models/$MODEL_FILE").use { it.fileDescriptor.toString() }
            "models/$MODEL_FILE"
        } catch (e: Exception) {
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
        val session = createSession(engine)
        try {
            session.addQueryChunk(buildPrompt(systemPrompt, conversationHistory, userPrompt))
            session.generateResponse()
        } finally {
            session.close()
        }
    }

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

