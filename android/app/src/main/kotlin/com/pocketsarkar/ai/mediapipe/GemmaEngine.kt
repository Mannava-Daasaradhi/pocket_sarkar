package com.pocketsarkar.ai.mediapipe

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
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

    val modelPath: String =
        context.getExternalFilesDir(null)?.absolutePath + "/models/gemma-4-E4B-it-litert-lm.litertlm"

    private val defaultSampler = SamplerConfig(topK = 40, temperature = 0.8f)

    // Public so callers (DocumentDecoder, SchemeExplainer) can preload eagerly
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
                systemInstruction = Contents.of(
                    "You are Pocket Sarkar, a helpful AI assistant for Indian citizens. " +
                    "Answer in simple Hindi or English based on the user's language."
                ),
                samplerConfig = defaultSampler,
            )
            eng.createConversation(config).use { conv ->
                conv.sendMessage(prompt).text ?: ""
            }
        }
    }

    fun generateStreaming(prompt: String): Flow<String> = flow {
        val eng = ensureLoaded()
        val config = ConversationConfig(
            systemInstruction = Contents.of(
                "You are Pocket Sarkar, a helpful AI assistant for Indian citizens. " +
                "Answer in simple Hindi or English based on the user's language."
            ),
            samplerConfig = defaultSampler,
        )
        eng.createConversation(config).use { conv ->
            conv.sendMessageAsync(prompt).collect { message ->
                val token = message.text ?: ""
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    // ── Rich API (used by SchemeExplainer, DocumentDecoder) ──────────────────

    /**
     * Non-streaming call with custom system prompt and conversation history.
     * History is replayed via LiteRT-LM's initialMessages — no extra model calls.
     */
    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): String {
        val eng = ensureLoaded()
        return withContext(Dispatchers.Default) {
            val config = ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                initialMessages = conversationHistory.map { turn ->
                    when (turn.role) {
                        ChatRole.USER -> Message.user(turn.content)
                        ChatRole.ASSISTANT -> Message.model(turn.content)
                    }
                },
                samplerConfig = defaultSampler,
            )
            eng.createConversation(config).use { conv ->
                conv.sendMessage(userPrompt).text ?: ""
            }
        }
    }

    /**
     * Streaming call with custom system prompt and conversation history.
     */
    fun generateStream(
        systemPrompt: String,
        userPrompt: String,
        conversationHistory: List<ChatTurn> = emptyList()
    ): Flow<String> = flow {
        val eng = ensureLoaded()
        val config = ConversationConfig(
            systemInstruction = Contents.of(systemPrompt),
            initialMessages = conversationHistory.map { turn ->
                when (turn.role) {
                    ChatRole.USER -> Message.user(turn.content)
                    ChatRole.ASSISTANT -> Message.model(turn.content)
                }
            },
            samplerConfig = defaultSampler,
        )
        eng.createConversation(config).use { conv ->
            conv.sendMessageAsync(userPrompt).collect { message ->
                val token = message.text ?: ""
                if (token.isNotEmpty()) emit(token)
            }
        }
    }

    /**
     * Vision + text streaming (DocumentDecoder).
     * Converts Bitmap → JPEG bytes → LiteRT-LM Content.ImageBytes.
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
            systemInstruction = Contents.of(systemPrompt),
            samplerConfig = defaultSampler,
        )
        eng.createConversation(config).use { conv ->
            conv.sendMessageAsync(
                Contents.of(
                    Content.ImageBytes(imageBytes),
                    Content.Text(userPrompt),
                )
            ).collect { message ->
                val token = message.text ?: ""
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