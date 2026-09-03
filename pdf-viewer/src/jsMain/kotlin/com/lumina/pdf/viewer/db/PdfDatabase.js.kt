package com.lumina.pdf.viewer.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

class JsPdfDatabase : PdfDatabase {
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

    override fun insertNote(note: DbNote) {
        window.localStorage.setItem("${note.documentId}_note_${note.id}", "${note.page}|${note.timestamp}|${note.text}")
    }

    override fun getNotes(documentId: String): List<DbNote> {
        val list = mutableListOf<DbNote>()
        val length = window.localStorage.length
        for (i in 0 until length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.startsWith("${documentId}_note_")) {
                val value = window.localStorage.getItem(key) ?: continue
                val parts = value.split("|", limit = 3)
                if (parts.size == 3) {
                    val page = parts[0].toIntOrNull() ?: 0
                    val timestamp = parts[1].toLongOrNull() ?: 0L
                    val text = parts[2]
                    val noteId = key.removePrefix("${documentId}_note_")
                    list.add(DbNote(noteId, documentId, page, text, timestamp))
                }
            }
        }
        return list.sortedByDescending { it.timestamp }
    }

    override fun getDocumentIdsWithNotes(): Set<String> {
        val ids = mutableSetOf<String>()
        val length = window.localStorage.length
        for (i in 0 until length) {
            val key = window.localStorage.key(i) ?: continue
            val noteIndex = key.indexOf("_note_")
            if (noteIndex >= 0) {
                ids.add(key.substring(0, noteIndex))
            }
        }
        return ids
    }

    override fun deleteNote(noteId: String) {
        val length = window.localStorage.length
        for (i in 0 until length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.endsWith("_note_$noteId")) {
                window.localStorage.removeItem(key)
                break
            }
        }
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
    return remember { JsPdfDatabase() }
}

actual fun getCurrentTimeMillis(): Long = js("Date.now()").toString().toLongOrNull() ?: 0L
