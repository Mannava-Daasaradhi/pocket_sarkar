package com.pocketsarkar.db.entities

import androidx.room.*

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Scheme â€” one row per government scheme (447 at launch)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Entity(tableName = "schemes")
data class Scheme(
    @PrimaryKey val id: String,                  // e.g. "PM_KISAN_001"
    val nameEn: String,
    val nameHi: String,
    val nameLocal: String? = null,               // State-language name

    val category: String,                        // "agriculture" | "education" | "housing" â€¦
    val ministryEn: String,
    val descriptionEn: String,
    val descriptionHi: String,

    val benefitAmount: String? = null,           // "â‚¹6,000/year" â€” human readable
    val benefitType: String,                     // "cash" | "subsidy" | "service" | "insurance"

    val targetStates: String = "ALL",            // "ALL" or "UP,MP,RJ" etc.
    val targetGender: String = "ALL",            // "ALL" | "F" | "M"
    val targetCategory: String = "ALL",          // "ALL" | "SC" | "ST" | "OBC" | "GEN" | "EWS"

    val portalUrl: String? = null,
    val helplineNumber: String? = null,

    // Confidence decay: 1.0 = just verified, decays 0.01/week
    val confidenceScore: Float = 1.0f,
    val lastVerifiedEpoch: Long = System.currentTimeMillis(),

    val isActive: Boolean = true
)

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// SchemeFts â€” FTS5 virtual table for fast full-text search
// Mirrors the text fields from Scheme that users might search by.
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Fts5(contentEntity = Scheme::class)
@Entity(tableName = "schemes_fts")
data class SchemeFts(
    val nameEn: String,
    val nameHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val category: String,
    val benefitType: String,
)

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// EligibilityRule â€” one row per eligibility criterion per scheme
// Stored separately so the engine can evaluate without full-text loading.
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // Field being tested: "annual_income" | "land_hectares" | "age" | "state" | "category" | "gender"
    val field: String,

    // Operator: "lt" | "lte" | "gt" | "gte" | "eq" | "in"
    val operator: String,

    // Value as string (parsed at runtime based on field type)
    val value: String,

    // Human-readable label shown to user
    val labelEn: String,
    val labelHi: String,
)

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// HelplineNumber â€” offline emergency contacts
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Entity(tableName = "helpline_numbers")
data class HelplineNumber(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameHi: String,
    val number: String,
    val category: String,    // "police" | "women" | "child" | "labour" | "health" | "legal"
    val states: String = "ALL",
    val available24x7: Boolean = false,
    val isTollFree: Boolean = true,
)