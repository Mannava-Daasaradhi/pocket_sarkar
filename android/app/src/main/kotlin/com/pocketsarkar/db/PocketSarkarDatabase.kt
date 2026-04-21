package com.pocketsarkar.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pocketsarkar.db.dao.SchemeDao
import com.pocketsarkar.db.entities.Scheme
import com.pocketsarkar.db.entities.SchemeFts
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.HelplineNumber

@Database(
    entities = [
        Scheme::class,
        SchemeFts::class,      // FTS5 virtual table for full-text search
        EligibilityRule::class,
        HelplineNumber::class,
    ],
    version = 2,
    exportSchema = true        // Schema exported to /schemas/ for version history
)
@TypeConverters(Converters::class)
abstract class PocketSarkarDatabase : RoomDatabase() {
    abstract fun schemeDao(): SchemeDao

    companion object {
        /**
         * Migration from version 1 to 2:
         * - Changed SchemeFts from FTS4 to FTS5 to support ORDER BY rank
         * - Drop and recreate the schemes_fts table with FTS5
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old FTS4 table
                db.execSQL("DROP TABLE IF EXISTS schemes_fts")

                // Recreate as FTS5 table with the same columns
                // Room will automatically populate it from the content table (schemes)
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS schemes_fts USING fts5(
                        nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType,
                        content=schemes
                    )
                """)
            }
        }
    }
}