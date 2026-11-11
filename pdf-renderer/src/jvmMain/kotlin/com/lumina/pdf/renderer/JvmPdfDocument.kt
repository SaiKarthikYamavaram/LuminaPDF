package com.lumina.pdf.renderer

import com.lumina.pdf.core.OutlineItem
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfGlyph
import com.lumina.pdf.core.PdfRect
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage

class JvmPdfDocument(private val document: PDDocument) : PdfDocument {
    private val renderer = PDFRenderer(document)

    override suspend fun pageCount(): Int = document.numberOfPages

    override suspend fun pageAspectRatio(page: Int): Float = withContext(Dispatchers.IO) {
        val pdPage = document.getPage(page)
        val cropBox = pdPage.cropBox
        if (pdPage.rotation % 180 == 0) cropBox.height / cropBox.width else cropBox.width / cropBox.height
    }

    // PDFBox applies the page's rotation when rendering, so a sideways page's output width comes
    // from the crop box height.
    private fun displayedWidth(pdPage: org.apache.pdfbox.pdmodel.PDPage): Float =
        if (pdPage.rotation % 180 == 0) pdPage.cropBox.width else pdPage.cropBox.height

    override suspend fun renderPage(page: Int, width: Int, height: Int): PdfBitmap = withContext(Dispatchers.IO) {
        val scale = width.toFloat() / displayedWidth(document.getPage(page))
        val image = renderer.renderImage(page, scale)
        JvmPdfBitmap(image)
    }

    override suspend fun renderTile(
        page: Int,
        region: PdfRect,
        scale: Float,
        fullPageWidthPx: Int,
        fullPageHeightPx: Int
    ): PdfBitmap = withContext(Dispatchers.IO) {
        // PDFBox's PDFRenderer.renderPageToGraphics(page, Graphics2D, scaleX, scaleY) would let us
        // rasterize only the tile region directly, avoiding a full-page render per tile entirely.
        // Verified against this repo's PDFBox 3.0.1 + JDK: that call reliably produces a blank/black
        // image (confirmed via a standalone repro rendering a full page through it with no
        // translate at all) - not usable as-is. Falling back to rendering the full page once (as
        // before) and drawing it translated into a tile-sized image. This still fixes the real
        // memory bug in the old code: BufferedImage.getSubimage() returns a view sharing the
        // parent's backing buffer, so caching one tile kept the entire full-page raster pinned in
        // memory. Drawing into a fresh tile-sized BufferedImage avoids that retention, even though
        // it doesn't avoid the redundant per-tile full-page rendering cost.
        // region's coordinates are pixels in a fullPageWidthPx-wide rendering of the whole page
        // (the PdfDocument.renderTile contract) - PDFBox's renderImage(page, scale) instead treats
        // scale as native-points-to-pixels, same mismatch renderPage() above already accounts for
        // via width/cropBox.width. Using `scale` directly here (as a prior version did) ignored
        // that conversion entirely, cropping from the wrong location once zoomed in enough to see
        // tiles. renderImage takes one uniform scale, so this uses the width-based ratio for both
        // axes - same width-driven approximation this app already makes when it estimates page
        // height as width * 1.4 rather than using the PDF's true aspect ratio.
        val baseScale = fullPageWidthPx / displayedWidth(document.getPage(page))
        val renderScale = scale * baseScale

        val tileWidthPx = (region.width * scale).toInt().coerceAtLeast(1)
        val tileHeightPx = (region.height * scale).toInt().coerceAtLeast(1)
        val fullImage = renderer.renderImage(page, renderScale)
        val tileImage = BufferedImage(tileWidthPx, tileHeightPx, BufferedImage.TYPE_INT_ARGB)
        val graphics = tileImage.createGraphics()
        try {
            graphics.drawImage(fullImage, -(region.left * scale).toInt(), -(region.top * scale).toInt(), null)
        } finally {
            graphics.dispose()
        }
        JvmPdfBitmap(tileImage)
    }

    override suspend fun extractText(page: Int): String = withContext(Dispatchers.IO) {
        val stripper = PDFTextStripper()
        stripper.startPage = page + 1
        stripper.endPage = page + 1
        stripper.getText(document)
    }

