package com.pocketsarkar.db.dao

import androidx.room.*
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.HelplineNumber
import com.pocketsarkar.db.entities.Scheme
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeDao {

    // ── Reads ─────────────────────────────────────────────────────────────────

    /** Full-text search via FTS4 – used by Scheme Explainer */
    @SkipQueryVerification
    @Query("""
        SELECT s.* FROM schemes s
        JOIN schemes_fts fts ON s.rowid = fts.rowid
        WHERE schemes_fts MATCH :query
        AND s.isActive = 1
        LIMIT :limit
    """)
    suspend fun searchSchemes(query: String, limit: Int = 10): List<Scheme>

    /**
     * Phase-2 spec alias for searchSchemes — same SQL, max 20 results.
     * JOIN must use s.rowid = fts.rowid (NOT s.id).
     */
    @SkipQueryVerification
    @Query("""
        SELECT s.* FROM schemes s
        JOIN schemes_fts fts ON s.rowid = fts.rowid
        WHERE schemes_fts MATCH :query
        AND s.isActive = 1
        LIMIT 20
    """)
    suspend fun searchByFTS(query: String): List<Scheme>

    /** Get a single scheme by exact ID – used by function calling response */
    @Query("SELECT * FROM schemes WHERE id = :id AND isActive = 1")
    suspend fun getSchemeById(id: String): Scheme?

    /** Filter by category + optional state – used by Opportunity Radar */
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
     * Filters schemes where:
     * - state matches (targetStates = 'ALL' OR contains :state)
     * - gender matches (targetGender = 'ALL' OR = :gender)
     * - No income-lte rule requires income < user's income
     * (:incomeLPA is in Lakhs Per Annum; rules store rupees)
     * - No age-lte rule requires age < user's age
     * - No age-gte rule requires age > user's age
     */
    @Query("""
        SELECT DISTINCT s.* FROM schemes s
        WHERE s.isActive = 1
        AND (s.targetStates = 'ALL' OR s.targetStates LIKE '%' || :state || '%')
        AND (s.targetGender = 'ALL' OR s.targetGender = :gender)
        AND NOT EXISTS (
            SELECT 1 FROM eligibility_rules r
            WHERE r.schemeId = s.id
            AND r.field = 'annual_income'
            AND r.operator IN ('lte', 'lt')
            AND (:incomeLPA * 100000) > CAST(r.value AS REAL)
        )
        AND NOT EXISTS (
            SELECT 1 FROM eligibility_rules r2
            WHERE r2.schemeId = s.id
            AND r2.field = 'age'
            AND r2.operator IN ('lte', 'lt')
            AND :age > CAST(r2.value AS INTEGER)
        )
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

    /** All eligibility rules for a scheme – used by eligibility engine */
    @Query("SELECT * FROM eligibility_rules WHERE schemeId = :schemeId")
    suspend fun getRulesForScheme(schemeId: String): List<EligibilityRule>

    /** Schemes with stale confidence – for sync warning UI (Flow variant) */
    @Query("SELECT * FROM schemes WHERE confidenceScore < 0.6 AND isActive = 1")
    fun getStaleSchemesFlow(): Flow<List<Scheme>>

    /**
     * Schemes below confidence threshold — suspend variant for one-shot queries.
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

    /** Delta sync: update only changed schemes */
    @Update
    suspend fun updateScheme(scheme: Scheme)

    /** Decay confidence scores – run weekly via WorkManager */
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