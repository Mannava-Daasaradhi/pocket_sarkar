package com.pocketsarkar.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pocketsarkar.ui.navigation.Screen
import com.pocketsarkar.ui.theme.*

@Composable
fun HomeScreen(
    userPrefs: com.pocketsarkar.data.UserPreferences,
    onNavigate: (Screen) -> Unit
) {
    val strings = Localization.getStrings(userPrefs.userLanguage)
    var selectedOpportunity by remember { mutableStateOf<Opportunity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PSCream)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            HeaderSection(strings, userPrefs.userName ?: "Bhai")
            SarkarScoreDashboard(strings)
            OpportunitySection(strings, onCardClick = { selectedOpportunity = it })
            AajKiJaankari(strings)
            AiConsoleCard(strings, onNavigate)
        }

        if (selectedOpportunity != null) {
            OpportunityDetailDialog(
                opportunity = selectedOpportunity!!,
                onDismiss = { selectedOpportunity = null }
            )
        }
    }
}

@Composable
private fun HeaderSection(strings: AppStrings, userName: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "${strings.welcome}, $userName!",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            color = PSNavy,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = strings.subWelcome,
            style = MaterialTheme.typography.bodySmall,
            color = PSTextSecondary
        )
    }
}

@Composable
private fun SarkarScoreDashboard(strings: AppStrings) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    drawArc(
                        color = PSNavy.copy(alpha = 0.1f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = PSSaffron,
                        startAngle = 135f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("6/10", style = MaterialTheme.typography.displaySmall, color = PSNavy, fontWeight = FontWeight.Bold)
                    Text(strings.scoreTitle, style = MaterialTheme.typography.labelSmall, color = PSTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(strings.pendingAmount, style = MaterialTheme.typography.titleMedium, color = PSSaffron, fontWeight = FontWeight.Bold)
            Text(strings.schemesFound, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
        }
    }
}

data class Opportunity(val title: String, val amount: String, val eligibility: String, val details: String, val docs: String)

@Composable
private fun OpportunitySection(strings: AppStrings, onCardClick: (Opportunity) -> Unit) {
    val opportunities = listOf(
        Opportunity(
            "PM Kisan Nidhi", "₹6,000/yr",
            "Small and marginal farmers with up to 2 hectares of land.",
            "Direct income support of ₹6,000 per year in 3 installments.",
            "Aadhaar, Land ownership papers, Bank account."
        ),
        Opportunity(
            "PM Awas Yojana", "₹2.67 Lakh",
            "Low-income families who do not own a pucca house.",
            "Subsidy for house construction or purchase interest.",
            "Aadhaar, Income certificate, Property papers (if any)."
        ),
        Opportunity(
            "Ayushman Bharat", "₹5 Lakh",
            "Vulnerable families listed in SECC-2011 database.",
            "Free health insurance for hospital treatments.",
            "Ration card, Aadhaar, PM Letter (if received)."
        )
    )

    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Text(strings.newOpportunities, style = MaterialTheme.typography.labelMedium, color = PSNavy, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(opportunities) { opp ->
                OpportunityCard(opp, onClick = { onCardClick(opp) })
            }
        }
    }
}

@Composable
private fun OpportunityCard(opp: Opportunity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(200.dp).height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        border = BorderStroke(1.dp, PSBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(PSNavy))
            Column(modifier = Modifier.padding(16.dp).padding(start = 8.dp)) {
                Text(opp.title, style = MaterialTheme.typography.titleSmall, color = PSNavy)
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(opp.amount, style = MaterialTheme.typography.titleMedium, color = PSSaffron, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PSTextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OpportunityDetailDialog(opportunity: Opportunity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = PSWhite),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(opportunity.title, style = MaterialTheme.typography.headlineSmall, color = PSNavy, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = null) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailItem("Benefit Amount", opportunity.amount, PSSaffron)
                DetailItem("Who is Eligible?", opportunity.eligibility, PSNavy)
                DetailItem("Details", opportunity.details, PSTextPrimary)
                DetailItem("Documents Needed", opportunity.docs, PSGreen)
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PSNavy)
                ) { Text("Samajh Gaya") }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, content: String, contentColor: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = PSTextSecondary)
        Text(content, style = MaterialTheme.typography.bodyMedium, color = contentColor)
    }
}

@Composable
private fun AajKiJaankari(strings: AppStrings) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = PSNavy), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = PSSaffron, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(strings.todayInfo, style = MaterialTheme.typography.labelSmall, color = PSSaffron)
                Text(strings.placeholderNote, style = MaterialTheme.typography.bodyMedium, color = PSWhite)
            }
        }
    }
}

@Composable
private fun AiConsoleCard(strings: AppStrings, onNavigate: (Screen) -> Unit) {
    Card(
        onClick = { onNavigate(Screen.TestAi) },
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, PSSaffron.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Redesigned AI Spark Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PSSaffron.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val center = center
                    val radius = size.minDimension / 2
                    
                    // Draw a sparkling neural hub
                    drawCircle(color = PSSaffron, radius = radius * 0.4f)
                    for (i in 0..7) {
                        val angle = i * 45f
                        val startX = center.x + (radius * 0.5f) * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                        val startY = center.y + (radius * 0.5f) * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                        val endX = center.x + radius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                        val endY = center.y + radius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                        drawLine(
                            color = PSSaffron,
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(strings.aiConsoleTitle, style = MaterialTheme.typography.titleMedium, color = PSNavy, fontWeight = FontWeight.Bold)
                Text(strings.aiConsoleSub, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PSSaffron)
        }
    }
}