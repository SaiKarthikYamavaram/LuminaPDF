package com.lumina.pdf.renderer

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
