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
// FTS virtual tables cause KSP [MissingType] when listed as @Database entities.
// The table is created via the onCreate callback below.
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
         * Fresh installs — creates the FTS5 virtual table after Room creates
         * the base tables. Seeder calls 'rebuild' again after inserts.
         */
        val ON_CREATE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // 1. Create standalone FTS5 table
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts5(
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType
                    )
                """.trimIndent())

                // 2. Trigger to auto-populate FTS5 on insert (helps Robolectric tests)
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS schemes_fts_insert
                    AFTER INSERT ON schemes BEGIN
                        INSERT INTO schemes_fts(
                            rowid, nameEn, nameHi, descriptionEn, descriptionHi,
                            category, benefitType
                        )
                        VALUES (
                            new.rowid, new.nameEn, new.nameHi, new.descriptionEn,
                            new.descriptionHi, new.category, new.benefitType
                        );
                    END;
                """.trimIndent())
            }
        }
        /**
        * Test-only callback — uses FTS4 because Robolectric's bundled SQLite
        * on Windows does not compile in FTS5. FTS4 supports the same MATCH
        * queries used by SchemeDao; the only loss is BM25 rank ordering,
        * which is not tested. Production always uses FTS5 via MIGRATION_2_3.
        */
        val TEST_CREATE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts4(
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS schemes_fts_insert
                    AFTER INSERT ON schemes BEGIN
                        INSERT INTO schemes_fts(
                            rowid, nameEn, nameHi, descriptionEn, descriptionHi,
                            category, benefitType
                        )
                        VALUES (
                            new.rowid, new.nameEn, new.nameHi, new.descriptionEn,
                            new.descriptionHi, new.category, new.benefitType
                        );
                    END;
                """.trimIndent())
            }
        }

        // ── Migration 1 → 2 ────────────────────────────────────────────────
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

        // ── Migration 2 → 3 ────────────────────────────────────────────────
        // Changes:
        //   1. Add 5 Phase-2 eligibility columns to `schemes` with safe defaults
        //   2. Drop FTS4, recreate as FTS5 (BM25 ranking)
        //   3. Rebuild FTS index
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