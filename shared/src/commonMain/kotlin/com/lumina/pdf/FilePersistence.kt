package com.lumina.pdf

expect object FilePersistence {
    fun saveRecentFile(id: String, bytes: ByteArray)
    fun loadRecentFile(id: String): ByteArray?
    fun deleteRecentFile(id: String)
    fun saveMetadata(metadata: String)
    fun loadMetadata(): String?
}
