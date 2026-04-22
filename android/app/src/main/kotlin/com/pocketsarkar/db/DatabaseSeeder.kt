package com.pocketsarkar.db

import android.content.Context
import android.util.Log
import com.pocketsarkar.db.dao.SchemeDao
import com.pocketsarkar.db.entities.EligibilityRule
import com.pocketsarkar.db.entities.HelplineNumber
import com.pocketsarkar.db.entities.Scheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DatabaseSeeder — Phase 2
 *
 * Seeds the on-device SQLite DB with real Indian government schemes.
 * Called once on first launch (or when DB is empty).
 *
 * Data sourced from: myscheme.gov.in, pmkisan.gov.in, jan-dhan-darshak.gov.in
 */
object DatabaseSeeder {

    private const val TAG = "DatabaseSeeder"
    private const val PREFS_NAME = "pocket_sarkar_prefs"
    private const val KEY_SEEDED = "db_seeded_v2"

    suspend fun seedIfNeeded(context: Context, dao: SchemeDao) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) {
            Log.d(TAG, "DB already seeded, skipping.")
            return@withContext
        }

        Log.i(TAG, "Seeding DB with government schemes...")

        dao.insertSchemes(ALL_SCHEMES)
        dao.insertRules(ALL_RULES)
        dao.insertHelplines(ALL_HELPLINES)

        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
        Log.i(TAG, "Seeded ${ALL_SCHEMES.size} schemes, ${ALL_RULES.size} rules, ${ALL_HELPLINES.size} helplines.")
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // SCHEMES  (47 representative schemes across categories; expand to 447 later)
    // ─────────────────────────────────────────────────────────────────────────────

    private val ALL_SCHEMES = listOf(

        // ── AGRICULTURE ──────────────────────────────────────────────────────────

        Scheme(
            id = "PM_KISAN_001",
            nameEn = "PM Kisan Samman Nidhi",
            nameHi = "प्रधानमंत्री किसान सम्मान निधि",
            category = "agriculture",
            ministryEn = "Ministry of Agriculture & Farmers' Welfare",
            descriptionEn = "Direct income support of ₹6,000/year to small and marginal farmers holding up to 2 hectares of land. Paid in 3 instalments of ₹2,000 directly into bank account.",
            descriptionHi = "2 हेक्टेयर तक जमीन वाले छोटे और सीमांत किसानों को ₹6,000/वर्ष की प्रत्यक्ष आय सहायता। ₹2,000 की 3 किस्तों में सीधे बैंक खाते में।",
            benefitAmount = "₹6,000/year",
            benefitType = "cash",
            targetStates = "ALL",
            targetGender = "ALL",
            targetCategory = "ALL",
            portalUrl = "https://pmkisan.gov.in",
            helplineNumber = "155261",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "PM_FASAL_BIMA_002",
            nameEn = "PM Fasal Bima Yojana",
            nameHi = "प्रधानमंत्री फसल बीमा योजना",
            category = "agriculture",
            ministryEn = "Ministry of Agriculture & Farmers' Welfare",
            descriptionEn = "Crop insurance scheme covering all food crops, oilseeds and annual commercial/horticultural crops. Premium as low as 1.5% for kharif and 2% for rabi crops.",
            descriptionHi = "सभी खाद्य फसलों, तिलहन और वार्षिक वाणिज्यिक/बागवानी फसलों को कवर करने वाली फसल बीमा योजना। खरीफ के लिए 1.5% और रबी फसलों के लिए 2% तक प्रीमियम।",
            benefitAmount = "Up to full sum insured",
            benefitType = "insurance",
            targetStates = "ALL",
            portalUrl = "https://pmfby.gov.in",
            helplineNumber = "1800-200-7710",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "KISAN_CREDIT_CARD_003",
            nameEn = "Kisan Credit Card",
            nameHi = "किसान क्रेडिट कार्ड",
            category = "agriculture",
            ministryEn = "Ministry of Agriculture & Farmers' Welfare",
            descriptionEn = "Credit up to ₹3 lakh at 4% interest rate to farmers for crop cultivation, post-harvest expenses and allied activities. Interest subvention provided by government.",
            descriptionHi = "फसल खेती, कटाई के बाद के खर्च और संबद्ध गतिविधियों के लिए किसानों को 4% ब्याज दर पर ₹3 लाख तक क्रेडिट।",
            benefitAmount = "Up to ₹3 lakh credit",
            benefitType = "subsidy",
            targetStates = "ALL",
            helplineNumber = "1800-11-0001",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "SOIL_HEALTH_CARD_004",
            nameEn = "Soil Health Card Scheme",
            nameHi = "मृदा स्वास्थ्य कार्ड योजना",
            category = "agriculture",
            ministryEn = "Ministry of Agriculture & Farmers' Welfare",
            descriptionEn = "Free soil testing and Soil Health Card issued to farmers with crop-wise recommendations for nutrients and fertilisers to improve productivity.",
            descriptionHi = "किसानों को मुफ्त मिट्टी परीक्षण और मृदा स्वास्थ्य कार्ड जारी किया जाता है जिसमें उत्पादकता सुधार के लिए पोषक तत्वों और उर्वरकों की फसल-वार सिफारिशें होती हैं।",
            benefitAmount = "Free soil analysis",
            benefitType = "service",
            targetStates = "ALL",
            portalUrl = "https://soilhealth.dac.gov.in",
            confidenceScore = 0.9f
        ),

        // ── HOUSING ──────────────────────────────────────────────────────────────

        Scheme(
            id = "PMAY_GRAMIN_005",
            nameEn = "PM Awas Yojana - Gramin",
            nameHi = "प्रधानमंत्री आवास योजना - ग्रामीण",
            category = "housing",
            ministryEn = "Ministry of Rural Development",
            descriptionEn = "Financial assistance of ₹1.20 lakh in plains and ₹1.30 lakh in hilly/difficult areas for construction of pucca houses for BPL families in rural areas.",
            descriptionHi = "ग्रामीण क्षेत्रों में बीपीएल परिवारों के लिए पक्के मकान निर्माण हेतु मैदानी इलाकों में ₹1.20 लाख और पहाड़ी/कठिन क्षेत्रों में ₹1.30 लाख की वित्तीय सहायता।",
            benefitAmount = "₹1.20–₹1.30 lakh",
            benefitType = "cash",
            targetStates = "ALL",
            portalUrl = "https://pmayg.nic.in",
            helplineNumber = "1800-11-6446",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "PMAY_URBAN_006",
            nameEn = "PM Awas Yojana - Urban",
            nameHi = "प्रधानमंत्री आवास योजना - शहरी",
            category = "housing",
            ministryEn = "Ministry of Housing and Urban Affairs",
            descriptionEn = "Interest subsidy of 6.5% on home loans up to ₹6 lakh for EWS/LIG categories. Urban slum dwellers and homeless families eligible.",
            descriptionHi = "EWS/LIG श्रेणियों के लिए ₹6 लाख तक के होम लोन पर 6.5% ब्याज सब्सिडी। शहरी झुग्गी निवासी और बेघर परिवार पात्र हैं।",
            benefitAmount = "6.5% interest subsidy",
            benefitType = "subsidy",
            targetStates = "ALL",
            targetCategory = "EWS",
            portalUrl = "https://pmaymis.gov.in",
            helplineNumber = "1800-11-3377",
            confidenceScore = 1.0f
        ),

        // ── EDUCATION ────────────────────────────────────────────────────────────

        Scheme(
            id = "NSP_SC_SCHOLARSHIP_007",
            nameEn = "Post Matric Scholarship for SC Students",
            nameHi = "अनुसूचित जाति के छात्रों के लिए पोस्ट मैट्रिक छात्रवृत्ति",
            category = "education",
            ministryEn = "Ministry of Social Justice and Empowerment",
            descriptionEn = "Full scholarship covering tuition fees, maintenance allowance and other charges for SC students pursuing post-matriculation courses. Parental income up to ₹2.5 lakh/year.",
            descriptionHi = "मैट्रिक के बाद के पाठ्यक्रमों में पढ़ रहे SC छात्रों के लिए ट्यूशन फीस, रखरखाव भत्ता और अन्य शुल्क को कवर करने वाली पूर्ण छात्रवृत्ति।",
            benefitAmount = "Full fee + maintenance",
            benefitType = "cash",
            targetStates = "ALL",
            targetCategory = "SC",
            portalUrl = "https://scholarships.gov.in",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "NSP_ST_SCHOLARSHIP_008",
            nameEn = "Post Matric Scholarship for ST Students",
            nameHi = "अनुसूचित जनजाति के छात्रों के लिए पोस्ट मैट्रिक छात्रवृत्ति",
            category = "education",
            ministryEn = "Ministry of Tribal Affairs",
            descriptionEn = "Scholarship for ST students in post-matriculation or post-secondary courses. Covers full fees and maintenance allowance. Parental income up to ₹2.5 lakh/year.",
            descriptionHi = "मैट्रिक के बाद या माध्यमिक पाठ्यक्रमों में ST छात्रों के लिए छात्रवृत्ति। पूर्ण शुल्क और रखरखाव भत्ता शामिल।",
            benefitAmount = "Full fee + maintenance",
            benefitType = "cash",
            targetStates = "ALL",
            targetCategory = "ST",
            portalUrl = "https://scholarships.gov.in",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "BETI_BACHAO_009",
            nameEn = "Beti Bachao Beti Padhao",
            nameHi = "बेटी बचाओ बेटी पढ़ाओ",
            category = "education",
            ministryEn = "Ministry of Women and Child Development",
            descriptionEn = "Scheme to address declining child sex ratio and promote welfare of girl child. Includes Sukanya Samriddhi Account with 8.2% interest for girl children below 10 years.",
            descriptionHi = "घटते बाल लिंगानुपात को दूर करने और बालिका कल्याण को बढ़ावा देने की योजना। 10 वर्ष से कम आयु की बालिकाओं के लिए 8.2% ब्याज के साथ सुकन्या समृद्धि खाता शामिल।",
            benefitAmount = "8.2% interest on savings",
            benefitType = "service",
            targetStates = "ALL",
            targetGender = "F",
            portalUrl = "https://wcd.nic.in/bbbp-schemes",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "MID_DAY_MEAL_010",
            nameEn = "PM POSHAN (Mid Day Meal)",
            nameHi = "पीएम पोषण (मिड डे मील)",
            category = "education",
            ministryEn = "Ministry of Education",
            descriptionEn = "Free hot cooked meal to children in government/government-aided schools (Class 1-8). Nutritional norms: 700 calories and 20g protein for upper primary students.",
            descriptionHi = "सरकारी/सरकारी सहायता प्राप्त स्कूलों में बच्चों को मुफ्त गर्म पका हुआ भोजन (कक्षा 1-8)।",
            benefitAmount = "Free daily meal",
            benefitType = "service",
            targetStates = "ALL",
            confidenceScore = 0.95f
        ),

        // ── HEALTH ───────────────────────────────────────────────────────────────

        Scheme(
            id = "AYUSHMAN_BHARAT_011",
            nameEn = "Ayushman Bharat - PM Jan Arogya Yojana",
            nameHi = "आयुष्मान भारत - प्रधानमंत्री जन आरोग्य योजना",
            category = "health",
            ministryEn = "Ministry of Health and Family Welfare",
            descriptionEn = "Health cover of ₹5 lakh per family per year for secondary and tertiary care hospitalisation. Covers 1,949+ medical procedures. Cashless treatment at empanelled hospitals.",
            descriptionHi = "द्वितीयक और तृतीयक देखभाल अस्पताल में भर्ती के लिए प्रति परिवार प्रति वर्ष ₹5 लाख का स्वास्थ्य कवर। 1,949+ चिकित्सा प्रक्रियाएं कवर।",
            benefitAmount = "₹5 lakh/year health cover",
            benefitType = "insurance",
            targetStates = "ALL",
            portalUrl = "https://pmjay.gov.in",
            helplineNumber = "14555",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "JANANI_SURAKSHA_012",
            nameEn = "Janani Suraksha Yojana",
            nameHi = "जननी सुरक्षा योजना",
            category = "health",
            ministryEn = "Ministry of Health and Family Welfare",
            descriptionEn = "Cash assistance to pregnant women from BPL/SC/ST families for institutional delivery. ₹1,400 in rural and ₹1,000 in urban areas. Reduces maternal and neonatal mortality.",
            descriptionHi = "BPL/SC/ST परिवारों की गर्भवती महिलाओं को संस्थागत प्रसव के लिए नकद सहायता। ग्रामीण में ₹1,400 और शहरी क्षेत्रों में ₹1,000।",
            benefitAmount = "₹1,000–₹1,400 per delivery",
            benefitType = "cash",
            targetStates = "ALL",
            targetGender = "F",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "PM_SURAKSHA_BIMA_013",
            nameEn = "PM Suraksha Bima Yojana",
            nameHi = "प्रधानमंत्री सुरक्षा बीमा योजना",
            category = "health",
            ministryEn = "Ministry of Finance",
            descriptionEn = "Accidental death and disability insurance cover of ₹2 lakh at just ₹20/year premium. For bank account holders aged 18-70 years. Auto-debited from savings account.",
            descriptionHi = "मात्र ₹20/वर्ष प्रीमियम पर ₹2 लाख का दुर्घटना मृत्यु और विकलांगता बीमा कवर। 18-70 वर्ष के बैंक खाताधारकों के लिए।",
            benefitAmount = "₹2 lakh accident cover",
            benefitType = "insurance",
            targetStates = "ALL",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "PM_JEEVAN_JYOTI_014",
            nameEn = "PM Jeevan Jyoti Bima Yojana",
            nameHi = "प्रधानमंत्री जीवन ज्योति बीमा योजना",
            category = "health",
            ministryEn = "Ministry of Finance",
            descriptionEn = "Life insurance cover of ₹2 lakh at ₹436/year for death due to any reason. For bank account holders aged 18-50 years. Annual renewal required.",
            descriptionHi = "किसी भी कारण से मृत्यु के लिए ₹436/वर्ष पर ₹2 लाख का जीवन बीमा कवर। 18-50 वर्ष के बैंक खाताधारकों के लिए।",
            benefitAmount = "₹2 lakh life cover",
            benefitType = "insurance",
            targetStates = "ALL",
            confidenceScore = 1.0f
        ),

        // ── FINANCIAL INCLUSION ──────────────────────────────────────────────────

        Scheme(
            id = "JAN_DHAN_015",
            nameEn = "PM Jan Dhan Yojana",
            nameHi = "प्रधानमंत्री जन-धन योजना",
            category = "financial",
            ministryEn = "Ministry of Finance",
            descriptionEn = "Zero-balance bank account with RuPay debit card, ₹10,000 overdraft facility, ₹2 lakh accidental insurance and ₹30,000 life cover. For unbanked individuals.",
            descriptionHi = "RuPay डेबिट कार्ड, ₹10,000 ओवरड्राफ्ट सुविधा, ₹2 लाख दुर्घटना बीमा और ₹30,000 जीवन कवर के साथ जीरो-बैलेंस बैंक खाता।",
            benefitAmount = "Free account + ₹2 lakh cover",
            benefitType = "service",
            targetStates = "ALL",
            helplineNumber = "1800-11-0001",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "MUDRA_LOAN_016",
            nameEn = "PM MUDRA Yojana",
            nameHi = "प्रधानमंत्री मुद्रा योजना",
            category = "financial",
            ministryEn = "Ministry of Finance",
            descriptionEn = "Collateral-free loans up to ₹10 lakh for non-corporate, non-farm small/micro enterprises. Three tiers: Shishu (up to ₹50K), Kishor (up to ₹5L), Tarun (up to ₹10L).",
            descriptionHi = "गैर-कॉर्पोरेट, गैर-कृषि लघु/सूक्ष्म उद्यमों के लिए ₹10 लाख तक के बिना जमानत के ऋण। तीन स्तर: शिशु (₹50K तक), किशोर (₹5L तक), तरुण (₹10L तक)।",
            benefitAmount = "Up to ₹10 lakh loan",
            benefitType = "subsidy",
            targetStates = "ALL",
            portalUrl = "https://mudra.org.in",
            helplineNumber = "1800-180-1111",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "ATAL_PENSION_017",
            nameEn = "Atal Pension Yojana",
            nameHi = "अटल पेंशन योजना",
            category = "financial",
            ministryEn = "Ministry of Finance",
            descriptionEn = "Guaranteed pension of ₹1,000 to ₹5,000/month after age 60 for unorganised sector workers. Government co-contributes 50% of contribution or ₹1,000/year (whichever is lower) for 5 years.",
            descriptionHi = "असंगठित क्षेत्र के कामगारों के लिए 60 वर्ष के बाद ₹1,000 से ₹5,000/माह की गारंटीड पेंशन।",
            benefitAmount = "₹1,000–₹5,000/month pension",
            benefitType = "insurance",
            targetStates = "ALL",
            helplineNumber = "1800-110-069",
            confidenceScore = 1.0f
        ),

        // ── EMPLOYMENT / SKILL ───────────────────────────────────────────────────

        Scheme(
            id = "MGNREGA_018",
            nameEn = "Mahatma Gandhi NREGA",
            nameHi = "महात्मा गांधी राष्ट्रीय ग्रामीण रोजगार गारंटी अधिनियम",
            category = "employment",
            ministryEn = "Ministry of Rural Development",
            descriptionEn = "Guaranteed 100 days of wage employment per year to rural households. Current wage ₹261/day (varies by state). Work within 5 km of residence, wages in 15 days.",
            descriptionHi = "ग्रामीण परिवारों को प्रति वर्ष 100 दिनों का मजदूरी रोजगार गारंटी। वर्तमान मजदूरी ₹261/दिन (राज्य अनुसार भिन्न)।",
            benefitAmount = "100 days @ ₹261/day",
            benefitType = "cash",
            targetStates = "ALL",
            portalUrl = "https://nrega.nic.in",
            helplineNumber = "1800-111-555",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "PM_KAUSHAL_VIKAS_019",
            nameEn = "PM Kaushal Vikas Yojana 4.0",
            nameHi = "प्रधानमंत्री कौशल विकास योजना 4.0",
            category = "employment",
            ministryEn = "Ministry of Skill Development and Entrepreneurship",
            descriptionEn = "Free short-term skill training in industry-relevant courses. Completion incentive of ₹8,000 and placement assistance. Covers 300+ sectors and job roles.",
            descriptionHi = "उद्योग-प्रासंगिक पाठ्यक्रमों में मुफ्त अल्पकालिक कौशल प्रशिक्षण। ₹8,000 का पूर्णता प्रोत्साहन और प्लेसमेंट सहायता।",
            benefitAmount = "Free training + ₹8,000",
            benefitType = "cash",
            targetStates = "ALL",
            portalUrl = "https://pmkvyofficial.org",
            helplineNumber = "1800-123-9626",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "STARTUP_INDIA_020",
            nameEn = "Startup India Seed Fund",
            nameHi = "स्टार्टअप इंडिया सीड फंड",
            category = "employment",
            ministryEn = "Ministry of Commerce and Industry",
            descriptionEn = "Seed funding up to ₹50 lakh for startups to validate proof of concept, prototype development and market entry. For DPIIT-recognised startups up to 2 years old.",
            descriptionHi = "प्रूफ ऑफ कॉन्सेप्ट, प्रोटोटाइप और बाजार प्रवेश के लिए स्टार्टअप को ₹50 लाख तक की सीड फंडिंग।",
            benefitAmount = "Up to ₹50 lakh",
            benefitType = "cash",
            targetStates = "ALL",
            portalUrl = "https://seedfund.startupindia.gov.in",
            confidenceScore = 0.9f
        ),

        // ── WOMEN & CHILD ────────────────────────────────────────────────────────

        Scheme(
            id = "POSHAN_ABHIYAAN_021",
            nameEn = "POSHAN Abhiyaan",
            nameHi = "पोषण अभियान",
            category = "women_child",
            ministryEn = "Ministry of Women and Child Development",
            descriptionEn = "Nutritional support for pregnant women, lactating mothers and children under 6. Supplementary nutrition, growth monitoring and health check-ups through Anganwadi centres.",
            descriptionHi = "गर्भवती महिलाओं, स्तनपान कराने वाली माताओं और 6 वर्ष से कम बच्चों के लिए पोषण सहायता।",
            benefitAmount = "Free nutrition + health",
            benefitType = "service",
            targetStates = "ALL",
            targetGender = "F",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "SUKANYA_SAMRIDDHI_022",
            nameEn = "Sukanya Samriddhi Yojana",
            nameHi = "सुकन्या समृद्धि योजना",
            category = "women_child",
            ministryEn = "Ministry of Finance",
            descriptionEn = "High-interest savings account (8.2% p.a.) for girl children below 10 years. Minimum ₹250/year, maximum ₹1.5 lakh/year. Tax-free maturity at age 21.",
            descriptionHi = "10 वर्ष से कम आयु की बालिकाओं के लिए उच्च-ब्याज बचत खाता (8.2% प्रति वर्ष)। न्यूनतम ₹250/वर्ष, अधिकतम ₹1.5 लाख/वर्ष।",
            benefitAmount = "8.2% interest, tax-free",
            benefitType = "service",
            targetStates = "ALL",
            targetGender = "F",
            helplineNumber = "1800-11-0001",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "MATRITVA_VANDANA_023",
            nameEn = "Pradhan Mantri Matru Vandana Yojana",
            nameHi = "प्रधानमंत्री मातृ वंदना योजना",
            category = "women_child",
            ministryEn = "Ministry of Women and Child Development",
            descriptionEn = "₹5,000 cash incentive for first live birth to compensate wage loss during pregnancy and lactation. Paid in two instalments.",
            descriptionHi = "गर्भावस्था और स्तनपान के दौरान मजदूरी हानि की भरपाई के लिए पहले जीवित बच्चे के जन्म पर ₹5,000 नकद प्रोत्साहन।",
            benefitAmount = "₹5,000 for first child",
            benefitType = "cash",
            targetStates = "ALL",
            targetGender = "F",
            portalUrl = "https://pmmvy.wcd.gov.in",
            confidenceScore = 1.0f
        ),

        // ── SC/ST WELFARE ─────────────────────────────────────────────────────────

        Scheme(
            id = "DR_AMBEDKAR_HOUSING_024",
            nameEn = "Dr. Ambedkar Awas Navinikaran Yojana",
            nameHi = "डॉ. अम्बेडकर आवास नवीनीकरण योजना",
            category = "housing",
            ministryEn = "Ministry of Social Justice and Empowerment",
            descriptionEn = "Grant of ₹80,000 for repair/renovation of house owned by SC families living below poverty line. Applied through district welfare officer.",
            descriptionHi = "गरीबी रेखा से नीचे रहने वाले SC परिवारों के स्वामित्व वाले घर की मरम्मत/नवीनीकरण के लिए ₹80,000 का अनुदान।",
            benefitAmount = "₹80,000 grant",
            benefitType = "cash",
            targetStates = "ALL",
            targetCategory = "SC",
            confidenceScore = 0.85f
        ),

        Scheme(
            id = "VAN_DHAN_VIKAS_025",
            nameEn = "Van Dhan Vikas Kendra",
            nameHi = "वन धन विकास केंद्र",
            category = "employment",
            ministryEn = "Ministry of Tribal Affairs",
            descriptionEn = "Value addition and marketing support for tribal forest produce. Each cluster gets ₹15 lakh. Tribal communities aggregate, process and market minor forest produce.",
            descriptionHi = "जनजातीय वन उत्पाद के लिए मूल्यवर्धन और विपणन सहायता। प्रत्येक समूह को ₹15 लाख मिलता है।",
            benefitAmount = "₹15 lakh/cluster",
            benefitType = "cash",
            targetStates = "ALL",
            targetCategory = "ST",
            portalUrl = "https://trifed.tribal.gov.in",
            confidenceScore = 0.9f
        ),

        // ── DISABILITY ───────────────────────────────────────────────────────────

        Scheme(
            id = "DIVYANGJAN_SCHOLARSHIP_026",
            nameEn = "Scholarship for Students with Disabilities",
            nameHi = "दिव्यांगजन छात्रों के लिए छात्रवृत्ति",
            category = "education",
            ministryEn = "Department of Empowerment of Persons with Disabilities",
            descriptionEn = "Pre-matric and post-matric scholarships for students with 40%+ disability. Covers tuition fees, books, maintenance allowance. Parental income up to ₹2.5 lakh.",
            descriptionHi = "40%+ विकलांगता वाले छात्रों के लिए प्री-मैट्रिक और पोस्ट-मैट्रिक छात्रवृत्ति।",
            benefitAmount = "Full fee + allowance",
            benefitType = "cash",
            targetStates = "ALL",
            portalUrl = "https://scholarships.gov.in",
            confidenceScore = 0.95f
        ),

        // ── ENERGY / ENVIRONMENT ─────────────────────────────────────────────────

        Scheme(
            id = "PM_UJJWALA_027",
            nameEn = "PM Ujjwala Yojana 2.0",
            nameHi = "प्रधानमंत्री उज्ज्वला योजना 2.0",
            category = "energy",
            ministryEn = "Ministry of Petroleum and Natural Gas",
            descriptionEn = "Free LPG connection with first refill and hotplate to BPL women, SC/ST families, migrants, construction workers and forest dwellers.",
            descriptionHi = "BPL महिलाओं, SC/ST परिवारों, प्रवासियों, निर्माण श्रमिकों और वनवासियों को मुफ्त एलपीजी कनेक्शन, पहली रिफिल और हॉटप्लेट।",
            benefitAmount = "Free LPG connection + refill",
            benefitType = "service",
            targetStates = "ALL",
            targetGender = "F",
            helplineNumber = "1800-266-6696",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "PM_SURYA_GHAR_028",
            nameEn = "PM Surya Ghar Muft Bijli Yojana",
            nameHi = "प्रधानमंत्री सूर्य घर मुफ्त बिजली योजना",
            category = "energy",
            ministryEn = "Ministry of New and Renewable Energy",
            descriptionEn = "Up to 300 units of free electricity per month via rooftop solar panels. Subsidy: ₹30,000 for 1kW, ₹60,000 for 2kW, ₹78,000 for 3kW systems.",
            descriptionHi = "छत पर सौर पैनलों के माध्यम से प्रति माह 300 यूनिट तक मुफ्त बिजली। 1kW के लिए ₹30,000, 2kW के लिए ₹60,000 सब्सिडी।",
            benefitAmount = "300 units free electricity",
            benefitType = "subsidy",
            targetStates = "ALL",
            portalUrl = "https://pmsuryaghar.gov.in",
            helplineNumber = "1800-180-3333",
            confidenceScore = 1.0f
        ),

        // ── DIGITAL / TECHNOLOGY ────────────────────────────────────────────────

        Scheme(
            id = "DIGITAL_INDIA_029",
            nameEn = "Common Service Centres (CSC)",
            nameHi = "सामान्य सेवा केंद्र",
            category = "digital",
            ministryEn = "Ministry of Electronics and Information Technology",
            descriptionEn = "Government digital services at village level — Aadhaar enrolment, PAN card, passport, insurance, banking, and 350+ G2C services within walking distance.",
            descriptionHi = "गांव स्तर पर सरकारी डिजिटल सेवाएं — आधार नामांकन, पैन कार्ड, पासपोर्ट, बीमा, बैंकिंग और 350+ G2C सेवाएं।",
            benefitAmount = "350+ services accessible",
            benefitType = "service",
            targetStates = "ALL",
            helplineNumber = "1800-121-3468",
            confidenceScore = 0.9f
        ),

        // ── SENIOR CITIZENS ──────────────────────────────────────────────────────

        Scheme(
            id = "IGNOAPS_030",
            nameEn = "Indira Gandhi National Old Age Pension",
            nameHi = "इंदिरा गांधी राष्ट्रीय वृद्धावस्था पेंशन",
            category = "social_security",
            ministryEn = "Ministry of Rural Development",
            descriptionEn = "Monthly pension of ₹200–₹500 for BPL senior citizens aged 60+. State governments often top-up to ₹1,000–₹3,000. Applied through gram panchayat.",
            descriptionHi = "BPL 60+ वर्ष के वरिष्ठ नागरिकों के लिए ₹200–₹500 मासिक पेंशन। राज्य सरकारें अक्सर ₹1,000–₹3,000 तक जोड़ती हैं।",
            benefitAmount = "₹200–₹500/month + state top-up",
            benefitType = "cash",
            targetStates = "ALL",
            confidenceScore = 0.95f
        ),

        // ── WATER & SANITATION ───────────────────────────────────────────────────

        Scheme(
            id = "JAL_JEEVAN_031",
            nameEn = "Jal Jeevan Mission",
            nameHi = "जल जीवन मिशन",
            category = "water_sanitation",
            ministryEn = "Ministry of Jal Shakti",
            descriptionEn = "Piped drinking water supply at 55 litres per capita per day to every rural household. Tap water connection provided free of cost.",
            descriptionHi = "प्रत्येक ग्रामीण परिवार को प्रति दिन 55 लीटर प्रति व्यक्ति पाइप से पेयजल आपूर्ति। नल जल कनेक्शन मुफ्त प्रदान किया जाता है।",
            benefitAmount = "Free piped water connection",
            benefitType = "service",
            targetStates = "ALL",
            helplineNumber = "1916",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "SBM_GRAMIN_032",
            nameEn = "Swachh Bharat Mission - Gramin",
            nameHi = "स्वच्छ भारत मिशन - ग्रामीण",
            category = "water_sanitation",
            ministryEn = "Ministry of Jal Shakti",
            descriptionEn = "Incentive of ₹12,000 for construction of individual household toilet to BPL families and other eligible categories in rural areas.",
            descriptionHi = "ग्रामीण क्षेत्रों में BPL परिवारों को व्यक्तिगत घरेलू शौचालय निर्माण के लिए ₹12,000 का प्रोत्साहन।",
            benefitAmount = "₹12,000 for toilet",
            benefitType = "cash",
            targetStates = "ALL",
            portalUrl = "https://sbm.gov.in",
            confidenceScore = 0.9f
        ),

        // ── FOOD SECURITY ────────────────────────────────────────────────────────

        Scheme(
            id = "PMGKAY_033",
            nameEn = "PM Garib Kalyan Anna Yojana",
            nameHi = "प्रधानमंत्री गरीब कल्याण अन्न योजना",
            category = "food",
            ministryEn = "Ministry of Consumer Affairs, Food and Public Distribution",
            descriptionEn = "5 kg free foodgrain (rice or wheat) per person per month to 80 crore National Food Security Act beneficiaries. Integrated into regular PDS from 2024.",
            descriptionHi = "80 करोड़ राष्ट्रीय खाद्य सुरक्षा अधिनियम लाभार्थियों को प्रति व्यक्ति प्रति माह 5 किलो मुफ्त खाद्यान्न।",
            benefitAmount = "5 kg free grain/month",
            benefitType = "service",
            targetStates = "ALL",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "ONE_NATION_RATION_034",
            nameEn = "One Nation One Ration Card",
            nameHi = "एक राष्ट्र एक राशन कार्ड",
            category = "food",
            ministryEn = "Ministry of Consumer Affairs, Food and Public Distribution",
            descriptionEn = "Portability of ration card across all states. Migrant workers can access PDS foodgrain from any fair price shop in India using existing ration card.",
            descriptionHi = "सभी राज्यों में राशन कार्ड की पोर्टेबिलिटी। प्रवासी श्रमिक मौजूदा राशन कार्ड का उपयोग करके किसी भी उचित मूल्य की दुकान से PDS खाद्यान्न प्राप्त कर सकते हैं।",
            benefitAmount = "PDS food access nationwide",
            benefitType = "service",
            targetStates = "ALL",
            helplineNumber = "14445",
            confidenceScore = 1.0f
        ),

        // ── LABOUR WELFARE ───────────────────────────────────────────────────────

        Scheme(
            id = "ESIC_INSURANCE_035",
            nameEn = "ESIC Medical Insurance",
            nameHi = "ईएसआईसी चिकित्सा बीमा",
            category = "health",
            ministryEn = "Ministry of Labour and Employment",
            descriptionEn = "Comprehensive medical care, cash benefits during sickness/maternity/disablement and dependant benefit for insured workers earning up to ₹21,000/month.",
            descriptionHi = "₹21,000/माह तक कमाने वाले बीमाकृत श्रमिकों के लिए व्यापक चिकित्सा देखभाल, बीमारी/मातृत्व/विकलांगता के दौरान नकद लाभ।",
            benefitAmount = "Full medical + cash benefits",
            benefitType = "insurance",
            targetStates = "ALL",
            helplineNumber = "1800-11-2526",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "E_SHRAM_036",
            nameEn = "e-Shram Card",
            nameHi = "ई-श्रम कार्ड",
            category = "employment",
            ministryEn = "Ministry of Labour and Employment",
            descriptionEn = "National database registration for unorganised workers. Free ₹2 lakh accidental insurance. Gateway to all labour welfare schemes.",
            descriptionHi = "असंगठित श्रमिकों के लिए राष्ट्रीय डेटाबेस पंजीकरण। ₹2 लाख दुर्घटना बीमा मुफ्त। सभी श्रम कल्याण योजनाओं का प्रवेश द्वार।",
            benefitAmount = "₹2 lakh accident cover",
            benefitType = "insurance",
            targetStates = "ALL",
            portalUrl = "https://eshram.gov.in",
            helplineNumber = "14434",
            confidenceScore = 1.0f
        ),

        // ── ROADS / RURAL INFRA ──────────────────────────────────────────────────

        Scheme(
            id = "PMGSY_037",
            nameEn = "PM Gram Sadak Yojana",
            nameHi = "प्रधानमंत्री ग्राम सड़क योजना",
            category = "infrastructure",
            ministryEn = "Ministry of Rural Development",
            descriptionEn = "All-weather road connectivity to unconnected habitations — 500+ population in plain areas and 250+ in hilly, tribal and desert areas.",
            descriptionHi = "असंबद्ध बस्तियों को सभी मौसम में सड़क संपर्क — मैदानी क्षेत्रों में 500+ जनसंख्या और पहाड़ी, आदिवासी और रेगिस्तानी क्षेत्रों में 250+ जनसंख्या।",
            benefitAmount = "Free all-weather road",
            benefitType = "service",
            targetStates = "ALL",
            confidenceScore = 0.9f
        ),

        // ── LEGAL AID ────────────────────────────────────────────────────────────

        Scheme(
            id = "NALSA_LEGAL_AID_038",
            nameEn = "NALSA Legal Aid Services",
            nameHi = "नालसा कानूनी सहायता सेवाएं",
            category = "legal",
            ministryEn = "Department of Justice",
            descriptionEn = "Free legal aid to SC/ST, women, children, victims of trafficking, disaster victims, industrial workers, persons with disability and those with income below ₹1 lakh/year.",
            descriptionHi = "SC/ST, महिलाओं, बच्चों, तस्करी के शिकार, आपदा पीड़ितों, औद्योगिक श्रमिकों, विकलांग व्यक्तियों और ₹1 लाख/वर्ष से कम आय वालों को मुफ्त कानूनी सहायता।",
            benefitAmount = "Free legal representation",
            benefitType = "service",
            targetStates = "ALL",
            helplineNumber = "15100",
            confidenceScore = 1.0f
        ),

        // ── PENSION ──────────────────────────────────────────────────────────────

        Scheme(
            id = "NPS_LITE_039",
            nameEn = "NPS Lite (Swavalamban)",
            nameHi = "एनपीएस लाइट (स्वावलम्बन)",
            category = "social_security",
            ministryEn = "Ministry of Finance",
            descriptionEn = "Pension scheme for unorganised sector workers with government co-contribution of ₹1,000/year for 3 years for accounts with ₹1,000–₹12,000 annual contribution.",
            descriptionHi = "असंगठित क्षेत्र के श्रमिकों के लिए पेंशन योजना। ₹1,000–₹12,000 वार्षिक योगदान वाले खातों के लिए 3 वर्षों तक ₹1,000/वर्ष सरकारी सह-योगदान।",
            benefitAmount = "₹1,000/year co-contribution",
            benefitType = "cash",
            targetStates = "ALL",
            confidenceScore = 0.8f
        ),

        // ── STATE-SPECIFIC EXAMPLES ──────────────────────────────────────────────

        Scheme(
            id = "UP_KANYA_SUMANGALA_040",
            nameEn = "Mukhyamantri Kanya Sumangala Yojana (UP)",
            nameHi = "मुख्यमंत्री कन्या सुमंगला योजना (UP)",
            nameLocal = "कन्या सुमंगला",
            category = "women_child",
            ministryEn = "Uttar Pradesh Government",
            descriptionEn = "₹15,000 in 6 instalments for girl children in UP families with income up to ₹3 lakh/year. Covers birth, vaccination, school admissions and graduation.",
            descriptionHi = "UP में ₹3 लाख/वर्ष तक की आय वाले परिवारों की बालिकाओं के लिए 6 किस्तों में ₹15,000। जन्म, टीकाकरण, स्कूल प्रवेश और स्नातक शामिल।",
            benefitAmount = "₹15,000 in 6 instalments",
            benefitType = "cash",
            targetStates = "UP",
            targetGender = "F",
            portalUrl = "https://mksy.up.gov.in",
            helplineNumber = "1800-1800-300",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "MH_LADKI_BAHIN_041",
            nameEn = "Ladki Bahin Yojana (Maharashtra)",
            nameHi = "लाडकी बहीण योजना (महाराष्ट्र)",
            nameLocal = "लाडकी बहीण",
            category = "women_child",
            ministryEn = "Maharashtra Government",
            descriptionEn = "₹1,500/month for women aged 21–65 in Maharashtra with family income up to ₹2.5 lakh/year. Direct benefit transfer to bank account.",
            descriptionHi = "महाराष्ट्र में ₹2.5 लाख/वर्ष तक की पारिवारिक आय वाली 21–65 वर्ष की महिलाओं को ₹1,500/माह।",
            benefitAmount = "₹1,500/month",
            benefitType = "cash",
            targetStates = "MH",
            targetGender = "F",
            confidenceScore = 1.0f
        ),

        Scheme(
            id = "RJ_CHIRANJEEVI_042",
            nameEn = "Mukhyamantri Chiranjeevi Swasthya Bima (RJ)",
            nameHi = "मुख्यमंत्री चिरंजीवी स्वास्थ्य बीमा योजना (राजस्थान)",
            category = "health",
            ministryEn = "Rajasthan Government",
            descriptionEn = "₹25 lakh annual health cover for all Rajasthan families. Free cashless treatment at government and empanelled private hospitals.",
            descriptionHi = "राजस्थान के सभी परिवारों के लिए ₹25 लाख वार्षिक स्वास्थ्य कवर। सरकारी और सूचीबद्ध निजी अस्पतालों में मुफ्त कैशलेस उपचार।",
            benefitAmount = "₹25 lakh health cover",
            benefitType = "insurance",
            targetStates = "RJ",
            helplineNumber = "18001806127",
            confidenceScore = 0.9f
        ),

        Scheme(
            id = "TN_KALAIGNAR_043",
            nameEn = "Kalaignar Magalir Urimai Thittam (TN)",
            nameHi = "कलैगनार मगलिर उरिमई थिट्टम (तमिलनाडु)",
            category = "women_child",
            ministryEn = "Tamil Nadu Government",
            descriptionEn = "₹1,000/month to women heads of family in Tamil Nadu. Covers 1 crore+ families across all categories.",
            descriptionHi = "तमिलनाडु में परिवार की महिला मुखिया को ₹1,000/माह। 1 करोड़+ परिवारों को कवर।",
            benefitAmount = "₹1,000/month",
            benefitType = "cash",
            targetStates = "TN",
            targetGender = "F",
            confidenceScore = 0.9f
        ),

        Scheme(
            id = "KA_GRUHA_JYOTHI_044",
            nameEn = "Gruha Jyothi (Karnataka)",
            nameHi = "गृह ज्योति (कर्नाटक)",
            category = "energy",
            ministryEn = "Karnataka Government",
            descriptionEn = "Free 200 units of electricity per month to domestic consumers in Karnataka. Excess units charged at normal tariff.",
            descriptionHi = "कर्नाटक में घरेलू उपभोक्ताओं को प्रति माह 200 यूनिट मुफ्त बिजली।",
            benefitAmount = "200 units free/month",
            benefitType = "service",
            targetStates = "KA",
            confidenceScore = 0.9f
        ),

        Scheme(
            id = "WB_LAKSHMIR_BHANDAR_045",
            nameEn = "Lakshmir Bhandar (West Bengal)",
            nameHi = "लक्ष्मीर भंडार (पश्चिम बंगाल)",
            category = "women_child",
            ministryEn = "West Bengal Government",
            descriptionEn = "₹500–₹1,000/month to women as head of household in West Bengal. ₹500 for general category, ₹1,000 for SC/ST families.",
            descriptionHi = "पश्चिम बंगाल में महिला परिवार प्रमुखों को ₹500–₹1,000/माह। सामान्य श्रेणी के लिए ₹500, SC/ST परिवारों के लिए ₹1,000।",
            benefitAmount = "₹500–₹1,000/month",
            benefitType = "cash",
            targetStates = "WB",
            targetGender = "F",
            confidenceScore = 0.9f
        ),

        Scheme(
            id = "GJ_NAMO_SARASWATI_046",
            nameEn = "Namo Saraswati Science Scholarship (Gujarat)",
            nameHi = "नमो सरस्वती विज्ञान छात्रवृत्ति (गुजरात)",
            category = "education",
            ministryEn = "Gujarat Government",
            descriptionEn = "₹25,000/year for girl students taking Science stream in Class 11 in Gujarat. For families with income up to ₹2 lakh/year.",
            descriptionHi = "गुजरात में कक्षा 11 में विज्ञान स्ट्रीम लेने वाली छात्राओं के लिए ₹25,000/वर्ष।",
            benefitAmount = "₹25,000/year",
            benefitType = "cash",
            targetStates = "GJ",
            targetGender = "F",
            confidenceScore = 0.9f
        ),

        Scheme(
            id = "HR_LADLI_047",
            nameEn = "Ladli Scheme (Haryana)",
            nameHi = "लाडली योजना (हरियाणा)",
            category = "women_child",
            ministryEn = "Haryana Government",
            descriptionEn = "₹5,000/year deposited in Kisan Vikas Patra for families with two or more girl children. Matures with interest when girl turns 18.",
            descriptionHi = "दो या अधिक बालिकाओं वाले परिवारों के लिए किसान विकास पत्र में ₹5,000/वर्ष जमा। बालिका के 18 वर्ष होने पर ब्याज सहित परिपक्व होता है।",
            benefitAmount = "₹5,000/year in KVP",
            benefitType = "cash",
            targetStates = "HR",
            targetGender = "F",
            confidenceScore = 0.85f
        )
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // ELIGIBILITY RULES
    // ─────────────────────────────────────────────────────────────────────────────

    private val ALL_RULES = listOf(

        // PM Kisan — up to 2 hectares land, must be a farmer
        EligibilityRule("PM_KISAN_001", "land_hectares", "lte", "2.0",
            "Land holding up to 2 hectares", "2 हेक्टेयर तक भूमि जोत"),
        EligibilityRule("PM_KISAN_001", "annual_income", "lte", "200000",
            "Annual family income up to ₹2 lakh", "वार्षिक पारिवारिक आय ₹2 लाख तक"),

        // Ayushman Bharat — SECC 2011 list
        EligibilityRule("AYUSHMAN_BHARAT_011", "annual_income", "lte", "150000",
            "Annual income up to ₹1.5 lakh (SECC)", "SECC के अनुसार ₹1.5 लाख तक वार्षिक आय"),

        // MGNREGA — any rural household
        EligibilityRule("MGNREGA_018", "state", "eq", "ALL",
            "Rural household (any state)", "ग्रामीण परिवार (कोई भी राज्य)"),

        // PM Ujjwala — BPL women only
        EligibilityRule("PM_UJJWALA_027", "gender", "eq", "F",
            "Must be a woman", "महिला होना आवश्यक है"),
        EligibilityRule("PM_UJJWALA_027", "annual_income", "lte", "100000",
            "BPL family (income below ₹1 lakh)", "BPL परिवार (₹1 लाख से कम आय)"),

        // Sukanya Samriddhi — girl below 10
        EligibilityRule("SUKANYA_SAMRIDDHI_022", "gender", "eq", "F",
            "Girl child only", "केवल बालिका"),
        EligibilityRule("SUKANYA_SAMRIDDHI_022", "age", "lt", "10",
            "Child below 10 years of age", "10 वर्ष से कम आयु का बच्चा"),

        // MUDRA — non-farm micro enterprise
        EligibilityRule("MUDRA_LOAN_016", "annual_income", "lte", "1000000",
            "Non-corporate, non-farm business", "गैर-कॉर्पोरेट, गैर-कृषि व्यवसाय"),

        // PM Suraksha Bima — age 18–70 with bank account
        EligibilityRule("PM_SURAKSHA_BIMA_013", "age", "gte", "18",
            "Age 18 or above", "18 वर्ष या उससे अधिक"),
        EligibilityRule("PM_SURAKSHA_BIMA_013", "age", "lte", "70",
            "Age 70 or below", "70 वर्ष या उससे कम"),

        // Atal Pension — age 18–40, unorganised sector
        EligibilityRule("ATAL_PENSION_017", "age", "gte", "18",
            "Age 18 or above", "18 वर्ष या उससे अधिक"),
        EligibilityRule("ATAL_PENSION_017", "age", "lte", "40",
            "Age 40 or below (to join)", "40 वर्ष या उससे कम (जुड़ने के लिए)"),

        // PMAY Gramin — BPL family
        EligibilityRule("PMAY_GRAMIN_005", "annual_income", "lte", "100000",
            "BPL family income", "BPL पारिवारिक आय"),

        // NSP SC Scholarship
        EligibilityRule("NSP_SC_SCHOLARSHIP_007", "category", "eq", "SC",
            "Must belong to SC category", "SC वर्ग से होना आवश्यक"),
        EligibilityRule("NSP_SC_SCHOLARSHIP_007", "annual_income", "lte", "250000",
            "Annual parental income up to ₹2.5 lakh", "माता-पिता की वार्षिक आय ₹2.5 लाख तक"),

        // NSP ST Scholarship
        EligibilityRule("NSP_ST_SCHOLARSHIP_008", "category", "eq", "ST",
            "Must belong to ST category", "ST वर्ग से होना आवश्यक"),
        EligibilityRule("NSP_ST_SCHOLARSHIP_008", "annual_income", "lte", "250000",
            "Annual parental income up to ₹2.5 lakh", "माता-पिता की वार्षिक आय ₹2.5 लाख तक"),

        // UP Kanya Sumangala
        EligibilityRule("UP_KANYA_SUMANGALA_040", "state", "eq", "UP",
            "Must be a resident of Uttar Pradesh", "उत्तर प्रदेश का निवासी होना आवश्यक"),
        EligibilityRule("UP_KANYA_SUMANGALA_040", "gender", "eq", "F",
            "Girl child only", "केवल बालिका"),
        EligibilityRule("UP_KANYA_SUMANGALA_040", "annual_income", "lte", "300000",
            "Family income up to ₹3 lakh/year", "पारिवारिक आय ₹3 लाख/वर्ष तक"),

        // MH Ladki Bahin
        EligibilityRule("MH_LADKI_BAHIN_041", "state", "eq", "MH",
            "Must be a resident of Maharashtra", "महाराष्ट्र का निवासी होना आवश्यक"),
        EligibilityRule("MH_LADKI_BAHIN_041", "gender", "eq", "F",
            "Women only", "केवल महिलाएं"),
        EligibilityRule("MH_LADKI_BAHIN_041", "age", "gte", "21",
            "Age 21 or above", "21 वर्ष या उससे अधिक"),
        EligibilityRule("MH_LADKI_BAHIN_041", "age", "lte", "65",
            "Age 65 or below", "65 वर्ष या उससे कम"),
        EligibilityRule("MH_LADKI_BAHIN_041", "annual_income", "lte", "250000",
            "Family income up to ₹2.5 lakh/year", "पारिवारिक आय ₹2.5 लाख/वर्ष तक"),

        // KCC — farmer with cultivable land
        EligibilityRule("KISAN_CREDIT_CARD_003", "land_hectares", "gt", "0",
            "Must own or lease cultivable land", "कृषि योग्य भूमि होना आवश्यक"),

        // Van Dhan — ST community members
        EligibilityRule("VAN_DHAN_VIKAS_025", "category", "eq", "ST",
            "Must belong to ST category", "ST वर्ग से होना आवश्यक"),

        // ESIC — employees earning up to ₹21,000/month
        EligibilityRule("ESIC_INSURANCE_035", "annual_income", "lte", "252000",
            "Monthly income up to ₹21,000", "मासिक आय ₹21,000 तक"),

        // Matritva Vandana — first child, pregnant woman
        EligibilityRule("MATRITVA_VANDANA_023", "gender", "eq", "F",
            "Pregnant/lactating woman only", "गर्भवती/स्तनपान कराने वाली महिला"),

        // IGNOAPS — senior citizen BPL
        EligibilityRule("IGNOAPS_030", "age", "gte", "60",
            "Age 60 or above", "60 वर्ष या उससे अधिक"),
        EligibilityRule("IGNOAPS_030", "annual_income", "lte", "100000",
            "BPL family", "BPL परिवार"),

        // RJ Chiranjeevi — Rajasthan residents
        EligibilityRule("RJ_CHIRANJEEVI_042", "state", "eq", "RJ",
            "Must be a resident of Rajasthan", "राजस्थान का निवासी होना आवश्यक"),

        // GJ Namo Saraswati — Gujarat girl students
        EligibilityRule("GJ_NAMO_SARASWATI_046", "state", "eq", "GJ",
            "Must be a resident of Gujarat", "गुजरात का निवासी होना आवश्यक"),
        EligibilityRule("GJ_NAMO_SARASWATI_046", "gender", "eq", "F",
            "Girl student only", "केवल छात्रा"),
        EligibilityRule("GJ_NAMO_SARASWATI_046", "annual_income", "lte", "200000",
            "Family income up to ₹2 lakh/year", "पारिवारिक आय ₹2 लाख/वर्ष तक")
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPLINE NUMBERS
    // ─────────────────────────────────────────────────────────────────────────────

    private val ALL_HELPLINES = listOf(
        HelplineNumber("POLICE_100", "Police Emergency", "पुलिस आपातकाल", "100", "police", available24x7 = true, isTollFree = true),
        HelplineNumber("AMBULANCE_108", "Ambulance", "एम्बुलेंस", "108", "health", available24x7 = true, isTollFree = true),
        HelplineNumber("FIRE_101", "Fire Brigade", "दमकल", "101", "emergency", available24x7 = true, isTollFree = true),
        HelplineNumber("WOMEN_1091", "Women Helpline", "महिला हेल्पलाइन", "1091", "women", available24x7 = true, isTollFree = true),
        HelplineNumber("CHILD_1098", "Childline", "चाइल्डलाइन", "1098", "child", available24x7 = true, isTollFree = true),
        HelplineNumber("SENIOR_14567", "Senior Citizen Helpline", "वरिष्ठ नागरिक हेल्पलाइन", "14567", "senior", available24x7 = true, isTollFree = true),
        HelplineNumber("LABOUR_1800111", "Labour Helpline", "श्रम हेल्पलाइन", "1800-11-8500", "labour", isTollFree = true),
        HelplineNumber("LEGAL_15100", "NALSA Legal Aid", "NALSA कानूनी सहायता", "15100", "legal", isTollFree = true),
        HelplineNumber("KISAN_155261", "Kisan Call Centre", "किसान कॉल सेंटर", "155261", "agriculture", available24x7 = true, isTollFree = true),
        HelplineNumber("AYUSHMAN_14555", "Ayushman Bharat Helpline", "आयुष्मान भारत हेल्पलाइन", "14555", "health", isTollFree = true),
        HelplineNumber("UJJWALA_18002666", "PM Ujjwala Helpline", "PM उज्ज्वला हेल्पलाइन", "1800-266-6696", "energy", isTollFree = true),
        HelplineNumber("RATION_14445", "PDS / Ration Helpline", "PDS / राशन हेल्पलाइन", "14445", "food", isTollFree = true),
        HelplineNumber("ESHRAM_14434", "e-Shram Helpline", "ई-श्रम हेल्पलाइन", "14434", "labour", isTollFree = true),
        HelplineNumber("CYBER_1930", "Cyber Crime Helpline", "साइबर अपराध हेल्पलाइन", "1930", "legal", available24x7 = true, isTollFree = true),
        HelplineNumber("WATER_1916", "Jal Jeevan Helpline", "जल जीवन मिशन हेल्पलाइन", "1916", "water_sanitation", isTollFree = true),
        HelplineNumber("MENTAL_iCall", "iCall Mental Health", "iCall मानसिक स्वास्थ्य", "9152987821", "health", isTollFree = false),
        HelplineNumber("DISABILITY_1800", "Divyang Helpline", "दिव्यांग हेल्पलाइन", "1800-11-0031", "health", isTollFree = true),
        HelplineNumber("EDUCATION_1800111", "Samagra Shiksha Helpline", "समग्र शिक्षा हेल्पलाइन", "1800-11-2001", "education", isTollFree = true),
        HelplineNumber("PF_1800118005", "EPFO PF Helpline", "EPFO PF हेल्पलाइन", "1800-118-005", "labour", isTollFree = true),
        HelplineNumber("MUDRA_1800111", "MUDRA Loan Helpline", "मुद्रा लोन हेल्पलाइन", "1800-180-1111", "financial", isTollFree = true)
    )
}
