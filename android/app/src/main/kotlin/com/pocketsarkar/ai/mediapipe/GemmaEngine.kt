package com.pocketsarkar.ai.mediapipe

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private val initMutex = Mutex()

    // Set to true if engine fails to load — triggers Ollama fallback in AiRouter
    private var engineLoadFailed = false

    val modelPath: String =
        context.getExternalFilesDir(null)?.absolutePath + "/models/gemma-4-E4B-it.litertlm"

    private val defaultSampler = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8)

    private val baseSystemPrompt =
        "You are Pocket Sarkar, a helpful AI assistant for Indian citizens. " +
        "Answer in simple Hindi or English based on the user's language."

    suspend fun ensureLoaded(): Engine = initMutex.withLock {
        if (engineLoadFailed) throw IllegalStateException("Engine failed to initialize — falling back to Ollama")
        if (engine == null) {
            try {
                val config = EngineConfig(
                    modelPath = modelPath,
                    cacheDir  = context.cacheDir.path,
                )
                val e = Engine(config)
                withContext(Dispatchers.IO) { e.initialize() }
                engine = e
            } catch (e: Exception) {
                engineLoadFailed = true
                throw e
            }
        }
        engine!!
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Injects the system prompt + history into the user message.
     * Works around the removal of systemMessage from ConversationConfig
     * in LiteRT-LM latest.release.
     */
    private fun buildPrompt(
        systemPrompt: String,
        userPrompt: String,
        history: List<ChatTurn> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.append("[System]\n$systemPrompt\n\n")
        if (history.isNotEmpty()) {
            sb.append("[Previous conversation]\n")
            history.forEach { turn ->
                val role = if (turn.role == ChatRole.USER) "User" else "Assistant"
                sb.append("$role: ${turn.content}\n")
            }
            sb.append("\n")
        }
        sb.append("[User]\n$userPrompt")
        return sb.toString()
    }

    private fun defaultConfig() = ConversationConfig(
        samplerConfig = defaultSampler
    )

    // ── Simple single-prompt API (used by AiRouter) ───────────────────────────

    suspend fun generate(prompt: String): String {
        val eng = ensureLoaded()
        return withContext(Dispatchers.Default) {
            eng.createConversation(defaultConfig()).use { conv ->
                conv.sendMessage(
                    Message.of(buildPrompt(baseSystemPrompt, prompt))
                ).toString()
            }
        }
    }

    fun generateStreaming(prompt: String): Flow<String> = flow {
        val eng = ensureLoaded()
        eng.createConversation(defaultConfig()).use { conv ->
            conv.sendMessageAsync(
                Message.of(buildPrompt(baseSystemPrompt, prompt))
            ).collect { msg ->
                val token = msg.toString()
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    // ── Rich API (used by SchemeExplainer, DocumentDecoder) ──────────────────

    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): String {
        val eng = ensureLoaded()
        return withContext(Dispatchers.Default) {
            eng.createConversation(defaultConfig()).use { conv ->
                conv.sendMessage(
                    Message.of(buildPrompt(systemPrompt, userPrompt, conversationHistory))
                ).toString()
            }
        }
    }

    fun generateStream(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String> = flow {
        val eng = ensureLoaded()
        eng.createConversation(defaultConfig()).use { conv ->
            conv.sendMessageAsync(
                Message.of(buildPrompt(systemPrompt, userPrompt, conversationHistory))
            ).collect { msg ->
                val token = msg.toString()
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    fun generateWithImage(
        systemPrompt: String,
        userPrompt: String,
        image: Bitmap
    ): Flow<String> = flow {
        val eng = ensureLoaded()
        val imageBytes = ByteArrayOutputStream().use { baos ->
            image.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            baos.toByteArray()
        }
        eng.createConversation(defaultConfig()).use { conv ->
            val multipart = Message.of(
                listOf(
                    Content.ImageBytes(imageBytes),
                    Content.Text(buildPrompt(systemPrompt, userPrompt)),
                )
            )
            conv.sendMessageAsync(multipart).collect { msg ->
                val token = msg.toString()
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    /**
     * Returns true only if the model file exists AND the engine hasn't
     * previously failed to load. A corrupt/incomplete file will set
     * engineLoadFailed=true on first attempt, triggering Ollama fallback.
     */
    fun isModelAvailable(): Boolean = File(modelPath).exists() && !engineLoadFailed

    fun release() {
        engine?.close()
        engine = null
        engineLoadFailed = false
    }
}