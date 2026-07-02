package com.lumina.pdf.core

data class Point2D(val x: Float, val y: Float)

object SutherlandHodgman {

    fun clipToRect(
        subject: List<Point2D>,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): List<Point2D> {
        if (subject.isEmpty()) return emptyList()

        var outputList = subject

        // 1. Clip against left plane (x >= left)
        var inputList = outputList
        outputList = mutableListOf()
        if (inputList.isNotEmpty()) {
            var s = inputList.last()
            for (p in inputList) {
                val sInside = s.x >= left
                val pInside = p.x >= left
                if (pInside) {
                    if (!sInside) {
                        val t = (left - s.x) / (p.x - s.x)
                        val iy = s.y + t * (p.y - s.y)
                        outputList.add(Point2D(left, iy))
                    }
                    outputList.add(p)
                } else if (sInside) {
                    val t = (left - s.x) / (p.x - s.x)
                    val iy = s.y + t * (p.y - s.y)
                    outputList.add(Point2D(left, iy))
                }
                s = p
            }
        }

        // 2. Clip against right plane (x <= right)
        inputList = outputList
        outputList = mutableListOf()
        if (inputList.isNotEmpty()) {
            var s = inputList.last()
            for (p in inputList) {
                val sInside = s.x <= right
                val pInside = p.x <= right
                if (pInside) {
                    if (!sInside) {
                        val t = (right - s.x) / (p.x - s.x)
                        val iy = s.y + t * (p.y - s.y)
                        outputList.add(Point2D(right, iy))
                    }
                    outputList.add(p)
                } else if (sInside) {
                    val t = (right - s.x) / (p.x - s.x)
                    val iy = s.y + t * (p.y - s.y)
                    outputList.add(Point2D(right, iy))
                }
                s = p
            }
        }

        // 3. Clip against top plane (y >= top)
        inputList = outputList
        outputList = mutableListOf()
        if (inputList.isNotEmpty()) {
            var s = inputList.last()
            for (p in inputList) {
                val sInside = s.y >= top
                val pInside = p.y >= top
                if (pInside) {
                    if (!sInside) {
                        val t = (top - s.y) / (p.y - s.y)
                        val ix = s.x + t * (p.x - s.x)
                        outputList.add(Point2D(ix, top))
                    }
                    outputList.add(p)
                } else if (sInside) {
                    val t = (top - s.y) / (p.y - s.y)
                    val ix = s.x + t * (p.x - s.x)
                    outputList.add(Point2D(ix, top))
                }
                s = p
            }
        }

        // 4. Clip against bottom plane (y <= bottom)
        inputList = outputList
        outputList = mutableListOf()
        if (inputList.isNotEmpty()) {
            var s = inputList.last()
            for (p in inputList) {
                val sInside = s.y <= bottom
                val pInside = p.y <= bottom
                if (pInside) {
                    if (!sInside) {
                        val t = (bottom - s.y) / (p.y - s.y)
                        val ix = s.x + t * (p.x - s.x)
                        outputList.add(Point2D(ix, bottom))
                    }
                    outputList.add(p)
                } else if (sInside) {
                    val t = (bottom - s.y) / (p.y - s.y)
                    val ix = s.x + t * (p.x - s.x)
                    outputList.add(Point2D(ix, bottom))
                }
                s = p
            }
        }

        return outputList
    }

    fun clip(subject: List<Point2D>, clipPolygon: List<Point2D>): List<Point2D> {
        if (subject.isEmpty() || clipPolygon.isEmpty()) return emptyList()

        // Determine winding direction of the clipping polygon using signed area
        var area = 0f
        for (i in clipPolygon.indices) {
            val p1 = clipPolygon[i]
            val p2 = clipPolygon[(i + 1) % clipPolygon.size]
            area += (p1.x * p2.y - p2.x * p1.y)
        }
        val isCounterClockwise = area > 0

        var outputList = subject

        for (i in clipPolygon.indices) {
            val edgeStart = clipPolygon[i]
            val edgeEnd = clipPolygon[(i + 1) % clipPolygon.size]

            val inputList = outputList
            outputList = mutableListOf()

            if (inputList.isEmpty()) return emptyList()

            var s = inputList.last()
            for (p in inputList) {
                val sInside = isInside(s, edgeStart, edgeEnd, isCounterClockwise)
                val pInside = isInside(p, edgeStart, edgeEnd, isCounterClockwise)

                if (pInside) {
                    if (!sInside) {
                        val intersect = intersection(s, p, edgeStart, edgeEnd)
                        if (intersect != null) outputList.add(intersect)
                    }
                    outputList.add(p)
                } else if (sInside) {
                    val intersect = intersection(s, p, edgeStart, edgeEnd)
                    if (intersect != null) outputList.add(intersect)
                }
                s = p
            }
        }

        return outputList
    }

    private fun isInside(p: Point2D, edgeStart: Point2D, edgeEnd: Point2D, isCCW: Boolean): Boolean {
        val crossProduct = (edgeEnd.x - edgeStart.x) * (p.y - edgeStart.y) - (edgeEnd.y - edgeStart.y) * (p.x - edgeStart.x)
        return if (isCCW) crossProduct >= -1e-5f else crossProduct <= 1e-5f
    }

    private fun intersection(s: Point2D, p: Point2D, edgeStart: Point2D, edgeEnd: Point2D): Point2D? {
        val numX = (s.x * p.y - s.y * p.x) * (edgeStart.x - edgeEnd.x) - (s.x - p.x) * (edgeStart.x * edgeEnd.y - edgeStart.y * edgeEnd.x)
        val numY = (s.x * p.y - s.y * p.x) * (edgeStart.y - edgeEnd.y) - (s.y - p.y) * (edgeStart.x * edgeEnd.y - edgeStart.y * edgeEnd.x)
        val den = (s.x - p.x) * (edgeStart.y - edgeEnd.y) - (s.y - p.y) * (edgeStart.x - edgeEnd.x)
        
        if (den == 0f) return null
        return Point2D(numX / den, numY / den)
    }
}
