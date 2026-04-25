package com.pocketsarkar.ui.theme

data class AppStrings(
    val welcome: String, val subWelcome: String, val scoreTitle: String, val pendingAmount: String,
    val schemesFound: String, val newOpportunities: String, val todayInfo: String, val aiConsoleTitle: String,
    val aiConsoleSub: String, val startButton: String, val placeholderNote: String, val documentDecoder: String,
    val cameraAccess: String, val allowCamera: String, val uploadDoc: String, val pasteText: String,
    val navHome: String, val navDecoder: String, val navSchemes: String, val navRights: String, val navProfile: String,
    val benefitAmount: String, val whoIsEligible: String, val details: String, val docsNeeded: String, val closeButton: String,
    val analyze: String, val cancel: String, val summary: String, val whatToDo: String, val scanAnother: String, val riskScore: String,
    val scheme1Title: String, val scheme1Amount: String, val scheme1Eligible: String, val scheme1Details: String, val scheme1Docs: String,
    val scheme2Title: String, val scheme2Amount: String, val scheme2Eligible: String, val scheme2Details: String, val scheme2Docs: String,
    val scheme3Title: String, val scheme3Amount: String, val scheme3Eligible: String, val scheme3Details: String, val scheme3Docs: String
)

object Localization {
    private val english = AppStrings(
        welcome = "Welcome", subWelcome = "Your government, your language.", scoreTitle = "Sarkar Score",
        pendingAmount = "₹18,500 benefit pending", schemesFound = "3 new opportunities identified",
        newOpportunities = "New Opportunities", todayInfo = "Daily Briefing", aiConsoleTitle = "AI Test Console",
        aiConsoleSub = "System Diagnostic", startButton = "Get Started", placeholderNote = "Ration card update deadline: June 30th.",
        documentDecoder = "Document Decoder", cameraAccess = "Camera access required", allowCamera = "Grant Access",
        uploadDoc = "Upload Document", pasteText = "Paste Text", navHome = "Home", navDecoder = "Scanner",
        navSchemes = "Benefits", navRights = "Rights", navProfile = "Account", benefitAmount = "Benefit Amount",
        whoIsEligible = "Eligibility Criteria", details = "Scheme Details", docsNeeded = "Required Documents",
        closeButton = "Got it", analyze = "Analyze", cancel = "Clear", summary = "Summary",
        whatToDo = "What to do?", scanAnother = "Scan Another", riskScore = "Risk Score",
        scheme1Title = "PM Kisan Nidhi", scheme1Amount = "₹6,000/year", scheme1Eligible = "Small and marginal farmers.",
        scheme1Details = "Direct income support in 3 installments.", scheme1Docs = "Aadhaar, Land papers, Bank account.",
        scheme2Title = "PM Awas Yojana", scheme2Amount = "₹2.67 Lakh", scheme2Eligible = "Families without a permanent house.",
        scheme2Details = "Subsidy for house construction.", scheme2Docs = "Aadhaar, Income certificate.",
        scheme3Title = "Ayushman Bharat", scheme3Amount = "₹5 Lakh", scheme3Eligible = "Low-income families.",
        scheme3Details = "Free health insurance for hospital treatments.", scheme3Docs = "Ration card, Aadhaar."
    )

    private val hindi = AppStrings(
        welcome = "नमस्ते", subWelcome = "आपकी सरकार, आपकी भाषा।", scoreTitle = "सरकार स्कोर",
        pendingAmount = "₹18,500 लाभ लंबित है", schemesFound = "3 नए अवसर मिले हैं",
        newOpportunities = "नए अवसर", todayInfo = "दैनिक जानकारी", aiConsoleTitle = "AI टेस्ट कंसोल",
        aiConsoleSub = "सिस्टम डायग्नोस्टिक", startButton = "शुरू करें", placeholderNote = "राशन कार्ड अपडेट की अंतिम तिथि: 30 जून।",
        documentDecoder = "दस्तावेज़ डिकोडर", cameraAccess = "कैमरा एक्सेस आवश्यक है", allowCamera = "अनुमति दें",
        uploadDoc = "दस्तावेज़ अपलोड करें", pasteText = "टेक्स्ट पेस्ट करें", navHome = "होम", navDecoder = "स्कैनर",
        navSchemes = "लाभ", navRights = "अधिकार", navProfile = "खाता", benefitAmount = "लाभ राशि",
        whoIsEligible = "पात्रता मानदंड", details = "योजना विवरण", docsNeeded = "आवश्यक दस्तावेज़",
        closeButton = "समझ गया", analyze = "विश्लेषण", cancel = "साफ करें", summary = "सारांश",
        whatToDo = "क्या करें?", scanAnother = "दूसरा स्कैन करें", riskScore = "जोखิม स्कोर",
        scheme1Title = "पीएम किसान निधि", scheme1Amount = "₹6,000/वर्ष", scheme1Eligible = "छोटे और सीमांत किसान।",
        scheme1Details = "3 किस्तों में सीधा आय सहायता।", scheme1Docs = "आधार, भूमि दस्तावेज, बैंक खाता।",
        scheme2Title = "पीएम आवास योजना", scheme2Amount = "₹2.67 लाख", scheme2Eligible = "बिना पक्के घर वाले परिवार।",
        scheme2Details = "घर निर्माण के लिए सब्सिडी।", scheme2Docs = "आधार, आय प्रमाण पत्र।",
        scheme3Title = "आयुष्मान भारत", scheme3Amount = "₹5 लाख", scheme3Eligible = "कम आय वाले परिवार।",
        scheme3Details = "अस्पताल के इलाज के लिए मुफ्त स्वास्थ्य बीमा।", scheme3Docs = "राशन कार्ड, आधार।"
    )

