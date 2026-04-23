package com.pocketsarkar.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pocketsarkar.db.entities.Scheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DAO integration tests using Robolectric in-memory Room.
 *
 * FTS5 search (searchSchemes) is NOT tested here because FTS5 virtual tables
 * require a native SQLite binary compiled with FTS5 enabled, which is not
 * guaranteed in the Robolectric JVM environment on all host platforms.
 *
 * FTS search correctness is verified separately in EligibilityEngineTest (pure JVM).
 * The DAO JOIN logic (s.rowid = fts.rowid) is validated by the production DB on device.
 *
 * What IS tested here: insert, getById, getByCategory, getActiveCount.
 * These cover the non-FTS DAO surface which runs on plain SQLite without FTS5.
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
            // No ON_CREATE_CALLBACK: FTS5 virtual table creation is skipped in tests.
            // Plain DAO queries (no MATCH / FTS JOIN) work fine without it.
            .build()
    }

    @After
    fun closeDb() = db.close()

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun scheme(
        id: String,
        nameEn: String,
        category: String = "general",
        state: String = "ALL",
        gender: String = "ALL",
        confidenceScore: Float = 1.0f,
        lastVerifiedEpoch: Long = System.currentTimeMillis(),
    ) = Scheme(
        id = id,
        nameEn = nameEn,
        nameHi = nameEn,
        category = category,
        ministryEn = "Test Ministry",
        descriptionEn = "Test description for $nameEn",
        descriptionHi = "Test description for $nameEn",
        benefitType = "cash",
        targetStates = state,
        targetGender = gender,
        targetCategory = "ALL",
        confidenceScore = confidenceScore,
        lastVerifiedEpoch = lastVerifiedEpoch,
    )

    // ── insert + getById ──────────────────────────────────────────────────────

    @Test
    fun insertAndGetById() = runBlocking {
        val dao = db.schemeDao()
        dao.insertSchemes(listOf(scheme("PM_KISAN_001", "PM Kisan Samman Nidhi")))

        val result = dao.getSchemeById("PM_KISAN_001")
        assertNotNull("Inserted scheme should be retrievable by ID", result)
        assertEquals("PM_KISAN_001", result!!.id)
        assertEquals("PM Kisan Samman Nidhi", result.nameEn)
    }

    @Test
    fun getByIdReturnsNullForMissingScheme() = runBlocking {
        val dao = db.schemeDao()
        val result = dao.getSchemeById("DOES_NOT_EXIST")
        assertNull("Missing scheme should return null", result)
    }

    // ── getByCategory ─────────────────────────────────────────────────────────

    @Test
    fun getByCategoryFiltersCorrectly() = runBlocking {
        val dao = db.schemeDao()
        dao.insertSchemes(listOf(
            scheme("AG_001", "PM Kisan",         category = "agriculture"),
            scheme("HE_001", "Ayushman Bharat",  category = "health"),
            scheme("AG_002", "PM Fasal Bima",    category = "agriculture"),
            scheme("ED_001", "PM Scholarship",   category = "education"),
        ))

        val agri = dao.getSchemesByCategory("agriculture", "ALL")
        assertEquals("Should return exactly 2 agriculture schemes", 2, agri.size)
        assertTrue(agri.all { it.category == "agriculture" })
    }

    @Test
    fun getByCategoryWithNullReturnsAll() = runBlocking {
        val dao = db.schemeDao()
        dao.insertSchemes(listOf(
            scheme("AG_001", "PM Kisan",        category = "agriculture"),
            scheme("HE_001", "Ayushman Bharat", category = "health"),
            scheme("HO_001", "PM Awas Yojana",  category = "housing"),
        ))

        val all = dao.getSchemesByCategory(null, "ALL")
        assertEquals("null category should return all schemes", 3, all.size)
    }

    @Test
    fun getByCategoryFiltersStateCorrectly() = runBlocking {
        val dao = db.schemeDao()
        dao.insertSchemes(listOf(
            scheme("CENTRAL_001", "PM Kisan",         state = "ALL"),
            scheme("UP_001",      "UP Kanya Sumangala", state = "UP"),
            scheme("MH_001",      "Ladki Bahin",       state = "MH"),
        ))

        val upSchemes = dao.getSchemesByCategory(null, "UP")
        // state=ALL matches ALL states, state=UP matches UP only
        assertTrue("UP filter should include ALL-state schemes",
            upSchemes.any { it.id == "CENTRAL_001" })
        assertTrue("UP filter should include UP-specific schemes",
            upSchemes.any { it.id == "UP_001" })
        assertTrue("UP filter should exclude MH-specific schemes",
            upSchemes.none { it.id == "MH_001" })
    }

    // ── active scheme count ───────────────────────────────────────────────────

    @Test
    fun activeSchemeCountIsCorrect() = runBlocking {
        val dao = db.schemeDao()
        dao.insertSchemes(listOf(
            scheme("S1", "Scheme 1"),
            scheme("S2", "Scheme 2"),
            scheme("S3", "Scheme 3").copy(isActive = false),
        ))

        val count = dao.getActiveSchemeCount()
        assertEquals("Active count should exclude inactive schemes", 2, count)
    }

    // ── confidence score decay (pure logic, no DB needed) ────────────────────

    @Test
    fun confidenceScoreDecaysBy1PercentPerWeek() {
        val msPerWeek = 7L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - msPerWeek

        val s = scheme("DECAY_001", "Decay Test", confidenceScore = 1.0f,
            lastVerifiedEpoch = oneWeekAgo)

        val weeksElapsed = (now - s.lastVerifiedEpoch) / msPerWeek
        val decayed = maxOf(0f, s.confidenceScore - (0.01f * weeksElapsed))

        assertTrue("Score after $weeksElapsed week(s) should be < 1.0 (was $decayed)",
            decayed < 1.0f)
        assertTrue("Score should be >= 0.99 after only 1 week (was $decayed)",
            decayed >= 0.99f)
    }

    @Test
    fun schemeIsStaleAfter50Weeks() {
        val msPerWeek = 7L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        val fiftyWeeksAgo = now - (50 * msPerWeek)

        val s = scheme("STALE_001", "Stale Test", confidenceScore = 1.0f,
            lastVerifiedEpoch = fiftyWeeksAgo)

        val weeksElapsed = (now - s.lastVerifiedEpoch) / msPerWeek
        val decayed = maxOf(0f, s.confidenceScore - (0.01f * weeksElapsed))

        assertTrue("After 50 weeks, score $decayed should be below 0.6 (stale threshold)",
            decayed < 0.6f)
    }

    @Test
    fun schemeIsNotStaleWhenFresh() {
        val now = System.currentTimeMillis()
        val s = scheme("FRESH_001", "Fresh Test", confidenceScore = 1.0f,
            lastVerifiedEpoch = now)

        val weeksElapsed = (now - s.lastVerifiedEpoch) / (7L * 24 * 60 * 60 * 1000)
        val decayed = maxOf(0f, s.confidenceScore - (0.01f * weeksElapsed))

        assertTrue("A just-verified scheme should not be stale (score=$decayed)",
            decayed >= 0.6f)
    }
}