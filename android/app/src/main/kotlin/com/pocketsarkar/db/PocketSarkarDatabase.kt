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
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PocketSarkarDatabase : RoomDatabase() {

    abstract fun schemeDao(): SchemeDao

    companion object {

        // Called on brand-new installs (version 1 never shipped, so this
        // creates the FTS4 table alongside the normal tables).
        val ON_CREATE_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts4(
                        content='schemes',
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType
                    )
                """.trimIndent())

                // Populate FTS index from existing schemes rows (empty on fresh install,
                // but safe to run — DatabaseSeeder will insert rows after this).
                db.execSQL("""
                    INSERT INTO schemes_fts(schemes_fts) VALUES('rebuild')
                """.trimIndent())
            }
        }

        // Migration 1→2: drop old FTS table, recreate as FTS4.
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
    }
}