    private val telugu = AppStrings(
        welcome = "నమస్కారం", subWelcome = "మీ ప్రభుత్వం, మీ భాష.", scoreTitle = "సర్కార్ స్కోర్",
        pendingAmount = "₹18,500 ప్రయోజనం పెండింగ్‌లో ఉంది", schemesFound = "3 కొత్త అవకాశాలు గుర్తించబడ్డాయి",
        newOpportunities = "కొత్త అవకాశాలు", todayInfo = "రోజువారీ సమాచారం", aiConsoleTitle = "AI టెస్ట్ కన్సోల్",
        aiConsoleSub = "సిస్టమ్ డయాగ్నస్టిక్", startButton = "ప్రారంభించండి", placeholderNote = "రేషన్ కార్డ్ అప్‌డేట్ గడువు: జూన్ 30.",
        documentDecoder = "డాక్యుమెంట్ డీకోడర్", cameraAccess = "కెమెరా యాక్సెస్ అవసరం", allowCamera = "అనుమతించు",
        uploadDoc = "డాక్యుమెంట్ అప్‌లోడ్", pasteText = "టెక్స్ట్‌ని అతికించండి", navHome = "హోమ్", navDecoder = "స్కానర్",
        navSchemes = "ప్రయోజనాలు", navRights = "హక్కులు", navProfile = "ఖాతా", benefitAmount = "ప్రయోజనం మొత్తం",
        whoIsEligible = "అర్హత ప్రమాణాలు", details = "పథకం వివరాలు", docsNeeded = "అవసరమైన పత్రాలు",
        closeButton = "అర్థమైంది", analyze = "విశ్లేషించు", cancel = "తుడిచివేయి", summary = "సారాంశం",
        whatToDo = "ఏమి చేయాలి?", scanAnother = "మరోసారి స్కాన్ చేయి", riskScore = "ప్రమాద స్કોర్",
        scheme1Title = "పీఎం కిసాన్ నిధి", scheme1Amount = "₹6,000/సంవత్సరం", scheme1Eligible = "చిన్న మరియు సన్నకారు రైతులు.",
        scheme1Details = "3 వాయిదాలలో నేరుగా ఆదాయ మద్దతు.", scheme1Docs = "ఆధార్, భూమి పత్రాలు, బ్యాంక్ ఖాతా.",
        scheme2Title = "పీఎం ఆవాస్ యోజన", scheme2Amount = "₹2.67 లక్షలు", scheme2Eligible = "సొంత ఇల్లు లేని కుటుంబాలు.",
        scheme2Details = "ఇంటి నిర్మాణానికి సబ్సిడీ.", scheme2Docs = "ఆధార్, ఆదాయ ధృవీకరణ పత్రం.",
        scheme3Title = "ఆయుష్మాన్ భారత్", scheme3Amount = "₹5 లక్షలు", scheme3Eligible = "తక్కువ ఆదాయం ఉన్న కుటుంబాలు.",
        scheme3Details = "ఆసుపత్రి చికిత్సల కోసం ఉచిత ఆరోగ్య భീమా.", scheme3Docs = "రేషన్ కార్డ్, ఆధార్."
    )

    private val bengali = AppStrings(
        welcome = "স্বাগতম", subWelcome = "আপনার সরকার, আপনার ভাষা।", scoreTitle = "সরকার স্কোর",
        pendingAmount = "₹১৮,৫০০ সুবিধা বাকি আছে", schemesFound = "৩টি নতুন সুযোগ পাওয়া গেছে",
        newOpportunities = "নতুন সুযোগ", todayInfo = "দৈনিক তথ্য", aiConsoleTitle = "AI টেস্ট কনসোল",
        aiConsoleSub = "সিস্টেম ডায়াগনস্টিক", startButton = "শুরু করুন", placeholderNote = "রেশন কার্ড আপডেটের শেষ তারিখ: ৩০ জুন।",
        documentDecoder = "ডকুমেন্ট ডিকোডার", cameraAccess = "ক্যামেরা অ্যাক্সেস প্রয়োজন", allowCamera = "অনুমতি দিন",
        uploadDoc = "ডকুমেন্ট আপলোড করুন", pasteText = "টেক্সট পেস্ট করুন", navHome = "হোম", navDecoder = "স্ক্যানার",
        navSchemes = "সুবিধা", navRights = "অধিকার", navProfile = "অ্যাকাউন্ট", benefitAmount = "সুবিধার পরিমাণ",
        whoIsEligible = "যোগ্যতার মানদণ্ড", details = "প্রকল্পের বিবরণ", docsNeeded = "প্রয়োজনীয় নথি",
        closeButton = "বুঝেছি", analyze = "বিশ্লেষণ", cancel = "মুছে ফেলুন", summary = "সারসংক্ষেপ",
        whatToDo = "কি করতে হবে?", scanAnother = "আরেকটি স্ক্যান করুন", riskScore = "ঝুঁকির স্কোর",
        scheme1Title = "পিএম কিষাণ নিধি", scheme1Amount = "₹৬,০০০/বছর", scheme1Eligible = "ক্ষুদ্র ও প্রান্তিক চাষী।",
        scheme1Details = "৩টি কিস্তিতে সরাসরি আয় সহায়তা।", scheme1Docs = "আধার, জমির কাগজ, ব্যাঙ্ক অ্যাকাউন্ট।",
        scheme2Title = "পিএম আবাস যোজনা", scheme2Amount = "₹২.৬৭ লক্ষ", scheme2Eligible = "পাকা বাড়ি নেই এমন পরিবার।",
        scheme2Details = "বাড়ি তৈরির জন্য ভর্তুকি।", scheme2Docs = "আধার, আয়ের শংসাপত্র।",
        scheme3Title = "আয়ুষ্মান ভারত", scheme3Amount = "₹৫ লক্ষ", scheme3Eligible = "স্বল্প আয়ের পরিবার।",
        scheme3Details = "হাসপাতালে চিকিৎসার জন্য বিনামূল্যে স্বাস্থ্য বীমা।", scheme3Docs = "রেশন কার্ড, আধার।"
    )

    private val marathi = AppStrings(
        welcome = "स्वागत आहे", subWelcome = "तुमचे सरकार, तुमची भाषा.", scoreTitle = "सरकार स्कोअर",
        pendingAmount = "₹१८,५०० लाभ प्रलंबित आहे", schemesFound = "३ नवीन संधी आढळल्या आहेत",
        newOpportunities = "नवीन संधी", todayInfo = "दैनिक माहिती", aiConsoleTitle = "AI टेस्ट कन्सोल",
        aiConsoleSub = "सिस्टम डायग्नोस्टिक", startButton = "सुरू करा", placeholderNote = "रेशन कार्ड अपडेटची शेवटची तारीख: ३० जून.",
        documentDecoder = "दस्तऐवज डिकोडर", cameraAccess = "कॅमेरा प्रवेश आवश्यक आहे", allowCamera = "परवानगी द्या",
        uploadDoc = "दस्तऐवज अपलोड करा", pasteText = "मजकूर पेस्ट करा", navHome = "होम", navDecoder = "स्कॅनर",
        navSchemes = "लाभ", navRights = "हक्क", navProfile = "खाते", benefitAmount = "लाभाची रक्कम",
        whoIsEligible = "पात्रता निकष", details = "योजनेचा तपशील", docsNeeded = "आवश्यक कागदपत्रे",
        closeButton = "समजले", analyze = "विश्लेषण", cancel = "साफ करा", summary = "सारांश",
        whatToDo = "काय करावे?", scanAnother = "दुसरे स्कॅन करा", riskScore = "धोका स्कोअर",
        scheme1Title = "पीएम किसान निधी", scheme1Amount = "₹६,०००/वर्ष", scheme1Eligible = "अल्प आणि अत्यल्प भूधारक शेतकरी.",
        scheme1Details = "३ हप्त्यांमध्ये थेट उत्पन्न सहाय्य.", scheme1Docs = "आधार, जमिनीची कागदपत्रे, बँक खाते.",
        scheme2Title = "पीएम आवास योजना", scheme2Amount = "₹२.६७ लाख", scheme2Eligible = "पक्के घर नसलेली कुटुंबे.",
        scheme2Details = "घर बांधण्यासाठी अनुदान.", scheme2Docs = "आधार, उत्पन्नाचा दाखला.",
        scheme3Title = "आयुष्मान भारत", scheme3Amount = "₹५ लाख", scheme3Eligible = "कमी उत्पन्न असलेली कुटुंबे.",
        scheme3Details = "रुग्णालयातील उपचारांसाठी मोफत आरोग्य विमा.", scheme3Docs = "रेशन कार्ड, आधार।"
    )

