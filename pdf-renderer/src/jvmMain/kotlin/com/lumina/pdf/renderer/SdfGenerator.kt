package com.lumina.pdf.renderer

import com.lumina.pdf.core.SdfGlyph
import com.lumina.pdf.core.SdfGlyphAtlas
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.sqrt

object SdfGenerator {

    fun generateGlyph(char: Char, font: Font, atlasResolution: Int = 64): SdfGlyph {
        // Render glyph in high-res (e.g. atlasResolution x atlasResolution)
        val img = BufferedImage(atlasResolution, atlasResolution, BufferedImage.TYPE_BYTE_GRAY)
        val g2d = img.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.background = Color.BLACK
        g2d.clearRect(0, 0, atlasResolution, atlasResolution)
        g2d.color = Color.WHITE
        g2d.font = font.deriveFont(atlasResolution * 0.8f) // Scale font size to fit

        val fm = g2d.fontMetrics
        val rect = fm.getStringBounds(char.toString(), g2d)
        val x = ((atlasResolution - rect.width) / 2).toFloat()
        val y = ((atlasResolution - rect.height) / 2 + fm.ascent).toFloat()
        g2d.drawString(char.toString(), x, y)
        g2d.dispose()

        // Get inside/outside mapping
        val grid = BooleanArray(atlasResolution * atlasResolution)
        val edgePixels = mutableListOf<Pair<Int, Int>>()

        for (cy in 0 until atlasResolution) {
            for (cx in 0 until atlasResolution) {
                val rgb = img.getRGB(cx, cy) and 0xFF
                grid[cy * atlasResolution + cx] = rgb > 127
            }
        }

        // Detect edges
        for (cy in 0 until atlasResolution) {
            for (cx in 0 until atlasResolution) {
                val current = grid[cy * atlasResolution + cx]
                var isEdge = false
                // Check 8-neighbors
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = cx + dx
                        val ny = cy + dy
                        if (nx in 0 until atlasResolution && ny in 0 until atlasResolution) {
                            if (grid[ny * atlasResolution + nx] != current) {
                                isEdge = true
                                break
                            }
                        }
                    }
                    if (isEdge) break
                }
                if (isEdge) {
                    edgePixels.add(Pair(cx, cy))
                }
            }
        }

        // Generate target SDF grid (e.g. half the resolution)
        val targetSize = atlasResolution / 2
        val sdfData = FloatArray(targetSize * targetSize)
        val maxDist = (atlasResolution / 4).toFloat().coerceAtLeast(8f)

        for (ty in 0 until targetSize) {
            for (tx in 0 until targetSize) {
                val hx = tx * 2 + 1
                val hy = ty * 2 + 1
                val isInside = grid[hy * atlasResolution + hx]

                var minDistanceSq = Float.MAX_VALUE
                for (edge in edgePixels) {
                    val dx = hx - edge.first
                    val dy = hy - edge.second
                    val distSq = (dx * dx + dy * dy).toFloat()
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                    }
                }

                val distance = if (edgePixels.isEmpty()) maxDist else sqrt(minDistanceSq)
                val signedDistance = if (isInside) distance else -distance
                
                // Normalize to [0.0, 1.0] where 0.5 is edge
                val normalized = (signedDistance / (2f * maxDist) + 0.5f).coerceIn(0f, 1f)
                sdfData[ty * targetSize + tx] = normalized
            }
        }

        return SdfGlyph(char, targetSize, targetSize, sdfData)
    }

    fun buildAtlasForFont(font: Font, charSet: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,!?@#"): SdfGlyphAtlas {
        val atlas = SdfGlyphAtlas()
        for (char in charSet) {
            val glyph = generateGlyph(char, font)
            atlas.putGlyph(char, glyph)
        }
        return atlas
    }
}
