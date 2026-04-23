package com.pocketsarkar.db.dao

import androidx.room.*
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.HelplineNumber
import com.pocketsarkar.db.entities.Scheme
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeDao {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /** Full-text search via FTS5 – used by Scheme Explainer */
    @SkipQueryVerification
    @Query("""
        SELECT s.* FROM schemes s
        JOIN schemes_fts fts ON s.rowid = fts.rowid
        WHERE schemes_fts MATCH :query
        AND s.isActive = 1
        ORDER BY fts.rank
        LIMIT :limit
    """)
    suspend fun searchSchemes(query: String, limit: Int = 10): List<Scheme>

    /**
     * Phase-2 spec: FTS5 search, max 20 results, BM25 ranked.
     * JOIN uses s.rowid = fts.rowid (NOT s.id).
     */
    @SkipQueryVerification
    @Query("""
        SELECT s.* FROM schemes s
        JOIN schemes_fts fts ON s.rowid = fts.rowid
        WHERE schemes_fts MATCH :query
        AND s.isActive = 1
        ORDER BY fts.rank
        LIMIT 20
    """)
    suspend fun searchByFTS(query: String): List<Scheme>

    /** Get a single scheme by exact ID */
    @Query("SELECT * FROM schemes WHERE id = :id AND isActive = 1")
    suspend fun getSchemeById(id: String): Scheme?

    /** Filter by category + optional state */
    @Query("""
        SELECT * FROM schemes
        WHERE isActive = 1
        AND (:category IS NULL OR category = :category)
        AND (targetStates = 'ALL' OR targetStates LIKE '%' || :state || '%')
        ORDER BY confidenceScore DESC
    """)
    suspend fun getSchemesByCategory(category: String?, state: String): List<Scheme>

    /** Phase-2 spec alias: filter by category only */
    @Query("""
        SELECT * FROM schemes
        WHERE isActive = 1
        AND category = :category
        ORDER BY confidenceScore DESC
    """)
    suspend fun getByCategory(category: String): List<Scheme>

    /**
     * SQL-level eligibility filter — Phase 2 requirement.
     *
     * Layer 1 — flat column checks (new Phase-2 fields):
     *   - maxIncomeLPA = 0.0 means no limit
     *   - minAge/maxAge = 0 means no bound
     *
     * Layer 2 — EligibilityRule table subqueries (complex rules):
     *   - annual_income lte/lt rules
     *   - age lte/lt and gte/gt rules
     *
     * Both layers must pass. Layer 1 is evaluated first by SQLite
     * (cheaper, no subquery), so it short-circuits before the NOT EXISTS.
     */
    @Query("""
        SELECT DISTINCT s.* FROM schemes s
        WHERE s.isActive = 1

        -- State filter
        AND (s.targetStates = 'ALL' OR s.targetStates LIKE '%' || :state || '%')

        -- Gender filter
        AND (s.targetGender = 'ALL' OR s.targetGender = :gender)

        -- Flat income filter (Layer 1)
        AND (s.maxIncomeLPA = 0.0 OR :incomeLPA <= s.maxIncomeLPA)

        -- Flat age filters (Layer 1)
        AND (s.minAge = 0 OR :age >= s.minAge)
        AND (s.maxAge = 0 OR :age <= s.maxAge)

        -- Rule-table income filter (Layer 2)
        AND NOT EXISTS (
            SELECT 1 FROM eligibility_rules r
            WHERE r.schemeId = s.id
            AND r.field = 'annual_income'
            AND r.operator IN ('lte', 'lt')
            AND (:incomeLPA * 100000) > CAST(r.value AS REAL)
        )

        -- Rule-table age upper bound (Layer 2)
        AND NOT EXISTS (
            SELECT 1 FROM eligibility_rules r2
            WHERE r2.schemeId = s.id
            AND r2.field = 'age'
            AND r2.operator IN ('lte', 'lt')
            AND :age > CAST(r2.value AS INTEGER)
        )

        -- Rule-table age lower bound (Layer 2)
        AND NOT EXISTS (
            SELECT 1 FROM eligibility_rules r3
            WHERE r3.schemeId = s.id
            AND r3.field = 'age'
            AND r3.operator IN ('gte', 'gt')
            AND :age < CAST(r3.value AS INTEGER)
        )

        ORDER BY s.confidenceScore DESC
    """)
    suspend fun getEligibleSchemes(
        state: String,
        incomeLPA: Double,
        age: Int,
        gender: String
    ): List<Scheme>

    /** All eligibility rules for a scheme */
    @Query("SELECT * FROM eligibility_rules WHERE schemeId = :schemeId")
    suspend fun getRulesForScheme(schemeId: String): List<EligibilityRule>

    /** Schemes with stale confidence — Flow variant for UI */
    @Query("SELECT * FROM schemes WHERE confidenceScore < 0.6 AND isActive = 1")
    fun getStaleSchemesFlow(): Flow<List<Scheme>>

    /**
     * Schemes below confidence threshold — suspend variant.
     * Phase-2 spec: getStaleSchemes(threshold: Double = 0.6)
     */
    @Query("SELECT * FROM schemes WHERE confidenceScore < :threshold AND isActive = 1")
    suspend fun getStaleSchemes(threshold: Double = 0.6): List<Scheme>

    /** Helplines by category + state */
    @Query("""
        SELECT * FROM helpline_numbers
        WHERE (:category IS NULL OR category = :category)
        AND (states = 'ALL' OR states LIKE '%' || :state || '%')
    """)
    suspend fun getHelplines(category: String?, state: String = "ALL"): List<HelplineNumber>

    // ── Writes ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchemes(schemes: List<Scheme>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<EligibilityRule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelplines(helplines: List<HelplineNumber>)

    @Update
    suspend fun updateScheme(scheme: Scheme)

    /** Decay confidence scores — run weekly via WorkManager */
    @Query("""
        UPDATE schemes
        SET confidenceScore = MAX(0.0, confidenceScore - 0.01)
        WHERE isActive = 1
    """)
    suspend fun decayAllConfidenceScores()

    /** Soft-delete deprecated schemes */
    @Query("UPDATE schemes SET isActive = 0 WHERE id IN (:ids)")
    suspend fun deactivateSchemes(ids: List<String>)

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM schemes WHERE isActive = 1")
    suspend fun getActiveSchemeCount(): Int
}