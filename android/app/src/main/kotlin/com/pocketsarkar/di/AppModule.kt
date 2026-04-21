package com.pocketsarkar.di

import android.content.Context
import androidx.room.Room
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

    /**
     * Single Room database instance for the whole app lifetime.
     * FTS5 virtual table is defined in the DB class.
     */
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
            // Pre-populated from assets on first install
            // .createFromAsset("databases/pocket_sarkar_seed.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideSchemeDao(db: PocketSarkarDatabase) = db.schemeDao()
}
