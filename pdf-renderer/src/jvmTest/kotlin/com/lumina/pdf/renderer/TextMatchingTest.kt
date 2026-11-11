package com.lumina.pdf.renderer

import kotlin.test.Test
import kotlin.test.assertEquals

class TextMatchingTest {
    @Test
    fun singleCharacterUnitsMapDirectly() {
        assertEquals(listOf(1..2), findTextMatches(listOf("a", "b", "c"), "BC"))
    }

    @Test
    fun ligatureBeforeMatchDoesNotShiftIt() {
        // "fi" is one unit, so "nd" sits at string offset 2 but unit index 1.
        assertEquals(listOf(1..2), findTextMatches(listOf("fi", "n", "d"), "nd"))
    }

    @Test
    fun matchInsideLigatureCoversTheWholeUnit() {
        assertEquals(listOf(0..1), findTextMatches(listOf("fi", "n"), "in"))
    }

    @Test
    fun repeatedAndOverlappingMatchesAreAllFound() {
        assertEquals(listOf(0..1, 1..2), findTextMatches(listOf("a", "a", "a"), "aa"))
    }

    @Test
    fun emptyQueryAndNoMatchReturnNothing() {
        assertEquals(emptyList(), findTextMatches(listOf("a"), ""))
        assertEquals(emptyList(), findTextMatches(listOf("a"), "z"))
    }
}
