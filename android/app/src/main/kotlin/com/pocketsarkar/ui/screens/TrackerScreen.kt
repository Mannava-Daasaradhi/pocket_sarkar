package com.pocketsarkar.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsarkar.data.ApplicationStatus
import com.pocketsarkar.data.MockData
import com.pocketsarkar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(userLanguage: String) {
    val strings = Localization.getStrings(userLanguage)
    val applications = MockData.getApplications(userLanguage)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.navRights, fontWeight = FontWeight.Bold, color = PSNavy) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PSCream)
            )
        },
        containerColor = PSCream
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Track your active applications in real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PSTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(applications) { app ->
                ApplicationCard(app)
            }
        }
    }
}

@Composable
private fun ApplicationCard(app: ApplicationStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.schemeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PSNavy)
                    Text("Last update: ${app.lastUpdate}", style = MaterialTheme.typography.labelSmall, color = PSTextSecondary)
                }
                StatusChip(app.status)
            }

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { app.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (app.progress == 1f) Color(0xFF2E7D32) else PSSaffron,
                trackColor = PSBorder,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Applied", style = MaterialTheme.typography.labelSmall, color = PSTextSecondary)
                Text("Verification", style = MaterialTheme.typography.labelSmall, color = PSTextSecondary)
                Text("Approved", style = MaterialTheme.typography.labelSmall, color = PSTextSecondary)
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "Approved" -> Color(0xFFE8F5E9)
        "Processing" -> Color(0xFFFFF3E0)
        else -> Color(0xFFE3F2FD)
    }
    val textColor = when (status) {
        "Approved" -> Color(0xFF2E7D32)
        "Processing" -> Color(0xFFE65100)
        else -> Color(0xFF1565C0)
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
