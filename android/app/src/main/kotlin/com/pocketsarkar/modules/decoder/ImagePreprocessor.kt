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
 * Applied before passing to Gemma 4 vision encoder:
 * 1. Resize to reasonable dimensions (don't blow up memory)
 * 2. Normalize brightness (handle dark/washed-out scans)
 * 3. Increase contrast (makes text sharper for model)
 *
 * Deskew (rotation correction) is intentionally deferred —
 * Gemma 4's vision encoder handles mild rotation well.
 * If we add Tesseract fallback later, deskew matters more.
 */
class ImagePreprocessor @Inject constructor() {

    companion object {
        private const val MAX_DIMENSION = 1024  // px — balance quality vs memory
        private const val CONTRAST_FACTOR = 1.3f
        private const val BRIGHTNESS_OFFSET = -20f
    }

    fun prepare(input: Bitmap): Bitmap {
        val resized = resize(input)
        return enhanceContrast(resized)
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return bitmap

        val ratio = w.toFloat() / h.toFloat()
        val (newW, newH) = if (w > h) {
            MAX_DIMENSION to (MAX_DIMENSION / ratio).toInt()
        } else {
            (MAX_DIMENSION * ratio).toInt() to MAX_DIMENSION
        }

        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Contrast + brightness matrix
        val scale = CONTRAST_FACTOR
        val translate = BRIGHTNESS_OFFSET

        val matrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
