package com.pocketsarkar.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketsarkar.ai.DownloadState
import com.pocketsarkar.ai.ModelDownloadViewModel
import com.pocketsarkar.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ModelSetupScreen(
    onModelReady: () -> Unit,
    viewModel: ModelDownloadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.isModelReady()) onModelReady()
    }

    val state by viewModel.downloadState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is DownloadState.Complete) onModelReady()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PSCream) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.pocketsarkar.R.drawable.ic_pocket_sarkar_icon),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.Unspecified
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Pocket Sarkar",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium,
                color = PSNavy
            )
            Text(
                "Aapka Sarkar, Aapki Bhasha",
                style = MaterialTheme.typography.bodyLarge,
                color = PSTextSecondary
            )
            Spacer(Modifier.height(48.dp))

            when (val s = state) {
                is DownloadState.Idle -> SetupIdle(onStart = {
                    val pm = context.getSystemService(PowerManager::class.java)
                    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                    viewModel.startDownload()
                })
                is DownloadState.Downloading -> SetupDownloading(s)
                is DownloadState.Complete -> SetupComplete()
                is DownloadState.Error -> SetupError(s.message) { viewModel.retryDownload() }
            }
        }
    }
}

@Composable
private fun SetupIdle(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "One-Time Setup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = PSNavy
            )
            Text(
                "Pocket Sarkar runs entirely on your device.\n" +
                "Your documents never leave your phone.\n\n" +
                "A one-time download of 3.65 GB is needed to set up the AI model.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = PSTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PSSaffron, contentColor = PSWhite)
            ) {
                Text("Download AI Model (3.65 GB)", fontWeight = FontWeight.Medium)
            }
            Text(
                "Recommended: connect to Wi-Fi before downloading",
                style = MaterialTheme.typography.labelSmall,
                color = PSTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SetupDownloading(s: DownloadState.Downloading) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PSWhite)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Downloading AI Model…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = PSNavy
            )
            LinearProgressIndicator(
                progress = { s.progress },
                modifier = Modifier.fillMaxWidth(),
                color = PSSaffron,
                trackColor = PSSaffron.copy(alpha = 0.1f)
            )
            Text(
                "${s.downloadedMB.roundToInt()} MB / ${s.totalMB.roundToInt()} MB (${(s.progress * 100).roundToInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                color = PSTextSecondary
            )
        }
    }
}

@Composable
private fun SetupComplete() {
    CircularProgressIndicator(color = PSSaffron)
    Spacer(Modifier.height(16.dp))
    Text("Setting up…", style = MaterialTheme.typography.bodyMedium, color = PSNavy)
}

@Composable
private fun SetupError(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)) // Soft error bg
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Download Failed", style = MaterialTheme.typography.titleMedium, color = PSRedFlag)
            Text(message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = PSTextSecondary)
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PSNavy)
            ) { Text("Retry Download") }
        }
    }
}