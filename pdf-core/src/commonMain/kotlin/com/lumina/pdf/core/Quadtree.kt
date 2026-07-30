package com.lumina.pdf.core

data class QuadtreeBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun intersects(other: QuadtreeBounds): Boolean {
        return !(left > other.right || right < other.left || top > other.bottom || bottom < other.top)
    }

    fun intersects(rect: PdfRect): Boolean {
        return !(left > rect.right || right < rect.left || top > rect.bottom || bottom < rect.top)
    }

    fun contains(rect: PdfRect): Boolean {
        return rect.left >= left && rect.right <= right && rect.top >= top && rect.bottom <= bottom
    }
}

class PageQuadtree(
    val bounds: QuadtreeBounds,
    private val maxDepth: Int = 8,
    private val bucketSize: Int = 32,
    private val depth: Int = 0
) {
    private val items = mutableListOf<PdfRect>()
    private var isSplit = false

    private var nw: PageQuadtree? = null
    private var ne: PageQuadtree? = null
    private var sw: PageQuadtree? = null
    private var se: PageQuadtree? = null

    fun insert(rect: PdfRect): Boolean {
        // If the rectangle does not intersect this node's bounds, do not insert
        if (!bounds.intersects(rect)) {
            return false
        }

        if (!isSplit) {
            if (items.size < bucketSize || depth >= maxDepth) {
                items.add(rect)
                return true
            }
            subdivide()
        }

        // Try to insert into children
        val inserted = (nw?.insert(rect) ?: false) ||
                       (ne?.insert(rect) ?: false) ||
                       (sw?.insert(rect) ?: false) ||
                       (se?.insert(rect) ?: false)
        
        // If none of the children accepted it but it intersects us, keep it in this node
        if (!inserted) {
            items.add(rect)
        }
        return true
    }

    private fun subdivide() {
        val midX = (bounds.left + bounds.right) / 2f
        val midY = (bounds.top + bounds.bottom) / 2f

        nw = PageQuadtree(QuadtreeBounds(bounds.left, bounds.top, midX, midY), maxDepth, bucketSize, depth + 1)
        ne = PageQuadtree(QuadtreeBounds(midX, bounds.top, bounds.right, midY), maxDepth, bucketSize, depth + 1)
        sw = PageQuadtree(QuadtreeBounds(bounds.left, midY, midX, bounds.bottom), maxDepth, bucketSize, depth + 1)
        se = PageQuadtree(QuadtreeBounds(midX, midY, bounds.right, bounds.bottom), maxDepth, bucketSize, depth + 1)

        isSplit = true

        val oldItems = ArrayList(items)
        items.clear()

        for (item in oldItems) {
            val inserted = (nw?.insert(item) ?: false) ||
                           (ne?.insert(item) ?: false) ||
                           (sw?.insert(item) ?: false) ||
                           (se?.insert(item) ?: false)
            if (!inserted) {
                items.add(item)
            }
        }
    }

    fun query(viewport: QuadtreeBounds, result: MutableList<PdfRect> = mutableListOf()): List<PdfRect> {
        if (!bounds.intersects(viewport)) {
            return result
        }

        // Add matching items in this node
        for (item in items) {
            if (viewport.intersects(item)) {
                result.add(item)
            }
        }

        if (isSplit) {
            nw?.query(viewport, result)
            ne?.query(viewport, result)
            sw?.query(viewport, result)
            se?.query(viewport, result)
        }

        return result
    }
}
