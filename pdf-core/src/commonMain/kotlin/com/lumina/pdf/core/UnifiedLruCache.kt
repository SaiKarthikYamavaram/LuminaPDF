package com.lumina.pdf.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class CacheEntryType {
    TILE, THUMBNAIL
}

class CacheNode(
    val key: String,
    val bitmap: PdfBitmap,
    val sizeBytes: Long,
    val type: CacheEntryType,
    val zoomLevel: Int,
    var prev: CacheNode? = null,
    var next: CacheNode? = null
)

class UnifiedLruCache(private val maxBytes: Long) : MemoryCache {
    private val map = HashMap<String, CacheNode>()
    private val mutex = Mutex()
    private var currentBytes = 0L
    
    private var head: CacheNode? = null
    private var tail: CacheNode? = null

    private fun sizeOf(bitmap: PdfBitmap): Long {
        return bitmap.width.toLong() * bitmap.height * 4
    }

    private fun getEntryTypeAndZoom(key: String): Pair<CacheEntryType, Int> {
        return if (key.startsWith("tile_")) {
            val parts = key.split('_')
            val zoom = parts.lastOrNull()?.toIntOrNull() ?: 0
            Pair(CacheEntryType.TILE, zoom)
        } else {
            Pair(CacheEntryType.THUMBNAIL, 0)
        }
    }

    override suspend fun get(key: String): PdfBitmap? = mutex.withLock {
        val node = map[key] ?: return@withLock null
        moveToHead(node)
        node.bitmap
    }

    override suspend fun put(key: String, bitmap: PdfBitmap) {
        mutex.withLock {
            val oldNode = map.remove(key)
            if (oldNode != null) {
                removeNode(oldNode)
                currentBytes -= oldNode.sizeBytes
                oldNode.bitmap.close()
            }

            val size = sizeOf(bitmap)
            val (type, zoom) = getEntryTypeAndZoom(key)
            val newNode = CacheNode(key, bitmap, size, type, zoom)
            
            map[key] = newNode
            addToHead(newNode)
            currentBytes += size
            
            trimToSize()
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            val node = map.remove(key)
            if (node != null) {
                removeNode(node)
                currentBytes -= node.sizeBytes
                node.bitmap.close()
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            var curr = head
            while (curr != null) {
                curr.bitmap.close()
                curr = curr.next
            }
            map.clear()
            head = null
            tail = null
            currentBytes = 0L
        }
    }

    suspend fun evictByZoomLevel(currentZoomLevelInt: Int) = mutex.withLock {
        var curr = head
        while (curr != null) {
            val nextNode = curr.next
            if (curr.type == CacheEntryType.TILE && curr.zoomLevel != currentZoomLevelInt) {
                map.remove(curr.key)
                removeNode(curr)
                currentBytes -= curr.sizeBytes
                curr.bitmap.close()
            }
            curr = nextNode
        }
    }

    private fun addToHead(node: CacheNode) {
        node.next = head
        node.prev = null
        if (head != null) {
            head?.prev = node
        }
        head = node
        if (tail == null) {
            tail = node
        }
    }

    private fun removeNode(node: CacheNode) {
        val prev = node.prev
        val next = node.next
        if (prev != null) {
            prev.next = next
        } else {
            head = next
        }
        if (next != null) {
            next.prev = prev
        } else {
            tail = prev
        }
        node.prev = null
        node.next = null
    }

    private fun moveToHead(node: CacheNode) {
        removeNode(node)
        addToHead(node)
    }

    private fun trimToSize() {
        while (currentBytes > maxBytes) {
            val last = tail ?: break
            map.remove(last.key)
            removeNode(last)
            currentBytes -= last.sizeBytes
            last.bitmap.close()
        }
    }
}
