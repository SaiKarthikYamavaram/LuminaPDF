package com.lumina.pdf.viewer.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

class WasmJsPdfDatabase : PdfDatabase {
    override fun saveOffset(documentId: String, offset: PdfOffset) {
        window.localStorage.setItem("${documentId}_page", offset.page.toString())
        window.localStorage.setItem("${documentId}_zoom", offset.zoom.toString())
        window.localStorage.setItem("${documentId}_scroll_x", offset.scrollX.toString())
        window.localStorage.setItem("${documentId}_scroll_y", offset.scrollY.toString())
    }

    override fun getOffset(documentId: String): PdfOffset? {
        val pageStr = window.localStorage.getItem("${documentId}_page") ?: return null
        val zoomStr = window.localStorage.getItem("${documentId}_zoom") ?: "1.0"
        val scrollXStr = window.localStorage.getItem("${documentId}_scroll_x") ?: "0.0"
        val scrollYStr = window.localStorage.getItem("${documentId}_scroll_y") ?: "0.0"
        
        return PdfOffset(
            page = pageStr.toIntOrNull() ?: 0,
            zoom = zoomStr.toFloatOrNull() ?: 1f,
            scrollX = scrollXStr.toFloatOrNull() ?: 0f,
            scrollY = scrollYStr.toFloatOrNull() ?: 0f
        )
    }

    override fun saveNotes(documentId: String, notesJson: String) {
        window.localStorage.setItem("${documentId}_notes", notesJson)
    }

    override fun getNotes(documentId: String): String? {
        return window.localStorage.getItem("${documentId}_notes")
    }

    override fun saveHighlights(documentId: String, highlightsJson: String) {
        window.localStorage.setItem("${documentId}_highlights", highlightsJson)
    }

    override fun getHighlights(documentId: String): String? {
        return window.localStorage.getItem("${documentId}_highlights")
    }
}

@Composable
actual fun rememberPdfDatabase(): PdfDatabase {
    return remember { WasmJsPdfDatabase() }
}
