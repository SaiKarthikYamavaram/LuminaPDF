package com.lumina.pdf.core

private const val BYTES_PER_PIXEL = 4L // ARGB approximation; PdfBitmap exposes no format/byte-size info.

class LruMemoryCache(
    private val maxBytes: Long = 64L * 1024 * 1024, // 64MB default budget
    private val maxEntries: Int = 300 // belt-and-suspenders cap vs. many-tiny-bitmaps pathology
) : MemoryCache {
    private val map = LinkedHashMap<String, PdfBitmap>(16, 0.75f)
    private var currentBytes: Long = 0L

    override fun get(key: String): PdfBitmap? {
        // LinkedHashMap isn't automatically LRU in commonMain without JVM's accessOrder=true.
        // We simulate LRU by removing and re-inserting on get.
        val value = map.remove(key) ?: return null
        map[key] = value
        return value
    }

    override fun put(key: String, bitmap: PdfBitmap) {
        map.remove(key)?.let { old ->
            currentBytes -= sizeOf(old)
            old.close()
        }
        map[key] = bitmap
        currentBytes += sizeOf(bitmap)
        trimToSize()
    }

    override fun remove(key: String) {
        val value = map.remove(key) ?: return
        currentBytes -= sizeOf(value)
        value.close()
    }

    override fun clear() {
        map.values.forEach { it.close() }
        map.clear()
        currentBytes = 0L
    }

    private fun sizeOf(bitmap: PdfBitmap): Long =
        bitmap.width.toLong() * bitmap.height.toLong() * BYTES_PER_PIXEL

    private fun trimToSize() {
        while ((currentBytes > maxBytes || map.size > maxEntries) && map.isNotEmpty()) {
            val eldestKey = map.keys.firstOrNull() ?: break
            val eldestValue = map.remove(eldestKey) ?: break
            currentBytes -= sizeOf(eldestValue)
            eldestValue.close()
        }
    }
}
