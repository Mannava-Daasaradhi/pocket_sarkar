package com.pocketsarkar.modules.decoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import javax.inject.Inject

/**
 * Image preprocessing pipeline for Document Decoder.
 *
 * Applied before passing to Gemma 4 vision encoder or ML Kit OCR:
 *   1. Resize to 768×1024 max (portrait-optimised for A4/letter documents)
 *   2. Enhance contrast (makes text crisp for both model and OCR)
 *   3. Slight brightness correction for dark phone-camera scans
 *
 * Deskew / rotation correction:
 *   Gemma 4's vision encoder handles mild rotation well so we skip
 *   heavyweight OpenCV deskewing here. If we add Tesseract later,
 *   OpenCV deskew should be added before ML Kit OCR.
 */
class ImagePreprocessor @Inject constructor() {

    companion object {
        private const val TARGET_WIDTH  = 768
        private const val TARGET_HEIGHT = 1024
        private const val CONTRAST_FACTOR   = 1.35f
        private const val BRIGHTNESS_OFFSET = -15f
    }

    fun prepare(input: Bitmap): Bitmap {
        val resized = resize(input)
        return enhanceContrast(resized)
    }

    // ── Private steps ─────────────────────────────────────────────────────────

    /**
     * Scale down to fit within 768×1024 while maintaining aspect ratio.
     * Upscaling is intentionally skipped — it adds noise without value.
     */
    private fun resize(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height

        if (w <= TARGET_WIDTH && h <= TARGET_HEIGHT) return bitmap

        val scaleW = TARGET_WIDTH.toFloat() / w
        val scaleH = TARGET_HEIGHT.toFloat() / h
        val scale  = minOf(scaleW, scaleH)   // fit inside box

        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(bitmap, newW, newH, true /*filter=bilinear*/)
    }

    /**
     * Boost contrast and correct slight brightness, making printed text
     * sharper on both light and dark scan backgrounds.
     */
    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint  = Paint()

        val s = CONTRAST_FACTOR
        val t = BRIGHTNESS_OFFSET

        paint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix(floatArrayOf(
                s, 0f, 0f, 0f, t,
                0f,  s, 0f, 0f, t,
                0f, 0f,  s, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            ))
        )
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
