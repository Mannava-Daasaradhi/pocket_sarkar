package com.pocketsarkar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pocketsarkar.ai.AiRouter
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import com.pocketsarkar.ai.ollama.OllamaClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GemmaEngineTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun modelAvailabilityCheck() {
        // On emulator with no model file pushed — should return false
        val engine = GemmaEngine(context)
        assertFalse(
            "Model should not be present on clean emulator",
            engine.isModelAvailable()
        )
    }

    @Test
    fun ollamaGenerates() = runBlocking {
        // Requires Ollama running on host with: ollama serve
        // Run with: adb reverse tcp:11434 tcp:11434 first
        val client = OllamaClient()
        val result = client.generate("Say hello in Hindi in one line")
        assertTrue("Ollama response should not be blank", result.isNotBlank())
        println("Ollama response: $result")
    }

    @Test
    fun aiRouterFallsBackToOllamaWhenNoModel() = runBlocking {
        val router = AiRouter(context)
        // Model not present → should use Ollama (requires running Ollama on host)
        assertFalse(router.gemmaEngine.isModelAvailable())
        val result = router.generate("Ek line mein bolo Bharat ki rajdhani kya hai?")
        assertTrue("Router should return a non-blank response via Ollama", result.isNotBlank())
        println("AiRouter response: $result")
    }
}