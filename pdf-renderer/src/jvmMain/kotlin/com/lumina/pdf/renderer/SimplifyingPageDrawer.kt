package com.lumina.pdf.renderer

import com.lumina.pdf.core.ImagePyramid
import com.lumina.pdf.core.PyramidLevel
import org.apache.pdfbox.rendering.PageDrawer
import org.apache.pdfbox.rendering.PageDrawerParameters
import org.apache.pdfbox.pdmodel.graphics.image.PDImage
import java.awt.RenderingHints
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.IOException

class SimplifyingPageDrawer(
    parameters: PageDrawerParameters,
    private val epsilon: Double,
    private val sdfAtlas: com.lumina.pdf.core.SdfGlyphAtlas
) : PageDrawer(parameters) {

    private val imagePyramidCache = java.util.WeakHashMap<org.apache.pdfbox.cos.COSBase, ImagePyramid>()

    override fun strokePath() {
        val path = linePath
        if (path != null) {
            val simplified = RdpPathSimplifier.simplifyPath(path, epsilon)
            val clipped = clipPath(simplified)
            linePath.reset()
            linePath.append(clipped, false)
        }
        super.strokePath()
    }

    override fun fillPath(windingRule: Int) {
        val path = linePath
        if (path != null) {
            val simplified = RdpPathSimplifier.simplifyPath(path, epsilon)
            val clipped = clipPath(simplified)
            linePath.reset()
            linePath.append(clipped, false)
        }
        super.fillPath(windingRule)
    }

    private fun clipPath(path: GeneralPath): GeneralPath {
        val clipBounds = graphics.clipBounds ?: return path
        val left = clipBounds.minX.toFloat()
        val top = clipBounds.minY.toFloat()
        val right = clipBounds.maxX.toFloat()
        val bottom = clipBounds.maxY.toFloat()

        val newPath = GeneralPath()
        val iter = path.getPathIterator(null)
        val coords = FloatArray(6)
        
        val currentPolyline = mutableListOf<Point2D>()
        
        fun flushPolyline() {
            if (currentPolyline.isNotEmpty()) {
                val clipped = com.lumina.pdf.core.SutherlandHodgman.clipToRect(
                    currentPolyline.map { com.lumina.pdf.core.Point2D(it.x.toFloat(), it.y.toFloat()) },
                    left, top, right, bottom
                )
                if (clipped.isNotEmpty()) {
                    for (i in clipped.indices) {
                        val p = clipped[i]
                        if (i == 0) {
                            newPath.moveTo(p.x, p.y)
                        } else {
                            newPath.lineTo(p.x, p.y)
                        }
                    }
                }
                currentPolyline.clear()
            }
        }

        while (!iter.isDone) {
            val type = iter.currentSegment(coords)
            when (type) {
                java.awt.geom.PathIterator.SEG_MOVETO -> {
                    flushPolyline()
                    currentPolyline.add(Point2D.Double(coords[0].toDouble(), coords[1].toDouble()))
                }
                java.awt.geom.PathIterator.SEG_LINETO -> {
                    currentPolyline.add(Point2D.Double(coords[0].toDouble(), coords[1].toDouble()))
                }
                java.awt.geom.PathIterator.SEG_QUADTO -> {
                    flushPolyline()
                    newPath.quadTo(coords[0], coords[1], coords[2], coords[3])
                }
                java.awt.geom.PathIterator.SEG_CUBICTO -> {
                    flushPolyline()
                    newPath.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5])
                }
                java.awt.geom.PathIterator.SEG_CLOSE -> {
                    flushPolyline()
                    newPath.closePath()
                }
            }
            iter.next()
        }
        flushPolyline()
        return newPath
    }

    @Throws(IOException::class)
    override fun drawImage(pdImage: PDImage) {
        val cosObject = pdImage.cosObject
        val pyramid = synchronized(imagePyramidCache) {
            imagePyramidCache.getOrPut(cosObject) {
                generatePyramid(pdImage)
            }
        }

        // Get the current Graphics2D transform
        val transform = graphics.transform
        val p0 = transform.transform(Point2D.Double(0.0, 0.0), null)
        val p1 = transform.transform(Point2D.Double(1.0, 0.0), null)
        val scaleX = p0.distance(p1)
        val displayWidth = (pdImage.width * scaleX).toInt().coerceAtLeast(1)

        val bestLevel = pyramid.selectLevel(displayWidth)
        
        // Convert PyramidLevel data back to BufferedImage to render
        val levelImage = BufferedImage(bestLevel.width, bestLevel.height, BufferedImage.TYPE_INT_ARGB)
        val levelRgb = IntArray(bestLevel.width * bestLevel.height)
        for (i in levelRgb.indices) {
            val r = bestLevel.data[i * 4].toInt() and 0xFF
            val g = bestLevel.data[i * 4 + 1].toInt() and 0xFF
            val b = bestLevel.data[i * 4 + 2].toInt() and 0xFF
            val a = bestLevel.data[i * 4 + 3].toInt() and 0xFF
            levelRgb[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        levelImage.setRGB(0, 0, bestLevel.width, bestLevel.height, levelRgb, 0, bestLevel.width)

        // Draw it using the graphics context (which handles correct positioning and rendering hints)
        val oldRenderingHints = graphics.renderingHints
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(levelImage, 0, 0, 1, 1, null)
        graphics.setRenderingHints(oldRenderingHints)
    }

    private fun generatePyramid(pdImage: PDImage): ImagePyramid {
        val originalImage = pdImage.image
        val levels = mutableListOf<PyramidLevel>()
        
        var currentImage = originalImage
        while (currentImage.width >= 32 && currentImage.height >= 32) {
            levels.add(createLevel(currentImage))
            
            // Downsample by 2
            val nextW = currentImage.width / 2
            val nextH = currentImage.height / 2
            if (nextW < 16 || nextH < 16) break
            
            val scaled = BufferedImage(nextW, nextH, BufferedImage.TYPE_INT_ARGB)
            val g2d = scaled.createGraphics()
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.drawImage(currentImage, 0, 0, nextW, nextH, null)
            g2d.dispose()
            
            currentImage = scaled
        }
        
        return ImagePyramid(levels)
    }

    private fun createLevel(img: BufferedImage): PyramidLevel {
        val w = img.width
        val h = img.height
        val data = ByteArray(w * h * 4)
        val rgb = img.getRGB(0, 0, w, h, null, 0, w)
        for (i in rgb.indices) {
            val pixel = rgb[i]
            val a = (pixel shr 24 and 0xFF).toByte()
            val r = (pixel shr 16 and 0xFF).toByte()
            val g = (pixel shr 8 and 0xFF).toByte()
            val b = (pixel and 0xFF).toByte()
            
            data[i * 4] = r
            data[i * 4 + 1] = g
            data[i * 4 + 2] = b
            data[i * 4 + 3] = a
        }
        return PyramidLevel(w, h, data)
    }

    @Throws(IOException::class)
    override fun showGlyph(
        textRenderingMatrix: org.apache.pdfbox.util.Matrix,
        font: org.apache.pdfbox.pdmodel.font.PDFont,
        code: Int,
        displacement: org.apache.pdfbox.util.Vector
    ) {
        val unicode = font.toUnicode(code)
        val char = unicode?.firstOrNull()
        val transform = graphics.transform
        val p0 = transform.transform(Point2D.Double(0.0, 0.0), null)
        val p1 = transform.transform(Point2D.Double(1.0, 0.0), null)
        val scaleX = p0.distance(p1).toFloat()

        if (char != null && scaleX > 2.0f && sdfAtlas.hasGlyph(char)) {
            val sdfGlyph = sdfAtlas.getGlyph(char)
            if (sdfGlyph != null) {
                val w = font.getWidth(code) / 1000f
                val bounds = font.boundingBox
                val yStart = bounds.lowerLeftY / 1000f
                val yEnd = bounds.upperRightY / 1000f
                val h = yEnd - yStart

                val combinedScale = scaleX * textRenderingMatrix.scaleX
                val displayW = (w * combinedScale).toInt().coerceIn(1, 256)
                val displayH = (h * combinedScale).toInt().coerceIn(1, 256)

                val glyphImage = BufferedImage(displayW, displayH, BufferedImage.TYPE_INT_ARGB)
                val color = graphics.color
                val textRgb = color.rgb
                
                val levelRgb = IntArray(displayW * displayH)
                for (dy in 0 until displayH) {
                    for (dx in 0 until displayW) {
                        val u = dx.toFloat() / (displayW - 1).coerceAtLeast(1)
                        val v = dy.toFloat() / (displayH - 1).coerceAtLeast(1)
                        
                        val sdfVal = sdfAtlas.sampleAt(sdfGlyph, u, v)
                        
                        if (sdfVal >= 0.5f) {
                            levelRgb[dy * displayW + dx] = textRgb
                        } else {
                            levelRgb[dy * displayW + dx] = 0x00000000
                        }
                    }
                }
                glyphImage.setRGB(0, 0, displayW, displayH, levelRgb, 0, displayW)

                val oldTransform = graphics.transform
                val at = java.awt.geom.AffineTransform(
                    textRenderingMatrix.getValue(0, 0).toDouble(), textRenderingMatrix.getValue(0, 1).toDouble(),
                    textRenderingMatrix.getValue(1, 0).toDouble(), textRenderingMatrix.getValue(1, 1).toDouble(),
                    textRenderingMatrix.getValue(2, 0).toDouble(), textRenderingMatrix.getValue(2, 1).toDouble()
                )
                graphics.transform(at)

                val atDraw = java.awt.geom.AffineTransform.getTranslateInstance(0.0, -yEnd.toDouble())
                atDraw.scale(w.toDouble(), h.toDouble())
                graphics.drawImage(glyphImage, atDraw, null)

                graphics.transform = oldTransform
                return
            }
        }

        super.showGlyph(textRenderingMatrix, font, code, displacement)
    }
}
