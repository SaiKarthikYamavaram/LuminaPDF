package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument
import org.apache.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.apache.pdfbox.Loader

actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray): PdfDocument = withContext(Dispatchers.IO) {
        val document = Loader.loadPDF(bytes)
        JvmPdfDocument(document)
    }
}
