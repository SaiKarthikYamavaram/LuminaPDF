package com.lumina.pdf.viewer.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LuminaDatabaseHighlightTest {

    // An in-memory DB per test - not the app's real ~/.lumina/lumina.db (that's what
    // JvmPdfDatabase wires up), so this exercises the same queries without touching real state.
    private fun newQueries(): LuminaDatabaseQueries {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        LuminaDatabase.Schema.create(driver)
        return LuminaDatabase(driver).luminaDatabaseQueries
    }

    @Test
    fun insertAndGetHighlightsRoundTrips() {
        val queries = newQueries()
        queries.insertHighlight("h1", "doc1", 0L, "0.1,0.2,0.3,0.4", "#FFEB3B", 100L)
        queries.insertHighlight("h2", "doc1", 1L, "0.5,0.5,0.6,0.6", "#FFEB3B", 200L)
        queries.insertHighlight("h3", "doc2", 0L, "0.0,0.0,0.1,0.1", "#FFEB3B", 300L)

        val doc1Highlights = queries.getHighlights("doc1").executeAsList()
        assertEquals(2, doc1Highlights.size)
        // ORDER BY timestamp DESC
        assertEquals("h2", doc1Highlights[0].id)
        assertEquals("h1", doc1Highlights[1].id)

        val doc2Highlights = queries.getHighlights("doc2").executeAsList()
        assertEquals(1, doc2Highlights.size)
        assertEquals("h3", doc2Highlights[0].id)
    }

    @Test
    fun deleteHighlightRemovesOnlyThatRow() {
        val queries = newQueries()
        queries.insertHighlight("h1", "doc1", 0L, "0.1,0.2,0.3,0.4", "#FFEB3B", 100L)
        queries.insertHighlight("h2", "doc1", 1L, "0.5,0.5,0.6,0.6", "#FFEB3B", 200L)

        queries.deleteHighlight("h1")

        val remaining = queries.getHighlights("doc1").executeAsList()
        assertEquals(1, remaining.size)
        assertEquals("h2", remaining[0].id)
    }

    @Test
    fun getHighlightsForUnknownDocumentIsEmpty() {
        val queries = newQueries()
        assertTrue(queries.getHighlights("nonexistent").executeAsList().isEmpty())
    }
}
