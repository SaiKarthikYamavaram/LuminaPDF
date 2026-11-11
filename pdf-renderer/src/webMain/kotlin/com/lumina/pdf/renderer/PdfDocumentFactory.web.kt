package com.lumina.pdf.renderer

import com.lumina.pdf.core.OutlineItem
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.PdfDocument
import com.lumina.pdf.core.PdfGlyph
import com.lumina.pdf.core.PdfPasswordRequiredException
import com.lumina.pdf.core.PdfRect
import com.lumina.pdf.core.RgbaPdfBitmap
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsNumber
import kotlin.js.Promise
import kotlin.js.get
import kotlin.js.length
import kotlin.js.toDouble
import kotlinx.coroutines.await

internal expect fun ByteArray.toJsBytes(): JsAny
internal expect fun JsAny.toKotlinBytes(): ByteArray

private external interface OpenResult : JsAny {
    val status: String
    val doc: JsAny?
}

private external interface JsGlyph : JsAny {
    val text: String
    val left: Double
    val top: Double
    val right: Double
    val bottom: Double
}

private external interface JsOutlineItem : JsAny {
    val title: String
    val page: Int
    val children: JsArray<JsOutlineItem>
}

// Defined by webApp's pdfjs/lumina-bridge.mjs.
@Suppress("ClassName")
private external object luminaPdf : JsAny {
    fun open(data: JsAny, password: String?): Promise<OpenResult>
    fun pageCount(doc: JsAny): Int
    fun aspect(doc: JsAny, page: Int): Promise<JsNumber>
    fun render(
        doc: JsAny,
        page: Int,
        left: Double,
        top: Double,
        width: Int,
        height: Int,
        scale: Double,
        fullWidth: Int
    ): Promise<JsAny>
    fun glyphs(doc: JsAny, page: Int): Promise<JsArray<JsGlyph>>
    fun outline(doc: JsAny): Promise<JsArray<JsOutlineItem>>
    fun close(doc: JsAny)
}

private fun <T : JsAny?> JsArray<T>.toList(): List<T> = List(length) { get(it) as T }

private fun JsOutlineItem.toOutlineItem(): OutlineItem =
    OutlineItem(title, page, children.toList().map { it.toOutlineItem() })

private class WebPdfDocument(private val doc: JsAny) : PdfDocument {
    override suspend fun pageCount(): Int = luminaPdf.pageCount(doc)

    override suspend fun pageAspectRatio(page: Int): Float = luminaPdf.aspect(doc, page).await().toDouble().toFloat()

    override suspend fun renderPage(page: Int, width: Int, height: Int): PdfBitmap =
        RgbaPdfBitmap(width, height, luminaPdf.render(doc, page, 0.0, 0.0, width, height, 1.0, width).await().toKotlinBytes())

    override suspend fun renderTile(
        page: Int,
        region: PdfRect,
        scale: Float,
        fullPageWidthPx: Int,
        fullPageHeightPx: Int
    ): PdfBitmap {
        val width = (region.width * scale).toInt().coerceAtLeast(1)
        val height = (region.height * scale).toInt().coerceAtLeast(1)
        val pixels = luminaPdf.render(
            doc, page, region.left.toDouble(), region.top.toDouble(), width, height, scale.toDouble(), fullPageWidthPx
        ).await().toKotlinBytes()
        return RgbaPdfBitmap(width, height, pixels)
    }

    override suspend fun pageGlyphs(page: Int): List<PdfGlyph> = luminaPdf.glyphs(doc, page).await().toList().map {
        PdfGlyph(it.text, PdfRect(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat()))
    }

    override suspend fun extractText(page: Int): String = pageGlyphs(page).joinToString("") { it.text }

    override suspend fun searchText(page: Int, query: String): List<PdfRect> = searchGlyphs(pageGlyphs(page), query)

    override suspend fun getOutline(): List<OutlineItem> =
        luminaPdf.outline(doc).await().toList().map { it.toOutlineItem() }

    override fun close() = luminaPdf.close(doc)
}

actual class PdfDocumentFactory actual constructor() {
    actual suspend fun open(bytes: ByteArray, password: String?): PdfDocument {
        val result = luminaPdf.open(bytes.toJsBytes(), password).await()
        return when (result.status) {
            "password" -> throw PdfPasswordRequiredException(wrongPasswordTried = false)
            "wrong-password" -> throw PdfPasswordRequiredException(wrongPasswordTried = true)
            else -> WebPdfDocument(result.doc!!)
        }
    }
}
