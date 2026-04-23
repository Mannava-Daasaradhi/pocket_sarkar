package com.pocketsarkar.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketsarkar.db.dao.SchemeDao
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.HelplineNumber
import com.pocketsarkar.db.entities.Scheme

// SchemeFts is intentionally excluded from entities[].
// FTS virtual tables cause KSP [MissingType] when listed as @Database entities
// with @Fts4/@Fts5(contentEntity=). The table is created via the onCreate callback below.
@Database(
    entities = [
        Scheme::class,
        EligibilityRule::class,
        HelplineNumber::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PocketSarkarDatabase : RoomDatabase() {

    abstract fun schemeDao(): SchemeDao

    companion object {

        /**
         * Fresh installs (no prior version).
         * Creates the FTS5 virtual table after Room creates the base tables.
         * Seeder inserts rows AFTER this callback, so 'rebuild' here is a
         * safe no-op on empty DB — Seeder calls rebuild again after inserts.
         */
        val ON_CREATE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts5(
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType,
                        content='schemes',
                        content_rowid='rowid'
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO schemes_fts(schemes_fts) VALUES('rebuild')")
            }
        }

        // ── Migration 1 → 2 (kept for upgrade path from old installs) ─────────
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS schemes_fts")
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts4(
                        content='schemes',
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO schemes_fts(schemes_fts) VALUES('rebuild')")
            }
        }

        /**
         * Migration 2 → 3
         *
         * What changes:
         *   1. Add 5 Phase-2 eligibility columns to `schemes` with safe defaults:
         *        maxIncomeLPA  REAL    DEFAULT 0.0   (0.0 = no income limit)
         *        minAge        INTEGER DEFAULT 0     (0 = no lower bound)
         *        maxAge        INTEGER DEFAULT 0     (0 = no upper bound)
         *        casteEligibility TEXT DEFAULT '[]'  (empty = open to all)
         *        documentsRequired TEXT DEFAULT '[]'
         *
         *   2. Drop FTS4 virtual table, recreate as FTS5.
         *        FTS4 → FTS5: adds BM25 ranking (ORDER BY fts.rank in DAO).
         *        content_rowid='rowid' is explicit to match DAO JOIN condition.
         *
         *   3. Rebuild FTS index to cover existing 101 seeded rows.
         *
         * confidenceScore Float → Double: both map to SQLite REAL.
         * No DDL change needed — Room reads the value into whichever Kotlin
         * type the entity declares.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. New eligibility columns
                db.execSQL("ALTER TABLE schemes ADD COLUMN maxIncomeLPA REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE schemes ADD COLUMN minAge INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schemes ADD COLUMN maxAge INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schemes ADD COLUMN casteEligibility TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE schemes ADD COLUMN documentsRequired TEXT NOT NULL DEFAULT '[]'")

                // 2. Drop FTS4, recreate as FTS5
                db.execSQL("DROP TABLE IF EXISTS schemes_fts")
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts5(
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType,
                        content='schemes',
                        content_rowid='rowid'
                    )
                """.trimIndent())

                // 3. Rebuild FTS index over existing rows
                db.execSQL("INSERT INTO schemes_fts(schemes_fts) VALUES('rebuild')")
            }
        }
    }
}