    private val tamil = AppStrings(
        welcome = "வரவேற்பு", subWelcome = "உங்கள் அரசாங்கம், உங்கள் மொழி.", scoreTitle = "சர்க்கார் ஸ்கோர்",
        pendingAmount = "₹18,500 பலன் நிலுவையில் உள்ளது", schemesFound = "3 புதிய வாய்ப்புகள் கண்டறியப்பட்டுள்ளன",
        newOpportunities = "புதிய வாய்ப்புகள்", todayInfo = "தினசரி தகவல்", aiConsoleTitle = "AI டெஸ்ட் கன்சோல்",
        aiConsoleSub = "சிஸ்டம் கண்டறிதல்", startButton = "தொடங்கு", placeholderNote = "ரேஷன் கார்டு அப்டேட் கடைசி தேதி: ஜூன் 30.",
        documentDecoder = "ஆவண டிகோடர்", cameraAccess = "கேமரா அனுமதி தேவை", allowCamera = "அனுமதி அளி",
        uploadDoc = "ஆவணத்தை பதிவேற்று", pasteText = "உரையை ஒட்டு", navHome = "முகப்பு", navDecoder = "ஸ்கேனர்",
        navSchemes = "பலன்கள்", navRights = "உரிமைகள்", navProfile = "கணக்கு", benefitAmount = "பலன் தொகை",
        whoIsEligible = "தகுதி வரம்புகள்", details = "திட்ட விவரங்கள்", docsNeeded = "தேவையான ஆவணங்கள்",
        closeButton = "புரிந்தது", analyze = "பகுப்பாய்வு", cancel = "அழி", summary = "சுருக்கம்",
        whatToDo = "என்ன செய்ய வேண்டும்?", scanAnother = "மற்றொன்றை ஸ்கேன் செய்", riskScore = "ஆபத்து மதிப்பெண்",
        scheme1Title = "பிஎம் கிசான் நிதி", scheme1Amount = "₹6,000/ஆண்டு", scheme1Eligible = "சிறு மற்றும் குறு விவசாயிகள்.",
        scheme1Details = "3 தவணைகளில் நேரடி வருமான உதவி.", scheme1Docs = "ஆதார், நில ஆவணங்கள், வங்கி கணக்கு.",
        scheme2Title = "பிஎம் ஆவாஸ் யோஜனா", scheme2Amount = "₹2.67 லட்சம்", scheme2Eligible = "சொந்த வீடு இல்லாத குடும்பங்கள்.",
        scheme2Details = "வீடு கட்ட மானியம்.", scheme2Docs = "ஆதார், வருமான சான்றிதழ்.",
        scheme3Title = "ஆயுஷ்மான் பாரத்", scheme3Amount = "₹5 லட்சம்", scheme3Eligible = "குறைந்த வருமானமுள்ள குடும்பங்கள்.",
        scheme3Details = "மருத்துவமனை சிகிச்சைகளுக்கு இலவச மருத்துவ காப்பீடு.", scheme3Docs = "ரேஷன் கார்டு, ஆதார்."
    )

    private val gujarati = AppStrings(
        welcome = "સ્વાગત", subWelcome = "તમારી સરકાર, તમારી ભાષા.", scoreTitle = "સરકાર સ્કોર",
        pendingAmount = "₹૧૮,૫૦૦ લાભ બાકી છે", schemesFound = "૩ નવી તકો મળી છે",
        newOpportunities = "નવી તકો", todayInfo = "દૈનિક માહિતી", aiConsoleTitle = "AI ટેસ્ટ કન્સોલ",
        aiConsoleSub = "સિસ્ટમ ડાયગ્નોસ્ટિક", startButton = "શરૂ કરો", placeholderNote = "રેશન કાર્ડ અપડેટની છેલ્લી તારીખ: ૩૦ જૂન.",
        documentDecoder = "દસ્તાવેજ ડિકોડર", cameraAccess = "કેમેરા એક્સેસ જરૂરી", allowCamera = "મંજૂરી આપો",
        uploadDoc = "દસ્તાવેજ અપલોડ કરો", pasteText = "ટેક્સ્ટ પેસ્ટ કરો", navHome = "હોમ", navDecoder = "સ્કેનર",
        navSchemes = "લાભો", navRights = "અધિકારો", navProfile = "ખાતું", benefitAmount = "લાભની રકમ",
        whoIsEligible = "પાત્રતા માપદંડ", details = "યોજનાની વિગતો", docsNeeded = "જરૂરી દસ્તાવેજો",
        closeButton = "સમજાયું", analyze = "વિશ્લેષણ", cancel = "સાફ કરો", summary = "સારાંશ",
        whatToDo = "શું કરવું?", scanAnother = "બીજું સ્કેન કરો", riskScore = "જોખમ સ્કોર",
        scheme1Title = "પીએમ કિસાન નિધિ", scheme1Amount = "₹૬,૦૦૦/વર્ષ", scheme1Eligible = "નાના અને સીમાંત ખેડૂતો.",
        scheme1Details = "૩ હપ્તામાં સીધી આવક સહાય.", scheme1Docs = "આધાર, જમીનના કાગળો, બેંક ખાતું.",
        scheme2Title = "પીએમ આવાસ યોજના", scheme2Amount = "₹૨.૬૭ લાખ", scheme2Eligible = "પાકું મકાન ન હોય તેવા પરિવારો.",
        scheme2Details = "મકાન બાંધકામ માટે સબસિડી.", scheme2Docs = "આધાર, આવકનું પ્રમાણપત્ર.",
        scheme3Title = "આયુષ્માન ભારત", scheme3Amount = "₹૫ લાખ", scheme3Eligible = "ઓછી આવક ધરાવતા પરિવારો.",
        scheme3Details = "હોસ્પિટલની સારવાર માટે મફત આરોગ્ય વીમો.", scheme3Docs = "રેશન કાર્ડ, આધાર."
    )

