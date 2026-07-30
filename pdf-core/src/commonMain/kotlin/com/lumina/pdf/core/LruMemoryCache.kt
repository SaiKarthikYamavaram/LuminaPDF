package com.lumina.pdf.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LruMemoryCache(private val maxBytes: Long) : MemoryCache {
    private val map = LinkedHashMap<String, PdfBitmap>(16, 0.75f)
    private val mutex = Mutex()
    private var currentBytes = 0L

    private fun sizeOf(bitmap: PdfBitmap): Long {
        return bitmap.width.toLong() * bitmap.height * 4
    }

    override suspend fun get(key: String): PdfBitmap? = mutex.withLock {
        // LinkedHashMap isn't automatically LRU in commonMain without JVM's accessOrder=true.
        // We simulate LRU by removing and re-inserting on get.
        val value = map.remove(key) ?: return@withLock null
        map[key] = value
        value
    }

    override suspend fun put(key: String, bitmap: PdfBitmap) = mutex.withLock {
        val old = map.remove(key)
        if (old != null) {
            currentBytes -= sizeOf(old)
            old.close()
        }
        map[key] = bitmap
        currentBytes += sizeOf(bitmap)
        trimToSize()
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            val removed = map.remove(key)
            if (removed != null) {
                currentBytes -= sizeOf(removed)
                removed.close()
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            map.values.forEach { it.close() }
            map.clear()
            currentBytes = 0L
        }
    }

    private fun trimToSize() {
        while (currentBytes > maxBytes) {
            val eldestKey = map.keys.firstOrNull() ?: break
            val eldestValue = map.remove(eldestKey)
            if (eldestValue != null) {
                currentBytes -= sizeOf(eldestValue)
                eldestValue.close()
            }
        }
    }
}
