package com.pocketsarkar.modules.decoder

import android.graphics.Bitmap
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Document Decoder module.
 *
 * Pipeline:
 * 1. Receive image (camera frame, PDF page, screenshot)
 * 2. Preprocess: deskew, denoise, normalize brightness
 * 3. Pass directly to Gemma 4 vision encoder (no OCR step)
 * 4. Parse structured output → DecodeResult
 * 5. If vision confidence low → fallback to ML Kit OCR
 */
class DocumentDecoder @Inject constructor(
    private val gemma: GemmaEngine,
    private val preprocessor: ImagePreprocessor,
) {
    /**
     * Decode a document image.
     * Streams the raw AI response — call [parseDecodeResult] on the final string.
     */
    fun decodeStream(
        image: Bitmap,
        userLanguage: String = "hi"    // "hi" | "en" | "te" | "ta" etc.
    ): Flow<String> = flow {
        gemma.ensureLoaded()

        val processedImage = preprocessor.prepare(image)
        val prompt = buildUserPrompt(userLanguage)

        gemma.generateWithImage(
            systemPrompt = SYSTEM_PROMPT,
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
            .filter { it.startsWith("🚨") || it.startsWith("⚠️") }
            .map { line ->
                RedFlag(
                    severity = if (line.startsWith("🚨")) Severity.CRITICAL else Severity.MODERATE,
                    description = line.drop(2).trim()
                )
            }

        val riskScoreRaw = extractField("RISK_SCORE:")
        val riskScore = when {
            riskScoreRaw.contains("HIGH", ignoreCase = true) -> RiskScore.HIGH
            riskScoreRaw.contains("CAUTION", ignoreCase = true) -> RiskScore.CAUTION
            else -> RiskScore.SAFE
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

    companion object {
        /**
         * Production system prompt for Document Decoder.
         * Identical to the one in ai/prompts/decoder/system_prompt.txt
         * Keep both in sync when editing.
         */
        val SYSTEM_PROMPT = """
You are a plain-language legal translator for Indian citizens with limited literacy.

Analyze the document in this image. Do NOT describe what you see — analyze what it MEANS for the person holding it.

Return in this exact structure — nothing else:
DOCUMENT_TYPE: [one line identifying what this is]
SUMMARY: [2 sentences maximum, plain language in the user's language]
RED_FLAGS: [each risky clause on its own line, starting with 🚨 if critical, ⚠️ if moderate]
RISK_SCORE: [SAFE / CAUTION / HIGH RISK]
ACTION: [the single most important thing to do right now]
QUESTIONS: [exactly 2 questions to ask before signing]

Hard rules that cannot be broken:
- Never use: "beneficiary", "clause", "provisions", "pursuant", "hereinafter", "notwithstanding"
- Any interest rate not featured prominently in the document's headline → 🚨
- Any mention of contacts access, location tracking, or auto-debit → 🚨 immediately
- Any lock-in period longer than what was stated verbally → 🚨
- Any clause allowing self-assessment of damages → 🚨
- If document image quality is too low to read confidently: say so, do not guess
        """.trimIndent()
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
