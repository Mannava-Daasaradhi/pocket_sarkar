package com.pocketsarkar.modules.schemes

import android.content.Context
import android.util.Log
import com.pocketsarkar.ai.mediapipe.ChatRole
import com.pocketsarkar.ai.mediapipe.ChatTurn
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.db.dao.SchemeDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SchemeExplainer"

// ─────────────────────────────────────────────────────────────────────────────
// Data classes — ConversationState (Step 4)
// ─────────────────────────────────────────────────────────────────────────────

data class Message(
    val role: String,         // "user" or "assistant"
    val content: String,
    val schemeId: String? = null,
)

data class ConversationState(
    val messages: List<Message> = emptyList(),
    val currentSchemeContext: com.pocketsarkar.db.entities.Scheme? = null,
    val userProfileContext: UserProfile? = null,
)

data class FakeDetectionResult(
    val isFake: Boolean,
    val confidence: Double,
    val reasons: List<String>,
    val flagsDetected: List<String>,
)

data class SchemeQueryResult(
    val schemes: List<com.pocketsarkar.db.entities.Scheme>,
    val queryUsed: String,
    val isFakeCheck: Boolean = false,
)

sealed class SchemeStreamEvent {
    data class FakeDetectionComplete(val result: FakeDetectionResult) : SchemeStreamEvent()
    data class FunctionCallExecuted(val query: String, val resultCount: Int) : SchemeStreamEvent()
    data class Token(val text: String) : SchemeStreamEvent()
    data class Complete(val fullText: String) : SchemeStreamEvent()
    data class Error(val message: String) : SchemeStreamEvent()
}

