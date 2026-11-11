package com.lumina.pdf.viewer

import kotlin.test.Test
import kotlin.test.assertEquals

class PageLayoutTest {
    // Width 100, spacing 10. Page 0 is 141 tall, page 1 (landscape) is 70, page 2 is 141.
    private val layout = PageLayout(width = 100f, aspects = listOf(1.41f, 0.7f, 1.41f), spacing = 10f)

    @Test
    fun pagesStackByTheirOwnHeights() {
        assertEquals(0f, layout.top(0), 0.01f)
        assertEquals(151f, layout.top(1), 0.01f)
        assertEquals(231f, layout.top(2), 0.01f)
        assertEquals(70f, layout.height(1), 0.01f)
        assertEquals(372f, layout.totalHeight, 0.01f)
    }

    @Test
    fun pageAtFindsThePageContainingAPoint() {
        assertEquals(0, layout.pageAt(-5f))
        assertEquals(0, layout.pageAt(100f))
        assertEquals(1, layout.pageAt(151f))
        assertEquals(1, layout.pageAt(200f))
        assertEquals(2, layout.pageAt(10_000f))
    }

    @Test
    fun emptyLayoutIsSafe() {
        val empty = PageLayout(100f, emptyList(), 10f)
        assertEquals(0f, empty.totalHeight)
        assertEquals(0, empty.pageAt(50f))
        assertEquals(140f, empty.height(3), 0.01f)
    }
}
