package com.pocketsarkar.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.ai.ollama.OllamaClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the AI layer.
 *
 * Test 1: Model availability — passes on any emulator with no model pushed.
 * Test 2: Ollama fallback — requires `ollama serve` on the host machine.
 *         Before running: adb reverse tcp:11434 tcp:11434
 * Test 3: AiRouter routing — model absent → Ollama path exercised.
 */
@RunWith(AndroidJUnit4::class)
class GemmaEngineTest {

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    private lateinit var engine: GemmaEngine
    private lateinit var ollamaClient: OllamaClient

    @Before
    fun setUp() {
        engine = GemmaEngine(context)
        ollamaClient = OllamaClient()
    }

    // ── Test 1: Model availability check (does not require model file) ────────

    @Test
    fun modelAvailabilityCheck() {
        // Clean emulator has no model pushed → must be false
        assertFalse(
            "Model should NOT be present on a clean emulator (no adb push done)",
            engine.isModelAvailable()
        )
    }

    // ── Test 2: Ollama fallback (requires Ollama running locally) ─────────────

    @Test
    fun ollamaGenerates() = runBlocking {
        // Prerequisites:
        //   host$ ollama serve
        //   host$ ollama run pocket-sarkar  (or gemma4:e4b)
        //   host$ adb reverse tcp:11434 tcp:11434
        val result = ollamaClient.generate("Say hello in Hindi in one line")
        assertTrue("Ollama response should not be blank", result.isNotBlank())
        println("Ollama response: $result")
    }

    // ── Test 3: AiRouter routes to Ollama when model absent ───────────────────

    @Test
    fun aiRouterFallsBackToOllamaWhenNoModel() = runBlocking {
        val router = AiRouter(context)
        assertFalse(
            "Model must not be present for this test to exercise Ollama path",
            router.gemmaEngine.isModelAvailable()
        )
        val result = router.generate("Ek line mein bolo Bharat ki rajdhani kya hai?")
        assertTrue("Router should return a non-blank response via Ollama", result.isNotBlank())
        println("AiRouter response: $result")
    }
}