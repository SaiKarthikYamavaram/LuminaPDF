package com.lumina.pdf.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LruMemoryCache(private val maxSize: Int) : MemoryCache {
    private val map = LinkedHashMap<String, PdfBitmap>(maxSize, 0.75f)
    private val mutex = Mutex()

    override fun get(key: String): PdfBitmap? {
        // LinkedHashMap isn't automatically LRU in commonMain without JVM's accessOrder=true.
        // We simulate LRU by removing and re-inserting on get.
        val value = map.remove(key) ?: return null
        map[key] = value
        return value
    }

    override fun put(key: String, bitmap: PdfBitmap) {
        map[key] = bitmap
        trimToSize()
    }

    override fun remove(key: String) {
        map.remove(key)?.close()
    }

    override fun clear() {
        map.values.forEach { it.close() }
        map.clear()
    }

    private fun trimToSize() {
        while (map.size > maxSize) {
            val eldestKey = map.keys.firstOrNull() ?: break
            val eldestValue = map.remove(eldestKey)
            eldestValue?.close()
        }
    }
}
