package com.lumina.pdf

actual object FilePersistence {
    actual fun saveRecentFile(id: String, bytes: ByteArray) {}
    actual fun loadRecentFile(id: String): ByteArray? = null
    actual fun deleteRecentFile(id: String) {}
    actual fun saveMetadata(metadata: String) {}
    actual fun loadMetadata(): String? = null
}
