package com.lumina.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RecentFilesStoreTest {
    private val bytes = ByteArray(4096) { 1 }
    private val otherBytesSameSize = ByteArray(4096) { 2 }

    private fun stored(id: String, name: String, byteCount: Int = 4096, isSample: Boolean = false) = RecentFile(
        id = id,
        name = name,
        lastOpened = "",
        size = formatSize(4096),
        isSample = isSample,
        byteCount = byteCount
    )

    private fun find(files: List<RecentFile>, name: String, picked: ByteArray, disk: Map<String, ByteArray>) =
        findStoredDocument(files, name, picked) { disk[it] }

    @Test
    fun sameNameAndSizeWithDifferentContentIsADifferentDocument() {
        assertNull(find(listOf(stored("a", "report.pdf")), "report.pdf", otherBytesSameSize, mapOf("a" to bytes)))
    }

    @Test
    fun sameContentMatchesEvenAfterRename() {
        val file = stored("a", "renamed.pdf")
        assertSame(file, find(listOf(file), "report.pdf", bytes.copyOf(), mapOf("a" to bytes)))
    }

    @Test
    fun entryWithMissingBytesIsRestoredByNameAndSize() {
        val file = stored("a", "report.pdf")
        assertSame(file, find(listOf(file), "report.pdf", bytes, emptyMap()))
    }

    @Test
    fun contentMatchWinsOverMissingBytesEntry() {
        val missing = stored("a", "report.pdf")
        val match = stored("b", "copy.pdf")
        assertSame(match, find(listOf(missing, match), "report.pdf", bytes, mapOf("b" to bytes)))
    }

    @Test
    fun sampleEntryIsNeverClaimedByName() {
        val sample = stored("a", "Sample.pdf", isSample = true)
        assertNull(find(listOf(sample), "Sample.pdf", bytes, emptyMap()))
    }

    @Test
    fun differentSizedDocumentsAreNotReadFromDisk() {
        val other = stored("a", "other.pdf", byteCount = 10)
        val read = mutableListOf<String>()
        findStoredDocument(listOf(other), "report.pdf", bytes) { read += it; null }
        assertEquals(emptyList(), read)
    }

    @Test
    fun newDocumentIdIgnoresFilenameAndDiffersByContent() {
        assertEquals(newDocumentId(bytes), newDocumentId(bytes.copyOf()))
        assertEquals(false, newDocumentId(bytes) == newDocumentId(otherBytesSameSize))
    }

    @Test
    fun delimitersInFreeTextDoNotShiftColumns() {
        val metadata = serializeMetadata(listOf(stored("id", "a|b\nc.pdf").copy(folder = "x|y")))
        val rows = metadata.lines().filter { it.isNotBlank() }
        assertEquals(1, rows.size)
        assertEquals(listOf("id", "a b c.pdf", "", formatSize(4096), "0", "1", "x y", "4096"), rows.single().split('|'))
    }
}
