package com.lumina.pdf.viewer.db

import androidx.compose.runtime.Composable

data class PdfOffset(
    val page: Int,
    val zoom: Float,
    val scrollX: Float,
    val scrollY: Float
)

data class DbNote(
    val id: String,
    val documentId: String,
    val page: Int,
    val text: String,
    val timestamp: Long
)

interface PdfDatabase {
    fun saveOffset(documentId: String, offset: PdfOffset)
    fun getOffset(documentId: String): PdfOffset?
    
    fun insertNote(note: DbNote)
    fun getNotes(documentId: String): List<DbNote>
    fun getDocumentIdsWithNotes(): Set<String>
    fun deleteNote(noteId: String)
    
    fun saveHighlights(documentId: String, highlightsJson: String)
    fun getHighlights(documentId: String): String?
}

@Composable
expect fun rememberPdfDatabase(): PdfDatabase

expect fun getCurrentTimeMillis(): Long
