package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument

actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray, password: String?): PdfDocument {
        throw NotImplementedError("JS rendering not yet implemented")
    }
}
