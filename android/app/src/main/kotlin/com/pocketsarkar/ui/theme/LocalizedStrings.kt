package com.pocketsarkar.ui.theme

data class AppStrings(
    // Decoder screen
    val documentDecoder: String,
    val cameraAccess: String,
    val allowCamera: String,
    val uploadDoc: String,
    val pasteText: String,
    // Bottom nav
    val navHome: String,
    val navDecoder: String,
    val navSchemes: String,
    val navRights: String,
    val navProfile: String,
    // Misc
    val startButton: String,
)

object Localization {
    private val english = AppStrings(
        documentDecoder = "Document Decoder",
        cameraAccess    = "Camera access needed to scan documents",
        allowCamera     = "Allow Camera",
        uploadDoc       = "Upload PDF / Image",
        pasteText       = "Paste Text",
        navHome         = "Home",
        navDecoder      = "Decoder",
        navSchemes      = "Schemes",
        navRights       = "Rights",
        navProfile      = "Profile",
        startButton     = "Get Started",
    )

    private val hindi = AppStrings(
        documentDecoder = "दस्तावेज़ डिकोडर",
        cameraAccess    = "दस्तावेज़ स्कैन करने के लिए कैमरा एक्सेस चाहिए",
        allowCamera     = "कैमरा अनुमति दें",
        uploadDoc       = "PDF / इमेज अपलोड करें",
        pasteText       = "टेक्स्ट पेस्ट करें",
        navHome         = "होम",
        navDecoder      = "डिकोडर",
        navSchemes      = "योजनाएं",
        navRights       = "अधिकार",
        navProfile      = "प्रोफ़ाइल",
        startButton     = "शुरू करें",
    )

    private val telugu = AppStrings(
        documentDecoder = "డాక్యుమెంట్ డీకోడర్",
        cameraAccess    = "పత్రాలను స్కాన్ చేయడానికి కెమెరా యాక్సెస్ అవసరం",
        allowCamera     = "కెమెరాను అనుమతించు",
        uploadDoc       = "PDF / చిత్రాన్ని అప్‌లోడ్ చేయండి",
        pasteText       = "వచనాన్ని అతికించండి",
        navHome         = "హోమ్",
        navDecoder      = "డీకోడర్",
        navSchemes      = "పథకాలు",
        navRights       = "హక్కులు",
        navProfile      = "ప్రొఫైల్",
        startButton     = "ప్రారంభించండి",
    )

    private fun fallback(
        home: String, decoder: String, schemes: String, rights: String, profile: String, start: String
    ) = english.copy(
        navHome = home, navDecoder = decoder, navSchemes = schemes,
        navRights = rights, navProfile = profile, startButton = start,
    )

    fun getStrings(language: String): AppStrings = when {
        language.contains("Hindi",     ignoreCase = true) -> hindi
        language.contains("Telugu",    ignoreCase = true) -> telugu
        language.contains("Bengali",   ignoreCase = true) -> fallback("হোম","ডিকোডার","প্রকল্প","অধিকার","প্রোফাইল","শুরু করুন")
        language.contains("Marathi",   ignoreCase = true) -> fallback("होम","डिकोडर","योजना","हक्क","प्रोफाइल","सुरू करा")
        language.contains("Tamil",     ignoreCase = true) -> fallback("முகப்பு","டிகோடர்","திட்டங்கள்","உரிமைகள்","சுயவிவரம்","தொடங்கு")
        language.contains("Gujarati",  ignoreCase = true) -> fallback("હોમ","ડીકોડર","યોજનાઓ","અધિકારો","પ્રોફાઇલ","શરૂ કરો")
        language.contains("Kannada",   ignoreCase = true) -> fallback("ಮನೆ","ಡಿಕೋಡರ್","ಯೋಜನೆಗಳು","ಹಕ್ಕುಗಳು","ಪ್ರೊಫೈಲ್","ಪ್ರಾರಂಭಿಸಿ")
        language.contains("Malayalam", ignoreCase = true) -> fallback("ഹോം","ഡീകോഡർ","പദ്ധതികൾ","അവകാശങ്ങൾ","പ്രൊഫൈൽ","തുടങ്ങുക")
        language.contains("Punjabi",   ignoreCase = true) -> fallback("ਹੋਮ","ਡੀਕੋਡਰ","ਯੋਜਨਾਵਾਂ","ਅਧਿਕਾਰ","ਪ੍ਰੋਫਾਈਲ","ਸ਼ੁਰੂ ਕਰੋ")
        language.contains("Odia",      ignoreCase = true) -> fallback("ହୋମ","ଡିକୋଡର","ଯୋଜନା","ଅଧିକାର","ପ୍ରୋଫାଇଲ","ଆରମ୍ଭ")
        language.contains("Assamese",  ignoreCase = true) -> fallback("হোম","ডিকোডার","আঁচনি","অধিকাৰ","প্ৰ'ফাইল","আৰম্ভ")
        else -> english
    }
}