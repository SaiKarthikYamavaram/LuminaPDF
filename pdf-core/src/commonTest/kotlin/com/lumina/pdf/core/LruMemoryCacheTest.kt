package com.lumina.pdf.core

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakePdfBitmap(override val width: Int, override val height: Int) : PdfBitmap {
    var closed = false
        private set

    override fun close() {
        closed = true
    }
}

class LruMemoryCacheTest {

    @Test
    fun evictsOldestWhenByteBudgetExceeded() {
        val cache = LruMemoryCache(maxBytes = 400L, maxEntries = 100)
        val a = FakePdfBitmap(10, 10) // 400 bytes
        val b = FakePdfBitmap(10, 10) // 400 bytes
        cache.put("a", a)
        cache.put("b", b) // 800 > 400 -> evict "a"

        assertNull(cache.get("a"))
        assertTrue(a.closed)
        assertNotNull(cache.get("b"))
    }

    @Test
    fun getRefreshesRecencyOrder() {
        val cache = LruMemoryCache(maxBytes = 800L, maxEntries = 100)
        val a = FakePdfBitmap(10, 10)
        val b = FakePdfBitmap(10, 10)
        val c = FakePdfBitmap(10, 10)
        cache.put("a", a)
        cache.put("b", b)
        cache.get("a") // touch a -> b becomes LRU
        cache.put("c", c) // evicts b, not a

        assertNull(cache.get("b"))
        assertTrue(b.closed)
        assertNotNull(cache.get("a"))
        assertNotNull(cache.get("c"))
    }

    @Test
    fun entryCountCapEvictsEvenUnderByteBudget() {
        val cache = LruMemoryCache(maxBytes = Long.MAX_VALUE, maxEntries = 2)
        val a = FakePdfBitmap(1, 1)
        val b = FakePdfBitmap(1, 1)
        val c = FakePdfBitmap(1, 1)
        cache.put("a", a)
        cache.put("b", b)
        cache.put("c", c) // exceeds entry cap -> evict a

        assertNull(cache.get("a"))
        assertNotNull(cache.get("b"))
        assertNotNull(cache.get("c"))
    }

    @Test
    fun putReplacingExistingKeyClosesOldAndUpdatesAccounting() {
        val cache = LruMemoryCache(maxBytes = 400L, maxEntries = 100)
        val small = FakePdfBitmap(5, 5) // 100 bytes
        val big = FakePdfBitmap(10, 10) // 400 bytes
        cache.put("k", small)
        cache.put("k", big) // must not double-count small's bytes

        assertTrue(small.closed)
        assertNotNull(cache.get("k"))
    }

    @Test
    fun clearClosesAllAndResetsByteTotal() {
        val cache = LruMemoryCache(maxBytes = 10_000L, maxEntries = 100)
        val a = FakePdfBitmap(10, 10)
        cache.put("a", a)
        cache.clear()

        assertTrue(a.closed)
        assertNull(cache.get("a"))
    }
}
