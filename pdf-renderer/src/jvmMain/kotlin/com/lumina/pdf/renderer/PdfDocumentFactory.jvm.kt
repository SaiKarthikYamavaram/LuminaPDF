package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfPasswordRequiredException
import org.apache.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException

actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray, password: String?): PdfDocument = withContext(Dispatchers.IO) {
        val document = try {
            // Empty-string password (the default) already satisfies PDFs whose *user*
            // password is blank (only an owner password restricts permissions) - only a PDF
            // with a real user password throws here.
            Loader.loadPDF(bytes, password ?: "")
        } catch (e: InvalidPasswordException) {
            throw PdfPasswordRequiredException(wrongPasswordTried = password != null)
        }
        JvmPdfDocument(document)
    }
}
