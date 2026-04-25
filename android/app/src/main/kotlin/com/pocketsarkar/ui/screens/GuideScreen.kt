package com.pocketsarkar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketsarkar.ui.viewmodels.GuideViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsarkar.ui.theme.*

data class GuideStep(val title: String, val desc: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    schemeName: String, 
    userLanguage: String,
    onBack: () -> Unit,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val steps by viewModel.steps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.generateGuide(schemeName, userLanguage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(schemeName, fontWeight = FontWeight.Bold, color = PSNavy) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PSNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PSCream)
            )
        },
        containerColor = PSCream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "AI-Powered Application Roadmap",
                style = MaterialTheme.typography.titleLarge,
                color = PSNavy,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && steps.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PSSaffron)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI is generating your roadmap...", color = PSTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                steps.forEachIndexed { index, step ->
                    StepItem(index + 1, step.title, step.desc)
                    if (index < steps.size - 1) {
                        Connector()
                    }
                }
            }
        }
    }
}

private fun getStepsForScheme(name: String): List<GuideStep> {
    return when {
        name.contains("Kisan") -> listOf(
            GuideStep("Land Record Verification", "Get your 'Khatauni' or land ownership papers ready."),
            GuideStep("Aadhaar Seeding", "Visit the PM-Kisan portal or CSC to link Aadhaar with your bank."),
            GuideStep("Submit to Patwari", "Verify your documents with the local village Patwari/Lekhpal."),
            GuideStep("e-KYC Completion", "Complete biometric authentication at a Jan Seva Kendra.")
        )
        name.contains("Awas") -> listOf(
            GuideStep("Socio-Economic Survey", "Ensure your name is in the SECC-2011 list."),
            GuideStep("Current House Photo", "The official will take a photo of your current 'Kucha' house."),
            GuideStep("Verification", "Block officials will visit your site for physical verification."),
            GuideStep("Installment Release", "Money will be sent directly to your bank in 3 stages.")
        )
        name.contains("Ayushman") -> listOf(
            GuideStep("Check PMJAY List", "Verify your family ID on the Ayushman Bharat portal."),
            GuideStep("Golden Card Creation", "Go to a government hospital with your Ration Card."),
            GuideStep("Biometric Update", "Register your fingerprints for the e-card."),
            GuideStep("Hospitalization", "Show your card at any empanelled hospital for free treatment.")
        )
        else -> listOf(
            GuideStep("Document Check", "Gather your ID and Address proofs."),
            GuideStep("Visit Center", "Go to the nearest government service center."),
            GuideStep("Application Form", "Fill the specific scheme form provided by the official."),
            GuideStep("Tracking", "Note your application number to track status in this app.")
        )
    }
}

@Composable
private fun StepItem(number: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(36.dp).background(PSNavy, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number.toString(), color = PSWhite, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PSNavy)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = PSTextSecondary)
        }
    }
}

@Composable
private fun Connector() {
    Box(
        modifier = Modifier.padding(start = 17.dp).width(2.dp).height(40.dp).background(PSNavy.copy(alpha = 0.2f))
    )
}
