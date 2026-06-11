package com.lumina.pdf.viewer.db

import androidx.compose.runtime.Composable

data class PdfOffset(
    val page: Int,
    val zoom: Float,
    val scrollX: Float,
    val scrollY: Float
)

interface PdfDatabase {
    fun saveOffset(documentId: String, offset: PdfOffset)
    fun getOffset(documentId: String): PdfOffset?
    
    // Future expansion hooks for notes and highlights
    fun saveNotes(documentId: String, notesJson: String)
    fun getNotes(documentId: String): String?
    
    fun saveHighlights(documentId: String, highlightsJson: String)
    fun getHighlights(documentId: String): String?
}

@Composable
expect fun rememberPdfDatabase(): PdfDatabase
