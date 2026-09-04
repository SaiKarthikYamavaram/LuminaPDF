package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument

expect class PdfDocumentFactory() {
    // password is unused on iOS/JS/wasmJs - those targets can't open any PDF yet
    // (see their PdfDocumentFactory actuals), independent of encryption.
    suspend fun open(bytes: ByteArray, password: String? = null): PdfDocument
}
