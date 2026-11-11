package com.lumina.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RecentFilesStoreTest {
    private val bytes = ByteArray(4096) { 1 }
    private val otherBytesSameSize = ByteArray(4096) { 2 }

    private fun stored(name: String, fileBytes: ByteArray?, isSample: Boolean = false) = RecentFile(
        id = "id",
        name = name,
        lastOpened = "",
        size = formatSize(4096),
        isSample = isSample,
        fileBytes = fileBytes
    )

    @Test
    fun sameNameAndSizeWithDifferentContentIsADifferentDocument() {
        assertNull(findStoredDocument(listOf(stored("report.pdf", bytes)), "report.pdf", otherBytesSameSize))
    }

    @Test
    fun sameContentMatchesEvenAfterRename() {
        val file = stored("renamed.pdf", bytes)
        assertSame(file, findStoredDocument(listOf(file), "report.pdf", bytes.copyOf()))
    }

    @Test
    fun entryWithMissingBytesIsRestoredByNameAndSize() {
        val file = stored("report.pdf", null)
        assertSame(file, findStoredDocument(listOf(file), "report.pdf", bytes))
    }

    @Test
    fun contentMatchWinsOverMissingBytesEntry() {
        val missing = stored("report.pdf", null)
        val match = stored("copy.pdf", bytes)
        assertSame(match, findStoredDocument(listOf(missing, match), "report.pdf", bytes))
    }

    @Test
    fun sampleEntryIsNeverClaimedByName() {
        assertNull(findStoredDocument(listOf(stored("Sample.pdf", null, isSample = true)), "Sample.pdf", bytes))
    }

    @Test
    fun newDocumentIdIgnoresFilenameAndDiffersByContent() {
        assertEquals(newDocumentId(bytes), newDocumentId(bytes.copyOf()))
        assertEquals(false, newDocumentId(bytes) == newDocumentId(otherBytesSameSize))
    }

    @Test
    fun delimitersInFreeTextDoNotShiftColumns() {
        val metadata = serializeMetadata(listOf(stored("a|b\nc.pdf", null).copy(folder = "x|y")))
        val rows = metadata.lines().filter { it.isNotBlank() }
        assertEquals(1, rows.size)
        assertEquals(listOf("id", "a b c.pdf", "", formatSize(4096), "0", "1", "x y"), rows.single().split('|'))
    }
}
