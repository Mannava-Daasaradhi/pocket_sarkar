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

@HiltAndroidApp
class PocketSarkarApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var database: PocketSarkarDatabase

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            DatabaseSeeder.seedIfNeeded(
                context = applicationContext,
                dao = database.schemeDao(),
            )
        }
    }
}