    private val kannada = AppStrings(
        welcome = "ಸ್ವಾಗತ", subWelcome = "ನಿಮ್ಮ ಸರ್ಕಾರ, ನಿಮ್ಮ ಭಾಷೆ.", scoreTitle = "ಸರ್ಕಾರ ಸ್ಕೋರ್",
        pendingAmount = "₹18,500 ಪ್ರಯೋಜನ ಬಾಕಿ ಇದೆ", schemesFound = "3 ಹೊಸ ಅವಕಾಶಗಳು ಕಂಡುಬಂದಿವೆ",
        newOpportunities = "ಹೊಸ ಅವಕಾಶಗಳು", todayInfo = "ದೈನಂದಿನ ಮಾಹಿತಿ", aiConsoleTitle = "AI ಟೆಸ್ಟ್ ಕನ್ಸೋಲ್",
        aiConsoleSub = "ಸಿಸ್ಟಮ್ ಡಯಾಗ್ನೋಸ್ಟಿಕ್", startButton = "ಪ್ರಾರಂಭಿಸಿ", placeholderNote = "ರೇಷನ್ ಕಾರ್ಡ್ ಅಪ್‌ಡೇಟ್ ಕೊನೆಯ ದಿನಾಂಕ: ಜೂನ್ 30.",
        documentDecoder = "ಡಾಕ್ಯುಮೆಂಟ್ ಡಿಕೋಡರ್", cameraAccess = "ಕ್ಯಾಮೆರಾ ಪ್ರವೇಶ ಅಗತ್ಯವಿದೆ", allowCamera = "ಅನುಮತಿಸಿ",
        uploadDoc = "ಡಾಕ್ಯುಮೆಂಟ್ ಅಪ್‌ಲೋಡ್", pasteText = "ಪಠ್ಯವನ್ನು ಅಂಟಿಸಿ", navHome = "ಮುಖಪುಟ", navDecoder = "ಸ್ಕ್ಯಾನರ್",
        navSchemes = "ಪ್ರಯೋಜನಗಳು", navRights = "ಹಕ್ಕುಗಳು", navProfile = "ಖಾತೆ", benefitAmount = "ಪ್ರಯೋಜನದ ಮೊತ್ತ",
        whoIsEligible = "ಅರ್ಹತೆಯ ಮಾನದಂಡಗಳು", details = "ಯೋಜನೆಯ ವಿವರಗಳು", docsNeeded = "ಅಗತ್ಯ ದಾಖಲೆಗಳು",
        closeButton = "ಅರ್ಥವಾಯಿತು", analyze = "ವಿಶ್ಲೇಷಿಸಿ", cancel = "ಅಳಿಸಿ", summary = "ಸಾರಾಂಶ",
        whatToDo = "ಏನು ಮಾಡಬೇಕು?", scanAnother = "ಮತ್ತೊಂದನ್ನು ಸ್ಕ್ಯಾನ್ ಮಾಡಿ", riskScore = "ಅಪಾಯದ ಸ್ಕೋರ್",
        scheme1Title = "ಪಿಎಂ ಕಿಸಾನ್ ನಿಧಿ", scheme1Amount = "₹6,000/ವರ್ಷ", scheme1Eligible = "ಸಣ್ಣ ಮತ್ತು ಅತಿಸಣ್ಣ ರೈತರು.",
        scheme1Details = "3 ಕಂತುಗಳಲ್ಲಿ ನೇರ ಆದಾಯ ಬೆಂಬಲ.", scheme1Docs = "ಆಧಾರ್, ಭೂಮಿ ದಾಖಲೆಗಳು, ಬ್ಯಾಂಕ್ ಖಾತೆ.",
        scheme2Title = "ಪಿಎಂ ಆವಾಸ್ ಯೋಜನೆ", scheme2Amount = "₹2.67 ಲಕ್ಷ", scheme2Eligible = "ಪಕ್ಕಾ ಮನೆ ಇಲ್ಲದ ಕುಟುಂಬಗಳು.",
        scheme2Details = "ಮನೆ ನಿರ್ಮಾಣಕ್ಕೆ ಸಹಾಯಧನ.", scheme2Docs = "ಆಧಾರ್, ಆದಾಯ ಪ್ರಮಾಣಪತ್ರ.",
        scheme3Title = "ಆಯುಷ್ಮಾನ್ ಭಾರತ್", scheme3Amount = "₹5 ಲಕ್ಷ", scheme3Eligible = "ಕಡಿಮೆ ಆದಾಯದ ಕುಟುಂಬಗಳು.",
        scheme3Details = "ಆಸ್ಪತ್ರೆ ಚಿಕಿತ್ಸೆಗಳಿಗಾಗಿ ಉಚಿತ ಆರೋಗ್ಯ ವಿಮೆ.", scheme3Docs = "ರೇಷನ್ ಕಾರ್ಡ್, ಆಧಾರ್."
    )

