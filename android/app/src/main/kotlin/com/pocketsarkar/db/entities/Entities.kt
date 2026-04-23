package com.pocketsarkar.db.entities

import androidx.room.*

// ─────────────────────────────────────────────────────────────────────────────
// Scheme — one row per government scheme
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "schemes")
data class Scheme(
    @PrimaryKey val id: String,

    // Display names (multilingual)
    val nameEn: String,
    val nameHi: String,
    val nameLocal: String? = null,

    // Classification
    val category: String,           // AGRICULTURE/EDUCATION/HEALTH/HOUSING/WOMEN/YOUTH/SC_ST/GENERAL
    val ministryEn: String,
    val descriptionEn: String,
    val descriptionHi: String,

    // Benefit details
    val benefitAmount: String? = null,
    val benefitType: String,        // CASH/KIND/SUBSIDY/SCHOLARSHIP

    // ── Eligibility filters (flat columns — fast SQL WHERE clauses) ───────────
    // Phase-2 spec fields added below. Existing eligibility_rules table
    // handles complex/multi-value rules; these handle the common scalar cases.

    val targetStates: String = "ALL",       // "ALL" or comma-separated state codes e.g. "UP,MH"
    val targetGender: String = "ALL",       // "ALL" / "M" / "F"
    val targetCategory: String = "ALL",     // caste/social category shorthand (kept for backward compat)

    /** Max annual income in Lakhs. 0.0 = no income limit. */
    val maxIncomeLPA: Double = 0.0,

    /** Minimum age (inclusive). 0 = no lower bound. */
    val minAge: Int = 0,

    /** Maximum age (inclusive). 0 = no upper bound. */
    val maxAge: Int = 0,

    /**
     * JSON array of eligible caste groups.
     * Empty array "[]" means open to all.
     * Example: "[\"SC\",\"ST\",\"OBC\"]"
     */
    val casteEligibility: String = "[]",

    /**
     * JSON array of required documents.
     * Example: "[\"Aadhaar\",\"Income Certificate\",\"Bank Passbook\"]"
     */
    val documentsRequired: String = "[]",

    // ── Application info ─────────────────────────────────────────────────────
    val portalUrl: String? = null,
    val helplineNumber: String? = null,

    // ── Confidence & freshness ────────────────────────────────────────────────
    /** 0.0–1.0. Decays 0.01/week. Below 0.6 = STALE. */
    val confidenceScore: Double = 1.0,
    val lastVerifiedEpoch: Long = System.currentTimeMillis(),

    val isActive: Boolean = true
)

// NOTE: SchemeFts is intentionally NOT defined as a Room @Entity.
// The FTS5 virtual table is created manually via RoomDatabase.Callback
// in PocketSarkarDatabase. This avoids KSP [MissingType] bugs with
// @Fts5(contentEntity=) that affect Room 2.6.x and 2.7.x with KSP 2.x.
// The DAO query uses @SkipQueryVerification so Room doesn't need to know
// about the FTS table schema at compile time.

// ─────────────────────────────────────────────────────────────────────────────
// EligibilityRule — one row per eligibility criterion per scheme
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "eligibility_rules",
    foreignKeys = [ForeignKey(
        entity = Scheme::class,
        parentColumns = ["id"],
        childColumns = ["schemeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("schemeId")]
)
data class EligibilityRule(
    @PrimaryKey(autoGenerate = true) val ruleId: Int = 0,
    val schemeId: String,
    /** Spec alias: ruleType. Field name being evaluated e.g. "annual_income", "age", "caste" */
    val field: String,
    /** lte / lt / gte / gt / eq / in */
    val operator: String,
    val value: String,
    val labelEn: String,
    val labelHi: String
)

// ─────────────────────────────────────────────────────────────────────────────
// HelplineNumber — offline emergency contacts
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "helpline_numbers")
data class HelplineNumber(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameHi: String,
    val number: String,
    val category: String,
    val states: String = "ALL",
    val available24x7: Boolean = false,
    val isTollFree: Boolean = true
)