    override suspend fun searchText(page: Int, query: String): List<PdfRect> = withContext(Dispatchers.IO) {
        if (query.isEmpty()) return@withContext emptyList()
        val pdPage = document.getPage(page)
        val cropBox = pdPage.cropBox
        val pageWidth = cropBox.width
        val pageHeight = cropBox.height
        
        val locator = PDFTextLocator(query)
        locator.startPage = page + 1
        locator.endPage = page + 1
        locator.getText(document)
        locator.findMatches(pageWidth, pageHeight)
        locator.matches
    }

    override suspend fun pageGlyphs(page: Int): List<PdfGlyph> = withContext(Dispatchers.IO) {
        val cropBox = document.getPage(page).cropBox
        val locator = PDFTextLocator("")
        locator.startPage = page + 1
        locator.endPage = page + 1
        locator.getText(document)
        locator.glyphs(cropBox.width, cropBox.height)
    }

    override suspend fun getOutline(): List<OutlineItem> = withContext(Dispatchers.IO) {
        val outline = document.documentCatalog.documentOutline ?: return@withContext emptyList()
        val result = mutableListOf<OutlineItem>()
        var current = outline.firstChild
        while (current != null) {
            val mapped = mapOutlineItem(current)
            if (mapped != null) {
                result.add(mapped)
            }
            current = current.nextSibling
        }
        result
    }

    private fun mapOutlineItem(item: org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem): OutlineItem? {
        val title = item.title ?: ""
        var pageIndex = -1
        try {
            val page = item.findDestinationPage(document)
            if (page != null) {
                pageIndex = document.pages.indexOf(page)
            }
            
            if (pageIndex < 0) {
                val dest = item.destination ?: (item.action as? org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo)?.destination
                if (dest != null) {
                    val resolvedDest = if (dest is org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination) {
                        try {
                            document.documentCatalog.findNamedDestinationPage(dest)
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        dest as? org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
                    }
                    
                    if (resolvedDest != null) {
                        val pageNum = resolvedDest.retrievePageNumber()
                        if (pageNum >= 0) {
                            pageIndex = pageNum
                        } else {
                            val pdPage = resolvedDest.page
                            if (pdPage != null) {
                                pageIndex = document.pages.indexOf(pdPage)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore target page resolution errors
        }

        val children = mutableListOf<OutlineItem>()
        var child = item.firstChild
        while (child != null) {
            val mappedChild = mapOutlineItem(child)
            if (mappedChild != null) {
                children.add(mappedChild)
            }
            child = child.nextSibling
        }

        if (title.isNotEmpty()) {
            return OutlineItem(title, pageIndex, children)
        }
        return null
    }

    override fun close() {
        document.close()
    }
}

class PDFTextLocator(
    private val query: String
) : PDFTextStripper() {
    val matches = mutableListOf<PdfRect>()
    private val characters = mutableListOf<org.apache.pdfbox.text.TextPosition>()

    init {
        sortByPosition = true
    }

    override fun writeString(text: String?, textPositions: MutableList<org.apache.pdfbox.text.TextPosition>?) {
        if (text != null && textPositions != null) {
            characters.addAll(textPositions)
        }
    }

    fun glyphs(pageWidth: Float, pageHeight: Float): List<PdfGlyph> = characters.map { position ->
        PdfGlyph(
            text = position.unicode,
            rect = PdfRect(
                left = position.xDirAdj / pageWidth,
                top = (position.yDirAdj - position.heightDir) / pageHeight,
                right = (position.xDirAdj + position.widthDirAdj) / pageWidth,
                bottom = position.yDirAdj / pageHeight
            )
        )
    }

    fun findMatches(pageWidth: Float, pageHeight: Float) {
        findTextMatches(characters.map { it.unicode }, query).forEach { range ->
            val matchedPositions = characters.subList(range.first, range.last + 1)
            matches.add(
                PdfRect(
                    left = matchedPositions.minOf { it.xDirAdj } / pageWidth,
                    top = matchedPositions.minOf { it.yDirAdj - it.heightDir } / pageHeight,
                    right = matchedPositions.maxOf { it.xDirAdj + it.widthDirAdj } / pageWidth,
                    bottom = matchedPositions.maxOf { it.yDirAdj } / pageHeight
                )
            )
        }
    }
}
