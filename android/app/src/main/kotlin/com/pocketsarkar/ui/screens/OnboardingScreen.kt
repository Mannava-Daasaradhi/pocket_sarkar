package com.pocketsarkar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketsarkar.R
import com.pocketsarkar.data.UserPreferences
import com.pocketsarkar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    userPrefs: UserPreferences,
    onComplete: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf(userPrefs.userLanguage) }

    val languages = listOf(
        "English", "Hindi (हिन्दी)", "Telugu (తెలుగు)", "Bengali (বাংলা)",
        "Marathi (मराठी)", "Tamil (தமிழ்)", "Gujarati (ગુજરાતી)", "Kannada (ಕನ್ನಡ)",
        "Odia (ଓଡ଼ିଆ)", "Malayalam (മലയാളം)", "Punjabi (ਪੰਜਾਬੀ)", "Assamese (ଅସମୀୟା)",
        "Maithili (मैथिली)", "Santali (संताली)", "Kashmiri (کٲشُر)"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = PSCream) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            Icon(
                painter = painterResource(id = R.drawable.ic_pocket_sarkar_icon),
                contentDescription = "Pocket Sarkar Logo",
                modifier = Modifier.size(100.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Swagat Hai!",
                style = MaterialTheme.typography.headlineMedium,
                color = PSNavy,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Welcome to Pocket Sarkar",
                style = MaterialTheme.typography.bodyLarge,
                color = PSTextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Name Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Aapka Naam? (Your Name)",
                    style = MaterialTheme.typography.labelLarge,
                    color = PSNavy,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PSNavy,
                        unfocusedBorderColor = PSBorder,
                        focusedLabelColor = PSNavy
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Language Selection
            Text(
                "Select Language",
                style = MaterialTheme.typography.labelLarge,
                color = PSNavy,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { lang ->
                    LanguageChip(
                        name = lang,
                        isSelected = selectedLang == lang,
                        onClick = { selectedLang = lang }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        userPrefs.userName = name
                        userPrefs.userLanguage = selectedLang
                        userPrefs.isOnboardingComplete = true
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PSNavy,
                    contentColor = PSWhite,
                    disabledContainerColor = PSNavy.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Shuru Karein (Get Started)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LanguageChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) PSNavy else PSWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) PSNavy else PSBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = if (isSelected) PSWhite else PSNavy,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PSSaffron,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