    private val punjabi = AppStrings(
        welcome = "ਜੀ ਆਇਆਂ ਨੂੰ", subWelcome = "ਤੁਹਾਡੀ ਸਰਕਾਰ, ਤੁਹਾਡੀ ਭਾਸ਼ਾ।", scoreTitle = "ਸਰਕਾਰ ਸਕੋਰ",
        pendingAmount = "₹18,500 ਲਾਭ ਬਾਕੀ ਹੈ", schemesFound = "3 ਨਵੇਂ ਮੌਕੇ ਮਿਲੇ ਹਨ",
        newOpportunities = "ਨਵੇਂ ਮੌਕੇ", todayInfo = "ਰੋਜ਼ਾਨਾ ਜਾਣਕਾਰੀ", aiConsoleTitle = "AI ਟੈਸਟ ਕੰਸੋਲ",
        aiConsoleSub = "ਸਿਸਟਮ ਡਾਇਗਨੌਸਟਿਕ", startButton = "ਸ਼ੁਰੂ ਕਰੋ", placeholderNote = "ਰਾਸ਼ਨ ਕਾਰਡ ਅਪਡੇਟ ਦੀ ਆਖਰੀ ਮਿਤੀ: 30 ਜੂਨ।",
        documentDecoder = "ਦਸਤਾਵੇਜ਼ ਡੀਕੋਡਰ", cameraAccess = "ਕੈਮਰਾ ਐਕਸੈਸ ਦੀ ਲੋੜ ਹੈ", allowCamera = "ਇਜਾਜ਼ਤ ਦਿਓ",
        uploadDoc = "ਦਸਤਾਵੇਜ਼ ਅਪਲੋਡ ਕਰੋ", pasteText = "ਟੈਕਸਟ ਪੇਸਟ ਕਰੋ", navHome = "ਹੋਮ", navDecoder = "ਸਕੈਨਰ",
        navSchemes = "ਲਾਭ", navRights = "ਅਧਿਕਾਰ", navProfile = "ਖਾਤਾ", benefitAmount = "ਲਾਭ ਦੀ ਰਕਮ",
        whoIsEligible = "ਯੋਗਤਾ ਮਾਪਦੰਡ", details = "ਯੋਜਨਾ ਦਾ ਵੇਰਵਾ", docsNeeded = "ਲੋੜੀਂਦੇ ਦਸਤਾਵੇਜ਼",
        closeButton = "ਸਮਝ ਗਿਆ", analyze = "ਵਿਸ਼ਲੇਸ਼ਣ", cancel = "ਸਾਫ ਕਰੋ", summary = "ਸਾਰ",
        whatToDo = "ਕੀ ਕਰਨਾ ਹੈ?", scanAnother = "ਇੱਕ ਹੋਰ ਸਕੈਨ ਕਰੋ", riskScore = "ਜੋਖਮ ਸਕੋਰ",
        scheme1Title = "ਪੀਐਮ ਕਿਸਾਨ ਨਿਧੀ", scheme1Amount = "₹6,000/ਸਾਲ", scheme1Eligible = "ਛੋਟੇ ਅਤੇ ਸੀਮਾਂਤ ਕਿਸਾਨ।",
        scheme1Details = "3 ਕਿਸ਼ਤਾਂ ਵਿੱਚ ਸਿੱਧੀ ਆਮਦਨ ਸਹਾਇਤਾ।", scheme1Docs = "ਆਧਾਰ, ਜ਼ਮੀਨ ਦੇ ਕਾਗਜ਼, ਬੈਂਕ ਖਾਤਾ।",
        scheme2Title = "ਪੀਐਮ ਆਵਾਸ ਯੋਜਨਾ", scheme2Amount = "₹2.67 ਲੱਖ", scheme2Eligible = "ਪੱਕੇ ਘਰ ਤੋਂ ਬਿਨਾਂ ਪਰਿਵਾਰ।",
        scheme2Details = "ਘਰ ਬਣਾਉਣ ਲਈ ਸਬਸਿਡੀ।", scheme2Docs = "ਆਧਾਰ, ਆਮਦਨ ਸਰਟੀਫਿਕੇਟ।",
        scheme3Title = "ਆਯੁਸ਼ਮਾਨ ਭਾਰਤ", scheme3Amount = "₹5 ਲੱਖ", scheme3Eligible = "ਘੱਟ ਆਮਦਨ ਵਾਲੇ ਪਰਿਵਾਰ।",
        scheme3Details = "ਹਸਪਤਾਲ ਦੇ ਇਲਾਜ ਲਈ ਮੁਫ਼ਤ ਸਿਹਤ ਬੀਮਾ।", scheme3Docs = "ਰਾਸ਼ਨ ਕਾਰਡ, ਆਧਾਰ।"
    )

    private val malayalam = AppStrings(
        welcome = "സ്വാഗതം", subWelcome = "നിങ്ങളുടെ സർക്കാർ, നിങ്ങളുടെ ഭാഷ.", scoreTitle = "സർക്കാർ സ്കോർ",
        pendingAmount = "₹18,500 ആനുകൂല്യം ബാക്കിയുണ്ട്", schemesFound = "3 പുതിയ അവസരങ്ങൾ കണ്ടെത്തി",
        newOpportunities = "പുതിയ അവസരങ്ങൾ", todayInfo = "ദിനചര്യ വിവരങ്ങൾ", aiConsoleTitle = "AI ടെസ്റ്റ് കൺസോൾ",
        aiConsoleSub = "സിസ്റ്റം ഡയഗ്നോസ്റ്റിക്", startButton = "തുടങ്ങുക", placeholderNote = "റേഷൻ കാർഡ് പുതുക്കാനുള്ള അവസാന തീയതി: ജൂൺ 30.",
        documentDecoder = "ഡോക്യുമെന്റ് ഡീകോഡർ", cameraAccess = "ക്യാമറ അനുമതി ആവശ്യമാണ്", allowCamera = "അനുവദിക്കുക",
        uploadDoc = "ഡോക്യുമെന്റ് അപ്‌ലോഡ് ചെയ്യുക", pasteText = "ടെക്സ്റ്റ് പേസ്റ്റ് ചെയ്യുക", navHome = "ഹോം", navDecoder = "സ്കാനർ",
        navSchemes = "ആനുകൂല്യങ്ങൾ", navRights = "അവകാശങ്ങൾ", navProfile = "അക്കൗണ്ട്", benefitAmount = "ആനുകൂല്യ തുക",
        whoIsEligible = "അർഹതാ മാനദണ്ഡം", details = "പദ്ധതി വിവരം", docsNeeded = "ആവശ്യമായ രേഖകൾ",
        closeButton = "മനസ്സിലായി", analyze = "വിശകലനം ചെയ്യുക", cancel = "മായ്ക്കുക", summary = "സംഗ്രഹം",
        whatToDo = "എന്ത് ചെയ്യണം?", scanAnother = "മറ്റൊന്ന് സ്കാൻ ചെയ്യുക", riskScore = "അപായ സ്കോർ",
        scheme1Title = "പിഎം കിസാൻ നിധി", scheme1Amount = "₹6,000/വർഷം", scheme1Eligible = "ചെറുകിട നാമമാത്ര കർഷകർ.",
        scheme1Details = "3 ഗഡുക്കളായി നേരിട്ടുള്ള വരുമാന സഹായം.", scheme1Docs = "ആധാർ, ഭൂമി രേഖകൾ, ബാങ്ക് അക്കൗണ്ട്.",
        scheme2Title = "പിഎം ആവാസ് യോജന", scheme2Amount = "₹2.67 ലക്ഷം", scheme2Eligible = "സ്ഥിരമായ വീടില്ലാത്ത കുടുംബങ്ങൾ.",
        scheme2Details = "വീട് നിർമ്മാണത്തിന് സബ്സിഡി.", scheme2Docs = "ആധാർ, വരുമാന സർട്ടിഫിക്കറ്റ്.",
        scheme3Title = "ആയുഷ്മാൻ ഭാരത്", scheme3Amount = "₹5 ലക്ഷം", scheme3Eligible = "കുറഞ്ഞ വരുമാനമുള്ള കുടുംബങ്ങൾ.",
        scheme3Details = "ആശുപത്രി ചികിത്സകൾക്കായി സൗജന്യ ആരോഗ്യ ഇൻഷുറൻസ്.", scheme3Docs = "റേഷൻ കാർഡ്, ആധാർ."
    )

