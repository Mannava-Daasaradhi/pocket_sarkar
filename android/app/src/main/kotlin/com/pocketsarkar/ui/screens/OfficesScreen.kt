package com.pocketsarkar.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketsarkar.data.MockData
import com.pocketsarkar.data.OfficeLocation
import com.pocketsarkar.ui.theme.*
import com.pocketsarkar.ui.viewmodels.CategoryAiViewModel
import com.pocketsarkar.utils.VoiceHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficesScreen(userLanguage: String, viewModel: CategoryAiViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val strings = Localization.getStrings(userLanguage)
    val offices = MockData.getOffices()
    var userNeed by remember { mutableStateOf("") }
    val aiGuidance by viewModel.recommendation.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val voiceHelper = remember {
        VoiceHelper(
            context = context,
            onResult = { result -> 
                userNeed = result
                viewModel.clear()
                viewModel.getRecommendation("Office Selection", listOf(result), userLanguage)
            },
            onError = { /* Handle error */ }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) voiceHelper.startListening()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Local Offices", fontWeight = FontWeight.Bold, color = PSNavy) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = PSCream)
            )
        },
        containerColor = PSCream
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PSNavy),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = PSSaffron, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI OFFICE ASSISTANT", color = PSSaffron, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("What service do you need today?", color = PSWhite, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userNeed,
                            onValueChange = { userNeed = it },
                            placeholder = { Text("e.g., Ration Card, Land Records...", color = PSWhite.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PSWhite, unfocusedTextColor = PSWhite,
                                focusedBorderColor = PSSaffron, unfocusedBorderColor = PSWhite.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                IconButton(onClick = { 
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }) {
                                    Icon(Icons.Default.Mic, null, tint = PSSaffron)
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { 
                                    if (userNeed.isNotBlank()) {
                                        viewModel.clear()
                                        viewModel.getRecommendation("Office Selection", listOf(userNeed), userLanguage) 
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = PSSaffron)
                                }
                            }
                        )
                    }
                }
            }

            if (isAnalyzing || aiGuidance.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PSWhite),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PSSaffron)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AI Guidance", color = PSNavy, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isAnalyzing && aiGuidance.isEmpty()) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PSSaffron)
                            } else {
                                Text(aiGuidance, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Nearby Centers",
                    style = MaterialTheme.typography.titleMedium,
                    color = PSNavy,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(offices) { office ->
                OfficeCard(office)
            }
        }
    }
}

@Composable
private fun OfficeCard(office: OfficeLocation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).background(PSNavy.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, null, tint = PSNavy, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(office.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PSNavy)
                Text(office.address, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
                Text(office.distance, style = MaterialTheme.typography.labelSmall, color = PSSaffron, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { /* Call */ }, modifier = Modifier.background(PSCream, CircleShape)) {
                Icon(Icons.Default.Call, null, tint = PSNavy, modifier = Modifier.size(20.dp))
            }
        }
    }
}
