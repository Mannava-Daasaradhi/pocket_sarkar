package com.pocketsarkar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pocketsarkar.ui.navigation.PocketSarkarNavHost
import com.pocketsarkar.ui.theme.PocketSarkarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PocketSarkarTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PocketSarkarNavHost()
                }
            }
        }
    }
}
