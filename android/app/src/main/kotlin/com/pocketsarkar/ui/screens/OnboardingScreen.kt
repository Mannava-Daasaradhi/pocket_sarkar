package com.pocketsarkar.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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

private data class LangOption(
    val code: String,
    val label: String,
    val welcomeText: String,
    val nameLabel: String,
    val namePlaceholder: String,
    val buttonText: String,
    val selectLangLabel: String,
)

private val LANGUAGES = listOf(
    LangOption("English",   "English",    "Welcome!",           "Your Name",            "Enter your name",              "Get Started",       "Select Language"),
    LangOption("Hindi",     "हिन्दी",      "नमस्ते!",             "आपका नाम",             "अपना नाम लिखें",               "शुरू करें",          "भाषा चुनें"),
    LangOption("Telugu",    "తెలుగు",     "నమస్కారం!",           "మీ పేరు",              "మీ పేరు రాయండి",               "ప్రారంభించండి",     "భాష ఎంచుకోండి"),
    LangOption("Bengali",   "বাংলা",      "স্বাগতম!",            "আপনার নাম",            "আপনার নাম লিখুন",              "শুরু করুন",          "ভাষা বেছে নিন"),
    LangOption("Marathi",   "मराठी",      "स्वागत आहे!",         "तुमचे नाव",            "तुमचे नाव लिहा",               "सुरू करा",           "भाषा निवडा"),
    LangOption("Tamil",     "தமிழ்",      "வணக்கம்!",            "உங்கள் பெயர்",         "உங்கள் பெயரை உள்ளிடவும்",     "தொடங்கு",           "மொழி தேர்வு"),
    LangOption("Gujarati",  "ગુજરાતી",    "સ્વાગત છે!",          "તમારું નામ",           "તમારું નામ લખો",               "શરૂ કરો",            "ભાષા પસંદ કરો"),
    LangOption("Kannada",   "ಕನ್ನಡ",      "ಸ್ವಾಗತ!",             "ನಿಮ್ಮ ಹೆಸರು",          "ನಿಮ್ಮ ಹೆಸರನ್ನು ನಮೂದಿಸಿ",      "ಪ್ರಾರಂಭಿಸಿ",        "ಭಾಷೆ ಆಯ್ಕೆ"),
    LangOption("Odia",      "ଓଡ଼ିଆ",       "ସ୍ୱାଗତ!",             "ଆପଣଙ୍କ ନାମ",           "ଆପଣଙ୍କ ନାମ ଲିଖନ୍ତୁ",          "ଆରମ୍ଭ କରନ୍ତୁ",      "ଭାଷା ବାଛନ୍ତୁ"),
    LangOption("Malayalam", "മലയാളം",     "സ്വാഗതം!",            "നിങ്ങളുടെ പേര്",       "നിങ്ങളുടെ പേര് ടൈപ്പ് ചെയ്യുക","തുടങ്ങുക",          "ഭാഷ തിരഞ്ഞെടുക്കൂ"),
    LangOption("Punjabi",   "ਪੰਜਾਬੀ",      "ਜੀ ਆਇਆਂ ਨੂੰ!",       "ਤੁਹਾਡਾ ਨਾਮ",           "ਆਪਣਾ ਨਾਮ ਲਿਖੋ",               "ਸ਼ੁਰੂ ਕਰੋ",          "ਭਾਸ਼ਾ ਚੁਣੋ"),
    LangOption("Assamese",  "অসমীয়া",    "স্বাগতম!",            "আপোনাৰ নাম",           "আপোনাৰ নাম লিখক",             "আৰম্ভ কৰক",         "ভাষা বাছক"),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    userPrefs: UserPreferences,
    onComplete: () -> Unit
) {
    var selectedLang by remember {
        mutableStateOf(LANGUAGES.find { it.code == userPrefs.userLanguage } ?: LANGUAGES[0])
    }
    var name by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = PSCream) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pocket_sarkar_icon),
                contentDescription = "Pocket Sarkar",
                modifier = Modifier.size(88.dp),
                tint = Color.Unspecified
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = selectedLang.welcomeText,
                style = MaterialTheme.typography.headlineMedium,
                color = PSNavy,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Pocket Sarkar",
                style = MaterialTheme.typography.bodyLarge,
                color = PSTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // Step 1 — Language (shown first so welcome text updates immediately)
            Text(
                text = selectedLang.selectLangLabel,
                style = MaterialTheme.typography.labelLarge,
                color = PSNavy,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LANGUAGES.forEach { lang ->
                    LanguageChip(
                        label = lang.label,
                        isSelected = selectedLang.code == lang.code,
                        onClick = { selectedLang = lang }
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // Step 2 — Name (label and placeholder now reflect chosen language)
            Text(
                text = selectedLang.nameLabel,
                style = MaterialTheme.typography.labelLarge,
                color = PSNavy,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(selectedLang.namePlaceholder) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor    = PSTextPrimary,
                    unfocusedTextColor  = PSTextPrimary,
                    focusedBorderColor  = PSNavy,
                    unfocusedBorderColor = PSBorder,
                    focusedPlaceholderColor   = PSTextSecondary,
                    unfocusedPlaceholderColor = PSTextSecondary,
                    cursorColor = PSNavy,
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) {
                        userPrefs.userName     = trimmed
                        userPrefs.userLanguage = selectedLang.code
                        userPrefs.isOnboardingComplete = true
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PSNavy,
                    contentColor   = PSWhite,
                    disabledContainerColor = PSNavy.copy(alpha = 0.3f),
                    disabledContentColor   = PSWhite.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(selectedLang.buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LanguageChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color  = if (isSelected) PSNavy else PSWhite,
        shape  = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) PSNavy else PSBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isSelected) PSWhite else PSNavy,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.CheckCircle, null, tint = PSSaffron, modifier = Modifier.size(15.dp))
            }
        }
    }
}