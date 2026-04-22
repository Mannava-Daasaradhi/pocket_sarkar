package com.pocketsarkar.db.entities

import androidx.room.*

// ─────────────────────────────────────────────────────────────────────────────
// Scheme — one row per government scheme
// ─────────────────────────────────────────────────────────────────────────────

@Entity(tableName = "schemes")
data class Scheme(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameHi: String,
    val nameLocal: String? = null,
    val category: String,
    val ministryEn: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val benefitAmount: String? = null,
    val benefitType: String,
    val targetStates: String = "ALL",
    val targetGender: String = "ALL",
    val targetCategory: String = "ALL",
    val portalUrl: String? = null,
    val helplineNumber: String? = null,
    val confidenceScore: Float = 1.0f,
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
    val field: String,
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