    private val odia = AppStrings(
        welcome = "ସ୍ୱାଗତ", subWelcome = "ଆପଣଙ୍କ ସରକାର, ଆପଣଙ୍କ ଭାଷା।", scoreTitle = "ସରକାର ସ୍କୋର",
        pendingAmount = "₹୧୮,୫୦୦ ସୁବିଧା ବାକି ଅଛି", schemesFound = "୩ଟି ନୂଆ ସୁଯୋଗ ମିଳିଛି",
        newOpportunities = "ନୂଆ ସୁଯୋଗ", todayInfo = "ଦୈନିକ ସୂଚନା", aiConsoleTitle = "AI ଟେଷ୍ଟ କନସୋଲ",
        aiConsoleSub = "ସିଷ୍ଟମ ଡାଇଗ୍ନୋଷ୍ଟିକ", startButton = "ଆରମ୍ଭ କରନ୍ତୁ", placeholderNote = "ରାସନ କାର୍ଡ ଅପଡେଟ୍ ଶେଷ ତାରିଖ: ଜୁନ୍ ୩୦।",
        documentDecoder = "ଡକ୍ୟୁମେଣ୍ଟ ଡିକୋଡର୍", cameraAccess = "କ୍ୟାମେରା ଆକ୍ସେସ୍ ଆବଶ୍ୟକ", allowCamera = "ଅନୁମତି ଦିଅନ୍ତୁ",
        uploadDoc = "ଡକ୍ୟୁମେଣ୍ଟ ଅପଲୋଡ୍", pasteText = "ଟେକ୍ସଟ୍ ପେଷ୍ଟ କରନ୍ତୁ", navHome = "ହୋମ", navDecoder = "ସ୍କାନର",
        navSchemes = "ସୁବିଧା", navRights = "ଅଧିକାର", navProfile = "ଖାତା", benefitAmount = "ସୁବିଧା ପରିମାଣ",
        whoIsEligible = "ଯୋଗ୍ୟତା ମାପଦଣ୍ଡ", details = "ଯୋଜନା ବିବରଣୀ", docsNeeded = "ଆବଶ୍ୟକ ଦଲିଲ",
        closeButton = "ବୁଝିଲି", analyze = "ବିଶ୍ଳେଷଣ", cancel = "ସଫା କରନ୍ତୁ", summary = "ସାରାଂଶ",
        whatToDo = "କଣ କରିବା?", scanAnother = "ଆଉ ଏਕ ସ୍କାନ କରନ୍ତୁ", riskScore = "ବିପଦ ସ୍କୋର",
        scheme1Title = "ପିଏମ କିଷାନ ନିଧି", scheme1Amount = "₹୬,୦୦୦/ବର୍ଷ", scheme1Eligible = "କ୍ଷୁଦ୍ର ଓ ନାମମାତ୍ର ଚାଷୀ।",
        scheme1Details = "୩ଟି କିସ୍ତିରେ ସିଧାସଳଖ ଆୟ ସହାୟତା।", scheme1Docs = "ଆଧାର, ଜମି କାଗଜପତ୍ର, ବ୍ୟାଙ୍କ ଖାତା।",
        scheme2Title = "ପିଏମ ଆବାସ ଯୋଜନା", scheme2Amount = "₹୨.୬୭ ଲକ୍ଷ", scheme2Eligible = "ପକ୍କା ଘର ନଥିବା ପରିବਾਰ।",
        scheme2Details = "ଘର ତିଆରି ପାଇଁ ସବସିଡି।", scheme2Docs = "ଆଧାର, ଆୟ ପ୍ରମାଣପତ୍ର।",
        scheme3Title = "ଆୟୁଷ୍ମାନ ଭାରତ", scheme3Amount = "₹୫ ଲକ୍ଷ", scheme3Eligible = "ସ୍ୱଳ୍ପ ଆୟକାରୀ ପରିବାର।",
        scheme3Details = "ଡାକ୍ତរଖାନା ଚିକିତ୍ସା ପାଇଁ ମାଗଣା ସ୍ୱାସ୍ଥ୍ୟ ବୀମା।", scheme3Docs = "ରାସନ କାର୍ଡ, ଆଧାର।"
    )

    private val assamese = AppStrings(
        welcome = "স্বাগতম", subWelcome = "আপোনাৰ চৰকাৰ, আপোনাৰ ভাষা।", scoreTitle = "চৰকাৰ স্ক’ৰ",
        pendingAmount = "₹১৮,৫০০ সুবিধা বাকী আছে", schemesFound = "৩টা নতুন সুযোগ পোৱা গৈছে",
        newOpportunities = "নতুন সুযোগ", todayInfo = "দৈনিক তথ্য", aiConsoleTitle = "AI টেষ্ট কনচোল",
        aiConsoleSub = "ছিষ্টেম ডায়াগনষ্টিক", startButton = "আৰম্ভ কৰক", placeholderNote = "ৰেচন কাৰ্ড আপডেটৰ শেষ তাৰিখ: ৩০ জুন।",
        documentDecoder = "ডকুমেন্ট ডিকোডাৰ", cameraAccess = "কেমেৰা এক্সেছৰ প্ৰয়োজন", allowCamera = "অনুমতি দিয়ক",
        uploadDoc = "ডকুমেন্ট আপলোড কৰক", pasteText = "টেক্সট পেষ্ট কৰক", navHome = "গৃহ", navDecoder = "স্ক্যানাৰ",
        navSchemes = "সুবিধা", navRights = "অধিকাৰ", navProfile = "অ্যাকাউন্ট", benefitAmount = "সুবিধাৰ পৰিমাণ",
        whoIsEligible = "যোগ্যতাৰ মাপদণ্ড", details = "আঁচনিৰ বিৱৰণ", docsNeeded = "প্ৰয়োজনীয় নথি-পত্ৰ",
        closeButton = "বুজি পালোঁ", analyze = "বিশ্লেষণ", cancel = "মচি পেলাওক", summary = "সাৰাংশ",
        whatToDo = "কি কৰিব লাগিব?", scanAnother = "আন এটা স্ক্যান কৰক", riskScore = "বিপদাশংকা স্ক’ৰ",
        scheme1Title = "পিএম কিষাণ নিধি", scheme1Amount = "₹৬,০০০/বছৰ", scheme1Eligible = "ক্ষুদ্ৰ আৰু উপান্ত কৃষক।",
        scheme1Details = "৩টা কিস্তিত পোনপটীয়া উপাৰ্জনৰ সহায়।", scheme1Docs = "আধাৰ, মাটিৰ নথি, বেংক একাউণ্ট।",
        scheme2Title = "পিএম আৱাস যোজনা", scheme2Amount = "₹২.৬৭ লাখ", scheme2Eligible = "পকী ঘৰ নথকা পৰিয়াল।",
        scheme2Details = "ঘৰ নিৰ্মাণৰ বাবে ৰাজসাহায্য।", scheme2Docs = "আধাৰ, উপাৰ্জনৰ প্ৰমাণপত্ৰ।",
        scheme3Title = "আয়ুষ্মান ভাৰত", scheme3Amount = "₹৫ লাখ", scheme3Eligible = "কম উপাৰ্জন কৰা পৰিয়াল।",
        scheme3Details = "চিকিৎসালয়ৰ চিকিৎসাৰ বাবে বিনামূলীয়া স্বাস্থ্য বীমা।", scheme3Docs = "ৰেচন কাৰ্ড, আধাৰ।"
    )

