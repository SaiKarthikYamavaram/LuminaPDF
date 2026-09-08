package com.lumina.pdf.viewer.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

class JvmPdfDatabase : PdfDatabase {
    private val driver: SqlDriver
    private val database: LuminaDatabase
    private val queries: LuminaDatabaseQueries

    init {
        val dbFile = File(System.getProperty("user.home"), ".lumina/lumina.db")
        dbFile.parentFile.mkdirs()
        
        driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        
        try {
            LuminaDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // Already created
        }
        
        database = LuminaDatabase(driver)
        queries = database.luminaDatabaseQueries
    }

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
}

@Composable
actual fun rememberPdfDatabase(): PdfDatabase {
    return remember { JvmPdfDatabase() }
}

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
