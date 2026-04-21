package com.pocketsarkar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — Hilt entry point.
 * Also the right place to initialise crash reporting, analytics (none for us — privacy first),
 * and any app-wide singletons that aren't injected.
 */
@HiltAndroidApp
class PocketSarkarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Nothing here yet. Model loading is lazy (not at startup).
        // DB is initialised by Hilt via AppModule.
    }
}
