package com.lumina.pdf.core

import kotlin.math.floor

data class SdfGlyph(
    val char: Char,
    val width: Int,
    val height: Int,
    val sdfData: FloatArray // SDF values normalized [0.0, 1.0]
)

class SdfGlyphAtlas {
    private val glyphs = HashMap<Char, SdfGlyph>()

    fun hasGlyph(char: Char): Boolean {
        return glyphs.containsKey(char)
    }

    fun getGlyph(char: Char): SdfGlyph? {
        return glyphs[char]
    }

    fun putGlyph(char: Char, glyph: SdfGlyph) {
        glyphs[char] = glyph
    }

    fun sampleAt(glyph: SdfGlyph, u: Float, v: Float): Float {
        val w = glyph.width
        val h = glyph.height
        if (w <= 0 || h <= 0) return 0f

        val x = u.coerceIn(0f, 1f) * (w - 1)
        val y = v.coerceIn(0f, 1f) * (h - 1)

        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val x1 = (x0 + 1).coerceAtMost(w - 1)
        val y1 = (y0 + 1).coerceAtMost(h - 1)

        val dx = x - x0
        val dy = y - y0

        val val00 = glyph.sdfData[y0 * w + x0]
        val val10 = glyph.sdfData[y0 * w + x1]
        val val01 = glyph.sdfData[y1 * w + x0]
        val val11 = glyph.sdfData[y1 * w + x1]

        val w00 = (1.0f - dx) * (1.0f - dy)
        val w10 = dx * (1.0f - dy)
        val w01 = (1.0f - dx) * dy
        val w11 = dx * dy

        return val00 * w00 + val10 * w10 + val01 * w01 + val11 * w11
    }
}
