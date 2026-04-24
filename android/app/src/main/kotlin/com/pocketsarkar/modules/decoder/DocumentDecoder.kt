package com.pocketsarkar.modules.decoder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pocketsarkar.ai.mediapipe.GemmaEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Document Decoder — Phase 4.
 *
 * Full input pipeline:
 *   CameraImage  → ImagePreprocessor → Gemma vision  ──┐
 *   GalleryImage → load Bitmap       → ImagePreprocessor ┘
 *                                    → if vision fails → ML Kit OCR → text prompt
 *   PdfFile      → PdfRenderer (pages 0–4) → ML Kit OCR → text prompt
 *   PlainText    ─────────────────────────────────────→ text prompt
 *
 * System prompt is loaded from assets — NEVER hardcoded (Phase 1 bug fix rule 3).
 * Response is raw JSON — call [DecoderResponseParser.parse] on the final string.
 */
class DocumentDecoder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemma: GemmaEngine,
    private val preprocessor: ImagePreprocessor,
) {

    /** Loaded once from versioned asset file — never a hardcoded constant. */
    private val systemPrompt: String by lazy {
        context.assets
            .open("ai/prompts/decoder/system_prompt.txt")
            .bufferedReader()
            .readText()
            .trim()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Streams the raw AI JSON response token by token.
     * Collect all tokens into a String, then call [DecoderResponseParser.parse].
     */
    fun decodeStream(input: DocumentInput): Flow<String> = flow {
        when (input) {
            is DocumentInput.PlainText -> {
                streamText(input.text).collect { emit(it) }
            }

            is DocumentInput.PdfFile -> {
                val text = extractTextFromPdf(input.uri)
                if (text.isNullOrBlank()) {
                    emit(errorJson("Could not extract text from PDF. Is it a scanned PDF? Try a photo instead."))
                    return@flow
                }
                streamText(text).collect { emit(it) }
            }

            is DocumentInput.CameraImage -> {
                streamImage(input.bitmap).collect { emit(it) }
            }

            is DocumentInput.GalleryImage -> {
                val bitmap = loadBitmapFromUri(input.uri)
                if (bitmap == null) {
                    emit(errorJson("Could not open image. Please try a different file."))
                    return@flow
                }
                streamImage(bitmap).collect { emit(it) }
            }
        }
    }

    // ── Private pipeline steps ────────────────────────────────────────────────

    private fun streamText(rawText: String): Flow<String> = flow {
        if (!ensureModelAvailable()) {
            emit(errorJson("AI model not available. Please download the model first."))
            return@flow
        }
        val prompt = buildTextPrompt(rawText)
        gemma.generateStream(
            systemPrompt = systemPrompt,
            userPrompt = prompt,
        ).collect { emit(it) }
    }

    private fun streamImage(rawBitmap: Bitmap): Flow<String> = flow {
        val processed = withContext(Dispatchers.Default) { preprocessor.prepare(rawBitmap) }

        if (!ensureModelAvailable()) {
            // Model missing — fall straight to OCR path
            streamViaOcr(processed).collect { emit(it) }
            return@flow
        }

        // Attempt Gemma vision first
        var visionFailed = false
        try {
            gemma.generateWithImage(
                systemPrompt = systemPrompt,
                userPrompt   = buildVisionPrompt(),
                image        = processed,
            ).collect { emit(it) }
        } catch (e: Exception) {
            visionFailed = true
        }

        // Fall back to ML Kit OCR if vision threw
        if (visionFailed) {
            streamViaOcr(processed).collect { emit(it) }
        }
    }

    private fun streamViaOcr(bitmap: Bitmap): Flow<String> = flow {
        val ocrText = runMlKitOcr(bitmap)
        if (ocrText.isBlank()) {
            emit(errorJson("Could not read document. Please try a clearer, better-lit photo."))
            return@flow
        }
        streamText(ocrText).collect { emit(it) }
    }

    // ── PDF extraction ────────────────────────────────────────────────────────

    private suspend fun extractTextFromPdf(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext null
                val renderer = PdfRenderer(pfd)
                val sb = StringBuilder()

                val pageCount = minOf(renderer.pageCount, 5) // max 5 pages to keep memory sane
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    // Render at 2× native resolution for better OCR accuracy
                    val bmp = Bitmap.createBitmap(
                        page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
                    )
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageText = runMlKitOcr(bmp)
                    if (pageText.isNotBlank()) {
                        sb.append("--- Page ${i + 1} ---\n")
                        sb.append(pageText)
                        sb.append("\n\n")
                    }
                }

                renderer.close()
                pfd.close()
                sb.toString().trim().ifBlank { null }
            }.getOrNull()
        }

    // ── ML Kit OCR ────────────────────────────────────────────────────────────

    /**
     * Runs ML Kit Latin OCR on the given bitmap.
     * Uses suspendCancellableCoroutine — no play-services-tasks dependency required.
     */
    private suspend fun runMlKitOcr(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        if (cont.isActive) cont.resume(result.text)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume("")
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume("")
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun ensureModelAvailable(): Boolean =
        runCatching { gemma.ensureLoaded(); true }.getOrDefault(false)

    private fun loadBitmapFromUri(uri: Uri): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }.getOrNull()

    private fun buildVisionPrompt(): String =
        "Analyze every detail of this document image. Return ONLY the JSON as specified."

    private fun buildTextPrompt(text: String): String =
        "Analyze this document:\n\n$text\n\nReturn ONLY the JSON as specified. No other text."

    private fun errorJson(message: String): String =
        """{"documentType":"Error","languageDetected":"Unknown","riskScore":0,"redFlags":[],"userRights":[],"suggestedQuestions":[],"actionRequired":"$message","summary":"$message"}"""
}
