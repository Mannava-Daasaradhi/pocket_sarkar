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

/**
 * GemmaEngine — wraps LiteRT-LM 0.9.0-alpha01.
 *
 * Exact API derived from compiler errors (ground truth):
 *   EngineConfig(modelPath, cacheDir)         — no backend param; defaults to CPU
 *   SamplerConfig(topK: Int, topP: Double, temperature: Double)  — all Double
 *   ConversationConfig(systemMessage, samplerConfig)  — NO initialMessages in alpha01
 *   Message.of(text: String): Message         — only factory that exists
 *   sendMessage(Message): Message
 *   sendMessageAsync(Message): Flow<Message>
 *   Message.toString() gives the response text
 *
 * Conversation history: injected into the system prompt string as "Previous turns:"
 * since initialMessages is not available in this alpha.
 */
@Singleton
class GemmaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private val initMutex = Mutex()

    val modelPath: String =
        context.getExternalFilesDir(null)?.absolutePath + "/models/gemma-4-E4B-it-litert-lm.litertlm"

    // All SamplerConfig params are Double in 0.9.0-alpha01
    private val defaultSampler = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8)

    private val baseSystemPrompt =
        "You are Pocket Sarkar, a helpful AI assistant for Indian citizens. " +
        "Answer in simple Hindi or English based on the user's language."

    /** Public so callers (DocumentDecoder, SchemeExplainer) can preload eagerly. */
    suspend fun ensureLoaded(): Engine = initMutex.withLock {
        if (engine == null) {
            // No 'backend' param in 0.9.0-alpha01 EngineConfig — defaults to CPU
            val config = EngineConfig(
                modelPath = modelPath,
                cacheDir = context.cacheDir.path,
            )
            val e = Engine(config)
            withContext(Dispatchers.IO) { e.initialize() }
            engine = e
        }
        engine!!
    }

    // ── Simple single-prompt API (used by AiRouter) ───────────────────────────

    suspend fun generate(prompt: String): String {
        val eng = ensureLoaded()
        return withContext(Dispatchers.Default) {
            val config = ConversationConfig(
                systemMessage = Message.of(baseSystemPrompt),
                samplerConfig = defaultSampler,
            )
            eng.createConversation(config).use { conv ->
                conv.sendMessage(Message.of(prompt)).toString()
            }
        }
    }

    fun generateStreaming(prompt: String): Flow<String> = flow {
        val eng = ensureLoaded()
        val config = ConversationConfig(
            systemMessage = Message.of(baseSystemPrompt),
            samplerConfig = defaultSampler,
        )
        eng.createConversation(config).use { conv ->
            conv.sendMessageAsync(Message.of(prompt)).collect { msg ->
                val token = msg.toString()
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    // ── Rich API (used by SchemeExplainer, DocumentDecoder) ──────────────────

    /**
     * Non-streaming with custom system prompt + history.
     * History is serialised into the system prompt since ConversationConfig
     * has no initialMessages param in 0.9.0-alpha01.
     */
    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): String {
        val eng = ensureLoaded()
        return withContext(Dispatchers.Default) {
            val fullSystem = buildSystemWithHistory(systemPrompt, conversationHistory)
            val config = ConversationConfig(
                systemMessage = Message.of(fullSystem),
                samplerConfig = defaultSampler,
            )
            eng.createConversation(config).use { conv ->
                conv.sendMessage(Message.of(userPrompt)).toString()
            }
        }
    }

    /** Streaming with custom system prompt + history. */
    fun generateStream(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String> = flow {
        val eng = ensureLoaded()
        val fullSystem = buildSystemWithHistory(systemPrompt, conversationHistory)
        val config = ConversationConfig(
            systemMessage = Message.of(fullSystem),
            samplerConfig = defaultSampler,
        )
        eng.createConversation(config).use { conv ->
            conv.sendMessageAsync(Message.of(userPrompt)).collect { msg ->
                val token = msg.toString()
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    /**
     * Vision + text streaming (DocumentDecoder).
     * Multi-part message via Message.of(List<Content>).
     */
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
        val config = ConversationConfig(
            systemMessage = Message.of(systemPrompt),
            samplerConfig = defaultSampler,
        )
        eng.createConversation(config).use { conv ->
            val multipart = Message.of(
                listOf(
                    Content.ImageBytes(imageBytes),
                    Content.Text(userPrompt),
                )
            )
            conv.sendMessageAsync(multipart).collect { msg ->
                val token = msg.toString()
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    // Serialise history into system prompt (workaround for missing initialMessages)
    private fun buildSystemWithHistory(
        systemPrompt: String,
        history: List<ChatTurn>
    ): String {
        if (history.isEmpty()) return systemPrompt
        val turns = history.joinToString("\n") { turn ->
            val role = when (turn.role) {
                ChatRole.USER      -> "User"
                ChatRole.ASSISTANT -> "Assistant"
            }
            "$role: ${turn.content}"
        }
        return "$systemPrompt\n\nPrevious conversation:\n$turns"
    }

    fun isModelAvailable(): Boolean = File(modelPath).exists()

    fun release() {
        engine?.close()
        engine = null
    }
}