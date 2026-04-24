package com.pocketsarkar.modules.decoder

// ─────────────────────────────────────────────────────────────────────────────
// Phase 4 — Document Decoder output model
//
// DecoderResult is the single structured object produced after:
//   DocumentInput → DocumentDecoder → DecoderResponseParser → DecoderResult
// ─────────────────────────────────────────────────────────────────────────────

data class DecoderResult(
    /** e.g. "Rental Agreement", "Personal Loan T&C", "Employment Bond" */
    val documentType: String,

    /** e.g. "Hindi", "English", "Marathi" */
    val languageDetected: String,

    /** 0–100. 0 = completely safe, 100 = extremely dangerous. */
    val riskScore: Int,

    /** Derived from riskScore: GREEN (0-30), YELLOW (31-60), RED (61-100) */
    val riskLevel: RiskLevel,

    /** Every exploitative or non-standard clause found. Empty list = clean document. */
    val redFlags: List<RedFlag>,

    /** Legal rights/protections available to the user under Indian law. */
    val userRights: List<String>,

    /** 3–5 questions to ask the other party before signing. */
    val suggestedQuestions: List<String>,

    /** Single most important next step in plain language. */
    val actionRequired: String,

    /** 2–3 sentence plain-language summary. */
    val summary: String,
)

data class RedFlag(
    /** Short exact quote from the document (≤10 words). */
    val clause: String,

    /** Plain-language explanation of how this hurts the user. */
    val risk: String,

    /** HIGH / MEDIUM / LOW */
    val severity: String,
)

enum class RiskLevel {
    GREEN,   // 0–30  — safe
    YELLOW,  // 31–60 — caution
    RED;     // 61–100 — high risk

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score <= 30 -> GREEN
            score <= 60 -> YELLOW
            else        -> RED
        }
    }
}
