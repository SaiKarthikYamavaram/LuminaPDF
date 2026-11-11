package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfGlyph
import com.lumina.pdf.core.PdfRect

// Finds case-insensitive matches of query in text split into units, one per drawn glyph. A unit can
// hold several characters (a ligature's "fi"), so string offsets don't line up with unit indices.
// Returns the range of units each match covers.
internal fun findTextMatches(units: List<String>, query: String): List<IntRange> {
    if (query.isEmpty()) return emptyList()

    val owners = IntArray(units.sumOf { it.length })
    var offset = 0
    units.forEachIndexed { unit, characters ->
        repeat(characters.length) { owners[offset++] = unit }
    }

    val text = units.joinToString("")
    val matches = mutableListOf<IntRange>()
    var index = text.indexOf(query, ignoreCase = true)
    while (index != -1) {
        matches.add(owners[index]..owners[index + query.length - 1])
        index = text.indexOf(query, index + 1, ignoreCase = true)
    }
    return matches
}

// Search built on a page's glyphs, for renderers with no search of their own.
internal fun searchGlyphs(glyphs: List<PdfGlyph>, query: String): List<PdfRect> =
    findTextMatches(glyphs.map { it.text }, query).map { range ->
        val matched = glyphs.subList(range.first, range.last + 1)
        PdfRect(
            left = matched.minOf { it.rect.left },
            top = matched.minOf { it.rect.top },
            right = matched.maxOf { it.rect.right },
            bottom = matched.maxOf { it.rect.bottom }
        )
    }
