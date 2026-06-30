package com.lumina.pdf.core

interface MemoryCache {
    suspend fun get(key: String): PdfBitmap?
    suspend fun put(key: String, bitmap: PdfBitmap)
    suspend fun remove(key: String)
    suspend fun clear()
}

interface DiskCache {
    suspend fun get(key: String): ByteArray?
    suspend fun put(key: String, data: ByteArray)
    suspend fun remove(key: String)
    suspend fun clear()
}
