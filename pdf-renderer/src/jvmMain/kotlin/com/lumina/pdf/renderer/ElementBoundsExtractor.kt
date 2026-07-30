package com.lumina.pdf.renderer

import com.lumina.pdf.core.PdfRect
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.image.PDImage
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.io.IOException

class ElementBoundsExtractor(private val page: PDPage) : PDFGraphicsStreamEngine(page) {
    private val elementBoundsList = mutableListOf<PdfRect>()
    private var currentPathBounds: Rectangle2D.Float? = null

    val cropBox = page.cropBox
    val pageWidth = cropBox.width
    val pageHeight = cropBox.height

    fun getBounds(): List<PdfRect> {
        return elementBoundsList
    }

    private fun transformX(x: Float, y: Float): Float {
        val ctm = graphicsState.currentTransformationMatrix
        return ctm.getValue(0, 0) * x + ctm.getValue(1, 0) * y + ctm.getValue(2, 0)
    }

    private fun transformY(x: Float, y: Float): Float {
        val ctm = graphicsState.currentTransformationMatrix
        return ctm.getValue(0, 1) * x + ctm.getValue(1, 1) * y + ctm.getValue(2, 1)
    }

    private fun updatePathBounds(x: Float, y: Float) {
        val tx = transformX(x, y)
        val ty = transformY(x, y)
        val bounds = currentPathBounds
        if (bounds == null) {
            currentPathBounds = Rectangle2D.Float(tx, ty, 0f, 0f)
        } else {
            bounds.add(tx.toDouble(), ty.toDouble())
        }
    }

    private fun addRectToElements(bounds2D: Rectangle2D) {
        // Normalize coordinates to 0f..1f (top-left origin)
        // PDF coordinates are bottom-left origin, so y is inverted
        val left = (bounds2D.minX / pageWidth).toFloat().coerceIn(0f, 1f)
        val right = (bounds2D.maxX / pageWidth).toFloat().coerceIn(0f, 1f)
        val bottom = (1f - (bounds2D.minY / pageHeight)).toFloat().coerceIn(0f, 1f)
        val top = (1f - (bounds2D.maxY / pageHeight)).toFloat().coerceIn(0f, 1f)

        if (left < right && top < bottom) {
            elementBoundsList.add(PdfRect(left, top, right, bottom))
        }
    }

    @Throws(IOException::class)
    override fun appendRectangle(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D) {
        updatePathBounds(p0.x.toFloat(), p0.y.toFloat())
        updatePathBounds(p1.x.toFloat(), p1.y.toFloat())
        updatePathBounds(p2.x.toFloat(), p2.y.toFloat())
        updatePathBounds(p3.x.toFloat(), p3.y.toFloat())
    }

    @Throws(IOException::class)
    override fun drawImage(pdImage: PDImage) {
        // An image is placed according to CTM
        // Let's get CTM bounds for the unit rectangle (0,0) to (1,1)
        val x0 = transformX(0f, 0f)
        val y0 = transformY(0f, 0f)
        val x1 = transformX(1f, 0f)
        val y1 = transformY(1f, 0f)
        val x2 = transformX(1f, 1f)
        val y2 = transformY(1f, 1f)
        val x3 = transformX(0f, 1f)
        val y3 = transformY(0f, 1f)

        val bounds = Rectangle2D.Float(x0, y0, 0f, 0f)
        bounds.add(x1.toDouble(), y1.toDouble())
        bounds.add(x2.toDouble(), y2.toDouble())
        bounds.add(x3.toDouble(), y3.toDouble())

        addRectToElements(bounds)
    }

    @Throws(IOException::class)
    override fun clip(windingRule: Int) {
        // Clipping doesn't draw elements directly, but can limit them
    }

    @Throws(IOException::class)
    override fun moveTo(x: Float, y: Float) {
        updatePathBounds(x, y)
    }

    @Throws(IOException::class)
    override fun lineTo(x: Float, y: Float) {
        updatePathBounds(x, y)
    }

    @Throws(IOException::class)
    override fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        updatePathBounds(x1, y1)
        updatePathBounds(x2, y2)
        updatePathBounds(x3, y3)
    }

    override fun getCurrentPoint(): Point2D? {
        val bounds = currentPathBounds ?: return null
        return Point2D.Float(bounds.x + bounds.width, bounds.y + bounds.height)
    }

    @Throws(IOException::class)
    override fun closePath() {
    }

    @Throws(IOException::class)
    override fun endPath() {
        currentPathBounds = null
    }

    @Throws(IOException::class)
    override fun strokePath() {
        val bounds = currentPathBounds
        if (bounds != null) {
            addRectToElements(bounds)
        }
        currentPathBounds = null
    }

    @Throws(IOException::class)
    override fun fillPath(windingRule: Int) {
        val bounds = currentPathBounds
        if (bounds != null) {
            addRectToElements(bounds)
        }
        currentPathBounds = null
    }

    @Throws(IOException::class)
    override fun fillAndStrokePath(windingRule: Int) {
        val bounds = currentPathBounds
        if (bounds != null) {
            addRectToElements(bounds)
        }
        currentPathBounds = null
    }

    @Throws(IOException::class)
    override fun shadingFill(shadingName: COSName) {
    }
}
