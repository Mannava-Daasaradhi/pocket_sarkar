package com.pocketsarkar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsarkar.data.UserPreferences
import com.pocketsarkar.ui.navigation.Screen
import com.pocketsarkar.ui.theme.*

@Composable
fun HomeScreen(
    userPrefs: com.pocketsarkar.data.UserPreferences,
    onNavigate: (Screen) -> Unit
) {
    val userName = userPrefs.userName?.takeIf { it.isNotBlank() } ?: "Saathi"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PSCream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Surface(color = PSNavy) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
                Text(
                    text = "Namaste, $userName 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PSWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Aapki sarkar, aapki bhasha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PSWhite.copy(alpha = 0.75f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Main feature tiles ───────────────────────────────────────────────
        Text(
            text = "Kya karna chahte hain?",
            style = MaterialTheme.typography.labelLarge,
            color = PSTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureTile(
                icon        = Icons.Default.Star,
                iconBg      = Color(0xFFFFF3E0),
                iconTint    = PSSaffron,
                title       = "Sarkari Yojanaen",
                subtitle    = "Apne liye scheme dhundhein aur samjhein",
                onClick     = { onNavigate(Screen.Schemes) }
            )
            FeatureTile(
                icon        = Icons.Default.CameraAlt,
                iconBg      = Color(0xFFE8F5E9),
                iconTint    = PSGreen,
                title       = "Document Decoder",
                subtitle    = "Koi bhi kaagaz Hindi mein samjhein",
                onClick     = { onNavigate(Screen.Decoder) }
            )
            FeatureTile(
                icon        = Icons.Default.Shield,
                iconBg      = Color(0xFFE3F2FD),
                iconTint    = Color(0xFF1976D2),
                title       = "Aapke Adhikar",
                subtitle    = "Kanoon aur naagrik adhikar jaanein",
                onClick     = { onNavigate(Screen.Rights) }
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Quick scheme tips ────────────────────────────────────────────────
        Text(
            text = "Jaanene layak",
            style = MaterialTheme.typography.labelLarge,
            color = PSTextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoCard(
                emoji = "🌾",
                title = "PM Kisan Samman Nidhi",
                body  = "Chhote kisan parivaron ko har saal ₹6,000 milte hain — teen kiston mein."
            )
            InfoCard(
                emoji = "🏥",
                title = "Ayushman Bharat",
                body  = "Har saal ₹5 lakh tak ka muft ilaaj — 50 crore se zyada log eligible hain."
            )
            InfoCard(
                emoji = "🏠",
                title = "PM Awas Yojana",
                body  = "Pakke ghar ke liye subsidy — urban aur rural dono ke liye available."
            )
            InfoCard(
                emoji = "👧",
                title = "Sukanya Samriddhi",
                body  = "Beti ke liye savings scheme — 8.2% byaaj, tax free."
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Tip box ──────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = PSNavy),
            shape  = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Tip",
                        style = MaterialTheme.typography.labelSmall,
                        color = PSSaffron,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Scheme Explainer se koi bhi yojana ke baare mein seedha poochh sakte hain — Hindi mein.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PSWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureTile(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PSWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = PSNavy, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PSTextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = PSTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun InfoCard(emoji: String, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color  = PSWhite,
        shape  = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, PSBorder)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = PSNavy,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = PSTextSecondary
                )
            }
        }
    }
}