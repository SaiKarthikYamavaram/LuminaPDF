package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument

expect class PdfDocumentFactory() {
    suspend fun open(bytes: ByteArray): PdfDocument
}
