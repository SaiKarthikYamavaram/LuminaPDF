package com.lumina.pdf.viewer.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

class IosPdfDatabase : PdfDatabase {
    private val driver: SqlDriver = NativeSqliteDriver(LuminaDatabase.Schema, "lumina.db")
    private val database = LuminaDatabase(driver)
    private val queries = database.luminaDatabaseQueries

    override fun saveOffset(documentId: String, offset: PdfOffset) {
        queries.saveOffset(documentId, offset.page.toLong(), offset.zoom.toDouble(), offset.scrollX.toDouble(), offset.scrollY.toDouble())
    }

    override fun getOffset(documentId: String): PdfOffset? {
        val row = queries.getOffset(documentId).executeAsOneOrNull() ?: return null
        return PdfOffset(
            page = row.page.toInt(),
            zoom = row.zoom.toFloat(),
            scrollX = row.scrollX.toFloat(),
            scrollY = row.scrollY.toFloat()
        )
    }

    override fun insertNote(note: DbNote) {
        queries.insertNote(note.id, note.documentId, note.page.toLong(), note.text, note.timestamp)
    }

    override fun getNotes(documentId: String): List<DbNote> {
        return queries.getNotes(documentId).executeAsList().map {
            DbNote(
                id = it.id,
                documentId = it.documentId,
                page = it.page.toInt(),
                text = it.noteText,
                timestamp = it.timestamp
            )
        }
    }

    override fun getDocumentIdsWithNotes(): Set<String> {
        return queries.getDocumentIdsWithNotes().executeAsList().toSet()
    }

    override fun deleteNote(noteId: String) {
        queries.deleteNote(noteId)
    }

    override fun insertHighlight(highlight: DbHighlight) {
        queries.insertHighlight(highlight.id, highlight.documentId, highlight.page.toLong(), highlight.rectsText, highlight.color, highlight.timestamp)
    }

    override fun getHighlights(documentId: String): List<DbHighlight> {
        return queries.getHighlights(documentId).executeAsList().map {
            DbHighlight(
                id = it.id,
                documentId = it.documentId,
                page = it.page.toInt(),
                rectsText = it.rectsText,
                color = it.color,
                timestamp = it.timestamp
            )
        }
    }

    override fun deleteHighlight(highlightId: String) {
        queries.deleteHighlight(highlightId)
    }

    override fun insertBookmark(bookmark: DbBookmark) {
        queries.insertBookmark(bookmark.id, bookmark.documentId, bookmark.page.toLong(), bookmark.timestamp)
    }

    override fun getBookmarks(documentId: String): List<DbBookmark> {
        return queries.getBookmarks(documentId).executeAsList().map {
            DbBookmark(
                id = it.id,
                documentId = it.documentId,
                page = it.page.toInt(),
                timestamp = it.timestamp
            )
        }
    }

    override fun deleteBookmark(bookmarkId: String) {
        queries.deleteBookmark(bookmarkId)
    }

    override fun deleteDocumentData(documentId: String) {
        queries.deleteDocumentData(documentId)
    }
}

// One connection for the whole process. Each driver holds its own and nothing closes them.
private val sharedDatabase by lazy { IosPdfDatabase() }

@Composable
actual fun rememberPdfDatabase(): PdfDatabase = sharedDatabase

actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
