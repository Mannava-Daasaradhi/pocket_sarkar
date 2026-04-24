package com.pocketsarkar

import android.app.Application
import androidx.work.Configuration
import com.pocketsarkar.db.DatabaseSeeder
import com.pocketsarkar.db.PocketSarkarDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory

@HiltAndroidApp
class PocketSarkarApp : Application(), Configuration.Provider {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject lateinit var database: PocketSarkarDatabase
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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