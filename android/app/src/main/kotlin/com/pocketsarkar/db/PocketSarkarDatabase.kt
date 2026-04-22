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
// FTS5 virtual tables cause KSP [MissingType] when listed as @Database entities
// with @Fts5(contentEntity=). The table is created via the onCreate callback below.
@Database(
    entities = [
        Scheme::class,
        EligibilityRule::class,
        HelplineNumber::class,
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PocketSarkarDatabase : RoomDatabase() {

    abstract fun schemeDao(): SchemeDao

    companion object {

        // Called on brand-new installs (version 1 never shipped, so this
        // creates the FTS5 table alongside the normal tables).
        val ON_CREATE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts5(
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType,
                        content=schemes,
                        content_rowid=rowid
                    )
                """.trimIndent())

                // Populate FTS index from existing schemes rows (empty on fresh install,
                // but safe to run — DatabaseSeeder will insert rows after this).
                db.execSQL("""
                    INSERT INTO schemes_fts(rowid, nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType)
                    SELECT rowid, nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType FROM schemes
                """.trimIndent())
            }
        }

        // Migration 1→2: drop old FTS4 table, recreate as FTS5.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS schemes_fts")
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts5(
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType,
                        content=schemes,
                        content_rowid=rowid
                    )
                """.trimIndent())
            }
        }
    }
}