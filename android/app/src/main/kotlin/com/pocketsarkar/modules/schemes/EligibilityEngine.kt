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
     * Sorted by confidence score descending (most verified first).
     */
    suspend fun getEligibleSchemes(profile: UserProfile): List<EligibleSchemeResult> {
        val candidates = schemeDao.getSchemesByCategory(
            category = null,   // null = all categories
            state = profile.state
        )

        return candidates
            .mapNotNull { scheme -> evaluate(scheme, profile) }
            .sortedByDescending { it.scheme.confidenceScore }
    }

    /**
     * Evaluate a single scheme against the profile.
     * Returns null if ineligible.
     */
    suspend fun evaluate(scheme: Scheme, profile: UserProfile): EligibleSchemeResult? {
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
        return if (failedRules.isEmpty()) {
            EligibleSchemeResult(
                scheme = scheme,
                metCriteria = metRules,
                confidence = scheme.confidenceScore
            )
        } else {
            null
        }
    }

    private fun ruleMatches(rule: EligibilityRule, profile: UserProfile): Boolean {
        return when (rule.field) {
            "annual_income" -> {
                val income = profile.annualIncomeRupees ?: return true  // Unknown = assume eligible
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
            else -> true  // Unknown rule fields are treated as met (fail-open for discoverability)
        }
    }

    private fun compareValues(actual: Double, threshold: Double, operator: String): Boolean {
        return when (operator) {
            "lt"  -> actual < threshold
            "lte" -> actual <= threshold
            "gt"  -> actual > threshold
            "gte" -> actual >= threshold
            "eq"  -> actual == threshold
            else  -> true
        }
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
    val gender: String = "ALL",                // "M" | "F" | "O" | "ALL"
    val socialCategory: String = "ALL",        // "GEN" | "OBC" | "SC" | "ST" | "EWS"
    val occupation: String = "ALL",            // "farmer" | "student" | "worker" | "woman_shg" …
    val isDisabled: Boolean = false,
    val hasAadhaar: Boolean = true,
    val hasBankAccount: Boolean = true,
)

data class EligibleSchemeResult(
    val scheme: Scheme,
    val metCriteria: List<String>,
    val confidence: Float,
)
