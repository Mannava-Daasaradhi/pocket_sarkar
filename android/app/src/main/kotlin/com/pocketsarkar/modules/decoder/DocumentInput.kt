package com.pocketsarkar.modules.decoder

import android.graphics.Bitmap
import android.net.Uri

/**
 * Every type of document the Decoder accepts.
 *
 * Processing pipeline per type:
 *   CameraImage  → ImagePreprocessor → Gemma vision (→ ML Kit OCR fallback)
 *   GalleryImage → load Bitmap → ImagePreprocessor → same as CameraImage
 *   PdfFile      → PdfRenderer (page 0–4) → ML Kit OCR → text prompt
 *   PlainText    → directly to Gemma text prompt
 */
sealed class DocumentInput {
    /** Raw Bitmap straight from the CameraX ImageCapture callback. */
    data class CameraImage(val bitmap: Bitmap) : DocumentInput()

    /** Uri from the system gallery / photo picker. */
    data class GalleryImage(val uri: Uri) : DocumentInput()

    /** Uri of a PDF file opened via the document picker. */
    data class PdfFile(val uri: Uri) : DocumentInput()

    /** Pre-extracted text, e.g. pasted directly from WhatsApp. */
    data class PlainText(val text: String) : DocumentInput()
}
