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
import kotlinx.coroutines.withContext
import java.io.File

class AndroidPdfDocument(
    private val renderer: PdfRenderer,
    private val pfd: ParcelFileDescriptor,
    private val tempFile: File
) : PdfDocument {

    override suspend fun pageCount(): Int = renderer.pageCount

    override suspend fun renderPage(page: Int, width: Int, height: Int): PdfBitmap = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pdfPage = renderer.openPage(page)
        pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        pdfPage.close()
        AndroidPdfBitmap(bitmap)
    }

    override suspend fun renderTile(
        page: Int,
        region: PdfRect,
        scale: Float,
        fullPageWidthPx: Int,
        fullPageHeightPx: Int
    ): PdfBitmap = withContext(Dispatchers.IO) {
        val pdfPage = renderer.openPage(page)

        // Destination bitmap must be the SCALED region size (what the tile grid expects, e.g.
        // 512x512), not the raw region size in page space - creating it at the unscaled size
        // silently cropped every tile to 1/scale^2 of its intended area, leaving the rest of each
        // tile's slot blank (showing the blurry full-page thumbnail underneath instead).
        val tileWidthPx = (region.width * scale).toInt().coerceAtLeast(1)
        val tileHeightPx = (region.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(tileWidthPx, tileHeightPx, Bitmap.Config.ARGB_8888)

        // region's coordinates are pixels in a fullPageWidthPx x fullPageHeightPx rendering of the
        // whole page (the PdfDocument.renderTile contract), NOT the page's own point-space that
        // PdfRenderer.Page.render()'s transform actually operates in. pdfPage.width/height ARE in
        // points, so this ratio converts one to the other; using region's raw numbers directly
        // (as a prior version of this code did) cropped from the wrong location for any page whose
        // point size didn't happen to match the viewport pixel size - i.e. always, in practice -
        // producing scrambled/repeated-looking page fragments once zoomed in enough to see tiles.
        val pointsPerUnitX = pdfPage.width.toFloat() / fullPageWidthPx
        val pointsPerUnitY = pdfPage.height.toFloat() / fullPageHeightPx
        val regionLeftPoints = region.left * pointsPerUnitX
        val regionTopPoints = region.top * pointsPerUnitY
        val renderScaleX = scale / pointsPerUnitX
        val renderScaleY = scale / pointsPerUnitY

        // Translate the region's origin (in page points) to (0,0) BEFORE scaling up - doing it in
        // the other order (scale, then translate by an un-scaled offset) only cropped the correct
        // spot for the page's top-left tile; every other tile pulled its source pixels from the
        // wrong place.
        val transform = Matrix()
        transform.postTranslate(-regionLeftPoints, -regionTopPoints)
        transform.postScale(renderScaleX, renderScaleY)

        pdfPage.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        pdfPage.close()

        AndroidPdfBitmap(bitmap)
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