    private val maithili = AppStrings(
        welcome = "स्वागत अछि", subWelcome = "अहाँक सरकार, अहाँक भाषा।", scoreTitle = "सरकार स्कोर",
        pendingAmount = "₹১৮,৫০০ लाभ बाँकी अछि", schemesFound = "৩ টা नव अवसर भेटल अछि",
        newOpportunities = "नव अवसर", todayInfo = "दैनिक जानकारी", aiConsoleTitle = "AI टेस्ट कन्सोल",
        aiConsoleSub = "सिस्टम डायग्नोस्टिक", startButton = "शुरू करू", placeholderNote = "राशन कार्ड अपडेटक अंतिम तिथि: ३० जून।",
        documentDecoder = "दस्तावेज डिकोडर", cameraAccess = "कैमरा एक्सेस आवश्यक अछि", allowCamera = "अनुमति दियौ",
        uploadDoc = "दस्तावेज अपलोड करू", pasteText = "टेक्स्ट पेस्ट करू", navHome = "घर", navDecoder = "स्कैनर",
        navSchemes = "लाभ", navRights = "अधिकार", navProfile = "खाता", benefitAmount = "लाभ राशि",
        whoIsEligible = "पात्रता मानदंड", details = "योजनाक विवरण", docsNeeded = "आवश्यक दस्तावेज",
        closeButton = "बुझि गेलहुँ", analyze = "विश्लेषण", cancel = "साफ करू", summary = "सारांश",
        whatToDo = "की करू?", scanAnother = "आन स्कैन करू", riskScore = "जोखिम स्कोर",
        scheme1Title = "पीएम किसान निधि", scheme1Amount = "₹६,०००/वर्ष", scheme1Eligible = "छोट आर सीमांत किसान।",
        scheme1Details = "३ किश्त मे सीधा आय सहायता।", scheme1Docs = "आधार, जमीनक कागज, बैंक खाता।",
        scheme2Title = "पीएम आवास योजना", scheme2Amount = "₹२.৬৭ लाख", scheme2Eligible = "पक्का घर बिना परिवार।",
        scheme2Details = "घर निर्माणक लेल सब्सिडी।", scheme2Docs = "आधार, आय प्रमाणपत्र।",
        scheme3Title = "आयुष्मान भारत", scheme3Amount = "₹५ लाख", scheme3Eligible = "कम आय बला परिवार।",
        scheme3Details = "अस्पतालक इलाजक लेल मुफ्त स्वास्थ्य बीमा।", scheme3Docs = "राशन कार्ड, आधार।"
    )

    private val santali = AppStrings(
        welcome = "ᱥᱟᱹᱜᱩᱱ ᱫᱟᱨᱟᱢ", subWelcome = "ᱟᱢᱟᱜ ᱥᱚᱨᱠᱟᱨ, ᱟᱢᱟᱜ ᱯᱟᱹᱨᱥᱤ ᱾", scoreTitle = "ᱥᱚᱨᱠᱟᱨ ᱥᱠᱳᱨ",
        pendingAmount = "₹᱑᱘,᱕᱐᱐ ᱞᱟᱵᱷ ᱵᱟᱹᱠᱤ ᱢᱮᱱᱟᱜᱼᱟ", schemesFound = "᱓ ᱜᱚᱴᱟᱝ ᱱᱟᱶᱟ ᱫᱟᱣ ᱧᱟᱢ ᱟᱠᱟᱱᱟ",
        newOpportunities = "ᱱᱟᱶᱟ ᱫᱟᱣ", todayInfo = "ᱫᱤᱱᱟᱹᱢ ᱠᱷᱚᱵᱚᱨ", aiConsoleTitle = "AI ᱴᱮᱥᱴ ᱠᱚᱱᱥᱳᱞ",
        aiConsoleSub = "ᱥᱤᱥᱴᱮᱢ ᱰᱟᱭᱜᱽᱱᱚᱥᱴᱤᱠ", startButton = "ᱮᱛᱚᱦᱚᱵ ᱢᱮ", placeholderNote = "ᱨᱟᱥᱚᱱ ᱠᱟᱨᱰ ᱟᱯᱰᱮᱴ ᱢᱩᱪᱟᱹᱫ ᱢᱟᱹᱦᱤᱛ: ᱓᱐ ᱡᱩᱱ ᱾",
        documentDecoder = "ᱫᱚᱞᱤᱞ ᱰᱤᱠᱳᱰᱟᱨ", cameraAccess = "ᱠᱮᱢᱮᱨᱟ ᱮᱠᱥᱮᱥ ᱞᱟᱹᱠᱛᱤᱭᱟ", allowCamera = "ᱮᱠᱥᱮᱥ ᱮᱢ ᱢᱮ",
        uploadDoc = "ᱫᱚᱞᱤᱞ ᱟᱯᱞᱳᱰ ᱢᱮ", pasteText = "ᱴᱮᱠᱥᱴ ᱯᱮᱥᱴ ᱢᱮ", navHome = "ᱳᱲᱟᱜ", navDecoder = "ᱥᱠᱮᱱᱟᱨ",
        navSchemes = "ᱞᱟᱵᱷ", navRights = "ᱚᱭᱫᱟᱹᱨ", navProfile = "ᱮᱠᱟᱶᱩᱱᱴ", benefitAmount = "ᱞᱟᱵᱷ ᱴᱟᱠᱟ",
        whoIsEligible = "ᱡᱚᱜᱽ ᱛᱮᱛᱮᱫ", details = "ᱡᱚᱡᱚᱱᱟ ᱵᱤᱵᱽᱨᱚᱬ", docsNeeded = "ᱞᱟᱹᱠᱛᱤᱭᱟᱱ ᱫᱚᱞᱤᱞ",
        closeButton = "ᱵᱩᱡᱷᱟᱹᱣ ᱠᱮᱫᱟᱹᱧ", analyze = "ᱵᱤᱪᱟᱹᱨ", cancel = "ᱜᱤᱰᱤ ᱢᱮ", summary = "ᱥᱟᱨᱛᱮᱫ",
        whatToDo = "ᱪᱮᱫ ᱪᱮᱠᱟ ᱦᱩᱭᱩᱜᱼᱟ?", scanAnother = "ᱮᱴᱟᱜ ᱥᱠᱮᱱ ᱢᱮ", riskScore = "ᱵᱚᱛᱚᱨ ᱥᱠᱳᱨ",
        scheme1Title = "পিএম কিষাণ নিধি", scheme1Amount = "₹᱖,᱐᱐᱐/ᱥᱮᱨᱢᱟ", scheme1Eligible = "ᱠᱟᱹᱴᱤᱡ ᱪᱟᱹᱥᱤ ᱠᱚ ᱾",
        scheme1Details = "᱓ ᱠᱤᱥᱛᱤ ᱛᱮ ᱥᱤᱫᱷᱟᱹ ᱟᱨᱡᱟᱣ ᱜᱚᱲᱚ ᱾", scheme1Docs = "ᱟᱫᱷᱟᱨ, ᱦᱟᱥᱟ ᱥᱟᱠᱟᱢ, ᱵᱮᱸᱠ ᱮᱠᱟᱶᱩᱱᱴ ᱾",
        scheme2Title = "পিএম আবাস যোজনা", scheme2Amount = "₹᱒.᱖᱗ ᱞᱟᱠᱷ", scheme2Eligible = "ᱯᱟᱠᱟ ᱳᱲᱟᱜ ᱵᱟᱹᱱᱩᱜ ᱜᱷᱟᱨᱚᱸᱡᱽ ᱾",
        scheme2Details = "ᱳᱲᱟᱜ ᱵᱮᱱᱟᱣ ᱞᱟᱹᱜᱤᱫ ᱥᱟᱵᱽᱥᱤᱰᱤ ᱾", scheme2Docs = "ᱟᱫᱷᱟᱨ, ᱟᱨᱡᱟᱣ ᱥᱟᱠᱟᱢ ᱾",
        scheme3Title = "আয়ুষ্মান ভারত", scheme3Amount = "₹᱕ ᱞᱟᱠᱷ", scheme3Eligible = "ᱨᱮᱸᱜᱮᱡ ᱜᱷᱟᱨᱚᱸᱡᱽ ᱾",
        scheme3Details = "ᱦᱟᱥᱯᱟᱛᱟᱞ ᱨᱟᱱ ᱞᱟᱹᱜᱤᱫ ᱯᱷᱨᱤ ᱥᱟᱶᱟᱨ ᱵᱤᱢᱟᱹ ᱾", scheme3Docs = "ᱨᱟᱥᱚᱱ ᱠᱟᱨᱰ, ᱟᱫᱷᱟᱨ ᱾"
    )

