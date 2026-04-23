package com.pocketsarkar.db

import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.Scheme
import com.pocketsarkar.modules.schemes.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure logic unit tests — no Room DB, no FTS, no Robolectric runner needed.
 *
 * Covers:
 * - flatFieldsMatch: income, age, caste
 * - ruleMatches: annual_income, age, gender, state, category
 * - matchSchemes: end-to-end pipeline with rulesMap
 * - computeDecayedScore: 0.01/week decay, floor at 0.0
 * - computeIsStale: threshold boundary
 */
class SchemeMatcherTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun scheme(
        id: String = "TEST_001",
        maxIncomeLPA: Double = 0.0,
        minAge: Int = 0,
        maxAge: Int = 0,
        casteEligibility: String = "[]",
        targetGender: String = "ALL",
        targetStates: String = "ALL",
        confidenceScore: Double = 1.0,
        lastVerifiedEpoch: Long = System.currentTimeMillis(),
    ) = Scheme(
        id = id,
        nameEn = "Test Scheme $id",
        nameHi = "Test Scheme $id",
        category = "general",
        ministryEn = "Test Ministry",
        descriptionEn = "Test description",
        descriptionHi = "Test description",
        benefitType = "cash",
        targetStates = targetStates,
        targetGender = targetGender,
        maxIncomeLPA = maxIncomeLPA,
        minAge = minAge,
        maxAge = maxAge,
        casteEligibility = casteEligibility,
        confidenceScore = confidenceScore,
        lastVerifiedEpoch = lastVerifiedEpoch,
    )

    private fun profile(
        incomeLPA: Double? = null,
        age: Int? = null,
        gender: String = "ALL",
        state: String = "ALL",
        caste: String = "ALL",
    ) = UserProfile(
        incomeLPA = incomeLPA,
        age = age,
        gender = gender,
        state = state,
        caste = caste,
    )

    private fun rule(
        schemeId: String = "TEST_001",
        field: String,
        operator: String,
        value: String,
    ) = EligibilityRule(
        schemeId = schemeId,
        field = field,
        operator = operator,
        value = value,
        labelEn = "$field $operator $value",
        labelHi = "$field $operator $value",
    )

    // ── flatFieldsMatch: income ───────────────────────────────────────────────

    @Test
    fun flatIncome_noLimit_alwaysPasses() {
        val s = scheme(maxIncomeLPA = 0.0)
        assertTrue(flatFieldsMatch(s, profile(incomeLPA = 99.0)))
    }

    @Test
    fun flatIncome_withinLimit_passes() {
        val s = scheme(maxIncomeLPA = 2.5)
        assertTrue(flatFieldsMatch(s, profile(incomeLPA = 2.0)))
    }

    @Test
    fun flatIncome_exactlyAtLimit_passes() {
        val s = scheme(maxIncomeLPA = 2.5)
        assertTrue(flatFieldsMatch(s, profile(incomeLPA = 2.5)))
    }

    @Test
    fun flatIncome_exceedsLimit_fails() {
        val s = scheme(maxIncomeLPA = 2.5)
        assertFalse(flatFieldsMatch(s, profile(incomeLPA = 3.0)))
    }

    @Test
    fun flatIncome_unknownProfile_passes() {
        val s = scheme(maxIncomeLPA = 2.5)
        assertTrue(flatFieldsMatch(s, profile(incomeLPA = null)))
    }

    // ── flatFieldsMatch: age ──────────────────────────────────────────────────

    @Test
    fun flatAge_noLimits_alwaysPasses() {
        val s = scheme(minAge = 0, maxAge = 0)
        assertTrue(flatFieldsMatch(s, profile(age = 99)))
    }

    @Test
    fun flatAge_withinRange_passes() {
        val s = scheme(minAge = 18, maxAge = 40)
        assertTrue(flatFieldsMatch(s, profile(age = 25)))
    }

    @Test
    fun flatAge_tooYoung_fails() {
        val s = scheme(minAge = 18, maxAge = 40)
        assertFalse(flatFieldsMatch(s, profile(age = 15)))
    }

    @Test
    fun flatAge_tooOld_fails() {
        val s = scheme(minAge = 18, maxAge = 40)
        assertFalse(flatFieldsMatch(s, profile(age = 45)))
    }

    @Test
    fun flatAge_exactlyAtBoundaries_passes() {
        val s = scheme(minAge = 18, maxAge = 40)
        assertTrue(flatFieldsMatch(s, profile(age = 18)))
        assertTrue(flatFieldsMatch(s, profile(age = 40)))
    }

    @Test
    fun flatAge_onlyMinSet_youngFails() {
        val s = scheme(minAge = 60, maxAge = 0)
        assertFalse(flatFieldsMatch(s, profile(age = 30)))
        assertTrue(flatFieldsMatch(s, profile(age = 65)))
    }

    // ── flatFieldsMatch: caste ────────────────────────────────────────────────

    @Test
    fun flatCaste_emptyArray_openToAll() {
        val s = scheme(casteEligibility = "[]")
        assertTrue(flatFieldsMatch(s, profile(caste = "GEN")))
        assertTrue(flatFieldsMatch(s, profile(caste = "SC")))
    }

    @Test
    fun flatCaste_restricted_matchingCastePasses() {
        val s = scheme(casteEligibility = """["SC","ST","OBC"]""")
        assertTrue(flatFieldsMatch(s, profile(caste = "SC")))
        assertTrue(flatFieldsMatch(s, profile(caste = "OBC")))
    }

    @Test
    fun flatCaste_restricted_nonMatchingCasteFails() {
        val s = scheme(casteEligibility = """["SC","ST"]""")
        assertFalse(flatFieldsMatch(s, profile(caste = "GEN")))
        assertFalse(flatFieldsMatch(s, profile(caste = "EWS")))
    }

    @Test
    fun flatCaste_userIsALL_passesRestrictedScheme() {
        // "ALL" in profile means unknown — treated as eligible (fail-open)
        val s = scheme(casteEligibility = """["SC","ST"]""")
        assertTrue(flatFieldsMatch(s, profile(caste = "ALL")))
    }

    // ── ruleMatches ───────────────────────────────────────────────────────────

    @Test
    fun rule_income_lte_qualifies() {
        val r = rule(field = "annual_income", operator = "lte", value = "200000")
        assertTrue(ruleMatches(r, profile(incomeLPA = 1.0)))   // 1 LPA = 1,00,000 rupees
    }

    @Test
    fun rule_income_lte_disqualifies() {
        val r = rule(field = "annual_income", operator = "lte", value = "200000")
        assertFalse(ruleMatches(r, profile(incomeLPA = 5.0)))  // 5 LPA = 5,00,000 rupees
    }

    @Test
    fun rule_age_range_qualifies() {
        val lower = rule(field = "age", operator = "gte", value = "18")
        val upper = rule(field = "age", operator = "lte", value = "35")
        val p = profile(age = 25)
        assertTrue(ruleMatches(lower, p))
        assertTrue(ruleMatches(upper, p))
    }

    @Test
    fun rule_age_range_disqualifies() {
        val upper = rule(field = "age", operator = "lte", value = "35")
        assertFalse(ruleMatches(upper, profile(age = 50)))
    }

    @Test
    fun rule_gender_matches() {
        val r = rule(field = "gender", operator = "eq", value = "F")
        assertTrue(ruleMatches(r, profile(gender = "F")))
        assertFalse(ruleMatches(r, profile(gender = "M")))
    }

    @Test
    fun rule_gender_ALL_alwaysMatches() {
        val r = rule(field = "gender", operator = "eq", value = "ALL")
        assertTrue(ruleMatches(r, profile(gender = "M")))
        assertTrue(ruleMatches(r, profile(gender = "F")))
    }

    @Test
    fun rule_state_matches() {
        val r = rule(field = "state", operator = "eq", value = "UP,MH")
        assertTrue(ruleMatches(r, profile(state = "UP")))
        assertTrue(ruleMatches(r, profile(state = "MH")))
        assertFalse(ruleMatches(r, profile(state = "KA")))
    }

    @Test
    fun rule_unknownField_failsOpen() {
        val r = rule(field = "unknown_future_field", operator = "eq", value = "x")
        assertTrue(ruleMatches(r, profile()))
    }

    // ── matchSchemes ──────────────────────────────────────────────────────────

    @Test
    fun matchSchemes_allEligible_returnsAll() {
        val schemes = listOf(
            scheme("S1"),
            scheme("S2"),
            scheme("S3"),
        )
        val results = matchSchemes(profile(), schemes, emptyMap())
        assertEquals(3, results.size)
    }

    @Test
    fun matchSchemes_incomeFilter_excludesOverLimit() {
        val schemes = listOf(
            scheme("LOW", maxIncomeLPA = 2.0),
            scheme("ANY", maxIncomeLPA = 0.0),
        )
        val results = matchSchemes(profile(incomeLPA = 5.0), schemes, emptyMap())
        assertEquals(1, results.size)
        assertEquals("ANY", results[0].scheme.id)
    }

    @Test
    fun matchSchemes_ruleFilter_excludesFailingRule() {
        val schemes = listOf(scheme("YOUTH"))
        val rulesMap = mapOf(
            "YOUTH" to listOf(rule("YOUTH", "age", "lte", "35"))
        )
        val oldProfile = profile(age = 50)
        val youngProfile = profile(age = 25)

        assertTrue(matchSchemes(youngProfile, schemes, rulesMap).isNotEmpty())
        assertTrue(matchSchemes(oldProfile, schemes, rulesMap).isEmpty())
    }

    @Test
    fun matchSchemes_staleFlag_setWhenDecayed() {
        val now = System.currentTimeMillis()
        val schemes = listOf(
            scheme("OLD", confidenceScore = 1.0, lastVerifiedEpoch = now - (50 * MS_PER_WEEK)),
            scheme("NEW", confidenceScore = 1.0, lastVerifiedEpoch = now),
        )
        val results = matchSchemes(profile(), schemes, emptyMap(), nowMs = now)
        assertTrue("OLD scheme should be stale",  results.first { it.scheme.id == "OLD" }.isStale)
        assertFalse("NEW scheme should not be stale", results.first { it.scheme.id == "NEW" }.isStale)
    }

    // ── computeDecayedScore ───────────────────────────────────────────────────

    @Test
    fun decay_oneWeek_reducesBy001() {
        val now = System.currentTimeMillis()
        val score = computeDecayedScore(1.0, now - MS_PER_WEEK, now)
        assertEquals(0.99, score, 0.001)
    }

    @Test
    fun decay_40weeks_exactlyAtThreshold() {
        val now = System.currentTimeMillis()
        val score = computeDecayedScore(1.0, now - (40 * MS_PER_WEEK), now)
        // 1.0 - (0.01 * 40) = 0.60 — at threshold, NOT stale
        assertEquals(0.60, score, 0.001)
        assertFalse(computeIsStale(1.0, now - (40 * MS_PER_WEEK), now))
    }

    @Test
    fun decay_50weeks_isStale() {
        val now = System.currentTimeMillis()
        assertTrue(computeIsStale(1.0, now - (50 * MS_PER_WEEK), now))
    }

    @Test
    fun decay_flooredAtZero() {
        val now = System.currentTimeMillis()
        val score = computeDecayedScore(1.0, now - (200 * MS_PER_WEEK), now)
        assertEquals(0.0, score, 0.001)
    }

    @Test
    fun decay_freshScheme_noChange() {
        val now = System.currentTimeMillis()
        val score = computeDecayedScore(1.0, now, now)
        assertEquals(1.0, score, 0.001)
    }

    @Test
    fun decay_futureVerifiedDate_noChange() {
        val now = System.currentTimeMillis()
        // lastVerifiedEpoch in the future — ageMs is negative, should return original
        val score = computeDecayedScore(0.8, now + MS_PER_WEEK, now)
        assertEquals(0.8, score, 0.001)
    }
}