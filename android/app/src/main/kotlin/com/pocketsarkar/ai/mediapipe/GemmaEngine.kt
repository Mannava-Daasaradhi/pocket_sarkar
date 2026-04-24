package com.pocketsarkar.ai.mediapipe

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
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
 * GemmaEngine — wraps the LiteRT-LM 0.9.0-alpha01 Android SDK.
 *
 * Verified API surface for 0.9.0-alpha01 (from compile errors + official docs):
 *   EngineConfig(modelPath, backend, cacheDir)
 *   Backend.CPU()                          ← constructor, not object singleton
 *   SamplerConfig(topK: Int, topP: Double, temperature: Float)
 *   ConversationConfig(
 *       systemMessage: Message?,           ← NOT systemInstruction / Contents
 *       initialMessages: List<Message>?,
 *       samplerConfig: SamplerConfig?
 *   )
 *   Message.of(text: String): Message      ← user turn
 *   Message.user(text: String): Message    ← explicit user role
 *   Message.model(text: String): Message   ← explicit assistant role
 *   Message.toString(): String             ← get text content (no .text property)
 *   sendMessage(message: Message): Message
 *   sendMessageAsync(message: Message): Flow<Message>
 *   Content.Text(text), Content.ImageBytes(bytes) ← multi-part (no Contents wrapper)
 */
@Singleton
class GemmaEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var engine: Engine? = null
    private val initMutex = Mutex()

    val modelPath: String =
        context.getExternalFilesDir(null)?.absolutePath + "/models/gemma-4-E4B-it-litert-lm.litertlm"

    // topP must be Double in 0.9.0-alpha01, temperature is Float
    private val defaultSampler = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8f)

    private val systemPromptText =
        "You are Pocket Sarkar, a helpful AI assistant for Indian citizens. " +
        "Answer in simple Hindi or English based on the user's language."

    /** Public so callers (DocumentDecoder, SchemeExplainer) can preload eagerly. */
    suspend fun ensureLoaded(): Engine = initMutex.withLock {
        if (engine == null) {
            val config = EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
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
                systemMessage = Message.of(systemPromptText),
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
            systemMessage = Message.of(systemPromptText),
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

    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): String {
        val eng = ensureLoaded()
        return withContext(Dispatchers.Default) {
            val config = ConversationConfig(
                systemMessage = Message.of(systemPrompt),
                initialMessages = conversationHistory.map { turn ->
                    when (turn.role) {
                        ChatRole.USER      -> Message.user(turn.content)
                        ChatRole.ASSISTANT -> Message.model(turn.content)
                    }
                },
                samplerConfig = defaultSampler,
            )
            eng.createConversation(config).use { conv ->
                conv.sendMessage(Message.of(userPrompt)).toString()
            }
        }
    }

    fun generateStream(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String> = flow {
        val eng = ensureLoaded()
        val config = ConversationConfig(
            systemMessage = Message.of(systemPrompt),
            initialMessages = conversationHistory.map { turn ->
                when (turn.role) {
                    ChatRole.USER      -> Message.user(turn.content)
                    ChatRole.ASSISTANT -> Message.model(turn.content)
                }
            },
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
     * Multipart message: Content.ImageBytes + Content.Text, no Contents wrapper.
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
            // Build a multi-part user message: image first, then text
            val multipartMessage = Message.of(
                listOf(
                    Content.ImageBytes(imageBytes),
                    Content.Text(userPrompt),
                )
            )
            conv.sendMessageAsync(multipartMessage).collect { msg ->
                val token = msg.toString()
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    fun isModelAvailable(): Boolean = File(modelPath).exists()

    fun release() {
        engine?.close()
        engine = null
    }
}