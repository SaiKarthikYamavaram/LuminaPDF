package com.lumina.pdf.renderer

import com.lumina.pdf.core.CharacterBox
import com.lumina.pdf.core.OutlineItem
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfRect
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class JvmPdfDocument(private val document: PDDocument) : PdfDocument {
    private val renderMutex = Mutex()

    override suspend fun pageCount(): Int = renderMutex.withLock {
        document.numberOfPages
    }

    override suspend fun renderPage(page: Int, width: Int, height: Int): PdfBitmap = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val pdPage = document.getPage(page)
            val cropBox = pdPage.cropBox
            val scale = width.toFloat() / cropBox.width
            val epsilon = (2.0f / scale).toDouble().coerceIn(0.3, 8.0)
            val customRenderer = SimplifyingPDFRenderer(document, epsilon)
            val image = customRenderer.renderImage(page, scale)
            JvmPdfBitmap(image)
        }
    }

    private val pageRenderCache = object : LinkedHashMap<Pair<Int, Float>, java.lang.ref.SoftReference<java.awt.image.BufferedImage>>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Pair<Int, Float>, java.lang.ref.SoftReference<java.awt.image.BufferedImage>>): Boolean {
            return size > 3
        }
    }

    override suspend fun renderTile(page: Int, region: PdfRect, scale: Float): PdfBitmap = withContext(Dispatchers.IO) {
        val cacheKey = Pair(page, scale)
        val fullImage = synchronized(pageRenderCache) {
            pageRenderCache[cacheKey]?.get()
        } ?: renderMutex.withLock {
            synchronized(pageRenderCache) {
                pageRenderCache[cacheKey]?.get()
            } ?: run {
                val epsilon = (2.0f / scale).toDouble().coerceIn(0.3, 8.0)
                val customRenderer = SimplifyingPDFRenderer(document, epsilon)
                val rendered = customRenderer.renderImage(page, scale)
                synchronized(pageRenderCache) {
                    pageRenderCache[cacheKey] = java.lang.ref.SoftReference(rendered)
                }
                rendered
            }
        }

        val x = region.left.toInt().coerceAtLeast(0)
        val y = region.top.toInt().coerceAtLeast(0)
        val w = region.width.toInt().coerceAtMost(fullImage.width - x).coerceAtLeast(1)
        val h = region.height.toInt().coerceAtMost(fullImage.height - y).coerceAtLeast(1)
        
        val subImage = fullImage.getSubimage(x, y, w, h)
        JvmPdfBitmap(subImage)
    }

    override suspend fun extractText(page: Int): String = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val stripper = PDFTextStripper()
            stripper.startPage = page + 1
            stripper.endPage = page + 1
            stripper.getText(document)
        }
    }

    override suspend fun searchText(page: Int, query: String): List<PdfRect> = withContext(Dispatchers.IO) {
        if (query.isEmpty()) return@withContext emptyList()
        renderMutex.withLock {
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
    }

    override suspend fun extractCharacterBoxes(page: Int): List<CharacterBox> = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val pdPage = document.getPage(page)
            val cropBox = pdPage.cropBox
            val pageWidth = cropBox.width
            val pageHeight = cropBox.height
            
            val extractor = PDFCharacterBoxExtractor(page)
            extractor.startPage = page + 1
            extractor.endPage = page + 1
            extractor.getText(document)
            extractor.getBoxes(pageWidth, pageHeight)
        }
    }

    override suspend fun getOutline(): List<OutlineItem> = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            val outline = document.documentCatalog.documentOutline ?: return@withLock emptyList()
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

class PDFCharacterBoxExtractor(
    private val pageIndex: Int
) : PDFTextStripper() {
    private val characters = mutableListOf<org.apache.pdfbox.text.TextPosition>()

    init {
        sortByPosition = true
    }

    override fun writeString(text: String?, textPositions: MutableList<org.apache.pdfbox.text.TextPosition>?) {
        if (textPositions != null) {
            characters.addAll(textPositions)
        }
    }

    fun getBoxes(pageWidth: Float, pageHeight: Float): List<CharacterBox> {
        return characters.map { tp ->
            val left = tp.xDirAdj / pageWidth
            val right = (tp.xDirAdj + tp.widthDirAdj) / pageWidth
            val top = (tp.yDirAdj - tp.heightDir) / pageHeight
            val bottom = tp.yDirAdj / pageHeight
            val char = if (tp.unicode.isNotEmpty()) tp.unicode[0] else ' '
            CharacterBox(
                char = char,
                pageIndex = pageIndex,
                left = left.coerceIn(0f, 1f),
                top = top.coerceIn(0f, 1f),
                right = right.coerceIn(0f, 1f),
                bottom = bottom.coerceIn(0f, 1f)
            )
        }
    }
}

class SimplifyingPDFRenderer(
    document: PDDocument,
    private val epsilon: Double
) : PDFRenderer(document) {
    override fun createPageDrawer(parameters: org.apache.pdfbox.rendering.PageDrawerParameters): org.apache.pdfbox.rendering.PageDrawer {
        return SimplifyingPageDrawer(parameters, epsilon)
    }
}

