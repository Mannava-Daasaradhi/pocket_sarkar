package com.pocketsarkar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketsarkar.ui.navigation.Screen

data class ModuleCard(
    val screen: Screen,
    val titleHindi: String,
    val titleEnglish: String,
    val description: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {

    val modules = listOf(
        ModuleCard(
            screen = Screen.Decoder,
            titleHindi = "à¤¦à¤¸à¥à¤¤à¤¾à¤µà¥‡à¤œà¤¼ à¤¡à¥€à¤•à¥‹à¤¡à¤°",
            titleEnglish = "Document Decoder",
            description = "Scan any document â€” loan, rental, legal notice",
            icon = Icons.Default.DocumentScanner,
            color = MaterialTheme.colorScheme.errorContainer
        ),
        ModuleCard(
            screen = Screen.Schemes,
            titleHindi = "à¤¸à¤°à¤•à¤¾à¤°à¥€ à¤¯à¥‹à¤œà¤¨à¤¾à¤à¤‚",
            titleEnglish = "Scheme Explainer",
            description = "Find government schemes you qualify for",
            icon = Icons.Default.AccountBalance,
            color = MaterialTheme.colorScheme.primaryContainer
        ),
        ModuleCard(
            screen = Screen.Radar,
            titleHindi = "à¤…à¤µà¤¸à¤° à¤°à¤¾à¤¡à¤¾à¤°",
            titleEnglish = "Opportunity Radar",
            description = "Schemes you didn't know you were eligible for",
            icon = Icons.Default.Radar,
            color = MaterialTheme.colorScheme.secondaryContainer
        ),
        ModuleCard(
            screen = Screen.Rights,
            titleHindi = "à¤…à¤§à¤¿à¤•à¤¾à¤° à¤¸à¤¾à¤¥à¥€",
            titleEnglish = "Rights Companion",
            description = "Know your rights in any situation",
            icon = Icons.Default.Gavel,
            color = MaterialTheme.colorScheme.tertiaryContainer
        ),
        ModuleCard(
            screen = Screen.TestAi,
            titleHindi = "AI परीक्षण",
            titleEnglish = "AI Test Console",
            description = "Test on-device & Ollama AI directly",
            icon = Icons.Default.SmartToy,
            color = MaterialTheme.colorScheme.secondaryContainer
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pocket Sarkar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "à¤ªà¥‰à¤•à¥‡à¤Ÿ à¤¸à¤°à¤•à¤¾à¤°",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sarkar ki bhasha, ab aapki bhasha mein.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(modules) { module ->
                    ModuleCardItem(module = module, onClick = { onNavigate(module.screen) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleCardItem(
    module: ModuleCard,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(containerColor = module.color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = module.titleEnglish,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = module.titleHindi,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = module.titleEnglish,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

