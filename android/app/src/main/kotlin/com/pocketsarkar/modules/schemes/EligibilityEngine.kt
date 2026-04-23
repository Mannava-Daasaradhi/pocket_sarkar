package com.pocketsarkar.modules.schemes

import com.pocketsarkar.db.dao.SchemeDao
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.Scheme
import javax.inject.Inject

/**
 * Client-side eligibility rule engine.
 *
 * Evaluates a UserProfile against the EligibilityRules stored in SQLite
 * and returns which schemes the user qualifies for.
 *
 * This runs 100% on-device. No profile data ever leaves the phone.
 * This is the engine behind both Scheme Explainer (targeted) and
 * Opportunity Radar (proactive / exploratory).
 */
class EligibilityEngine @Inject constructor(
    private val schemeDao: SchemeDao
) {

    /**
     * Main entry point: given a profile, return all schemes the user qualifies for.
     * Sorted by decayed confidence score descending (most verified first).
     */
    suspend fun getEligibleSchemes(profile: UserProfile): List<EligibleSchemeResult> {
        val stateFilter = if (profile.state == "ALL") "" else profile.state
        val candidates = schemeDao.getSchemesByCategory(
            category = null,   // null = all categories
            state = stateFilter
        )

        val now = System.currentTimeMillis()
        return candidates
            .mapNotNull { scheme -> evaluate(scheme, profile, now) }
            .sortedByDescending { it.confidence }
    }

    /**
     * Evaluate a single scheme against the profile.
     * Returns null if ineligible.
     */
    suspend fun evaluate(
        scheme: Scheme,
        profile: UserProfile,
        nowMs: Long = System.currentTimeMillis()
    ): EligibleSchemeResult? {
        val rules = schemeDao.getRulesForScheme(scheme.id)
        val failedRules = mutableListOf<String>()
        val metRules = mutableListOf<String>()

        for (rule in rules) {
            if (ruleMatches(rule, profile)) {
                metRules.add(rule.labelEn)
            } else {
                failedRules.add(rule.labelEn)
            }
        }

        // Must pass ALL rules to be eligible
        if (failedRules.isNotEmpty()) return null

        val decayedScore = computeDecayedScore(scheme.confidenceScore, scheme.lastVerifiedEpoch, nowMs)
        return EligibleSchemeResult(
            scheme = scheme,
            metCriteria = metRules,
            confidence = decayedScore,
            isStale = decayedScore < STALE_THRESHOLD
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Constants
// ─────────────────────────────────────────────────────────────────────────────

const val STALE_THRESHOLD = 0.6f
const val DECAY_PER_WEEK  = 0.01f
const val MS_PER_WEEK     = 7L * 24 * 60 * 60 * 1000

// ─────────────────────────────────────────────────────────────────────────────
// Confidence score decay
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reduces confidence score by 0.01 per week since lastVerifiedEpoch.
 * Floored at 0.0.
 */
fun computeDecayedScore(
    confidenceScore: Float,
    lastVerifiedEpoch: Long,
    nowMs: Long = System.currentTimeMillis()
): Float {
    val ageMs = nowMs - lastVerifiedEpoch
    if (ageMs <= 0L) return confidenceScore
    val weeksElapsed = ageMs / MS_PER_WEEK
    val decayed = confidenceScore - (DECAY_PER_WEEK * weeksElapsed)
    return maxOf(0f, decayed)
}

/**
 * Returns true when the decayed confidence score is below the stale threshold (0.6).
 */
fun computeIsStale(
    confidenceScore: Float,
    lastVerifiedEpoch: Long,
    nowMs: Long = System.currentTimeMillis()
): Boolean = computeDecayedScore(confidenceScore, lastVerifiedEpoch, nowMs) < STALE_THRESHOLD

// ─────────────────────────────────────────────────────────────────────────────
// Pure eligibility evaluation functions (also used in unit tests)
// ─────────────────────────────────────────────────────────────────────────────

fun ruleMatches(rule: EligibilityRule, profile: UserProfile): Boolean {
    return when (rule.field) {
        "annual_income" -> {
            val income = profile.annualIncomeRupees ?: return true  // unknown = assume eligible
            val threshold = rule.value.toLongOrNull() ?: return true
            compareValues(income.toDouble(), threshold.toDouble(), rule.operator)
        }
        "land_hectares" -> {
            val land = profile.landHectares ?: return true
            val threshold = rule.value.toDoubleOrNull() ?: return true
            compareValues(land, threshold, rule.operator)
        }
        "age" -> {
            val age = profile.age ?: return true
            val threshold = rule.value.toIntOrNull() ?: return true
            compareValues(age.toDouble(), threshold.toDouble(), rule.operator)
        }
        "state" -> {
            if (rule.value == "ALL") return true
            val allowedStates = rule.value.split(",").map { it.trim() }
            profile.state in allowedStates
        }
        "category" -> {
            if (rule.value == "ALL") return true
            val allowedCategories = rule.value.split(",").map { it.trim() }
            profile.socialCategory in allowedCategories
        }
        "gender" -> {
            if (rule.value == "ALL") return true
            profile.gender == rule.value
        }
        "occupation" -> {
            if (rule.value == "ALL") return true
            val allowedOccupations = rule.value.split(",").map { it.trim() }
            profile.occupation in allowedOccupations
        }
        else -> true  // unknown rule fields treated as met (fail-open for discoverability)
    }
}

fun compareValues(actual: Double, threshold: Double, operator: String): Boolean {
    return when (operator) {
        "lt"  -> actual < threshold
        "lte" -> actual <= threshold
        "gt"  -> actual > threshold
        "gte" -> actual >= threshold
        "eq"  -> actual == threshold
        else  -> false
    }
}

/**
 * Pure function variant of eligibility matching — useful for unit tests and
 * batch processing where rules are already loaded into memory.
 *
 * @param rulesMap map of schemeId → list of EligibilityRules (pre-fetched from DB)
 */
fun matchSchemes(
    profile: UserProfile,
    schemes: List<Scheme>,
    rulesMap: Map<String, List<EligibilityRule>>,
    nowMs: Long = System.currentTimeMillis()
): List<SchemeMatch> {
    return schemes.mapNotNull { scheme ->
        val rules = rulesMap[scheme.id] ?: emptyList()
        val isEligible = rules.all { ruleMatches(it, profile) }
        if (!isEligible) return@mapNotNull null

        val decayedScore = computeDecayedScore(scheme.confidenceScore, scheme.lastVerifiedEpoch, nowMs)
        SchemeMatch(
            scheme = scheme,
            isStale = decayedScore < STALE_THRESHOLD,
            effectiveScore = decayedScore
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The user's profile — built on-device via Opportunity Radar onboarding.
 * Never uploaded. Never stored server-side.
 */
data class UserProfile(
    val state: String = "ALL",
    val annualIncomeRupees: Long? = null,
    val landHectares: Double? = null,
    val age: Int? = null,
    val gender: String = "ALL",            // "M" | "F" | "O" | "ALL"
    val socialCategory: String = "ALL",    // "GEN" | "OBC" | "SC" | "ST" | "EWS"
    val occupation: String = "ALL",        // "farmer" | "student" | "worker" | "woman_shg" …
    val isDisabled: Boolean = false,
    val hasAadhaar: Boolean = true,
    val hasBankAccount: Boolean = true,
)

data class EligibleSchemeResult(
    val scheme: Scheme,
    val metCriteria: List<String>,
    val confidence: Float,
    val isStale: Boolean = false,          // true when decayed confidence < 0.6
)

data class SchemeMatch(
    val scheme: Scheme,
    val isStale: Boolean,
    val effectiveScore: Float = scheme.confidenceScore,
)