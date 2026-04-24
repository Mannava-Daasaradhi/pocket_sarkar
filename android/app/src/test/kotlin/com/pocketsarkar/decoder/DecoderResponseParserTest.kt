package com.pocketsarkar.decoder

import com.pocketsarkar.modules.decoder.DecoderResponseParser
import com.pocketsarkar.modules.decoder.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DecoderResponseParser].
 *
 * These tests run on the JVM — no Android emulator required.
 * Covers all four spec-mandated cases plus additional robustness checks.
 */
class DecoderResponseParserTest {

    private lateinit var parser: DecoderResponseParser

    @Before
    fun setUp() {
        parser = DecoderResponseParser()
    }

    // ── Required test 1: Valid JSON → correct DecoderResult ──────────────────

    @Test
    fun `valid JSON response parses into DecoderResult with correct risk level`() {
        val json = """
            {
              "documentType": "Rental Agreement",
              "languageDetected": "English",
              "riskScore": 82,
              "redFlags": [
                {
                  "clause": "sole and absolute discretion of the Landlord",
                  "risk": "The landlord can keep your deposit for any reason and you have no way to challenge it.",
                  "severity": "HIGH"
                },
                {
                  "clause": "irrevocably authorizes ECS auto-debit",
                  "risk": "Money can be taken from your bank account at any time without your consent.",
                  "severity": "HIGH"
                }
              ],
              "userRights": [
                "You have the right to receive your security deposit back within a reasonable time.",
                "You can approach the Rent Control Authority for disputes."
              ],
              "suggestedQuestions": [
                "Can you show me the written criteria you use to assess deposit deductions?",
                "Why is the lock-in period 24 months instead of the 11 months you mentioned?",
                "Can I cancel the auto-debit mandate if needed?"
              ],
              "actionRequired": "Do not sign this agreement until the deposit refund criteria are defined in writing.",
              "summary": "This rental agreement is heavily weighted in the landlord's favour. The deposit refund is at the landlord's sole discretion with no objective criteria. There are multiple HIGH risk clauses that could cost you significantly."
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertNotNull("Parser should return a result for valid JSON", result)
        result!!

        assertEquals("Rental Agreement", result.documentType)
        assertEquals("English", result.languageDetected)
        assertEquals(82, result.riskScore)
        assertEquals(RiskLevel.RED, result.riskLevel)
        assertEquals(2, result.redFlags.size)
        assertEquals("HIGH", result.redFlags[0].severity)
        assertEquals("HIGH", result.redFlags[1].severity)
        assertEquals(2, result.userRights.size)
        assertEquals(3, result.suggestedQuestions.size)
        assertNotNull(result.actionRequired)
        assertNotNull(result.summary)
    }

    // ── Required test 2: Markdown-wrapped JSON still parses ──────────────────

    @Test
    fun `markdown fenced JSON parses correctly`() {
        val fencedJson = """
            ```json
            {
              "documentType": "Loan T&C",
              "languageDetected": "English",
              "riskScore": 75,
              "redFlags": [
                {
                  "clause": "penalty-on-penalty compound interest",
                  "risk": "You will owe interest on your interest, which can spiral out of control quickly.",
                  "severity": "HIGH"
                }
              ],
              "userRights": ["You have the right to a copy of the loan agreement before signing."],
              "suggestedQuestions": ["What is the actual APR including all fees?"],
              "actionRequired": "Request a full amortisation schedule showing the effect of the penalty clauses.",
              "summary": "This loan has hidden penalty clauses that can cause your debt to multiply rapidly."
            }
            ```
        """.trimIndent()

        val result = parser.parse(fencedJson)

        assertNotNull("Markdown-fenced JSON should parse successfully", result)
        result!!

        assertEquals("Loan T&C", result.documentType)
        assertEquals(75, result.riskScore)
        assertEquals(RiskLevel.RED, result.riskLevel)
        assertEquals(1, result.redFlags.size)
        assertEquals("HIGH", result.redFlags[0].severity)
    }

    @Test
    fun `triple backtick without json label still parses`() {
        val fenced = """
            ```
            {"documentType":"Court Notice","languageDetected":"English","riskScore":45,"redFlags":[],"userRights":["You have 30 days to respond."],"suggestedQuestions":["Should I hire a lawyer?"],"actionRequired":"Appear in court on the given date.","summary":"A court has summoned you. You must respond or a judgment may be passed in your absence."}
            ```
        """.trimIndent()

        val result = parser.parse(fenced)
        assertNotNull(result)
        assertEquals("Court Notice", result!!.documentType)
        assertEquals(RiskLevel.YELLOW, result.riskLevel)
    }

    // ── Required test 3: Missing fields → null or safe defaults ─────────────

    @Test
    fun `missing redFlags field returns empty list not null`() {
        val json = """
            {
              "documentType": "Employment Bond",
              "languageDetected": "Hindi",
              "riskScore": 20,
              "userRights": ["You can resign with notice."],
              "suggestedQuestions": ["What is the notice period?"],
              "actionRequired": "Read the bond carefully.",
              "summary": "Standard employment agreement with reasonable terms."
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertNotNull(result)
        assertEquals(emptyList<Any>(), result!!.redFlags)
        assertEquals(RiskLevel.GREEN, result.riskLevel)
    }

    @Test
    fun `missing all optional fields returns result with safe defaults`() {
        val json = """{"riskScore": 10}"""

        val result = parser.parse(json)

        assertNotNull("Minimal JSON should still return a result with defaults", result)
        result!!

        assertEquals("Unknown Document", result.documentType)
        assertEquals("Unknown", result.languageDetected)
        assertEquals(10, result.riskScore)
        assertEquals(RiskLevel.GREEN, result.riskLevel)
        assertEquals(emptyList<Any>(), result.redFlags)
        assertEquals(emptyList<Any>(), result.userRights)
        assertEquals(emptyList<Any>(), result.suggestedQuestions)
    }

    @Test
    fun `completely unparseable response returns null`() {
        val garbage = "Sorry, I cannot analyze this document at this time."
        val result = parser.parse(garbage)
        assertNull("Garbage response should return null", result)
    }

    @Test
    fun `empty string returns null`() {
        assertNull(parser.parse(""))
        assertNull(parser.parse("   "))
        assertNull(parser.parse("\n\n\n"))
    }

    // ── Required test 4: riskScore=150 is clamped to 100 ────────────────────

    @Test
    fun `riskScore above 100 is clamped to 100`() {
        val json = """
            {
              "documentType": "Scam Contract",
              "languageDetected": "English",
              "riskScore": 150,
              "redFlags": [],
              "userRights": [],
              "suggestedQuestions": [],
              "actionRequired": "Do not sign.",
              "summary": "This contract is extremely dangerous."
            }
        """.trimIndent()

        val result = parser.parse(json)

        assertNotNull(result)
        assertEquals("riskScore should be clamped to 100", 100, result!!.riskScore)
        assertEquals(RiskLevel.RED, result.riskLevel)
    }

    @Test
    fun `negative riskScore is clamped to 0`() {
        val json = """{"riskScore": -50, "redFlags": []}"""
        val result = parser.parse(json)
        assertNotNull(result)
        assertEquals(0, result!!.riskScore)
        assertEquals(RiskLevel.GREEN, result.riskLevel)
    }

    // ── Risk level boundary tests ─────────────────────────────────────────────

    @Test
    fun `riskScore 30 maps to GREEN`() {
        assertEquals(RiskLevel.GREEN, RiskLevel.fromScore(30))
    }

    @Test
    fun `riskScore 31 maps to YELLOW`() {
        assertEquals(RiskLevel.YELLOW, RiskLevel.fromScore(31))
    }

    @Test
    fun `riskScore 60 maps to YELLOW`() {
        assertEquals(RiskLevel.YELLOW, RiskLevel.fromScore(60))
    }

    @Test
    fun `riskScore 61 maps to RED`() {
        assertEquals(RiskLevel.RED, RiskLevel.fromScore(61))
    }

    @Test
    fun `riskScore 100 maps to RED`() {
        assertEquals(RiskLevel.RED, RiskLevel.fromScore(100))
    }

    @Test
    fun `riskScore 0 maps to GREEN`() {
        assertEquals(RiskLevel.GREEN, RiskLevel.fromScore(0))
    }

    // ── Severity normalisation ────────────────────────────────────────────────

    @Test
    fun `unknown severity defaults to MEDIUM`() {
        val json = """
            {
              "riskScore": 55,
              "redFlags": [
                {"clause": "something weird", "risk": "unclear", "severity": "CRITICAL"}
              ],
              "userRights": [],
              "suggestedQuestions": [],
              "actionRequired": "Review carefully.",
              "summary": "Moderate risk document."
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertNotNull(result)
        assertEquals("MEDIUM", result!!.redFlags[0].severity)
    }

    @Test
    fun `HIGH and LOW severity pass through unchanged`() {
        val json = """
            {
              "riskScore": 70,
              "redFlags": [
                {"clause": "auto-debit mandate", "risk": "risk", "severity": "HIGH"},
                {"clause": "30-day notice period", "risk": "minor", "severity": "LOW"}
              ],
              "userRights": [],
              "suggestedQuestions": [],
              "actionRequired": "Consult a lawyer.",
              "summary": "High risk."
            }
        """.trimIndent()

        val result = parser.parse(json)
        assertNotNull(result)
        assertEquals("HIGH", result!!.redFlags[0].severity)
        assertEquals("LOW",  result.redFlags[1].severity)
    }

    // ── JSON with leading text before the object ──────────────────────────────

    @Test
    fun `JSON preceded by preamble text is extracted and parsed`() {
        val messy = """
            Sure! Here is the analysis of the document you provided:
            
            {
              "documentType": "Rental Agreement",
              "languageDetected": "English",
              "riskScore": 65,
              "redFlags": [],
              "userRights": ["You can approach the rent court."],
              "suggestedQuestions": ["What is the notice period?"],
              "actionRequired": "Do not sign without legal review.",
              "summary": "High risk rental agreement."
            }
            
            I hope this helps!
        """.trimIndent()

        val result = parser.parse(messy)
        assertNotNull("Should extract JSON even with surrounding text", result)
        assertEquals("Rental Agreement", result!!.documentType)
        assertEquals(65, result.riskScore)
    }
}
