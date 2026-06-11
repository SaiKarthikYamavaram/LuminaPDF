package com.lumina.pdf.viewer

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PdfViewerConfig(
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 5f,
    val pageSpacing: Dp = 12.dp,
    val continuousScroll: Boolean = true,
    val nightMode: Boolean = false
)
