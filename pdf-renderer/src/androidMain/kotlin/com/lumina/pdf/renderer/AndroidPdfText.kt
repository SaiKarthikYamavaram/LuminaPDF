package com.lumina.pdf.renderer

import com.lumina.pdf.core.OutlineItem
import com.lumina.pdf.core.PdfGlyph
import com.lumina.pdf.core.PdfRect
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

// Android's PdfRenderer has no text layer before API 35, so search, text and outline go through
// PDFBox here. Mirrors JvmPdfDocument, which uses desktop PDFBox under a different package.

internal fun extractPageText(document: PDDocument, page: Int): String {
    val stripper = PDFTextStripper()
    stripper.startPage = page + 1
    stripper.endPage = page + 1
    return stripper.getText(document)
}

internal fun searchPageText(document: PDDocument, page: Int, query: String): List<PdfRect> {
    val cropBox = document.getPage(page).cropBox
    val locator = TextLocator()
    locator.startPage = page + 1
    locator.endPage = page + 1
    locator.getText(document)
    return locator.findMatches(query, cropBox.width, cropBox.height)
}

internal fun readPageGlyphs(document: PDDocument, page: Int): List<PdfGlyph> {
    val cropBox = document.getPage(page).cropBox
    val locator = TextLocator()
    locator.startPage = page + 1
    locator.endPage = page + 1
    locator.getText(document)
    return locator.glyphs(cropBox.width, cropBox.height)
}

internal fun readOutline(document: PDDocument): List<OutlineItem> {
    val outline = document.documentCatalog.documentOutline ?: return emptyList()
    return generateSequence(outline.firstChild) { it.nextSibling }.mapNotNull { mapOutlineItem(document, it) }.toList()
}

private fun mapOutlineItem(document: PDDocument, item: PDOutlineItem): OutlineItem? {
    val title = item.title.orEmpty()
    if (title.isEmpty()) return null
    val children = generateSequence(item.firstChild) { it.nextSibling }.mapNotNull { mapOutlineItem(document, it) }.toList()
    return OutlineItem(title, targetPage(document, item), children)
}

// -1 when the destination can't be resolved, as in JvmPdfDocument.
private fun targetPage(document: PDDocument, item: PDOutlineItem): Int = try {
    val direct = item.findDestinationPage(document)?.let { document.pages.indexOf(it) } ?: -1
    if (direct >= 0) {
        direct
    } else {
        val destination = item.destination ?: (item.action as? PDActionGoTo)?.destination
        val pageDestination = if (destination is PDNamedDestination) {
            document.documentCatalog.findNamedDestinationPage(destination)
        } else {
            destination as? PDPageDestination
        }
        pageDestination?.let { resolved ->
            resolved.retrievePageNumber().takeIf { it >= 0 } ?: resolved.page?.let { document.pages.indexOf(it) }
        } ?: -1
    }
} catch (e: Exception) {
    -1
}

private class TextLocator : PDFTextStripper() {
    private val characters = mutableListOf<TextPosition>()

    init {
        sortByPosition = true
    }

    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
        if (textPositions != null) characters.addAll(textPositions)
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

    fun findMatches(query: String, pageWidth: Float, pageHeight: Float): List<PdfRect> =
        findTextMatches(characters.map { it.unicode }, query).map { range ->
            val matched = characters.subList(range.first, range.last + 1)
            PdfRect(
                left = matched.minOf { it.xDirAdj } / pageWidth,
                top = matched.minOf { it.yDirAdj - it.heightDir } / pageHeight,
                right = matched.maxOf { it.xDirAdj + it.widthDirAdj } / pageWidth,
                bottom = matched.maxOf { it.yDirAdj } / pageHeight
            )
        }
}
