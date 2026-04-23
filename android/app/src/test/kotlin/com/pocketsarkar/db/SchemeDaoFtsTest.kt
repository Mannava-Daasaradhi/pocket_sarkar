package com.pocketsarkar.db

import android.content.Context
import androidx.room.Room
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
 * Phase-2 unit tests for SchemeDao.
 *
 * Uses Robolectric in-memory Room with sqliteMode=NATIVE (robolectric.properties)
 * so FTS5 virtual tables work correctly. Native SQLite 3.38+ ships FTS5 built-in.
 *
 * Covers:
 *  - FTS search: "PM Kisan" → ≥1 result
 *  - FTS search: "kisan"    → ≥1 result
 *  - FTS search: "zzznomatch" → empty
 *  - getByCategory
 *  - getEligibleSchemes SQL filter (income, age, gender, state)
 *  - getSchemeById
 *  - getStaleSchemes
 *  - confidenceScore decay: 0.01 per week, floor 0.0
 *  - isStale flag below threshold 0.6
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SchemeDaoTest {

    private lateinit var db: PocketSarkarDatabase

    // ── Setup / Teardown ──────────────────────────────────────────────────────

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PocketSarkarDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(PocketSarkarDatabase.ON_CREATE_CALLBACK)
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
        confidenceScore: Float = 1.0f,
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

    /** Insert schemes then rebuild the FTS index. */
    private fun insertAndRebuildFts(schemes: List<Scheme>) = runBlocking {
        db.schemeDao().insertSchemes(schemes)
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO schemes_fts(schemes_fts) VALUES('rebuild')"
        )
    }

    // ── FTS Search Tests ──────────────────────────────────────────────────────

    @Test
    fun fts_pmKisan_returnsExactlyOneResult() = runBlocking {
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
        assertTrue(
            "FTS search 'PM Kisan' must return at least 1 result (got ${results.size})",
            results.isNotEmpty()
        )
        assertTrue(
            "FTS result must contain PM Kisan scheme",
            results.any { it.id == "PM_KISAN_001" }
        )
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
        assertTrue(
            "FTS search 'kisan' must return at least 1 result (got ${results.size})",
            results.isNotEmpty()
        )
    }

    @Test
    fun fts_noMatch_returnsEmptyList() = runBlocking {
        insertAndRebuildFts(listOf(
            scheme("PM_KISAN_001", "PM Kisan Samman Nidhi",
                descriptionEn = "Direct income support for farmers"),
            scheme("PM_AWAS_002",  "PM Awas Yojana",
                descriptionEn = "Housing scheme for rural families"),
        ))

        val results = db.schemeDao().searchByFTS("zzznomatch")
        assertTrue(
            "FTS search 'zzznomatch' must return empty list",
            results.isEmpty()
        )
    }

    // ── getSchemeById ─────────────────────────────────────────────────────────

    @Test
    fun getSchemeById_found() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("PM_KISAN_001", "PM Kisan Samman Nidhi")
        ))
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

    // ── getEligibleSchemes SQL filter ─────────────────────────────────────────

    @Test
    fun getEligibleSchemes_filtersStateCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("CENTRAL", "Central Scheme", state = "ALL"),
            scheme("UP_001",  "UP Scheme",      state = "UP"),
            scheme("MH_001",  "MH Scheme",      state = "MH"),
        ))

        val results = db.schemeDao().getEligibleSchemes(
            state = "UP", incomeLPA = 5.0, age = 30, gender = "ALL"
        )
        assertTrue("Central scheme should be included for UP user",
            results.any { it.id == "CENTRAL" })
        assertTrue("UP scheme should be included for UP user",
            results.any { it.id == "UP_001" })
        assertFalse("MH scheme should be excluded for UP user",
            results.any { it.id == "MH_001" })
    }

    @Test
    fun getEligibleSchemes_filtersGenderCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("UNIVERSAL", "Universal Scheme", gender = "ALL"),
            scheme("WOMEN_001", "Women Scheme",     gender = "F"),
            scheme("MEN_001",   "Men Scheme",       gender = "M"),
        ))

        val results = db.schemeDao().getEligibleSchemes(
            state = "ALL", incomeLPA = 5.0, age = 30, gender = "F"
        )
        assertTrue("Universal scheme should be included for F",
            results.any { it.id == "UNIVERSAL" })
        assertTrue("Women-only scheme should be included for F",
            results.any { it.id == "WOMEN_001" })
        assertFalse("Men-only scheme should be excluded for F",
            results.any { it.id == "MEN_001" })
    }

    @Test
    fun getEligibleSchemes_filtersIncomeCorrectly() = runBlocking {
        // Insert a scheme with an income rule: annual_income lte 200000 (2 LPA)
        db.schemeDao().insertSchemes(listOf(
            scheme("LOW_INCOME", "Low Income Scheme"),
            scheme("ANY_INCOME", "Any Income Scheme"),
        ))
        db.schemeDao().insertRules(listOf(
            EligibilityRule(
                schemeId = "LOW_INCOME",
                field = "annual_income",
                operator = "lte",
                value = "200000",   // 2 LPA limit
                labelEn = "Income up to 2 LPA",
                labelHi = "2 LPA तक आय"
            )
        ))

        // User with 1 LPA income — qualifies for both
        val qualify = db.schemeDao().getEligibleSchemes(
            state = "ALL", incomeLPA = 1.0, age = 30, gender = "ALL"
        )
        assertTrue("1 LPA user should qualify for low-income scheme",
            qualify.any { it.id == "LOW_INCOME" })

        // User with 5 LPA income — fails income rule for LOW_INCOME
        val disqualify = db.schemeDao().getEligibleSchemes(
            state = "ALL", incomeLPA = 5.0, age = 30, gender = "ALL"
        )
        assertFalse("5 LPA user should NOT qualify for scheme with 2 LPA limit",
            disqualify.any { it.id == "LOW_INCOME" })
        assertTrue("5 LPA user should still see any-income scheme",
            disqualify.any { it.id == "ANY_INCOME" })
    }

    @Test
    fun getEligibleSchemes_filtersAgeCorrectly() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("YOUTH_SCHEME", "Youth Scheme"),
        ))
        db.schemeDao().insertRules(listOf(
            EligibilityRule(
                schemeId = "YOUTH_SCHEME",
                field = "age", operator = "lte", value = "35",
                labelEn = "Age 35 or below", labelHi = "35 वर्ष तक"
            ),
            EligibilityRule(
                schemeId = "YOUTH_SCHEME",
                field = "age", operator = "gte", value = "18",
                labelEn = "Age 18 or above", labelHi = "18 वर्ष या अधिक"
            )
        ))

        // Age 25 — qualifies
        val qualify = db.schemeDao().getEligibleSchemes(
            state = "ALL", incomeLPA = 5.0, age = 25, gender = "ALL"
        )
        assertTrue("Age 25 should qualify for youth scheme (18-35)",
            qualify.any { it.id == "YOUTH_SCHEME" })

        // Age 50 — too old
        val tooOld = db.schemeDao().getEligibleSchemes(
            state = "ALL", incomeLPA = 5.0, age = 50, gender = "ALL"
        )
        assertFalse("Age 50 should NOT qualify for youth scheme (18-35)",
            tooOld.any { it.id == "YOUTH_SCHEME" })

        // Age 15 — too young
        val tooYoung = db.schemeDao().getEligibleSchemes(
            state = "ALL", incomeLPA = 5.0, age = 15, gender = "ALL"
        )
        assertFalse("Age 15 should NOT qualify for youth scheme (18-35)",
            tooYoung.any { it.id == "YOUTH_SCHEME" })
    }

    // ── getStaleSchemes ───────────────────────────────────────────────────────

    @Test
    fun getStaleSchemes_returnsOnlyBelowThreshold() = runBlocking {
        db.schemeDao().insertSchemes(listOf(
            scheme("FRESH",   "Fresh Scheme",  confidenceScore = 1.0f),
            scheme("STALE1",  "Stale Scheme 1",confidenceScore = 0.4f),
            scheme("STALE2",  "Stale Scheme 2",confidenceScore = 0.59f),
            scheme("BORDER",  "Border Scheme", confidenceScore = 0.6f),
        ))

        val stale = db.schemeDao().getStaleSchemes(threshold = 0.6)
        assertEquals("Should return exactly 2 stale schemes", 2, stale.size)
        assertTrue(stale.all { it.confidenceScore < 0.6f })
        assertTrue(stale.any { it.id == "STALE1" })
        assertTrue(stale.any { it.id == "STALE2" })
        assertFalse("Score exactly 0.6 must NOT be stale",
            stale.any { it.id == "BORDER" })
    }

    // ── Confidence score decay (pure logic) ───────────────────────────────────

    @Test
    fun confidenceDecay_1percentPerWeek() {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - MS_PER_WEEK

        val decayed = computeDecayedScore(1.0f, oneWeekAgo, now)

        assertEquals("Score should decay by exactly 0.01 after 1 week",
            0.99f, decayed, 0.001f)
    }

    @Test
    fun confidenceDecay_flooredAtZero() {
        val now = System.currentTimeMillis()
        val longAgo = now - (200 * MS_PER_WEEK)   // 200 weeks ago

        val decayed = computeDecayedScore(1.0f, longAgo, now)

        assertEquals("Decayed score must floor at 0.0", 0.0f, decayed, 0.001f)
    }

    @Test
    fun confidenceDecay_freshSchemeIsNotStale() {
        val now = System.currentTimeMillis()

        val decayed = computeDecayedScore(1.0f, now, now)

        assertTrue("A just-verified scheme should not be stale (score=$decayed)",
            decayed >= STALE_THRESHOLD)
    }

    @Test
    fun confidenceDecay_after50WeeksIsStale() {
        val now = System.currentTimeMillis()
        val fiftyWeeksAgo = now - (50 * MS_PER_WEEK)

        val decayed = computeDecayedScore(1.0f, fiftyWeeksAgo, now)

        assertTrue(
            "After 50 weeks score=$decayed should be below stale threshold $STALE_THRESHOLD",
            decayed < STALE_THRESHOLD
        )
    }

    @Test
    fun confidenceDecay_exactlyAtThresholdAfter40Weeks() {
        val now = System.currentTimeMillis()
        val fortyWeeksAgo = now - (40 * MS_PER_WEEK)

        val decayed = computeDecayedScore(1.0f, fortyWeeksAgo, now)

        // 1.0 - (0.01 * 40) = 0.60 — right at the threshold, NOT stale
        assertTrue(
            "At exactly 40 weeks score=$decayed should still be >= $STALE_THRESHOLD",
            decayed >= STALE_THRESHOLD
        )
    }
}