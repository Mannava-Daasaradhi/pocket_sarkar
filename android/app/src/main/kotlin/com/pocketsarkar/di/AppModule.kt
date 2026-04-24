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
            .setDriver(BundledSQLiteDriver())   // bundled SQLite — Samsung strips FTS5 from system SQLite
            .addCallback(PocketSarkarDatabase.ON_CREATE_CALLBACK)
            .addMigrations(
                PocketSarkarDatabase.MIGRATION_1_2,
                PocketSarkarDatabase.MIGRATION_2_3
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideSchemeDao(db: PocketSarkarDatabase) = db.schemeDao()
}