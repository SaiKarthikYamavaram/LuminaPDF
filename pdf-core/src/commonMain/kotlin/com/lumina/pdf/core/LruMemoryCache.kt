package com.lumina.pdf.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LruMemoryCache(private val maxSize: Int) : MemoryCache {
    private val map = LinkedHashMap<String, PdfBitmap>(maxSize, 0.75f)
    private val mutex = Mutex()

    override suspend fun get(key: String): PdfBitmap? = mutex.withLock {
        // LinkedHashMap isn't automatically LRU in commonMain without JVM's accessOrder=true.
        // We simulate LRU by removing and re-inserting on get.
        val value = map.remove(key) ?: return@withLock null
        map[key] = value
        value
    }

    override suspend fun put(key: String, bitmap: PdfBitmap) = mutex.withLock {
        map[key] = bitmap
        trimToSize()
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            map.remove(key)?.close()
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            map.values.forEach { it.close() }
            map.clear()
        }
    }

    private fun trimToSize() {
        while (map.size > maxSize) {
            val eldestKey = map.keys.firstOrNull() ?: break
            val eldestValue = map.remove(eldestKey)
            eldestValue?.close()
        }
    }
}
