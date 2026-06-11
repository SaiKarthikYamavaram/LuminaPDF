package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray): PdfDocument = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("pdf_doc_", ".pdf")
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { it.write(bytes) }
        
        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        AndroidPdfDocument(renderer, pfd, tempFile)
    }
}
