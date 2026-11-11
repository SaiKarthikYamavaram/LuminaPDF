package com.lumina.pdf.viewer

internal const val DEFAULT_PAGE_ASPECT = 1.4f

// Continuous vertical layout: every page fills the view width, and its height follows its own
// aspect ratio (height / width), so documents with mixed page sizes need no stretching.
internal class PageLayout(private val width: Float, private val aspects: List<Float>, spacing: Float) {
    private val tops = FloatArray(aspects.size)
    val totalHeight: Float

    init {
        var y = 0f
        aspects.forEachIndexed { page, aspect ->
            tops[page] = y
            y += aspect * width + spacing
        }
        totalHeight = if (aspects.isEmpty()) 0f else y - spacing
    }

    fun top(page: Int): Float = tops.getOrElse(page) { 0f }

    fun height(page: Int): Float = aspects.getOrElse(page) { DEFAULT_PAGE_ASPECT } * width

    // Binary search because this runs on every scroll frame. Returns the last page whose top is at
    // or above y, which may be a page y has already passed if y sits in the gap below it.
    fun pageAt(y: Float): Int {
        var low = 0
        var high = tops.lastIndex
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (tops[mid] <= y) low = mid else high = mid - 1
        }
        return low
    }
}
