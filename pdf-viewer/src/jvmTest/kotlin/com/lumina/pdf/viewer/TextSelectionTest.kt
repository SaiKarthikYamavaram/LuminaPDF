package com.lumina.pdf.viewer

import com.lumina.pdf.core.PdfGlyph
import com.lumina.pdf.core.PdfRect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextSelectionTest {
    private fun glyph(text: String, left: Float, top: Float) =
        PdfGlyph(text, PdfRect(left, top, left + 0.1f, top + 0.05f))

    // "ab" then a gap and "c" on line one, "d" on line two.
    private val glyphs = listOf(
        glyph("a", 0.0f, 0.1f),
        glyph("b", 0.1f, 0.1f),
        glyph("c", 0.3f, 0.1f),
        glyph("d", 0.0f, 0.3f)
    )

    @Test
    fun picksGlyphUnderPoint() {
        assertEquals(1, glyphIndexAt(glyphs, 0.15f, 0.12f))
    }

    @Test
    fun betweenLinesPicksNearestLineThenPosition() {
        assertEquals(3, glyphIndexAt(glyphs, 0.05f, 0.29f))
    }

    @Test
    fun toleranceRejectsFarPoints() {
        assertNull(glyphIndexAt(glyphs, 0.9f, 0.9f, tolerance = 0.03f))
        assertEquals(0, glyphIndexAt(glyphs, 0.05f, 0.08f, tolerance = 0.03f))
        assertNull(glyphIndexAt(emptyList(), 0f, 0f))
    }

    @Test
    fun rangeIsOrderedWhenDraggingBackwards() {
        assertEquals(1..3, TextSelection(0, anchor = 3, focus = 1).range)
    }

    @Test
    fun oneRectPerLine() {
        val rects = selectionLineRects(glyphs, 0..3)
        assertEquals(2, rects.size)
        assertEquals(PdfRect(0f, 0.1f, 0.4f, 0.15f), rects[0].let { PdfRect(it.left, it.top, it.right, it.bottom) })
        assertEquals(glyphs[3].rect, rects[1])
    }

    @Test
    fun textAddsWordGapsAndLineBreaks() {
        assertEquals("ab c\nd", selectionText(glyphs, 0..3))
    }

    @Test
    fun outOfRangeIndicesAreIgnored() {
        assertEquals("d", selectionText(glyphs, 3..9))
    }

    @Test
    fun highlightRectsRoundTripIncludingOldSingleRectRows() {
        val rects = listOf(PdfRect(0.1f, 0.2f, 0.3f, 0.4f), PdfRect(0.5f, 0.6f, 0.7f, 0.8f))
        assertEquals(rects, parseHighlightRects(encodeHighlightRects(rects)))
        assertEquals(listOf(rects[0]), parseHighlightRects("0.1,0.2,0.3,0.4"))
        assertEquals(emptyList(), parseHighlightRects("garbage"))
    }
}
