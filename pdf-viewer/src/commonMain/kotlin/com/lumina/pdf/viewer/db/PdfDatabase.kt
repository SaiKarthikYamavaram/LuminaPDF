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

data class DbHighlight(
    val id: String,
    val documentId: String,
    val page: Int,
    // "left,top,right,bottom" in the same 0..1 normalized page-fraction space as PdfRect.
    // Holds one rect for now; a future highlight spanning a line wrap could extend this to
    // multiple ";"-separated rect groups without a schema change.
    val rectsText: String,
    val color: String,
    val timestamp: Long
)

data class DbBookmark(
    val id: String,
    val documentId: String,
    val page: Int,
    val timestamp: Long
)

interface PdfDatabase {
    fun saveOffset(documentId: String, offset: PdfOffset)
    fun getOffset(documentId: String): PdfOffset?

    fun insertNote(note: DbNote)
    fun getNotes(documentId: String): List<DbNote>
    fun getDocumentIdsWithNotes(): Set<String>
    fun deleteNote(noteId: String)

    fun insertHighlight(highlight: DbHighlight)
    fun getHighlights(documentId: String): List<DbHighlight>
    fun deleteHighlight(highlightId: String)

    fun insertBookmark(bookmark: DbBookmark)
    fun getBookmarks(documentId: String): List<DbBookmark>
    fun deleteBookmark(bookmarkId: String)
}

@Composable
expect fun rememberPdfDatabase(): PdfDatabase

expect fun getCurrentTimeMillis(): Long
