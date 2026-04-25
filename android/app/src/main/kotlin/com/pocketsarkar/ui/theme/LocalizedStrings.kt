package com.pocketsarkar.ui.theme

data class AppStrings(
    val welcome: String,
    val subWelcome: String,
    val scoreTitle: String,
    val pendingAmount: String,
    val schemesFound: String,
    val newOpportunities: String,
    val todayInfo: String,
    val aiConsoleTitle: String,
    val aiConsoleSub: String,
    val startButton: String,
    val placeholderNote: String,
    val documentDecoder: String,
    val cameraAccess: String,
    val allowCamera: String,
    val uploadDoc: String,
    val pasteText: String,
    // Bottom Bar
    val navHome: String,
    val navDecoder: String,
    val navSchemes: String,
    val navRights: String,
    val navProfile: String
)

object Localization {
    private val english = AppStrings(
        welcome = "Welcome",
        subWelcome = "Your Government, Your Language",
        scoreTitle = "Sarkar Score",
        pendingAmount = "₹18,500 still pending",
        schemesFound = "3 new schemes found for you",
        newOpportunities = "New Opportunities",
        todayInfo = "Today's Information",
        aiConsoleTitle = "AI Test Console",
        aiConsoleSub = "Test your local AI server",
        startButton = "Get Started",
        placeholderNote = "Ration card update deadline is June 30th.",
        documentDecoder = "Document Decoder",
        cameraAccess = "Camera access needed to scan documents",
        allowCamera = "Allow Camera",
        uploadDoc = "Upload PDF / Image",
        pasteText = "Paste Text",
        navHome = "Home",
        navDecoder = "Decoder",
        navSchemes = "Schemes",
        navRights = "Rights",
        navProfile = "Profile"
    )

    private val hindi = AppStrings(
        welcome = "नमस्ते",
        subWelcome = "आपकी सरकार, आपकी भाषा",
        scoreTitle = "सरकार स्कोर",
        pendingAmount = "₹18,500 अभी भी बाकी है",
        schemesFound = "आपके लिए 3 नई योजनाएं मिली हैं",
        newOpportunities = "नई योजनाएं",
        todayInfo = "आज की जानकारी",
        aiConsoleTitle = "AI टेस्ट कंसोल",
        aiConsoleSub = "अपने स्थानीय AI सर्वर का परीक्षण करें",
        startButton = "शुरू करें",
        placeholderNote = "राशन कार्ड अपडेट करने की अंतिम तिथि 30 जून है।",
        documentDecoder = "दस्तावेज़ डिकोडर",
        cameraAccess = "दस्तावेज़ स्कैन करने के लिए कैमरा एक्सेस की आवश्यकता है",
        allowCamera = "कैमरा अनुमति दें",
        uploadDoc = "पीडीएफ / इमेज अपलोड करें",
        pasteText = "टेक्स्ट पेस्ट करें",
        navHome = "होम",
        navDecoder = "डिकोडर",
        navSchemes = "योजनाएं",
        navRights = "अधिकार",
        navProfile = "प्रोफ़ाइल"
    )

    private val telugu = AppStrings(
        welcome = "నమస్కారం",
        subWelcome = "మీ ప్రభుత్వం, మీ భాష",
        scoreTitle = "సర్కార్ స్కోర్",
        pendingAmount = "₹18,500 ఇంకా పెండింగ్‌లో ఉంది",
        schemesFound = "మీ కోసం 3 కొత్త పథకాలు కనుగొనబడ్డాయి",
        newOpportunities = "కొత్త అవకాశాలు",
        todayInfo = "నేటి సమాచారం",
        aiConsoleTitle = "AI టెస్ట్ కన్సోల్",
        aiConsoleSub = "మీ లోకల్ AI సర్వర్ కనెక్షన్‌ని పరీక్షించండి",
        startButton = "ప్రారంభించండి",
        placeholderNote = "రేషన్ కార్డ్ అప్‌డేట్ గడువు జూన్ 30.",
        documentDecoder = "డాక్యుమెంట్ డీకోడర్",
        cameraAccess = "పత్రాలను స్కాన్ చేయడానికి కెమెరా యాక్సెస్ అవసరం",
        allowCamera = "కెమెరాను అనుమతించు",
        uploadDoc = "PDF / చిత్రాన్ని అప్‌లోడ్ చేయండి",
        pasteText = "వచనాన్ని అతికించండి",
        navHome = "హోమ్",
        navDecoder = "డీకోడర్",
        navSchemes = "పథకాలు",
        navRights = "హక్కులు",
        navProfile = "ప్రొఫైల్"
    )

    private fun createFallback(welcome: String, sub: String) = english.copy(welcome = welcome, subWelcome = sub)

    fun getStrings(language: String): AppStrings {
        return when {
            language.contains("Hindi", ignoreCase = true) -> hindi
            language.contains("Telugu", ignoreCase = true) -> telugu
            language.contains("Bengali", ignoreCase = true) -> createFallback("স্বাগতম", "আপনার সরকার, আপনার ভাষা")
            language.contains("Marathi", ignoreCase = true) -> createFallback("स्वागत आहे", "तुमचे सरकार, तुमची भाषा")
            language.contains("Tamil", ignoreCase = true) -> createFallback("வரவேற்பு", "உங்கள் அரசு, உங்கள் மொழி")
            language.contains("Gujarati", ignoreCase = true) -> createFallback("સ્વાગત", "તમારી સરકાર, તમારી ભાષા")
            language.contains("Kannada", ignoreCase = true) -> createFallback("ಸ್ವಾಗತ", "ನಿಮ್ಮ ಸರ್ಕಾರ, ನಿಮ್ಮ ಭಾಷೆ")
            language.contains("Malayalam", ignoreCase = true) -> createFallback("സ്വാഗതം", "നിങ്ങളുടെ സർക്കാർ, നിങ്ങളുടെ ഭാഷ")
            language.contains("Punjabi", ignoreCase = true) -> createFallback("ਸੁਆਗਤ ਹੈ", "ਤੁਹਾਡੀ ਸਰਕਾਰ, ਤੁਹਾਡੀ ਭਾਸ਼ਾ")
            language.contains("Odia", ignoreCase = true) -> createFallback("ସ୍ୱାଗତ", "ଆପଣଙ୍କ ସରକାର, ଆପଣଙ୍କ ଭାଷା")
            language.contains("Assamese", ignoreCase = true) -> createFallback("স্বাগতম", "আপোনাৰ চৰকাৰ, আপোনাৰ ভাষা")
            language.contains("Kashmiri", ignoreCase = true) -> createFallback("خوش آمدید", "تہہنز سرکار، تہہنز زبان")
            language.contains("Maithili", ignoreCase = true) -> createFallback("स्वागत अछि", "अहाँक सरकार, अहाँक भाषा")
            language.contains("Santali", ignoreCase = true) -> createFallback("ᱥᱟᱹᱜᱩᱱ ᱫᱟᱨᱟᱢ", "ᱟᱢᱟᱜ ᱥᱚᱨᱠᱟᱨ, ᱟᱢᱟᱜ ᱯᱟᱹᱨᱥᱤ")
            else -> english
        }
    }
}
