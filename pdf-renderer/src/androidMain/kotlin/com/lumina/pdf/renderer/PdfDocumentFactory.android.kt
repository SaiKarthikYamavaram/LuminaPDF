package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfPasswordRequiredException
import com.lumina.pdf.core.PdfPasswordUnsupportedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import android.graphics.pdf.LoadParams
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray, password: String?): PdfDocument = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("pdf_doc_", ".pdf")
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { it.write(bytes) }

        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try {
            // PdfRenderer only accepts a password from API 35 onward (LoadParams). Below
            // that there's no way to unlock a protected PDF via this API at all.
            if (Build.VERSION.SDK_INT >= 35) {
                val loadParams = LoadParams.Builder()
                    .setPassword(password ?: "")
                    .build()
                PdfRenderer(pfd, loadParams)
            } else if (password != null) {
                pfd.close()
                tempFile.delete()
                throw PdfPasswordUnsupportedException()
            } else {
                PdfRenderer(pfd)
            }
        } catch (e: SecurityException) {
            pfd.close()
            tempFile.delete()
            if (Build.VERSION.SDK_INT < 35) throw PdfPasswordUnsupportedException()
            throw PdfPasswordRequiredException(wrongPasswordTried = password != null)
        }
        AndroidPdfDocument(renderer, pfd, tempFile, password)
    }
}

// PDFBox reads its font and character tables from app assets, so it needs a Context first.
fun initPdfRenderer(context: Context) = PDFBoxResourceLoader.init(context)
