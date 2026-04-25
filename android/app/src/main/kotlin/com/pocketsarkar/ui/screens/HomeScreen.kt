package com.pocketsarkar.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketsarkar.ui.viewmodels.BriefingViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pocketsarkar.data.MockData
import com.pocketsarkar.ui.navigation.Screen
import com.pocketsarkar.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    userPrefs: com.pocketsarkar.data.UserPreferences,
    onMenuClick: () -> Unit,
    onNavigate: (String) -> Unit,
    briefingViewModel: BriefingViewModel = hiltViewModel()
) {
    val strings = Localization.getStrings(userPrefs.userLanguage)
    var selectedOpportunity by remember { mutableStateOf<Opportunity?>(null) }
    val aiAlerts by briefingViewModel.alerts.collectAsState()
    val isLoading by briefingViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        briefingViewModel.generateDailyBriefing(userPrefs.userLanguage)
    }

    Box(modifier = Modifier.fillMaxSize().background(PSCream)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            HeaderSection(strings, userPrefs.userName ?: "Sathi", onMenuClick)
            
            AnimatedEntrance(delay = 100) {
                SarkarScoreDashboard(strings)
            }
            
            AnimatedEntrance(delay = 300) {
                OpportunitySection(strings, onCardClick = { selectedOpportunity = it })
            }
            
            AnimatedEntrance(delay = 500) {
                AajKiJaankari(strings, aiAlerts, isLoading)
            }
            
            AnimatedEntrance(delay = 700) {
                AiConsoleCard(strings, onNavigate)
            }
        }

        if (selectedOpportunity != null) {
            OpportunityDetailDialog(
                strings = strings,
                opportunity = selectedOpportunity!!,
                onNavigate = onNavigate,
                onDismiss = { selectedOpportunity = null }
            )
        }
    }
}

@Composable
private fun HeaderSection(strings: AppStrings, userName: String, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick, modifier = Modifier.background(PSWhite, CircleShape)) {
            Icon(Icons.Default.Menu, null, tint = PSNavy)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "${strings.welcome}, $userName!",
                style = MaterialTheme.typography.titleLarge,
                color = PSNavy,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = strings.subWelcome,
                style = MaterialTheme.typography.bodySmall,
                color = PSTextSecondary
            )
        }
    }
}

@Composable
private fun SarkarScoreDashboard(strings: AppStrings) {
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(400)
        animatedScore.animateTo(
            targetValue = 180f, // 6/10 of 300 degrees
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    drawArc(
                        color = PSNavy.copy(alpha = 0.05f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = PSSaffron,
                        startAngle = 135f,
                        sweepAngle = animatedScore.value * 270f / 300f, 
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("6/10", style = MaterialTheme.typography.displayMedium, color = PSNavy, fontWeight = FontWeight.Bold)
                    Text(strings.scoreTitle, style = MaterialTheme.typography.labelMedium, color = PSTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(strings.pendingAmount, style = MaterialTheme.typography.titleLarge, color = PSSaffron, fontWeight = FontWeight.Bold)
            Text(strings.schemesFound, style = MaterialTheme.typography.bodyMedium, color = PSTextSecondary)
        }
    }
}

@Composable
private fun AnimatedEntrance(
    delay: Int,
    content: @Composable () -> Unit
) {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay.toLong())
        visible.value = true
    }
    
    val alphaValue by animateFloatAsState(
        targetValue = if (visible.value) 1f else 0f,
        animationSpec = tween(600), label = ""
    )
    val translateYValue by animateDpAsState(
        targetValue = if (visible.value) 0.dp else 20.dp,
        animationSpec = tween(600), label = ""
    )

    Box(modifier = Modifier.graphicsLayer {
        alpha = alphaValue
        translationY = translateYValue.toPx()
    }) {
        content()
    }
}

data class Opportunity(val title: String, val amount: String, val eligibility: String, val details: String, val docs: String)

@Composable
private fun OpportunitySection(strings: AppStrings, onCardClick: (Opportunity) -> Unit) {
    val opportunities = listOf(
        Opportunity(strings.scheme1Title, strings.scheme1Amount, strings.scheme1Eligible, strings.scheme1Details, strings.scheme1Docs),
        Opportunity(strings.scheme2Title, strings.scheme2Amount, strings.scheme2Eligible, strings.scheme2Details, strings.scheme2Docs),
        Opportunity(strings.scheme3Title, strings.scheme3Amount, strings.scheme3Eligible, strings.scheme3Details, strings.scheme3Docs)
    )

    Column(modifier = Modifier.padding(top = 32.dp)) {
        Text(
            text = strings.newOpportunities,
            style = MaterialTheme.typography.titleLarge,
            color = PSNavy,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(opportunities) { opp ->
                OpportunityCard(opp, onCardClick)
            }
        }
    }
}

@Composable
private fun OpportunityCard(opp: Opportunity, onClick: (Opportunity) -> Unit) {
    Card(
        modifier = Modifier.size(width = 240.dp, height = 140.dp).clickable { onClick(opp) },
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PSBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = opp.title, style = MaterialTheme.typography.titleMedium, color = PSNavy, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = opp.amount, style = MaterialTheme.typography.titleLarge, color = PSSaffron, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = PSSaffron, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AajKiJaankari(strings: AppStrings, alerts: List<com.pocketsarkar.data.SchemeAlert>, isLoading: Boolean) {
    val pagerState = rememberPagerState(pageCount = { if (alerts.isEmpty()) 1 else alerts.size })

    Column(modifier = Modifier.padding(top = 32.dp)) {
        Text(
            text = strings.todayInfo,
            style = MaterialTheme.typography.titleLarge,
            color = PSNavy,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        if (isLoading && alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(110.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PSSaffron)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 12.dp
            ) { page ->
                val alert = if (alerts.isEmpty()) {
                    com.pocketsarkar.data.SchemeAlert("AI Loading...", "The local AI is generating updates for you.", "Live", com.pocketsarkar.data.AlertType.UPDATE)
                } else {
                    alerts[page]
                }
                Card(
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = PSNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(PSSaffron, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = PSNavy, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(alert.title, color = PSSaffron, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(alert.description, color = PSWhite, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(alert.date, color = PSWhite.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiConsoleCard(strings: AppStrings, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
            .clickable { onNavigate(Screen.Decoder.route) },
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PSBorder)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(strings.aiConsoleTitle, style = MaterialTheme.typography.titleMedium, color = PSNavy, fontWeight = FontWeight.Bold)
                Text(strings.aiConsoleSub, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
            }
            Button(
                onClick = { onNavigate(Screen.Decoder.route) },
                colors = ButtonDefaults.buttonColors(containerColor = PSNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(strings.startButton, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun OpportunityDetailDialog(
    strings: AppStrings,
    opportunity: Opportunity,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = PSWhite)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = opportunity.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = PSNavy,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.background(PSCream, CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = PSNavy)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                DetailItem(strings.benefitAmount, opportunity.amount, PSSaffron)
                DetailItem(strings.whoIsEligible, opportunity.eligibility, PSNavy)
                DetailItem(strings.details, opportunity.details, PSTextPrimary)
                DetailItem(strings.docsNeeded, opportunity.docs, Color(0xFF2E7D32))

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PSNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(strings.closeButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { 
                        onDismiss()
                        onNavigate("guide/${opportunity.title}")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    border = BorderStroke(1.dp, PSNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("View How to Apply", color = PSNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = PSTextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
    }
}