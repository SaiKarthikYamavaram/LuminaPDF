package com.lumina.pdf.viewer.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeleteDocumentDataTest {
    private val queries = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).let { driver ->
        LuminaDatabase.Schema.create(driver)
        LuminaDatabase(driver).luminaDatabaseQueries
    }

    private fun seed(documentId: String) {
        queries.saveOffset(documentId, 1, 1.0, 0.0, 0.0)
        queries.insertNote("n_$documentId", documentId, 1, "note", 1)
        queries.insertHighlight("h_$documentId", documentId, 1, "0,0,1,1", "#FFEB3B", 1)
        queries.insertBookmark("b_$documentId", documentId, 1, 1)
    }

    @Test
    fun removesEveryRowForThatDocumentAndNoOther() {
        seed("deleted")
        seed("kept")

        queries.deleteDocumentData("deleted")

        assertNull(queries.getOffset("deleted").executeAsOneOrNull())
        assertEquals(0, queries.getNotes("deleted").executeAsList().size)
        assertEquals(0, queries.getHighlights("deleted").executeAsList().size)
        assertEquals(0, queries.getBookmarks("deleted").executeAsList().size)

        assertNotNull(queries.getOffset("kept").executeAsOneOrNull())
        assertEquals(1, queries.getNotes("kept").executeAsList().size)
        assertEquals(1, queries.getHighlights("kept").executeAsList().size)
        assertEquals(1, queries.getBookmarks("kept").executeAsList().size)
    }
}
