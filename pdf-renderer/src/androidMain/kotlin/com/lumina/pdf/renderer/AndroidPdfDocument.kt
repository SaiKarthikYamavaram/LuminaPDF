package com.lumina.pdf.renderer

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.lumina.pdf.core.OutlineItem
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class AndroidPdfDocument(
    private val renderer: PdfRenderer,
    private val pfd: ParcelFileDescriptor,
    private val tempFile: File
) : PdfDocument {
    private val renderMutex = Mutex()

    override suspend fun pageCount(): Int = renderMutex.withLock {
        renderer.pageCount
    }

    override suspend fun renderPage(page: Int, width: Int, height: Int): PdfBitmap = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pdfPage = renderer.openPage(page)
            pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPage.close()
            AndroidPdfBitmap(bitmap)
        }
    }

    override suspend fun renderTile(page: Int, region: PdfRect, scale: Float): PdfBitmap = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val pdfPage = renderer.openPage(page)
            val destWidth = (pdfPage.width * scale).toInt()
            val destHeight = (pdfPage.height * scale).toInt()
            
            val bitmap = Bitmap.createBitmap(region.width.toInt(), region.height.toInt(), Bitmap.Config.ARGB_8888)
            
            val transform = Matrix()
            transform.postScale(scale, scale)
            transform.postTranslate(-region.left, -region.top)

            pdfPage.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfPage.close()
            
            AndroidPdfBitmap(bitmap)
        }
    }

    override suspend fun extractText(page: Int): String {
        // android.graphics.pdf.PdfRenderer does not support text extraction.
        // Requires third party library on Android, or return empty for MVP.
        return ""
    }

    override suspend fun searchText(page: Int, query: String): List<PdfRect> {
        return emptyList()
    }

    override suspend fun getOutline(): List<OutlineItem> {
        return emptyList()
    }

    override fun close() {
        renderer.close()
        pfd.close()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
