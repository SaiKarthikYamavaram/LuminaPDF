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

    override fun saveNotes(documentId: String, notesJson: String) {
        // Future implementation
    }

    override fun getNotes(documentId: String): String? {
        return null
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
