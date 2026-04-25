package com.pocketsarkar.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketsarkar.ui.theme.*
import com.pocketsarkar.ui.viewmodels.CategoryAiViewModel
import com.pocketsarkar.utils.VoiceHelper

data class CategoryScheme(val title: String, val desc: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    userLanguage: String,
    onBack: () -> Unit,
    onNavigateToGuide: (String) -> Unit
) {
    val schemes = getSchemesForCategory(categoryName)
    var showAiInterview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName, fontWeight = FontWeight.Bold, color = PSNavy) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Top Recommended Schemes",
                    style = MaterialTheme.typography.titleLarge,
                    color = PSNavy,
                    fontWeight = FontWeight.Bold
                )
            }

            items(schemes) { scheme ->
                SchemeCard(scheme) { onNavigateToGuide(scheme.title) }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (!showAiInterview) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PSNavy),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoAwesome, null, tint = PSSaffron, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Not sure which one to pick?", color = PSWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Let our AI find the best scheme for you based on your situation.", color = PSWhite.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showAiInterview = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PSSaffron),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ask AI for Top Picks", color = PSNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showAiInterview) {
                item {
                    AiEligibilityChat(categoryName, userLanguage)
                }
            }
        }
    }
}

@Composable
private fun AiEligibilityChat(
    categoryName: String, 
    language: String,
    viewModel: CategoryAiViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    val answers = remember { mutableStateListOf<String>() }
    val recommendation by viewModel.recommendation.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    val voiceHelper = remember {
        VoiceHelper(
            context = context,
            onResult = { result -> userInput = result },
            onError = { /* Handle error */ }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) voiceHelper.startListening()
    }
    
    val questions = when(categoryName) {
        "Agriculture" -> listOf("Which state are you from?", "How much land do you own (in acres)?", "What is your main crop?", "What is your annual family income?")
        "Housing" -> listOf("Do you live in a Rural or Urban area?", "Do you currently own a pucca house?", "What is your monthly household income?", "Do you have an Aadhaar card?")
        "Health" -> listOf("What is your age?", "Do you have any existing insurance?", "Are you in the SECC-2011 list?", "Which state are you in?")
        else -> listOf("What is your state?", "What is your annual income?", "What is your primary need?", "Are you a student or worker?")
    }

    LaunchedEffect(step) {
        if (step == questions.size) {
            viewModel.getRecommendation(categoryName, answers, language)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, PSSaffron.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(PSSaffron, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI ELIGIBILITY CHECK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PSTextSecondary)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (step < questions.size) {
                Text(questions[step], style = MaterialTheme.typography.titleMedium, color = PSNavy, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    placeholder = { Text("Type or speak answer...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        IconButton(onClick = { 
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }) {
                            Icon(Icons.Default.Mic, null, tint = PSNavy)
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (userInput.isNotBlank()) {
                                answers.add(userInput)
                                userInput = ""
                                step++
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = PSNavy)
                        }
                    }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isAnalyzing && recommendation.isEmpty()) {
                        CircularProgressIndicator(color = PSSaffron, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("AI is analyzing your profile...", color = PSNavy, fontWeight = FontWeight.Medium)
                    } else {
                        Box(modifier = Modifier.background(PSCream, RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Text(
                                recommendation.ifEmpty { "Generating recommendation..." },
                                color = PSNavy,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { 
                                step = 0
                                answers.clear()
                                viewModel.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PSNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retake Test", color = PSWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchemeCard(scheme: CategoryScheme, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(scheme.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PSNavy)
                Text(scheme.desc, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = PSNavy)
        }
    }
}

private fun getSchemesForCategory(name: String): List<CategoryScheme> {
    return when(name) {
        "Agriculture" -> listOf(
            CategoryScheme("PM Kisan Nidhi", "Direct income support of ₹6000/year to farmers."),
            CategoryScheme("PM Fasal Bima", "Comprehensive crop insurance against natural risks."),
            CategoryScheme("Soil Health Card", "Optimize crop productivity with soil analysis."),
            CategoryScheme("Kisan Credit Card", "Low-interest loans for agricultural needs."),
            CategoryScheme("Paramparagat Krishi", "Support for organic farming and soil health.")
        )
        "Housing" -> listOf(
            CategoryScheme("PM Awas (Urban)", "Affordable housing for urban poor."),
            CategoryScheme("PM Awas (Rural)", "Housing for all in rural areas by 2024."),
            CategoryScheme("CLSS Subsidy", "Interest subsidy on home loans for EWS/LIG."),
            CategoryScheme("PMAY Grameen", "Financial assistance for house construction.")
        )
        "Health" -> listOf(
            CategoryScheme("Ayushman Bharat", "₹5 Lakh health cover per family per year."),
            CategoryScheme("PM Jan Aushadhi", "Affordable quality generic medicines for all."),
            CategoryScheme("Mission Indradhanush", "Full immunization for children and pregnant women."),
            CategoryScheme("RSBY", "Health insurance for unorganized sector workers.")
        )
        "Education" -> listOf(
            CategoryScheme("Post Metric Scholarship", "Financial aid for students from SC/ST/OBC."),
            CategoryScheme("Means cum Merit", "Scholarship for meritorious students from poor families."),
            CategoryScheme("PM Research Fellowship", "Support for doctoral research in IITs/IISc.")
        )
        else -> emptyList()
    }
}
