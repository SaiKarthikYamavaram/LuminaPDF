package com.lumina.pdf.renderer

import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import kotlin.math.abs
import kotlin.math.sqrt

object RdpPathSimplifier {
    fun simplify(points: List<Point2D>, epsilon: Double): List<Point2D> {
        if (points.size <= 2) return points

        var maxDist = 0.0
        var index = -1
        val end = points.size - 1

        for (i in 1 until end) {
            val dist = perpendicularDistance(points[i], points[0], points[end])
            if (dist > maxDist) {
                maxDist = dist
                index = i
            }
        }

        return if (maxDist > epsilon) {
            val results1 = simplify(points.subList(0, index + 1), epsilon)
            val results2 = simplify(points.subList(index, points.size), epsilon)
            results1.dropLast(1) + results2
        } else {
            listOf(points.first(), points.last())
        }
    }

    private fun perpendicularDistance(p: Point2D, lineStart: Point2D, lineEnd: Point2D): Double {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y

        val lenSq = dx * dx + dy * dy
        if (lenSq == 0.0) return p.distance(lineStart)

        val num = abs(dy * p.x - dx * p.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x)
        val den = sqrt(lenSq)
        return num / den
    }

    fun simplifyPath(path: GeneralPath, epsilon: Double): GeneralPath {
        val newPath = GeneralPath()
        val iter = path.getPathIterator(null)
        val coords = FloatArray(6)
        
        val polyline = mutableListOf<Point2D>()
        
        fun flushPolyline() {
            if (polyline.isNotEmpty()) {
                val simplified = simplify(polyline, epsilon)
                for (i in simplified.indices) {
                    val p = simplified[i]
                    if (i == 0) {
                        val curr = newPath.currentPoint
                        if (curr == null) {
                            newPath.moveTo(p.x.toFloat(), p.y.toFloat())
                        } else if (abs(curr.x - p.x) > 1e-4 || abs(curr.y - p.y) > 1e-4) {
                            newPath.lineTo(p.x.toFloat(), p.y.toFloat())
                        }
                    } else {
                        newPath.lineTo(p.x.toFloat(), p.y.toFloat())
                    }
                }
                polyline.clear()
            }
        }

        while (!iter.isDone) {
            val type = iter.currentSegment(coords)
            when (type) {
                PathIterator.SEG_MOVETO -> {
                    flushPolyline()
                    newPath.moveTo(coords[0], coords[1])
                    polyline.add(Point2D.Double(coords[0].toDouble(), coords[1].toDouble()))
                }
                PathIterator.SEG_LINETO -> {
                    polyline.add(Point2D.Double(coords[0].toDouble(), coords[1].toDouble()))
                }
                PathIterator.SEG_QUADTO -> {
                    flushPolyline()
                    newPath.quadTo(coords[0], coords[1], coords[2], coords[3])
                }
                PathIterator.SEG_CUBICTO -> {
                    flushPolyline()
                    newPath.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5])
                }
                PathIterator.SEG_CLOSE -> {
                    flushPolyline()
                    newPath.closePath()
                }
            }
            iter.next()
        }
        flushPolyline()
        return newPath
    }
}
