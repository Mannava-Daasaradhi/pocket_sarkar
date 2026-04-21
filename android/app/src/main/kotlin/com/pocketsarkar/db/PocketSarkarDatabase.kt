package com.pocketsarkar.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 1,
    exportSchema = true        // Schema exported to /schemas/ for version history
)
@TypeConverters(Converters::class)
abstract class PocketSarkarDatabase : RoomDatabase() {
    abstract fun schemeDao(): SchemeDao
}
