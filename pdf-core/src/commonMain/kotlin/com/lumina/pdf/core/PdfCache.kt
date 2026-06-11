package com.lumina.pdf.core

interface MemoryCache {
    fun get(key: String): PdfBitmap?
    fun put(key: String, bitmap: PdfBitmap)
    fun remove(key: String)
    fun clear()
}

interface DiskCache {
    suspend fun get(key: String): ByteArray?
    suspend fun put(key: String, data: ByteArray)
    suspend fun remove(key: String)
    suspend fun clear()
}
