package com.lumina.pdf.core

class StripSpatialIndex(private val stripHeight: Float = 0.015f) {
    private val strips = HashMap<Int, MutableList<CharacterBox>>()

    fun build(characters: List<CharacterBox>) {
        strips.clear()
        for (charBox in characters) {
            val startStrip = (charBox.top / stripHeight).toInt()
            val endStrip = (charBox.bottom / stripHeight).toInt()
            for (strip in startStrip..endStrip) {
                strips.getOrPut(strip) { mutableListOf() }.add(charBox)
            }
        }
        for (list in strips.values) {
            list.sortBy { it.left }
        }
    }

    fun query(x: Float, y: Float): CharacterBox? {
        val strip = (y / stripHeight).toInt()
        val boxes = strips[strip] ?: return null
        
        var closest: CharacterBox? = null
        var minDistance = Float.MAX_VALUE
        
        for (box in boxes) {
            val centerX = (box.left + box.right) / 2f
            val centerY = (box.top + box.bottom) / 2f
            val dx = x - centerX
            val dy = y - centerY
            val dist = dx * dx + dy * dy
            if (dist < minDistance) {
                minDistance = dist
                closest = box
            }
        }
        return closest
    }

    fun queryRange(x1: Float, y1: Float, x2: Float, y2: Float): List<CharacterBox> {
        val minX = minOf(x1, x2)
        val maxX = maxOf(x1, x2)
        val minY = minOf(y1, y2)
        val maxY = maxOf(y1, y2)

        val startStrip = (minY / stripHeight).toInt()
        val endStrip = (maxY / stripHeight).toInt()

        val results = mutableSetOf<CharacterBox>()
        for (strip in startStrip..endStrip) {
            val boxes = strips[strip] ?: continue
            for (box in boxes) {
                if (box.right >= minX && box.left <= maxX && box.bottom >= minY && box.top <= maxY) {
                    results.add(box)
                }
            }
        }
        return results.toList().sortedWith(compareBy({ it.top }, { it.left }))
    }
}
