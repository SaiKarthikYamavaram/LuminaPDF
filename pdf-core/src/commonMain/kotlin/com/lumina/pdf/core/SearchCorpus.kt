package com.lumina.pdf.core

data class PageSlice(
    val pageIndex: Int,
    val startCharIndex: Int,
    val endCharIndex: Int
)

class SearchCorpus(
    val text: String,
    val slices: List<PageSlice>
) {
    fun findPageForCharIndex(charIndex: Int): Int {
        var low = 0
        var high = slices.size - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val slice = slices[mid]
            if (charIndex >= slice.startCharIndex && charIndex < slice.endCharIndex) {
                return slice.pageIndex
            } else if (charIndex < slice.startCharIndex) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return -1
    }
}
