package com.pocketsarkar.tests.ai

import com.pocketsarkar.db.entities.Scheme
import com.pocketsarkar.modules.schemes.ConversationState
import com.pocketsarkar.modules.schemes.FakeDetectionResult
import com.pocketsarkar.modules.schemes.Message
import com.pocketsarkar.modules.schemes.SchemeExplainer
import com.pocketsarkar.modules.schemes.SchemeStreamEvent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI pipeline tests for Pocket Sarkar Scheme Explainer.
 *
 * Tests run against mock infrastructure (no real Gemma or DB) to ensure:
 * 1. Real scheme query → function call intercepted, correct facts returned
 * 2. Fake scheme → isFake=true, ⚠️ warning emitted before any tokens
 * 3. Unknown scheme → exact refusal message, no hallucinated content
 * 4. Multi-turn context → follow-up question references correct scheme
 */
class SchemeExplainerTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun pmKisanScheme() = Scheme(
        id               = "PM_KISAN_001",
        nameEn           = "PM Kisan Samman Nidhi",
        nameHi           = "प्रधान मंत्री किसान सम्मान निधि",
        category         = "AGRICULTURE",
        ministryEn       = "Ministry of Agriculture & Farmers Welfare",
        descriptionEn    = "Income support of ₹6,000 per year to small and marginal farmers in three equal instalments.",
        descriptionHi    = "छोटे और सीमांत किसानों को ₹6,000 प्रति वर्ष तीन समान किश्तों में दिया जाता है।",
        benefitAmount    = "₹6,000 per year (3 instalments of ₹2,000)",
        benefitType      = "CASH",
        targetStates     = "ALL",
        targetGender     = "ALL",
        targetCategory   = "ALL",
        maxIncomeLPA     = 0.0,  // no income limit
        minAge           = 18,
        portalUrl        = "https://pmkisan.gov.in",
        confidenceScore  = 0.95,
        lastVerifiedEpoch = 1706745600000L,  // Feb 2024
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1 — Real scheme query: PM Kisan
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GIVEN: User asks "Gorakhpur ke kisan ko PM Kisan mein kitna milta hai?"
     * THEN:
     *   - query_scheme_db function call is intercepted (SchemeDao is called)
     *   - FunctionCallExecuted event is emitted
     *   - Final response contains "₹6,000" or "6000"
     *   - Response mentions correct eligibility (farmers / kisan)
     */
    @Test
    fun `real scheme query - PM Kisan returns correct benefit amount and calls function`() = runTest {
        // Arrange
        val schemeDao    = mockk<com.pocketsarkar.db.dao.SchemeDao>()
        val gemmaEngine  = mockk<com.pocketsarkar.ai.mediapipe.GemmaEngine>()
        val eligEngine   = mockk<com.pocketsarkar.modules.schemes.EligibilityEngine>()
        val context      = mockk<android.content.Context>(relaxed = true)

        val pmKisan = pmKisanScheme()

        // Gemma Pass 1 emits the function call
        coEvery { gemmaEngine.ensureLoaded() } returns mockk()
        coEvery {
            gemmaEngine.generate(
                systemPrompt        = any(),
                userPrompt          = match { it.contains("PM Kisan") || it.contains("kisan") },
                conversationHistory = any(),
            )
        } returns """[FUNCTION_CALL: query_scheme_db({"query": "PM Kisan Samman Nidhi", "state": "UP", "category": "AGRICULTURE"})]"""

        // SchemeDao returns PM Kisan
        coEvery { schemeDao.searchSchemes(any(), any()) } returns listOf(pmKisan)

        // Gemma Pass 3 generates explanation with the ₹6,000 fact
        coEvery {
            gemmaEngine.generateStream(
                systemPrompt        = any(),
                userPrompt          = match { it.contains("₹6,000") || it.contains("PM_KISAN_001") },
                conversationHistory = any(),
            )
        } returns flowOf(
            "PM Kisan Samman Nidhi mein ",
            "₹6,000 per year milte hain ",
            "(as of last verification date: 01 Feb 2024). ",
            "Teen instalments mein diya jaata hai — ₹2,000 each. ",
            "Eligible: chhote aur seemant kisan (small and marginal farmers). ",
            "Aur jaankari ke liye: https://pmkisan.gov.in",
        )

        // Fake detection returns clean
        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("fake") || it.contains("fraud") || it.contains("RED FLAGS") },
                userPrompt   = any(),
                conversationHistory = any(),
            )
        } returns """{"isFake": false, "confidence": 0.05, "reasons": [], "flagsDetected": []}"""

        coEvery { context.assets } returns mockk(relaxed = true) {
            coEvery { open(any()) } throws java.io.IOException("test - use fallback")
        }

        val explainer = SchemeExplainer(gemmaEngine, schemeDao, eligEngine, context)

        // Act
        val events = explainer.query(
            userMessage  = "Gorakhpur ke kisan ko PM Kisan mein kitna milta hai?",
            userLanguage = "hi",
        ).toList()

        // Assert — FunctionCallExecuted was emitted
        val funcCallEvent = events.filterIsInstance<SchemeStreamEvent.FunctionCallExecuted>()
        assertTrue(
            "Expected FunctionCallExecuted event — query_scheme_db must be called",
            funcCallEvent.isNotEmpty()
        )
        assertTrue(
            "Expected at least 1 result from DB",
            funcCallEvent.first().resultCount >= 1
        )

        // Assert — response contains ₹6,000
        val completeEvent = events.filterIsInstance<SchemeStreamEvent.Complete>().firstOrNull()
        assertFalse("Expected Complete event", completeEvent == null)
        val responseText = completeEvent!!.fullText
        assertTrue(
            "Response must contain '₹6,000' or '6000' — got: $responseText",
            responseText.contains("₹6,000") || responseText.contains("6000") || responseText.contains("6,000")
        )

        // Assert — fake detection was clean
        val fakeEvent = events.filterIsInstance<SchemeStreamEvent.FakeDetectionComplete>().first()
        assertFalse("PM Kisan should NOT be flagged as fake", fakeEvent.result.isFake)

        // Assert — SchemeDao was called (function call was intercepted)
        coVerify { schemeDao.searchSchemes(any(), any()) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2 — Fake scheme detection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GIVEN: User asks "Mujhe batao PM Modi Cash Scheme mein 5 lakh kaise milenge abhi"
     * THEN:
     *   - FakeDetectionComplete(isFake=true) is emitted BEFORE any Token events
     *   - confidence > 0.7
     *   - reasons list is non-empty
     */
    @Test
    fun `fake scheme detection - PM Modi Cash Scheme triggers warning before response`() = runTest {
        val schemeDao   = mockk<com.pocketsarkar.db.dao.SchemeDao>()
        val gemmaEngine = mockk<com.pocketsarkar.ai.mediapipe.GemmaEngine>()
        val eligEngine  = mockk<com.pocketsarkar.modules.schemes.EligibilityEngine>()
        val context     = mockk<android.content.Context>(relaxed = true)

        coEvery { gemmaEngine.ensureLoaded() } returns mockk()

        // Fake detection returns HIGH confidence fraud
        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("fake") || it.contains("RED FLAGS") || it.contains("classifier") },
                userPrompt   = match { it.contains("Modi Cash") || it.contains("5 lakh") },
                conversationHistory = any(),
            )
        } returns """{
            "isFake": true,
            "confidence": 0.90,
            "reasons": [
                "PM Modi Cash Scheme koi official sarkari yojana nahi hai",
                "Turant 5 lakh milne ka claim asli scheme mein nahi hota",
                "Koi ministry ya vibhag ka naam nahi hai"
            ],
            "flagsDetected": ["INSTANT_CASH", "NOT_IN_DB", "UNVERIFIABLE_AMOUNT", "NO_MINISTRY"]
        }"""

        // Tool call and explanation still run after fake detection
        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("tool-call") || it.contains("router") || it.contains("FUNCTION_CALL") },
                userPrompt   = any(),
                conversationHistory = any(),
            )
        } returns """[FUNCTION_CALL: query_scheme_db({"query": "PM Modi Cash Scheme", "state": "ALL", "category": "ALL"})]"""

        coEvery { schemeDao.searchSchemes(any(), any()) } returns emptyList()

        coEvery {
            gemmaEngine.generateStream(any(), any(), any())
        } returns flowOf("Mujhe yeh scheme nahi mili. Kripya myscheme.gov.in dekhein.")

        coEvery { context.assets } returns mockk(relaxed = true) {
            coEvery { open(any()) } throws java.io.IOException("test - use fallback")
        }

        val explainer = SchemeExplainer(gemmaEngine, schemeDao, eligEngine, context)

        val events = explainer.query(
            userMessage  = "Mujhe batao PM Modi Cash Scheme mein 5 lakh kaise milenge abhi",
            userLanguage = "hi",
        ).toList()

        // Assert — FakeDetectionComplete is the FIRST event
        assertTrue("Events list must not be empty", events.isNotEmpty())
        assertTrue(
            "First event MUST be FakeDetectionComplete (fake check runs before everything)",
            events[0] is SchemeStreamEvent.FakeDetectionComplete
        )

        val fakeEvent = events[0] as SchemeStreamEvent.FakeDetectionComplete
        assertTrue("isFake must be true for PM Modi Cash Scheme", fakeEvent.result.isFake)
        assertTrue("Confidence must be > 0.7, got: ${fakeEvent.result.confidence}",
            fakeEvent.result.confidence > 0.7)
        assertTrue("Reasons list must not be empty", fakeEvent.result.reasons.isNotEmpty())

        // Assert — FakeDetectionComplete comes BEFORE any Token event
        val fakeIndex  = events.indexOfFirst { it is SchemeStreamEvent.FakeDetectionComplete }
        val firstToken = events.indexOfFirst { it is SchemeStreamEvent.Token }
        if (firstToken >= 0) {
            assertTrue(
                "FakeDetectionComplete (idx=$fakeIndex) must come BEFORE first Token (idx=$firstToken)",
                fakeIndex < firstToken
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3 — Unknown scheme → exact refusal, no hallucination
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GIVEN: User asks "Sarkar ki XYZ Golden Yojana kya hai?"
     * THEN:
     *   - DB returns empty list
     *   - Response contains the exact refusal message
     *   - Response does NOT contain invented scheme details
     */
    @Test
    fun `unknown scheme returns exact refusal message without hallucinating`() = runTest {
        val schemeDao   = mockk<com.pocketsarkar.db.dao.SchemeDao>()
        val gemmaEngine = mockk<com.pocketsarkar.ai.mediapipe.GemmaEngine>()
        val eligEngine  = mockk<com.pocketsarkar.modules.schemes.EligibilityEngine>()
        val context     = mockk<android.content.Context>(relaxed = true)

        coEvery { gemmaEngine.ensureLoaded() } returns mockk()

        // Fake detection — low confidence (scheme exists but unknown)
        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("fake") || it.contains("RED FLAGS") },
                userPrompt   = any(),
                conversationHistory = any(),
            )
        } returns """{"isFake": false, "confidence": 0.25, "reasons": ["Unknown scheme name"], "flagsDetected": ["NOT_IN_DB"]}"""

        // Tool call
        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("tool-call") || it.contains("router") || it.contains("FUNCTION_CALL") },
                userPrompt   = any(),
                conversationHistory = any(),
            )
        } returns """[FUNCTION_CALL: query_scheme_db({"query": "XYZ Golden Yojana", "state": "ALL", "category": "ALL"})]"""

        // DB returns EMPTY — scheme does not exist
        coEvery { schemeDao.searchSchemes("XYZ Golden Yojana", any()) } returns emptyList()
        coEvery { schemeDao.searchSchemes(any(), any()) } returns emptyList()

        // Explainer must produce the refusal message
        val refusalMessage = "Mujhe yeh scheme nahi mili. Kripya myscheme.gov.in dekhein."
        coEvery {
            gemmaEngine.generateStream(
                systemPrompt = any(),
                userPrompt   = match { it.contains("found: false") || it.contains("NOT FOUND") || it.contains("No matching") },
                conversationHistory = any(),
            )
        } returns flowOf(refusalMessage)

        coEvery { context.assets } returns mockk(relaxed = true) {
            coEvery { open(any()) } throws java.io.IOException("test - use fallback")
        }

        val explainer = SchemeExplainer(gemmaEngine, schemeDao, eligEngine, context)

        val events = explainer.query(
            userMessage  = "Sarkar ki XYZ Golden Yojana kya hai?",
            userLanguage = "hi",
        ).toList()

        val completeEvent = events.filterIsInstance<SchemeStreamEvent.Complete>().firstOrNull()
        assertFalse("Expected Complete event to be emitted", completeEvent == null)
        val responseText = completeEvent!!.fullText

        // Response must contain the refusal message
        assertTrue(
            "Response must contain refusal message. Got: $responseText",
            responseText.contains("nahi mili") || responseText.contains("myscheme.gov.in")
        )

        // Response must NOT contain hallucinated content
        val hallucinations = listOf("₹", "benefit", "lakh", "eligibility", "apply")
        hallucinations.forEach { word ->
            assertFalse(
                "Response MUST NOT contain hallucinated word '$word'. Got: $responseText",
                responseText.lowercase().contains(word.lowercase()) &&
                    !responseText.contains("myscheme.gov.in")  // applicationUrl is allowed
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4 — Multi-turn context
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GIVEN:
     *   Turn 1: "PM Kisan kya hai?"
     *   Turn 2: "Main eligible hun kya?"   ← no scheme named
     * THEN:
     *   Turn 2 tool call prompt includes "PM Kisan" context
     *   Turn 2 response references PM Kisan eligibility correctly
     */
    @Test
    fun `multi-turn context - follow-up question uses current scheme context`() = runTest {
        val schemeDao   = mockk<com.pocketsarkar.db.dao.SchemeDao>()
        val gemmaEngine = mockk<com.pocketsarkar.ai.mediapipe.GemmaEngine>()
        val eligEngine  = mockk<com.pocketsarkar.modules.schemes.EligibilityEngine>()
        val context     = mockk<android.content.Context>(relaxed = true)

        val pmKisan = pmKisanScheme()

        coEvery { gemmaEngine.ensureLoaded() } returns mockk()
        coEvery { context.assets } returns mockk(relaxed = true) {
            coEvery { open(any()) } throws java.io.IOException("test - use fallback")
        }

        // Fake detection always clean
        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("fake") || it.contains("RED FLAGS") },
                userPrompt   = any(),
                conversationHistory = any(),
            )
        } returns """{"isFake": false, "confidence": 0.05, "reasons": [], "flagsDetected": []}"""

        // ── Turn 1: "PM Kisan kya hai?" ──────────────────────────────────────

        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("tool-call") || it.contains("FUNCTION_CALL") },
                userPrompt   = match { it.contains("PM Kisan kya hai") },
                conversationHistory = any(),
            )
        } returns """[FUNCTION_CALL: query_scheme_db({"query": "PM Kisan Samman Nidhi", "state": "ALL", "category": "AGRICULTURE"})]"""

        coEvery { schemeDao.searchSchemes(any(), any()) } returns listOf(pmKisan)

        coEvery {
            gemmaEngine.generateStream(
                systemPrompt = any(),
                userPrompt   = match { it.contains("PM_KISAN_001") || it.contains("₹6,000") },
                conversationHistory = any(),
            )
        } returns flowOf("PM Kisan ek agricultural scheme hai jisme ₹6,000 milte hain. Aur jaankari ke liye: https://pmkisan.gov.in")

        val explainer = SchemeExplainer(gemmaEngine, schemeDao, eligEngine, context)

        // Execute Turn 1
        explainer.query("PM Kisan kya hai?", userLanguage = "hi").toList()

        // Verify scheme context was stored after Turn 1
        val stateAfterTurn1 = explainer.conversationState
        assertEquals(
            "After Turn 1, currentSchemeContext must be PM Kisan",
            "PM_KISAN_001",
            stateAfterTurn1.currentSchemeContext?.id
        )
        assertEquals("Messages list must have 2 entries after Turn 1", 2, stateAfterTurn1.messages.size)

        // ── Turn 2: "Main eligible hun kya?" ─────────────────────────────────
        // No scheme named — must use context from Turn 1

        val turn2ToolCallPromptSlot = slot<String>()
        coEvery {
            gemmaEngine.generate(
                systemPrompt = match { it.contains("tool-call") || it.contains("FUNCTION_CALL") },
                userPrompt   = capture(turn2ToolCallPromptSlot),
                conversationHistory = any(),
            )
        } returns """[FUNCTION_CALL: query_scheme_db({"query": "PM Kisan Samman Nidhi eligibility", "state": "ALL", "category": "AGRICULTURE"})]"""

        coEvery {
            gemmaEngine.generateStream(
                systemPrompt = any(),
                userPrompt   = any(),
                conversationHistory = any(),
            )
        } returns flowOf(
            "PM Kisan ke liye eligible hone ke liye aapko ek chhota ya seemant kisan hona chahiye. ",
            "Land holding 2 hectare se kam honi chahiye. ",
            "Aur jaankari ke liye: https://pmkisan.gov.in"
        )

        explainer.query("Main eligible hun kya?", userLanguage = "hi").toList()

        // Assert — Turn 2 tool call prompt MUST contain PM Kisan context
        val turn2Prompt = turn2ToolCallPromptSlot.captured
        assertTrue(
            "Turn 2 tool call prompt must contain PM Kisan context (from Turn 1). Got: $turn2Prompt",
            turn2Prompt.contains("PM Kisan") ||
                turn2Prompt.contains("PM_KISAN_001") ||
                turn2Prompt.contains("Currently discussing")
        )

        // Assert — conversation has 4 messages (2 per turn)
        val stateAfterTurn2 = explainer.conversationState
        assertEquals("After 2 turns, messages list must have 4 entries", 4, stateAfterTurn2.messages.size)

        // Assert — scheme context still PM Kisan after Turn 2
        assertEquals(
            "PM Kisan context must persist through Turn 2",
            "PM_KISAN_001",
            stateAfterTurn2.currentSchemeContext?.id
        )
    }
}

// Convenience extension — avoids import of coVerify from io.mockk in test body
private fun coVerify(block: suspend () -> Unit) {
    io.mockk.coVerify { block() }
}