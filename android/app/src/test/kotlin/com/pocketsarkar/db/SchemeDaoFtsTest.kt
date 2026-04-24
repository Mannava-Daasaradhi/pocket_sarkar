package com.pocketsarkar.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.Scheme
import com.pocketsarkar.modules.schemes.computeDecayedScore
import com.pocketsarkar.modules.schemes.STALE_THRESHOLD
import com.pocketsarkar.modules.schemes.MS_PER_WEEK
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/**
 * Phase-2 DAO tests — Robolectric with sqliteMode=NATIVE (robolectric.properties).
 * Robolectric 4.13 ships SQLite 3.44+ with FTS5 on all platforms including Windows.
 *
 * Covers:
 * - FTS5 search: "PM Kisan" → ≥1 result
 * - FTS5 search: "kisan"    → ≥1 result
 * - FTS5 search: "zzznomatch" → empty
 * - getByCategory
 * - getEligibleSchemes SQL filter (income, age, gender, state)
 * - getSchemeById
 * - getStaleSchemes
 * - confidenceScore decay: 0.01 per week, floor 0.0
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchemeDaoFtsTest {

    private lateinit var db: PocketSarkarDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PocketSarkarDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(PocketSarkarDatabase.TEST_CREATE_CALLBACK)  // ← was ON_CREATE_CALLBACK
            .build()
    }

    @After
    fun closeDb() = db.close()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun scheme(
        id: String,
        nameEn: String,
        nameHi: String = nameEn,
        category: String = "general",
        state: String = "ALL",
        gender: String = "ALL",
        confidenceScore: Double = 1.0,           // Double — matches Scheme entity
        lastVerifiedEpoch: Long = System.currentTimeMillis(),
        descriptionEn: String = "Description for $nameEn",
    ) = Scheme(
        id = id,
        nameEn = nameEn,
        nameHi = nameHi,
        category = category,
        ministryEn = "Test Ministry",
        descriptionEn = descriptionEn,
        descriptionHi = descriptionEn,
        benefitType = "cash",
        targetStates = state,
        targetGender = gender,
        targetCategory = "ALL",
        confidenceScore = confidenceScore,
        lastVerifiedEpoch = lastVerifiedEpoch,
    )

    private fun insertAndRebuildFts(schemes: List<Scheme>) = runBlocking {
        db.schemeDao().insertSchemes(schemes)
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO schemes_fts(schemes_fts) VALUES('rebuild')"
        )
    }

    // ── FTS Tests ─────────────────────────────────────────────────────────────

    @Test
    fun fts_pmKisan_returnsAtLeastOneResult() = runBlocking {
        insertAndRebuildFts(listOf(
            scheme("PM_KISAN_001", "PM Kisan Samman Nidhi",
                descriptionEn = "Direct income support for farmers up to 2 hectares"),
            scheme("PM_AWAS_002", "PM Awas Yojana",
                descriptionEn = "Housing scheme for rural poor families"),
            scheme("AYUSHMAN_003", "Ayushman Bharat PM-JAY",
                descriptionEn = "Health insurance for low income families"),
            scheme("MGNREGA_004", "MGNREGS",
                descriptionEn = "Rural employment guarantee for 100 days"),
            scheme("UJJWALA_005", "PM Ujjwala Yojana",
                descriptionEn = "Free LPG connection for BPL women"),
        ))
        val results = db.schemeDao().searchByFTS("PM Kisan")
        assertTrue("FTS 'PM Kisan' must return >= 1 result (got ${results.size})", results.isNotEmpty())
        assertTrue("FTS result must contain PM Kisan scheme", results.any { it.id == "PM_KISAN_001" })
    }

    @Test
    fun fts_kisan_returnsAtLeastOneResult() = runBlocking {
        insertAndRebuildFts(listOf(
            scheme("PM_KISAN_001", "PM Kisan Samman Nidhi",
                descriptionEn = "Direct income support of 6000 per year for farmers"),
            scheme("AYUSHMAN_003", "Ayushman Bharat PM-JAY",
                descriptionEn = "Health coverage for poor families"),
        ))
        val results = db.schemeDao().searchByFTS("kisan")
        assertTrue("FTS 'kisan' must return >= 1 result (got ${results.size})", results.isNotEmpty())
    }

    @Test
    fun fts_noMatch_returnsEmptyList() = runBlocking {
        insertAndRebuildFts(listOf(
            scheme("PM_KISAN_001", "PM Kisan Samman Nidhi",
                descriptionEn = "Direct income support for farmers"),
            scheme("PM_AWAS_002", "PM Awas Yojana",
                descriptionEn = "Housing scheme for rural families"),
        ))
        val results = db.schemeDao().searchByFTS("zzznomatch")
        assertTrue("FTS 'zzznomatch' must return empty list", results.isEmpty())
    }

    // ── getSchemeById ─────────────────────────────────────────────────────────

    @Test
    fun getSchemeById_found() = runBlocking {
        db.schemeDao().insertSchemes(listOf(scheme("PM_KISAN_001", "PM Kisan Samman Nidhi")))
        val result = db.schemeDao().getSchemeById("PM_KISAN_001")
        assertNotNull("Should find scheme by ID", result)
        assertEquals("PM_KISAN_001", result!!.id)
    }

    @Test
    fun getSchemeById_notFound_returnsNull() = runBlocking {
        val result = db.schemeDao().getSchemeById("NONEXISTENT")
        assertNull("Missing ID should return null", result)
    }

    // ── getByCategory ─────────────────────────────────────────────────────────

    @Test
    fun getByCategory_filtersCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("AG1", "PM Kisan",      category = "agriculture"),
            scheme("HE1", "Ayushman",      category = "health"),
            scheme("AG2", "PM Fasal Bima", category = "agriculture"),
        ))
        val results = db.schemeDao().getByCategory("agriculture")
        assertEquals("Should return exactly 2 agriculture schemes", 2, results.size)
        assertTrue(results.all { it.category == "agriculture" })
    }

    // ── getEligibleSchemes ────────────────────────────────────────────────────

    @Test
    fun getEligibleSchemes_filtersStateCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("CENTRAL", "Central Scheme", state = "ALL"),
            scheme("UP_001",  "UP Scheme",      state = "UP"),
            scheme("MH_001",  "MH Scheme",      state = "MH"),
        ))
        val results = db.schemeDao().getEligibleSchemes(state = "UP", incomeLPA = 5.0, age = 30, gender = "ALL")
        assertTrue("Central scheme included for UP user", results.any { it.id == "CENTRAL" })
        assertTrue("UP scheme included for UP user",      results.any { it.id == "UP_001" })
        assertFalse("MH scheme excluded for UP user",     results.any { it.id == "MH_001" })
    }

    @Test
    fun getEligibleSchemes_filtersGenderCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("UNIVERSAL", "Universal Scheme", gender = "ALL"),
            scheme("WOMEN_001", "Women Scheme",     gender = "F"),
            scheme("MEN_001",   "Men Scheme",       gender = "M"),
        ))
        val results = db.schemeDao().getEligibleSchemes(state = "ALL", incomeLPA = 5.0, age = 30, gender = "F")
        assertTrue("Universal included for F",  results.any { it.id == "UNIVERSAL" })
        assertTrue("Women-only included for F", results.any { it.id == "WOMEN_001" })
        assertFalse("Men-only excluded for F",  results.any { it.id == "MEN_001" })
    }

    @Test
    fun getEligibleSchemes_filtersIncomeCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("LOW_INCOME", "Low Income Scheme"),
            scheme("ANY_INCOME", "Any Income Scheme"),
        ))
        db.schemeDao().insertRules(listOf(
            EligibilityRule(
                schemeId = "LOW_INCOME", field = "annual_income",
                operator = "lte", value = "200000",
                labelEn = "Income up to 2 LPA", labelHi = "2 LPA तक आय"
            )
        ))
        val qualify = db.schemeDao().getEligibleSchemes(state = "ALL", incomeLPA = 1.0, age = 30, gender = "ALL")
        assertTrue("1 LPA qualifies for low-income scheme", qualify.any { it.id == "LOW_INCOME" })

        val disqualify = db.schemeDao().getEligibleSchemes(state = "ALL", incomeLPA = 5.0, age = 30, gender = "ALL")
        assertFalse("5 LPA does NOT qualify for 2 LPA scheme", disqualify.any { it.id == "LOW_INCOME" })
        assertTrue("5 LPA still sees any-income scheme",       disqualify.any { it.id == "ANY_INCOME" })
    }

    @Test
    fun getEligibleSchemes_filtersAgeCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(scheme("YOUTH_SCHEME", "Youth Scheme")))
        db.schemeDao().insertRules(listOf(
            EligibilityRule(schemeId = "YOUTH_SCHEME", field = "age", operator = "lte", value = "35",
                labelEn = "Age 35 or below", labelHi = "35 वर्ष तक"),
            EligibilityRule(schemeId = "YOUTH_SCHEME", field = "age", operator = "gte", value = "18",
                labelEn = "Age 18 or above", labelHi = "18 वर्ष या अधिक")
        ))
        assertTrue("Age 25 qualifies (18–35)",
            db.schemeDao().getEligibleSchemes("ALL", 5.0, 25, "ALL").any { it.id == "YOUTH_SCHEME" })
        assertFalse("Age 50 too old",
            db.schemeDao().getEligibleSchemes("ALL", 5.0, 50, "ALL").any { it.id == "YOUTH_SCHEME" })
        assertFalse("Age 15 too young",
            db.schemeDao().getEligibleSchemes("ALL", 5.0, 15, "ALL").any { it.id == "YOUTH_SCHEME" })
    }

    // ── getStaleSchemes ───────────────────────────────────────────────────────

    @Test
    fun getStaleSchemes_returnsOnlyBelowThreshold() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("FRESH",  "Fresh Scheme",   confidenceScore = 1.0),
            scheme("STALE1", "Stale Scheme 1", confidenceScore = 0.4),
            scheme("STALE2", "Stale Scheme 2", confidenceScore = 0.59),
            scheme("BORDER", "Border Scheme",  confidenceScore = 0.6),
        ))
        val stale = db.schemeDao().getStaleSchemes(threshold = 0.6)
        assertEquals("Exactly 2 stale schemes", 2, stale.size)
        assertTrue(stale.all { it.confidenceScore < 0.6 })   // Double comparison
        assertTrue(stale.any { it.id == "STALE1" })
        assertTrue(stale.any { it.id == "STALE2" })
        assertFalse("0.6 exactly is NOT stale", stale.any { it.id == "BORDER" })
    }

    // ── Confidence score decay (pure logic — no DB) ───────────────────────────

    @Test
    fun confidenceDecay_1percentPerWeek() {
        val now = System.currentTimeMillis()
        val decayed = computeDecayedScore(1.0, now - MS_PER_WEEK, now)
        assertEquals("Decay 0.01 after 1 week", 0.99, decayed, 0.001)
    }

    @Test
    fun confidenceDecay_flooredAtZero() {
        val now = System.currentTimeMillis()
        val decayed = computeDecayedScore(1.0, now - (200 * MS_PER_WEEK), now)
        assertEquals("Floor at 0.0", 0.0, decayed, 0.001)
    }

    @Test
    fun confidenceDecay_freshSchemeIsNotStale() {
        val now = System.currentTimeMillis()
        val decayed = computeDecayedScore(1.0, now, now)
        assertTrue("Just-verified scheme not stale (score=$decayed)", decayed >= STALE_THRESHOLD)
    }

    @Test
    fun confidenceDecay_after50WeeksIsStale() {
        val now = System.currentTimeMillis()
        val decayed = computeDecayedScore(1.0, now - (50 * MS_PER_WEEK), now)
        assertTrue("After 50 weeks score=$decayed < $STALE_THRESHOLD", decayed < STALE_THRESHOLD)
    }

    @Test
    fun confidenceDecay_exactlyAtThresholdAfter40Weeks() {
        val now = System.currentTimeMillis()
        val decayed = computeDecayedScore(1.0, now - (40 * MS_PER_WEEK), now)
        // 1.0 - (0.01 * 40) = 0.60 — at threshold, NOT stale
        assertTrue("At 40 weeks score=$decayed should be >= $STALE_THRESHOLD", decayed >= STALE_THRESHOLD)
    }
}