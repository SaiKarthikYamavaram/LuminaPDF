package com.lumina.pdf.core

interface PdfDocument {
    suspend fun pageCount(): Int

    suspend fun renderPage(
        page: Int,
        width: Int,
        height: Int
    ): PdfBitmap

    suspend fun renderTile(
        page: Int,
        region: PdfRect,
        scale: Float
    ): PdfBitmap

    suspend fun extractText(
        page: Int
    ): String

    suspend fun searchText(
        page: Int,
        query: String
    ): List<PdfRect>

    suspend fun getOutline(): List<OutlineItem>

    fun close()
}
