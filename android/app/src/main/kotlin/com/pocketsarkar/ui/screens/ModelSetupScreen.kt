package com.pocketsarkar.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketsarkar.ai.DownloadState
import com.pocketsarkar.ai.ModelDownloadViewModel
import kotlin.math.roundToInt

@Composable
fun ModelSetupScreen(
    onModelReady: () -> Unit,
    viewModel: ModelDownloadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Keep screen on for the entire setup screen lifetime
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Skip setup if model already on device
    LaunchedEffect(Unit) {
        if (viewModel.isModelReady()) onModelReady()
    }

    val state by viewModel.downloadState.collectAsStateWithLifecycle()

    // Navigate as soon as download completes
    LaunchedEffect(state) {
        if (state is DownloadState.Complete) onModelReady()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🇮🇳", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Pocket Sarkar",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Aapka Sarkar, Aapki Bhasha",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))

        when (val s = state) {

            is DownloadState.Idle -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "One-Time Setup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pocket Sarkar runs entirely on your device.\n" +
                            "Your documents never leave your phone.\n\n" +
                            "A one-time download of 2.58 GB is needed to set up the AI model.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                // Request battery optimization exemption so download
                                // isn't throttled when app is backgrounded
                                val pm = context.getSystemService(PowerManager::class.java)
                                if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                    val intent = Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                                viewModel.startDownload()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Download AI Model  (2.58 GB)")
                        }
                        Text(
                            "Recommended: connect to Wi-Fi before downloading",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is DownloadState.Downloading -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Downloading AI Model…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${s.downloadedMB.roundToInt()} MB  /  ${s.totalMB.roundToInt()} MB" +
                            "  (${(s.progress * 100).roundToInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Download continues even if you leave the app.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is DownloadState.Complete -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Setting up…", style = MaterialTheme.typography.bodyMedium)
            }

            is DownloadState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Download Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            s.message,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { viewModel.retryDownload() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Retry Download") }
                    }
                }
            }
        }
    }
}