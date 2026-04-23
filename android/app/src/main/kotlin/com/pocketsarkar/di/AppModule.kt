package com.pocketsarkar.di

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.pocketsarkar.db.PocketSarkarDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePocketSarkarDatabase(
        @ApplicationContext context: Context
    ): PocketSarkarDatabase {
        return Room.databaseBuilder(
            context,
            PocketSarkarDatabase::class.java,
            "pocket_sarkar.db"
        )
            // BundledSQLiteDriver ships its own modern SQLite with FTS5 compiled in.
            // This is the official Google replacement for requery/sqlite-android.
            // Required on Samsung One UI and other OEMs that strip FTS5 from the
            // system SQLite. Available on Google Maven — no JitPack needed.
            .setDriver(BundledSQLiteDriver())
            .addCallback(PocketSarkarDatabase.ON_CREATE_CALLBACK)
            .addMigrations(PocketSarkarDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSchemeDao(db: PocketSarkarDatabase) = db.schemeDao()
}