    private val kashmiri = AppStrings(
        welcome = "خوش آمدید", subWelcome = "تُہنٛز حکومت، تُہنٛز زبان۔", scoreTitle = "حکومت سکور",
        pendingAmount = "₹18,500 فائِدہ چھُ باقے", schemesFound = "3 نۆو مۆقہٕ آمِت لبنہٕ",
        newOpportunities = "نۆو مۆقہٕ", todayInfo = "رۆزانہ معلوٗمات", aiConsoleTitle = "AI ٹیسٹ کَنسول",
        aiConsoleSub = "سِسٹم معائنہٕ", startButton = "شۆروٗ کٔرِو", placeholderNote = "راشن کارڈ اپڈیٹ کرنُک آخری تٲریخ: 30 جوٗن۔",
        documentDecoder = "دستاویز ڈیکوڈر", cameraAccess = "کیمرہٕ اِجازت چھُ ضروٗری", allowCamera = "اِجازت دِیِو",
        uploadDoc = "دستاویز اپلوڈ کٔرِو", pasteText = "تحریر پیسٹ کٔرِو", navHome = "گھر", navDecoder = "سکیینر",
        navSchemes = "فائِدہٕ", navRights = "حقوٗق", navProfile = "کھاتہٕ", benefitAmount = "فائِدہٕ رقم",
        whoIsEligible = "اہلیت", details = "سکییم تَفصئیل", docsNeeded = "ضروٗری دستاویز",
        closeButton = "سمجھ آو", analyze = "تجزیہٕ", cancel = "صایِف کٔرِو", summary = "خلاصہٕ",
        whatToDo = "کیاہ کَرُن؟", scanAnother = "بئی سکیین کٔرِو", riskScore = "خطرہٕ سکور",
        scheme1Title = "پی ایم کِسان نِدھی", scheme1Amount = "₹6,000/ؤری", scheme1Eligible = "لۆکٹی زمیندار۔",
        scheme1Details = "3 قسطن منٛز براہِ راست مَدَد۔", scheme1Docs = "آدھار، زمینِ کاغذ، بینک کھاتہٕ۔",
        scheme2Title = "پی ایم آواس یوجنا", scheme2Amount = "₹2.67 لاکھ", scheme2Eligible = "گھرہٕ بغیر کُنبہٕ۔",
        scheme2Details = "گھر بناونہٕ خٲطرہ مَدَد۔", scheme2Docs = "آدھار، آمَدنی سارٹیفیکیٹ۔",
        scheme3Title = "آیوشمان بھارت", scheme3Amount = "₹5 لاکھ", scheme3Eligible = "غریب کُنبہٕ۔",
        scheme3Details = "ہسپتال عِلاج خٲطرہ مُفت صِحَت بیما۔", scheme3Docs = "راشن کارڈ، آدھار۔"
    )

    fun getStrings(language: String): AppStrings {
        return when {
            language.contains("Hindi", ignoreCase = true) -> hindi
            language.contains("Telugu", ignoreCase = true) -> telugu
            language.contains("Bengali", ignoreCase = true) -> bengali
            language.contains("Marathi", ignoreCase = true) -> marathi
            language.contains("Tamil", ignoreCase = true) -> tamil
            language.contains("Gujarati", ignoreCase = true) -> gujarati
            language.contains("Kannada", ignoreCase = true) -> kannada
            language.contains("Odia", ignoreCase = true) -> odia
            language.contains("Malayalam", ignoreCase = true) -> malayalam
            language.contains("Punjabi", ignoreCase = true) -> punjabi
            language.contains("Assamese", ignoreCase = true) -> assamese
            language.contains("Maithili", ignoreCase = true) -> maithili
            language.contains("Santali", ignoreCase = true) -> santali
            language.contains("Kashmiri", ignoreCase = true) -> kashmiri
            else -> english
        }
    }
}