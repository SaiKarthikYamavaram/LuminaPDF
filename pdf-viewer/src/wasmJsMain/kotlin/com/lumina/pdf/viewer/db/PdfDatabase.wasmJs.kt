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

    override fun insertHighlight(highlight: DbHighlight) {
        window.localStorage.setItem(
            "${highlight.documentId}_highlight_${highlight.id}",
            "${highlight.page}|${highlight.timestamp}|${highlight.color}|${highlight.rectsText}"
        )
    }

    override fun getHighlights(documentId: String): List<DbHighlight> {
        val list = mutableListOf<DbHighlight>()
        val length = window.localStorage.length
        for (i in 0 until length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.startsWith("${documentId}_highlight_")) {
                val value = window.localStorage.getItem(key) ?: continue
                val parts = value.split("|", limit = 4)
                if (parts.size == 4) {
                    val page = parts[0].toIntOrNull() ?: 0
                    val timestamp = parts[1].toLongOrNull() ?: 0L
                    val color = parts[2]
                    val rectsText = parts[3]
                    val id = key.removePrefix("${documentId}_highlight_")
                    list.add(DbHighlight(id, documentId, page, rectsText, color, timestamp))
                }
            }
        }
        return list.sortedByDescending { it.timestamp }
    }

    override fun deleteHighlight(highlightId: String) {
        val length = window.localStorage.length
        for (i in 0 until length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.endsWith("_highlight_$highlightId")) {
                window.localStorage.removeItem(key)
                break
            }
        }
    }

    override fun insertBookmark(bookmark: DbBookmark) {
        window.localStorage.setItem("${bookmark.documentId}_bookmark_${bookmark.id}", "${bookmark.page}|${bookmark.timestamp}")
    }

    override fun getBookmarks(documentId: String): List<DbBookmark> {
        val list = mutableListOf<DbBookmark>()
        val length = window.localStorage.length
        for (i in 0 until length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.startsWith("${documentId}_bookmark_")) {
                val value = window.localStorage.getItem(key) ?: continue
                val parts = value.split("|", limit = 2)
                if (parts.size == 2) {
                    val page = parts[0].toIntOrNull() ?: 0
                    val timestamp = parts[1].toLongOrNull() ?: 0L
                    val id = key.removePrefix("${documentId}_bookmark_")
                    list.add(DbBookmark(id, documentId, page, timestamp))
                }
            }
        }
        return list.sortedBy { it.page }
    }

    override fun deleteBookmark(bookmarkId: String) {
        val length = window.localStorage.length
        for (i in 0 until length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.endsWith("_bookmark_$bookmarkId")) {
                window.localStorage.removeItem(key)
                break
            }
        }
    }
}

@Composable
actual fun rememberPdfDatabase(): PdfDatabase {
    return remember { WasmJsPdfDatabase() }
}

private fun jsDateNow(): Double = js("Date.now()")

actual fun getCurrentTimeMillis(): Long = jsDateNow().toLong()
