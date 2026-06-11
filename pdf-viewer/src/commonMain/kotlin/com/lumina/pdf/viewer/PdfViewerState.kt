package com.lumina.pdf.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PdfViewerState(
    initialPage: Int = 0,
    initialZoom: Float = 1f,
    initialScrollOffset: Offset = Offset.Zero
) {
    var currentPage by mutableStateOf(initialPage)
    var zoom by mutableStateOf(initialZoom)
    var scrollOffset by mutableStateOf(initialScrollOffset)
    var visiblePages by mutableStateOf(emptyList<Int>())
}
