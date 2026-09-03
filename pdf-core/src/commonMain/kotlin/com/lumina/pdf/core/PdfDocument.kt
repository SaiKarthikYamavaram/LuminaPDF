package com.lumina.pdf.core

interface PdfDocument {
    suspend fun pageCount(): Int

    suspend fun renderPage(
        page: Int,
        width: Int,
        height: Int
    ): PdfBitmap

    // region's coordinates are pixels within a rendering of the whole page sized exactly
    // fullPageWidthPx x fullPageHeightPx (i.e. what renderPage(page, fullPageWidthPx,
    // fullPageHeightPx) would produce) - scale then multiplies that reference resolution up (or
    // down) for the actual output. Width and height are independent (the caller's page-height
    // estimate isn't the PDF's true aspect ratio), so implementations must derive separate X/Y
    // points-to-pixels ratios from them and the page's native size; passing region's raw numbers
    // straight into a native transform (which operates in page points, not this viewport-relative
    // space) crops from the wrong location.
    suspend fun renderTile(
        page: Int,
        region: PdfRect,
        scale: Float,
        fullPageWidthPx: Int,
        fullPageHeightPx: Int
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
