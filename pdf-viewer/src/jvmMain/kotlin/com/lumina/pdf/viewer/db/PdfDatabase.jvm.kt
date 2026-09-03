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

    override fun saveHighlights(documentId: String, highlightsJson: String) {
        // Future implementation
    }

    override fun getHighlights(documentId: String): String? {
        return null
    }
}

@Composable
actual fun rememberPdfDatabase(): PdfDatabase {
    return remember { JvmPdfDatabase() }
}

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
