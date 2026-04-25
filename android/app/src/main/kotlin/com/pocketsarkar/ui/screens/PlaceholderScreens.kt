package com.pocketsarkar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// Placeholder screens for modules not yet implemented.
// DecoderScreen has graduated to its own file (Phase 4).
// Each module gets its own file in ui/screens/ once its Phase begins.
// ─────────────────────────────────────────────────────────────────────────────

// SchemesScreen graduated to SchemeExplainerScreen.kt (Phase 5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title    = "Opportunity Radar",
        subtitle = "अवसर राडार",
        phase    = "Phase 7",
        onBack   = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RightsScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title    = "Rights Companion",
        subtitle = "अधिकार साथी",
        phase    = "Phase 7",
        onBack   = onBack,
    )
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    phase: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.Construction,
                contentDescription = null,
                modifier           = Modifier.size(48.dp),
                tint               = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Coming in $phase",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
