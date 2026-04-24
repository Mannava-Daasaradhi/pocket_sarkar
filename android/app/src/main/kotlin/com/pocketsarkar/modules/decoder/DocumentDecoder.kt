package com.pocketsarkar.modules.decoder

import android.content.Context
import android.graphics.Bitmap
import com.pocketsarkar.ai.mediapipe.GemmaEngine   // ← unchanged, but now @Inject-able
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject


/**
 * Document Decoder module.
 *
 * Pipeline:
 * 1. Receive image (camera frame, PDF page, screenshot)
 * 2. Preprocess: deskew, denoise, normalize brightness
 * 3. Pass directly to Gemma 4 vision encoder (no OCR step)
 * 4. Parse structured output -> DecodeResult
 * 5. If vision confidence low -> fallback to ML Kit OCR
 *
 * Bug 6 fix: SYSTEM_PROMPT is no longer hardcoded. It is loaded from
 * assets/ai/prompts/decoder/system_prompt.txt at runtime so that prompts
 * are versioned files (FOLDER_STRUCTURE.md rule 3), never constants.
 */
class DocumentDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemma: GemmaEngine,
    private val preprocessor: ImagePreprocessor,
) {
    /**
     * Loaded lazily — reads the versioned prompt file from assets once per
     * process lifetime. Thread-safe because `by lazy` uses a lock by default.
     */
    private val systemPrompt: String by lazy {
        context.assets
            .open("ai/prompts/decoder/system_prompt.txt")
            .bufferedReader()
            .readText()
            .trim()
    }

    /**
     * Decode a document image.
     * Streams the raw AI response — call [parseDecodeResult] on the final string.
     */
    fun decodeStream(
        image: Bitmap,
        userLanguage: String = "hi"
    ): Flow<String> = flow {
        runCatching { gemma.ensureLoaded() }.onFailure {
            emit("ERROR: AI model not available. Please download the model first.")
            return@flow
        }

        val processedImage = withContext(Dispatchers.Default) { preprocessor.prepare(image) }
        val prompt = buildUserPrompt(userLanguage)

        gemma.generateWithImage(
            systemPrompt = systemPrompt,
            userPrompt = prompt,
            image = processedImage
        ).collect { emit(it) }
    }

    /**
     * Parse the raw AI response string into a structured result.
     * Called once streaming is complete.
     */
    fun parseDecodeResult(rawResponse: String): DecodeResult {
        val lines = rawResponse.lines()

        fun extractField(prefix: String): String =
            lines.firstOrNull { it.startsWith(prefix) }
                ?.removePrefix(prefix)?.trim() ?: ""

        val redFlags = lines
            .filter { it.startsWith("\uD83D\uDEA8") || it.startsWith("\u26A0\uFE0F") }
            .map { line ->
                RedFlag(
                    severity = if (line.startsWith("RED_FLAG_CRITICAL:")) Severity.CRITICAL else Severity.MODERATE,
                    description = line.substringAfter(":").trim()
                )
            }

        val riskScoreRaw = extractField("RISK_SCORE:")
        val riskScore = when {
            riskScoreRaw.contains("HIGH", ignoreCase = true) -> RiskScore.HIGH
            riskScoreRaw.contains("CAUTION", ignoreCase = true) -> RiskScore.CAUTION
            riskScoreRaw.contains("SAFE", ignoreCase = true) -> RiskScore.SAFE
            else -> RiskScore.CAUTION
        }

        val questions = lines
            .dropWhile { !it.startsWith("QUESTIONS:") }
            .drop(1)
            .take(2)
            .filter { it.isNotBlank() }

        return DecodeResult(
            documentType = extractField("DOCUMENT_TYPE:"),
            summary = extractField("SUMMARY:"),
            redFlags = redFlags,
            riskScore = riskScore,
            action = extractField("ACTION:"),
            questionsToAsk = questions,
            rawResponse = rawResponse
        )
    }

    private fun buildUserPrompt(language: String): String {
        val langInstruction = when (language) {
            "hi" -> "Respond in Hindi (Devanagari). Plain language — no legalese."
            "te" -> "Respond in Telugu. Plain language — no legalese."
            "ta" -> "Respond in Tamil. Plain language — no legalese."
            "bn" -> "Respond in Bengali. Plain language — no legalese."
            else -> "Respond in simple English. No legal jargon."
        }
        return "Analyze this document. $langInstruction"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Output model
// ─────────────────────────────────────────────────────────────────────────────

data class DecodeResult(
    val documentType: String,
    val summary: String,
    val redFlags: List<RedFlag>,
    val riskScore: RiskScore,
    val action: String,
    val questionsToAsk: List<String>,
    val rawResponse: String,
)

data class RedFlag(
    val severity: Severity,
    val description: String,
)

enum class Severity { CRITICAL, MODERATE }

enum class RiskScore { SAFE, CAUTION, HIGH }
