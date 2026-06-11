package com.lumina.pdf.renderer

import com.lumina.pdf.core.OutlineItem
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfRect
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmPdfDocument(private val document: PDDocument) : PdfDocument {
    private val renderer = PDFRenderer(document)

    override suspend fun pageCount(): Int = document.numberOfPages

    override suspend fun renderPage(page: Int, width: Int, height: Int): PdfBitmap = withContext(Dispatchers.IO) {
        val pdPage = document.getPage(page)
        val cropBox = pdPage.cropBox
        val scale = width.toFloat() / cropBox.width
        val image = renderer.renderImage(page, scale)
        JvmPdfBitmap(image)
    }

    override suspend fun renderTile(page: Int, region: PdfRect, scale: Float): PdfBitmap = withContext(Dispatchers.IO) {
        // Basic implementation for MVP, PDFBox rendering might need more specific tile rendering
        // For now, render full and crop (not efficient, but works for Phase 1 MVP)
        // Advanced tile rendering is Phase 2.
        val image = renderer.renderImage(page, scale)
        val subImage = image.getSubimage(
            region.left.toInt().coerceAtLeast(0),
            region.top.toInt().coerceAtLeast(0),
            region.width.toInt().coerceAtMost(image.width - region.left.toInt()),
            region.height.toInt().coerceAtMost(image.height - region.top.toInt())
        )
        JvmPdfBitmap(subImage)
    }

    override suspend fun extractText(page: Int): String = withContext(Dispatchers.IO) {
        val stripper = PDFTextStripper()
        stripper.startPage = page + 1
        stripper.endPage = page + 1
        stripper.getText(document)
    }

    override suspend fun getOutline(): List<OutlineItem> = withContext(Dispatchers.IO) {
        // Simplify for Phase 1
        emptyList()
    }

    override fun close() {
        document.close()
    }
}
