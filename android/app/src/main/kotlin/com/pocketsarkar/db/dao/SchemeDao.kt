package com.pocketsarkar.db.dao

import androidx.room.*
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.HelplineNumber
import com.pocketsarkar.db.entities.Scheme
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeDao {

    // â”€â”€ Reads â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Full-text search via FTS5 â€” used by Scheme Explainer */
    @SkipQueryVerification
    @Query("""
        SELECT s.* FROM schemes s
        JOIN schemes_fts fts ON s.id = fts.rowid
        WHERE schemes_fts MATCH :query
        AND s.isActive = 1
        ORDER BY rank
        LIMIT :limit
    """)
    suspend fun searchSchemes(query: String, limit: Int = 10): List<Scheme>

    /** Get a single scheme by exact ID â€” used by function calling response */
    @Query("SELECT * FROM schemes WHERE id = :id AND isActive = 1")
    suspend fun getSchemeById(id: String): Scheme?

    /** Filter by category + optional state â€” used by Opportunity Radar */
    @Query("""
        SELECT * FROM schemes
        WHERE isActive = 1
        AND (:category IS NULL OR category = :category)
        AND (targetStates = 'ALL' OR targetStates LIKE '%' || :state || '%')
        ORDER BY confidenceScore DESC
    """)
    suspend fun getSchemesByCategory(category: String?, state: String): List<Scheme>

    /** All eligibility rules for a scheme â€” used by eligibility engine */
    @Query("SELECT * FROM eligibility_rules WHERE schemeId = :schemeId")
    suspend fun getRulesForScheme(schemeId: String): List<EligibilityRule>

    /** Schemes with stale confidence â€” for sync warning UI */
    @Query("SELECT * FROM schemes WHERE confidenceScore < 0.6 AND isActive = 1")
    fun getStaleSchemesFlow(): Flow<List<Scheme>>

    /** Helplines by category + state */
    @Query("""
        SELECT * FROM helpline_numbers
        WHERE (:category IS NULL OR category = :category)
        AND (states = 'ALL' OR states LIKE '%' || :state || '%')
    """)
    suspend fun getHelplines(category: String?, state: String = "ALL"): List<HelplineNumber>

    // â”€â”€ Writes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchemes(schemes: List<Scheme>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<EligibilityRule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelplines(helplines: List<HelplineNumber>)

    /** Delta sync: update only changed schemes */
    @Update
    suspend fun updateScheme(scheme: Scheme)

    /** Decay confidence scores â€” run weekly via WorkManager */
    @Query("""
        UPDATE schemes
        SET confidenceScore = MAX(0.0, confidenceScore - 0.01)
        WHERE isActive = 1
    """)
    suspend fun decayAllConfidenceScores()

    /** Soft-delete deprecated schemes */
    @Query("UPDATE schemes SET isActive = 0 WHERE id IN (:ids)")
    suspend fun deactivateSchemes(ids: List<String>)

    // â”€â”€ Stats â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Query("SELECT COUNT(*) FROM schemes WHERE isActive = 1")
    suspend fun getActiveSchemeCount(): Int
}

