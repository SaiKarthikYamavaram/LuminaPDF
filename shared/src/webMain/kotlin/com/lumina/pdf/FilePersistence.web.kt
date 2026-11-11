package com.lumina.pdf

// ponytail: session only, so the library empties on reload. localStorage is too small for PDFs and
// IndexedDB is async, which needs FilePersistence to become suspending. Notes and reading position
// do survive, because they live in localStorage keyed by content-based document id.
actual object FilePersistence {
    private val files = mutableMapOf<String, ByteArray>()
    private var metadata: String? = null

    actual fun saveRecentFile(id: String, bytes: ByteArray) {
        files[id] = bytes
    }

    actual fun loadRecentFile(id: String): ByteArray? = files[id]

    actual fun deleteRecentFile(id: String) {
        files.remove(id)
    }

    actual fun saveMetadata(metadata: String) {
        this.metadata = metadata
    }

    actual fun loadMetadata(): String? = metadata
}
