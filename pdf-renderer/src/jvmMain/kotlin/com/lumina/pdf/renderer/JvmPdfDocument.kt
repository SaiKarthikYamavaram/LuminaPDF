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

    fun findMatches(pageWidth: Float, pageHeight: Float) {
        val text = characters.map { it.unicode }.joinToString("")
        var index = text.indexOf(query, ignoreCase = true)
        while (index != -1) {
            val matchedPositions = characters.subList(index, index + query.length)
            if (matchedPositions.isNotEmpty()) {
                val minX = matchedPositions.minOf { it.xDirAdj }
                val maxX = matchedPositions.maxOf { it.xDirAdj + it.widthDirAdj }
                val minY = matchedPositions.minOf { it.yDirAdj - it.heightDir }
                val maxY = matchedPositions.maxOf { it.yDirAdj }
                
                matches.add(
                    PdfRect(
                        left = minX / pageWidth,
                        top = minY / pageHeight,
                        right = maxX / pageWidth,
                        bottom = maxY / pageHeight
                    )
                )
            }
            index = text.indexOf(query, index + 1, ignoreCase = true)
        }
    }
}
