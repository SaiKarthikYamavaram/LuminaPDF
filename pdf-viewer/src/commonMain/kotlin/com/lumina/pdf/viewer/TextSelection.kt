package com.lumina.pdf.viewer

import com.lumina.pdf.core.PdfGlyph
import com.lumina.pdf.core.PdfRect

// ponytail: one page at a time, and in the renderer's reading order, so a selection across two
// columns follows that order. Extend TextSelection with an end page if multi-page selection matters.
internal data class TextSelection(val page: Int, val anchor: Int, val focus: Int) {
    val range: IntRange get() = minOf(anchor, focus)..maxOf(anchor, focus)
}

private fun distanceOutside(value: Float, low: Float, high: Float): Float = maxOf(low - value, value - high, 0f)

// x and y are 0..1 page fractions. Returns the nearest glyph, or null if it is further than
// tolerance on either axis. Vertical distance dominates, so a point between two lines picks a
// line first and then a position along it.
internal fun glyphIndexAt(glyphs: List<PdfGlyph>, x: Float, y: Float, tolerance: Float = Float.MAX_VALUE): Int? {
    val index = glyphs.indices.minByOrNull { i ->
        val rect = glyphs[i].rect
        distanceOutside(y, rect.top, rect.bottom) * 1000f + distanceOutside(x, rect.left, rect.right)
    } ?: return null
    val rect = glyphs[index].rect
    val isClose = distanceOutside(y, rect.top, rect.bottom) <= tolerance && distanceOutside(x, rect.left, rect.right) <= tolerance
    return if (isClose) index else null
}

private fun onSameLine(a: PdfRect, b: PdfRect): Boolean =
    minOf(a.bottom, b.bottom) - maxOf(a.top, b.top) >= minOf(a.height, b.height) / 2f

// One rectangle per line of the selection.
internal fun selectionLineRects(glyphs: List<PdfGlyph>, range: IntRange): List<PdfRect> {
    val lines = mutableListOf<PdfRect>()
    for (i in range) {
        val rect = glyphs.getOrNull(i)?.rect ?: continue
        val last = lines.lastOrNull()
        if (last != null && onSameLine(last, rect)) {
            lines[lines.lastIndex] = PdfRect(
                left = minOf(last.left, rect.left),
                top = minOf(last.top, rect.top),
                right = maxOf(last.right, rect.right),
                bottom = maxOf(last.bottom, rect.bottom)
            )
        } else {
            lines.add(rect)
        }
    }
    return lines
}

internal fun selectionText(glyphs: List<PdfGlyph>, range: IntRange): String {
    val text = buildString {
        var previous: PdfGlyph? = null
        for (i in range) {
            val glyph = glyphs.getOrNull(i) ?: continue
            val before = previous
            if (before != null) {
                val gap = glyph.rect.left - before.rect.right
                when {
                    !onSameLine(before.rect, glyph.rect) -> append('\n')
                    // Many PDFs position words apart without drawing a space character.
                    gap > before.rect.width * 0.3f && glyph.text.isNotBlank() && before.text.isNotBlank() -> append(' ')
                }
            }
            append(glyph.text)
            previous = glyph
        }
    }
    return text.lines().joinToString("\n") { it.trimEnd() }.trim()
}

// Stored as "left,top,right,bottom" groups joined by ";", one group per highlighted line.
internal fun encodeHighlightRects(rects: List<PdfRect>): String =
    rects.joinToString(";") { "${it.left},${it.top},${it.right},${it.bottom}" }

internal fun parseHighlightRects(text: String): List<PdfRect> = text.split(";").mapNotNull { group ->
    val parts = group.split(",").mapNotNull { it.toFloatOrNull() }
    if (parts.size == 4) PdfRect(left = parts[0], top = parts[1], right = parts[2], bottom = parts[3]) else null
}
