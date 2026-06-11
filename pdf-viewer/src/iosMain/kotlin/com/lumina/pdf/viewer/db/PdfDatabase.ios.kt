package com.lumina.pdf.viewer.db

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

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
    return remember { IosPdfDatabase() }
}
