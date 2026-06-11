package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument

actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray): PdfDocument {
        throw NotImplementedError("WasmJS rendering not yet implemented")
    }
}
