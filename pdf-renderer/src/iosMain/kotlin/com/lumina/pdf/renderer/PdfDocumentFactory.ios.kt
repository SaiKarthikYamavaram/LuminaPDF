package com.lumina.pdf.renderer

import com.lumina.pdf.core.OutlineItem
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfGlyph
import com.lumina.pdf.core.PdfPasswordRequiredException
import com.lumina.pdf.core.PdfRect
import com.lumina.pdf.core.RgbaPdfBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.PDFKit.PDFDocument as KitDocument
import platform.PDFKit.PDFOutline
import platform.PDFKit.PDFPage
import platform.PDFKit.kPDFDisplayBoxCropBox

@OptIn(ExperimentalForeignApi::class, ExperimentalCoroutinesApi::class)
private class IosPdfDocument(private val document: KitDocument) : PdfDocument {
    // PDFKit isn't thread-safe, and rendering and text reads can arrive together.
    private val pdfKit = Dispatchers.Default.limitedParallelism(1)

    private fun pageAt(page: Int): PDFPage = requireNotNull(document.pageAtIndex(page.toULong())) { "No page $page" }

    // Unrotated size in points. PDFKit applies the page's own rotation when drawing.
    private fun PDFPage.cropSize(): Pair<Double, Double> =
        boundsForBox(kPDFDisplayBoxCropBox).useContents { size.width to size.height }

    private fun PDFPage.displayedSize(): Pair<Double, Double> {
        val (width, height) = cropSize()
        return if (rotation % 180L == 0L) width to height else height to width
    }

    private fun renderInto(page: PDFPage, width: Int, height: Int, transform: (CGContextRef) -> Unit): RgbaPdfBitmap {
        val pixels = ByteArray(width * height * 4)
        pixels.usePinned { pinned ->
            val colorSpace = CGColorSpaceCreateDeviceRGB()
            val context = requireNotNull(
                CGBitmapContextCreate(
                    pinned.addressOf(0),
                    width.toULong(),
                    height.toULong(),
                    8u,
                    (width * 4).toULong(),
                    colorSpace,
                    CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                )
            ) { "Could not create a bitmap context" }
            CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
            transform(context)
            page.drawWithBox(kPDFDisplayBoxCropBox, toContext = context)
            CGContextRelease(context)
            CGColorSpaceRelease(colorSpace)
        }
        return RgbaPdfBitmap(width, height, pixels)
    }

    override suspend fun pageCount(): Int = document.pageCount.toInt()

    override suspend fun pageAspectRatio(page: Int): Float = withContext(pdfKit) {
        val (width, height) = pageAt(page).displayedSize()
        (height / width).toFloat()
    }

    override suspend fun renderPage(page: Int, width: Int, height: Int): PdfBitmap = withContext(pdfKit) {
        val pdfPage = pageAt(page)
        val (pageWidth, pageHeight) = pdfPage.displayedSize()
        renderInto(pdfPage, width, height) { context ->
            CGContextScaleCTM(context, width / pageWidth, height / pageHeight)
        }
    }

    override suspend fun renderTile(
        page: Int,
        region: PdfRect,
        scale: Float,
        fullPageWidthPx: Int,
        fullPageHeightPx: Int
    ): PdfBitmap = withContext(pdfKit) {
        val pdfPage = pageAt(page)
        val (pageWidth, pageHeight) = pdfPage.displayedSize()
        val width = (region.width * scale).toInt().coerceAtLeast(1)
        val height = (region.height * scale).toInt().coerceAtLeast(1)
        renderInto(pdfPage, width, height) { context ->
            // Core Graphics measures y upward from the bottom, while region measures from the top.
            val bottomOffset = (fullPageHeightPx - region.top) * scale - height
            CGContextTranslateCTM(context, -region.left.toDouble() * scale, -bottomOffset.toDouble())
            CGContextScaleCTM(context, fullPageWidthPx * scale / pageWidth, fullPageHeightPx * scale / pageHeight)
        }
    }

    // ponytail: assumes an unrotated page, as search does on the other platforms.
    override suspend fun pageGlyphs(page: Int): List<PdfGlyph> = withContext(pdfKit) {
        val pdfPage = pageAt(page)
        val text = pdfPage.string ?: return@withContext emptyList()
        val (cropX, cropY, cropWidth, cropHeight) = pdfPage.boundsForBox(kPDFDisplayBoxCropBox).useContents {
            listOf(origin.x, origin.y, size.width, size.height)
        }
        text.indices.mapNotNull { index ->
            pdfPage.characterBoundsAtIndex(index.toLong()).useContents {
                // Line breaks and other undrawn characters have no bounds.
                if (size.width <= 0.0) return@useContents null
                PdfGlyph(
                    text = text[index].toString(),
                    rect = PdfRect(
                        left = ((origin.x - cropX) / cropWidth).toFloat(),
                        top = (1.0 - (origin.y + size.height - cropY) / cropHeight).toFloat(),
                        right = ((origin.x + size.width - cropX) / cropWidth).toFloat(),
                        bottom = (1.0 - (origin.y - cropY) / cropHeight).toFloat()
                    )
                )
            }
        }
    }

    override suspend fun extractText(page: Int): String = withContext(pdfKit) { pageAt(page).string.orEmpty() }

    override suspend fun searchText(page: Int, query: String): List<PdfRect> = searchGlyphs(pageGlyphs(page), query)

    private fun mapOutline(item: PDFOutline): OutlineItem? {
        val title = item.label?.takeIf { it.isNotEmpty() } ?: return null
        val target = item.destination?.page?.let { document.indexForPage(it).toInt() } ?: -1
        val children = (0uL until item.numberOfChildren).mapNotNull { index ->
            item.childAtIndex(index)?.let(::mapOutline)
        }
        return OutlineItem(title, target, children)
    }

    override suspend fun getOutline(): List<OutlineItem> = withContext(pdfKit) {
        val root = document.outlineRoot ?: return@withContext emptyList()
        (0uL until root.numberOfChildren).mapNotNull { index -> root.childAtIndex(index)?.let(::mapOutline) }
    }

    override fun close() {
        // PDFKit objects are released with this wrapper.
    }
}

@OptIn(ExperimentalForeignApi::class)
actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray, password: String?): PdfDocument = withContext(Dispatchers.Default) {
        val data = bytes.usePinned { NSData.dataWithBytes(it.addressOf(0), bytes.size.toULong()) }
        val document = requireNotNull(KitDocument(data = data)) { "This file could not be read as a PDF." }
        if (document.isLocked && (password == null || !document.unlockWithPassword(password))) {
            throw PdfPasswordRequiredException(wrongPasswordTried = password != null)
        }
        IosPdfDocument(document)
    }
}
