package com.lumina.pdf.core

private const val BYTES_PER_PIXEL = 4L // ARGB approximation; PdfBitmap exposes no format/byte-size info.

class LruMemoryCache(
    private val maxBytes: Long = 64L * 1024 * 1024, // 64MB default budget
    private val maxEntries: Int = 300, // belt-and-suspenders cap vs. many-tiny-bitmaps pathology
    // Keys this predicate marks true are skipped by eviction even under pressure. Without this,
    // trimToSize() could recycle a bitmap that a caller is still actively drawing on screen from
    // a separate reference (e.g. Compose state holding the same ImageBitmap outside the cache),
    // crashing with "Canvas: trying to use a recycled bitmap".
    private val isPinned: (String) -> Boolean = { false }
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
        val iterator = map.entries.iterator()
        while ((currentBytes > maxBytes || map.size > maxEntries) && iterator.hasNext()) {
            val (key, value) = iterator.next()
            if (isPinned(key)) continue
            iterator.remove()
            currentBytes -= sizeOf(value)
            value.close()
        }
    }
}
