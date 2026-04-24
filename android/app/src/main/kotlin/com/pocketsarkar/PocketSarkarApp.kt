package com.pocketsarkar

import android.app.Application
import com.pocketsarkar.db.DatabaseSeeder
import com.pocketsarkar.db.PocketSarkarDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class — Hilt entry point.
 * Seeds the scheme database on first launch.
 */
@HiltAndroidApp
class PocketSarkarApp : Application() {

    // App-scoped coroutine scope (lives as long as the process)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var database: PocketSarkarDatabase

    override fun onCreate() {
        super.onCreate()

        // Seed DB in background — does nothing after first run (idempotent)
        appScope.launch {
            DatabaseSeeder.seedIfNeeded(
                context = applicationContext,
                dao = database.schemeDao(),
                // Pass the raw DB handle so the seeder can populate the FTS index
                // via execSQL after inserting schemes (no triggers exist to sync it).
                
            )
        }
    }
}