// ─────────────────────────────────────────────────────────────────────────────
// SchemeExplainer
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class SchemeExplainer @Inject constructor(
    private val gemma: GemmaEngine,
    private val schemeDao: SchemeDao,
    private val eligibilityEngine: EligibilityEngine,
    @ApplicationContext private val context: Context,
) {
    private val conversationHistory = mutableListOf<ChatTurn>()
    private val historyMutex = Mutex()

    private var _conversationState = ConversationState()
    private val stateMutex = Mutex()

    val conversationState: ConversationState get() = _conversationState

    private val systemPrompt: String by lazy { loadAsset("ai/prompts/schemes/system_prompt.txt") }
    private val fakeDetectionPrompt: String by lazy { loadAsset("ai/prompts/schemes/fake_detection.txt") }

    // ── Public API ────────────────────────────────────────────────────────────

    fun query(
        userMessage: String,
        userProfile: UserProfile? = null,
        userLanguage: String = "hi",
    ): Flow<SchemeStreamEvent> = flow {

        // Pre-flight: check model availability before doing anything
        val modelReady = runCatching { gemma.ensureLoaded() }.isSuccess
        if (!modelReady) {
            emit(SchemeStreamEvent.FakeDetectionComplete(
                FakeDetectionResult(false, 0.0, emptyList(), emptyList())
            ))
            emit(SchemeStreamEvent.FunctionCallExecuted("", 0))
            val msg = when {
                userLanguage.contains("hi", ignoreCase = true) ->
                    "Model abhi download nahi hua hai. Kripya pehle Model Setup screen se Gemma download karein, ya phir internet se connect ho jaayein."
                else ->
                    "The AI model is not downloaded yet. Please go to Model Setup to download Gemma, or connect to the internet."
            }
            emit(SchemeStreamEvent.Token(msg))
            emit(SchemeStreamEvent.Complete(msg))
            return@flow
        }

        // Pass 0: Fake detection (runs first, always)
        val fakeResult = runCatching {
            detectFakeScheme(userMessage)
        }.getOrElse {
            Log.w(TAG, "Fake detection failed: ${it.message}")
            FakeDetectionResult(false, 0.0, emptyList(), emptyList())
        }
        emit(SchemeStreamEvent.FakeDetectionComplete(fakeResult))

        // Pass 1: Tool-call routing
        val historySnapshot = historyMutex.withLock { conversationHistory.toList() }
        val toolCallResponse = gemma.generate(
            systemPrompt        = TOOL_CALL_SYSTEM_PROMPT,
            userPrompt          = buildToolCallPrompt(userMessage, userProfile, historySnapshot),
            conversationHistory = historySnapshot,
        )
        Log.d(TAG, "Tool call response: $toolCallResponse")

        // Pass 2: Execute DB query
        val dbResults = executeToolCall(toolCallResponse, userProfile)
        Log.d(TAG, "DB results: ${dbResults.schemes.size} schemes, query='${dbResults.queryUsed}'")
        emit(SchemeStreamEvent.FunctionCallExecuted(dbResults.queryUsed, dbResults.schemes.size))

        // Pass 3: Stream explanation
        val explainPrompt = buildExplainPrompt(userMessage, dbResults, userLanguage)
        val fullResponse = StringBuilder()
        try {
            gemma.generateStream(
                systemPrompt        = systemPrompt,
                userPrompt          = explainPrompt,
                conversationHistory = historySnapshot,
            ).collect { token ->
                fullResponse.append(token)
                emit(SchemeStreamEvent.Token(token))
            }
        } finally {
            val responseText = fullResponse.toString()
            emit(SchemeStreamEvent.Complete(responseText))

            // Update history (max 10 messages → 5 pairs)
            historyMutex.withLock {
                if (conversationHistory.size >= 20) {
                    repeat(2) { conversationHistory.removeAt(0) }
                }
                conversationHistory.add(ChatTurn(ChatRole.USER, userMessage))
                conversationHistory.add(ChatTurn(ChatRole.ASSISTANT, responseText))
            }

            // Update ConversationState
            stateMutex.withLock {
                val activeSchemeId = dbResults.schemes.firstOrNull()?.id
                val updatedMessages = (_conversationState.messages + listOf(
                    Message("user", userMessage, activeSchemeId),
                    Message("assistant", responseText, activeSchemeId),
                )).takeLast(10)
                _conversationState = _conversationState.copy(
                    messages             = updatedMessages,
                    currentSchemeContext = dbResults.schemes.firstOrNull()
                        ?: _conversationState.currentSchemeContext,
                    userProfileContext   = userProfile ?: _conversationState.userProfileContext,
                )
            }
        }
    }

    suspend fun clearHistory() {
        historyMutex.withLock { conversationHistory.clear() }
        stateMutex.withLock { _conversationState = ConversationState() }
    }

    // ── Fake detection ────────────────────────────────────────────────────────

    private suspend fun detectFakeScheme(userMessage: String): FakeDetectionResult {
        val response = gemma.generate(
            systemPrompt        = fakeDetectionPrompt,
            userPrompt          = userMessage,
            conversationHistory = emptyList(),
        )
        return parseFakeDetectionResult(response)
    }

    private fun parseFakeDetectionResult(response: String): FakeDetectionResult {
        return runCatching {
            val json       = JSONObject(extractJson(response))
            val confidence = json.optDouble("confidence", 0.0)
            val reasons    = mutableListOf<String>()
            val flags      = mutableListOf<String>()
            json.optJSONArray("reasons")?.let  { for (i in 0 until it.length()) reasons.add(it.getString(i)) }
            json.optJSONArray("flagsDetected")?.let { for (i in 0 until it.length()) flags.add(it.getString(i)) }
            FakeDetectionResult(
                isFake        = confidence > 0.7,
                confidence    = confidence,
                reasons       = reasons,
                flagsDetected = flags,
            )
        }.getOrElse {
            Log.w(TAG, "Failed to parse fake detection JSON: ${it.message}")
            FakeDetectionResult(false, 0.0, emptyList(), emptyList())
        }
    }

    // ── Tool call execution ───────────────────────────────────────────────────

    /** Exposed for tests — parse and execute a raw model response containing a function call. */
    internal suspend fun parseAndExecuteFunctionCall(modelResponse: String): SchemeQueryResult =
        executeToolCall(modelResponse, null)

    private suspend fun executeToolCall(
        toolCallResponse: String,
        profile: UserProfile?,
    ): SchemeQueryResult {

        // Pattern 1: [FUNCTION_CALL: query_scheme_db({...})]
        val funcRegex = Regex(
            """\[FUNCTION_CALL:\s*query_scheme_db\(\s*(\{.*?})\s*\)\]""",
            RegexOption.DOT_MATCHES_ALL
        )
        funcRegex.find(toolCallResponse)?.let { match ->
            return runCatching {
                val args     = JSONObject(match.groupValues[1])
                val query    = args.optString("query", "").trim()
                val state    = args.optString("state", profile?.state ?: "ALL")
                val category = args.optString("category", "ALL")
                Log.d(TAG, "FUNCTION_CALL intercepted: query='$query' state='$state' category='$category'")

                val schemes = schemeDao.searchSchemes(query, limit = 20)
                    .filter { scheme ->
                        (state == "ALL" ||
                            scheme.targetStates == "ALL" ||
                            scheme.targetStates.split(",").map { it.trim() }.contains(state)) &&
                        (category == "ALL" || scheme.category == category)
                    }
                    .take(5)
                SchemeQueryResult(schemes = schemes, queryUsed = query)
            }.getOrElse {
                Log.w(TAG, "FUNCTION_CALL parse error: ${it.message}")
                fallbackSearch(toolCallResponse, profile)
            }
        }

        // Pattern 2: Legacy JSON tool call
        val jsonRegex = Regex("""\{[^{}]*"tool"[^{}]*}""", RegexOption.DOT_MATCHES_ALL)
        jsonRegex.find(toolCallResponse)?.let { match ->
            return runCatching {
                val json = JSONObject(match.value)
                when (json.optString("tool")) {
                    "query_scheme_db" -> {
                        val args  = json.getJSONObject("args")
                        val query = args.optString("query", "").trim()
                        val state = args.optString("state", profile?.state ?: "ALL")
                        val schemes = schemeDao.searchSchemes(query, limit = 20)
                            .filter { it.targetStates == "ALL" || it.targetStates.split(",").map { s -> s.trim() }.contains(state) }
                            .take(5)
                        SchemeQueryResult(schemes = schemes, queryUsed = query)
                    }
                    "get_eligible_schemes" -> {
                        val eligible = profile?.let { eligibilityEngine.getEligibleSchemes(it) } ?: emptyList()
                        SchemeQueryResult(schemes = eligible.map { it.scheme }, queryUsed = "eligibility_check")
                    }
                    else -> fallbackSearch(toolCallResponse, profile)
                }
            }.getOrElse { fallbackSearch(toolCallResponse, profile) }
        }

        return fallbackSearch(toolCallResponse, profile)
    }

    private suspend fun fallbackSearch(raw: String, profile: UserProfile?): SchemeQueryResult {
        val q = raw.replace(Regex("""[{}\[\]"\\:,]"""), " ").replace(Regex("""\s+"""), " ").trim().take(100)
        Log.d(TAG, "Fallback FTS search: '$q'")
        return SchemeQueryResult(schemes = schemeDao.searchSchemes(q, limit = 3), queryUsed = "fallback:$q")
    }

    // ── Prompt builders ───────────────────────────────────────────────────────

    private fun buildToolCallPrompt(
        userMessage: String,
        profile: UserProfile?,
        history: List<ChatTurn>,
    ): String = buildString {
        profile?.let { appendLine("User profile: state=${it.state}, income=${it.annualIncomeRupees}, category=${it.socialCategory}") }
        _conversationState.currentSchemeContext?.let {
            appendLine("Currently discussing: ${it.nameEn} (${it.nameHi}) [ID: ${it.id}]")
        }
        appendLine("User question: $userMessage")
    }.trimEnd()

    private fun buildExplainPrompt(
        originalQuery: String,
        dbResults: SchemeQueryResult,
        language: String,
    ): String {
        if (dbResults.schemes.isEmpty()) {
            return """
                User asked: "$originalQuery"
                query_scheme_db returned: {"found": false, "schemes": []}
                No matching schemes found. Follow the NOT FOUND rule exactly.
                Language: $language
            """.trimIndent()
        }

        val schemesJson = dbResults.schemes.joinToString("\n\n") { s ->
            val verifiedDate = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
                .format(java.util.Date(s.lastVerifiedEpoch))
            """
            SCHEME: ${s.nameHi} (${s.nameEn})
            ID: ${s.id}
            Description: ${s.descriptionHi}
            Benefit: ${s.benefitAmount ?: "Not specified in DB"}
            BenefitType: ${s.benefitType}
            Target: states=${s.targetStates}, gender=${s.targetGender}, category=${s.targetCategory}
            MaxIncomeLPA: ${s.maxIncomeLPA}
            MinAge: ${s.minAge}, MaxAge: ${s.maxAge}
            Confidence: ${(s.confidenceScore * 100).toInt()}%
            LastVerified: $verifiedDate
            ApplicationURL: ${s.portalUrl ?: "https://myscheme.gov.in"}
            """.trimIndent()
        }

        return """
            User asked: "$originalQuery"
            Language: $language
            
            VERIFIED DATABASE RESULTS — explain ONLY these. Do not add or invent.
            query_scheme_db returned: {"found": true}
            
            $schemesJson
        """.trimIndent()
    }

    private fun extractJson(text: String): String {
        val fenced = Regex("""```(?:json)?\s*(\{.*?})\s*```""", RegexOption.DOT_MATCHES_ALL).find(text)
        return fenced?.groupValues?.get(1) ?: text.trim()
    }

    private fun loadAsset(path: String): String =
        runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }
            .getOrElse {
                Log.w(TAG, "Could not load asset '$path': ${it.message}")
                FALLBACK_SYSTEM_PROMPT
            }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        internal val TOOL_CALL_SYSTEM_PROMPT = """
You are a tool-call router. Based on the user question, output EXACTLY one function call and NOTHING else.

SYNTAX:
[FUNCTION_CALL: query_scheme_db({"query": "<2-5 key words>", "state": "<2-letter state or ALL>", "category": "<AGRICULTURE|EDUCATION|HEALTH|HOUSING|WOMEN|YOUTH|SC_ST|GENERAL|ALL>"})]

- Use currently-discussing scheme context for follow-up questions if no new scheme is named.
- Output ONLY the [FUNCTION_CALL: ...] line. Zero other text.
        """.trimIndent()

        private val FALLBACK_SYSTEM_PROMPT =
            "You are Pocket Sarkar. Explain ONLY the scheme data provided. " +
            "If no data, say: Mujhe yeh scheme nahi mili. Kripya myscheme.gov.in dekhein."
    }
}