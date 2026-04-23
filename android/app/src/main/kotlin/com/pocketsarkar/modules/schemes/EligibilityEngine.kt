package com.pocketsarkar.modules.schemes

import com.pocketsarkar.db.dao.SchemeDao
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.Scheme
import org.json.JSONArray
import javax.inject.Inject

/**
 * Client-side eligibility rule engine.
 *
 * Evaluates a UserProfile against both:
 *   1. Flat scheme columns (maxIncomeLPA, minAge, maxAge, casteEligibility) — Phase-2 spec
 *   2. EligibilityRule rows in SQLite — complex/multi-value criteria
 *
 * Runs 100% on-device. No profile data ever leaves the phone.
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
            category = null,
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
     *
     * Layer 1: flat column checks (cheap, no extra query)
     * Layer 2: EligibilityRule table checks
     */
    suspend fun evaluate(
        scheme: Scheme,
        profile: UserProfile,
        nowMs: Long = System.currentTimeMillis()
    ): EligibleSchemeResult? {

        // ── Layer 1: flat field pre-filter ────────────────────────────────────
        if (!flatFieldsMatch(scheme, profile)) return null

        // ── Layer 2: rule table ───────────────────────────────────────────────
        val rules = schemeDao.getRulesForScheme(scheme.id)
        val failedRules = mutableListOf<String>()
        val metRules = mutableListOf<String>()

        for (rule in rules) {
            if (ruleMatches(rule, profile)) metRules.add(rule.labelEn)
            else failedRules.add(rule.labelEn)
        }

        if (failedRules.isNotEmpty()) return null

        val decayedScore = computeDecayedScore(scheme.confidenceScore, scheme.lastVerifiedEpoch, nowMs)
        return EligibleSchemeResult(
            scheme      = scheme,
            metCriteria = metRules,
            confidence  = decayedScore,
            isStale     = decayedScore < STALE_THRESHOLD
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Constants — Double throughout (Scheme.confidenceScore is Double)
// ─────────────────────────────────────────────────────────────────────────────

const val STALE_THRESHOLD = 0.6
const val DECAY_PER_WEEK  = 0.01
const val MS_PER_WEEK     = 7L * 24 * 60 * 60 * 1000

// ─────────────────────────────────────────────────────────────────────────────
// Flat field pre-filter (Phase-2 spec columns)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Checks the five flat eligibility columns added in Phase 2.
 * Returns false immediately if any hard criterion is unmet.
 *
 * This runs before the rules-table query — it's cheaper and eliminates
 * the majority of non-qualifying schemes without any extra DB round trip.
 */
fun flatFieldsMatch(scheme: Scheme, profile: UserProfile): Boolean {

    // Income: 0.0 on scheme = no limit
    val incomeLPA = profile.incomeLPA
        ?: (profile.annualIncomeRupees?.toDouble()?.div(100_000.0))
    if (incomeLPA != null && scheme.maxIncomeLPA > 0.0 && incomeLPA > scheme.maxIncomeLPA) {
        return false
    }

    // Age lower bound: 0 on scheme = no bound
    val age = profile.age ?: profile.ageNullable
    if (age != null) {
        if (scheme.minAge > 0 && age < scheme.minAge) return false
        if (scheme.maxAge > 0 && age > scheme.maxAge) return false
    }

    // Caste: empty array on scheme = open to all
    val casteList = parseCasteArray(scheme.casteEligibility)
    if (casteList.isNotEmpty()) {
        val userCaste = profile.caste.ifBlank { profile.socialCategory }
        if (userCaste != "ALL" && userCaste !in casteList) return false
    }

    return true
}

/** Parse JSON caste array safely. Returns empty list on any parse error. */
fun parseCasteArray(json: String): List<String> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Confidence score decay
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reduces confidence by 0.01 per week since lastVerifiedEpoch. Floor 0.0.
 */
fun computeDecayedScore(
    confidenceScore: Double,
    lastVerifiedEpoch: Long,
    nowMs: Long = System.currentTimeMillis()
): Double {
    val ageMs = nowMs - lastVerifiedEpoch
    if (ageMs <= 0L) return confidenceScore
    val weeksElapsed = ageMs / MS_PER_WEEK
    return maxOf(0.0, confidenceScore - (DECAY_PER_WEEK * weeksElapsed))
}

fun computeIsStale(
    confidenceScore: Double,
    lastVerifiedEpoch: Long,
    nowMs: Long = System.currentTimeMillis()
): Boolean = computeDecayedScore(confidenceScore, lastVerifiedEpoch, nowMs) < STALE_THRESHOLD

// ─────────────────────────────────────────────────────────────────────────────
// Rule table evaluation
// ─────────────────────────────────────────────────────────────────────────────

fun ruleMatches(rule: EligibilityRule, profile: UserProfile): Boolean {
    return when (rule.field) {
        "annual_income" -> {
            // Accept rupees from rule, convert profile income to rupees for comparison
            val incomeRupees = profile.annualIncomeRupees
                ?: profile.incomeLPA?.let { (it * 100_000).toLong() }
                ?: return true
            val threshold = rule.value.toLongOrNull() ?: return true
            compareValues(incomeRupees.toDouble(), threshold.toDouble(), rule.operator)
        }
        "land_hectares" -> {
            val land = profile.landHectares ?: return true
            val threshold = rule.value.toDoubleOrNull() ?: return true
            compareValues(land, threshold, rule.operator)
        }
        "age" -> {
            val age = profile.age ?: profile.ageNullable ?: return true
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
            val userCaste = profile.caste.ifBlank { profile.socialCategory }
            userCaste in allowedCategories
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
        else -> true  // unknown fields: fail-open for discoverability
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
 * Pure function variant — useful for batch processing where rules are
 * already loaded into memory. Also used by unit tests.
 */
fun matchSchemes(
    profile: UserProfile,
    schemes: List<Scheme>,
    rulesMap: Map<String, List<EligibilityRule>>,
    nowMs: Long = System.currentTimeMillis()
): List<SchemeMatch> {
    return schemes.mapNotNull { scheme ->
        if (!flatFieldsMatch(scheme, profile)) return@mapNotNull null
        val rules = rulesMap[scheme.id] ?: emptyList()
        if (!rules.all { ruleMatches(it, profile) }) return@mapNotNull null
        val decayedScore = computeDecayedScore(scheme.confidenceScore, scheme.lastVerifiedEpoch, nowMs)
        SchemeMatch(
            scheme        = scheme,
            isStale       = decayedScore < STALE_THRESHOLD,
            effectiveScore = decayedScore
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * User's profile — built on-device, never uploaded, never stored server-side.
 *
 * Spec fields (incomeLPA, caste, isFarmer, hasDisability, educationLevel) are
 * primary. Legacy fields (annualIncomeRupees, socialCategory, ageNullable) are
 * kept for backward compatibility with existing callers.
 */
data class UserProfile(
    val state: String = "ALL",

    // ── Spec fields (Phase 2) ─────────────────────────────────────────────
    /** Income in Lakhs Per Annum. Null = unknown (treated as eligible). */
    val incomeLPA: Double? = null,
    /** Age in years. Use this in preference to ageNullable. */
    val age: Int? = null,
    val gender: String = "ALL",             // "M" | "F" | "O" | "ALL"
    /** Caste/social group: "GEN" | "OBC" | "SC" | "ST" | "EWS" | "ALL" */
    val caste: String = "ALL",
    val isFarmer: Boolean = false,
    val hasDisability: Boolean = false,
    val educationLevel: String = "NONE",    // "NONE"|"PRIMARY"|"SECONDARY"|"GRADUATE"|"POSTGRADUATE"

    // ── Legacy fields (kept for backward compat) ──────────────────────────
    /** Annual income in rupees. Prefer incomeLPA. */
    val annualIncomeRupees: Long? = null,
    /** Nullable age alias. Prefer age. */
    val ageNullable: Int? = null,
    /** Social category alias. Prefer caste. */
    val socialCategory: String = "ALL",
    val occupation: String = "ALL",
    val landHectares: Double? = null,
    val isDisabled: Boolean = false,
    val hasAadhaar: Boolean = true,
    val hasBankAccount: Boolean = true,
)

data class EligibleSchemeResult(
    val scheme: Scheme,
    val metCriteria: List<String>,
    val confidence: Double,
    val isStale: Boolean = false,
)

data class SchemeMatch(
    val scheme: Scheme,
    val isStale: Boolean,
    val effectiveScore: Double = scheme.confidenceScore,
)