package com.pocketsarkar.modules.decoder

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Parses the raw string response from the AI model into a [DecoderResult].
 *
 * Handles all known model misbehaviours:
 *   1. Markdown-fenced JSON  (```json ... ``` or ``` ... ```)
 *   2. Leading/trailing whitespace or newlines
 *   3. Missing optional fields → safe defaults used
 *   4. riskScore outside 0–100 → clamped
 *   5. Invalid/unknown severity → defaults to "MEDIUM"
 *   6. Completely unparseable response → returns null
 */
class DecoderResponseParser @Inject constructor() {

    fun parse(rawResponse: String): DecoderResult? {
        val cleaned = stripMarkdownFences(rawResponse).trim()
        if (cleaned.isBlank()) return null

        return try {
            val json = JSONObject(cleaned)
            val riskScore = json.optInt("riskScore", 0).coerceIn(0, 100)

            DecoderResult(
                documentType      = json.optString("documentType", "Unknown Document"),
                languageDetected  = json.optString("languageDetected", "Unknown"),
                riskScore         = riskScore,
                riskLevel         = RiskLevel.fromScore(riskScore),
                redFlags          = parseRedFlags(json.optJSONArray("redFlags")),
                userRights        = parseStringList(json.optJSONArray("userRights")),
                suggestedQuestions = parseStringList(json.optJSONArray("suggestedQuestions")),
                actionRequired    = json.optString("actionRequired", "Consult a legal professional before signing."),
                summary           = json.optString("summary", "Document analysis complete."),
            )
        } catch (e: Exception) {
            // Completely unparseable — caller shows "Could not parse document"
            null
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Strip ``` or ```json fences that some model generations wrap around JSON.
     * Also strips any text before the first `{` and after the last `}`.
     */
    private fun stripMarkdownFences(raw: String): String {
        // Remove ```json or ``` fences
        var cleaned = raw
            .replace(Regex("^```(?:json)?\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("```\\s*$", RegexOption.MULTILINE), "")

        // Extract just the JSON object if there's surrounding text
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd   = cleaned.lastIndexOf('}')
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
        }

        return cleaned.trim()
    }

    private fun parseRedFlags(array: JSONArray?): List<RedFlag> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    RedFlag(
                        clause   = obj.optString("clause", ""),
                        risk     = obj.optString("risk", ""),
                        severity = normalizeSeverity(obj.optString("severity", "MEDIUM")),
                    )
                )
            }
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val s = array.optString(i, "")
                if (s.isNotBlank()) add(s)
            }
        }
    }

    private fun normalizeSeverity(raw: String): String =
        when (raw.uppercase().trim()) {
            "HIGH"   -> "HIGH"
            "LOW"    -> "LOW"
            else     -> "MEDIUM"
        }
}
