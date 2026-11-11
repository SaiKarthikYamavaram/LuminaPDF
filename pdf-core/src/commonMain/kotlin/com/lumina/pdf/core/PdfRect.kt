package com.lumina.pdf.core

data class PdfRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

data class PdfGlyph(val text: String, val rect: PdfRect)
