package com.lumina.pdf.core

import kotlin.math.floor

data class PyramidLevel(
    val width: Int,
    val height: Int,
    val data: ByteArray // RGBA format, 4 bytes per pixel
)

class ImagePyramid(val levels: List<PyramidLevel>) {
    
    fun selectLevel(displayWidth: Int): PyramidLevel {
        if (levels.isEmpty()) {
            throw IllegalStateException("Pyramid has no levels")
        }
        // Levels are ordered from largest to smallest (1/1, 1/2, 1/4, 1/8, ...)
        // Find the smallest level that is still >= displayWidth
        var bestLevel = levels.first()
        for (level in levels) {
            if (level.width >= displayWidth) {
                bestLevel = level
            } else {
                // Once we hit a level smaller than displayWidth, stop and return the last valid one
                break
            }
        }
        return bestLevel
    }

    fun sampleBilinear(level: PyramidLevel, u: Float, v: Float): Int {
        val w = level.width
        val h = level.height
        if (w <= 0 || h <= 0) return 0

        val x = (u.coerceIn(0f, 1f) * (w - 1))
        val y = (v.coerceIn(0f, 1f) * (h - 1))

        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val x1 = (x0 + 1).coerceAtMost(w - 1)
        val y1 = (y0 + 1).coerceAtMost(h - 1)

        val dx = x - x0
        val dy = y - y0

        val c00 = getPixel(level, x0, y0)
        val c10 = getPixel(level, x1, y0)
        val c01 = getPixel(level, x0, y1)
        val c11 = getPixel(level, x1, y1)

        // Interpolate each channel
        val a = interpolateChannel(c00 shr 24 and 0xFF, c10 shr 24 and 0xFF, c01 shr 24 and 0xFF, c11 shr 24 and 0xFF, dx, dy)
        val r = interpolateChannel(c00 shr 16 and 0xFF, c10 shr 16 and 0xFF, c01 shr 16 and 0xFF, c11 shr 16 and 0xFF, dx, dy)
        val g = interpolateChannel(c00 shr 8 and 0xFF, c10 shr 8 and 0xFF, c01 shr 8 and 0xFF, c11 shr 8 and 0xFF, dx, dy)
        val b = interpolateChannel(c00 and 0xFF, c10 and 0xFF, c01 and 0xFF, c11 and 0xFF, dx, dy)

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun getPixel(level: PyramidLevel, x: Int, y: Int): Int {
        val index = (y * level.width + x) * 4
        if (index + 3 >= level.data.size) return 0
        
        val r = level.data[index].toInt() and 0xFF
        val g = level.data[index + 1].toInt() and 0xFF
        val b = level.data[index + 2].toInt() and 0xFF
        val a = level.data[index + 3].toInt() and 0xFF
        
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun interpolateChannel(c00: Int, c10: Int, c01: Int, c11: Int, dx: Float, dy: Float): Int {
        val w00 = (1.0f - dx) * (1.0f - dy)
        val w10 = dx * (1.0f - dy)
        val w01 = (1.0f - dx) * dy
        val w11 = dx * dy
        return (c00 * w00 + c10 * w10 + c01 * w01 + c11 * w11).toInt().coerceIn(0, 255)
    }
}
