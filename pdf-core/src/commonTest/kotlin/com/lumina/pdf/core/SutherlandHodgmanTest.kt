package com.lumina.pdf.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SutherlandHodgmanTest {

    @Test
    fun testFullyInsideRect() {
        val subject = listOf(
            Point2D(2f, 2f),
            Point2D(4f, 2f),
            Point2D(3f, 4f)
        )
        // Clip window is (0, 0, 10, 10)
        val result = SutherlandHodgman.clipToRect(subject, 0f, 0f, 10f, 10f)
        assertEquals(3, result.size)
        assertEquals(subject, result)
    }

    @Test
    fun testFullyOutsideRect() {
        val subject = listOf(
            Point2D(12f, 12f),
            Point2D(14f, 12f),
            Point2D(13f, 14f)
        )
        val result = SutherlandHodgman.clipToRect(subject, 0f, 0f, 10f, 10f)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testPartialClip() {
        // Triangle from (5, -2) to (15, 5) to (5, 12)
        // Clip window (0, 0, 10, 10)
        val subject = listOf(
            Point2D(5f, -2f),
            Point2D(15f, 5f),
            Point2D(5f, 12f)
        )
        val result = SutherlandHodgman.clipToRect(subject, 0f, 0f, 10f, 10f)
        
        // Assert that all output coordinates are within bounds
        for (p in result) {
            assertTrue(p.x in 0f..10f, "Expected X ${p.x} to be in [0, 10]")
            assertTrue(p.y in 0f..10f, "Expected Y ${p.y} to be in [0, 10]")
        }
    }

    @Test
    fun testPolygonClipping() {
        val subject = listOf(
            Point2D(2f, 2f),
            Point2D(8f, 2f),
            Point2D(5f, 8f)
        )
        val clipPolygon = listOf(
            Point2D(0f, 0f),
            Point2D(10f, 0f),
            Point2D(10f, 5f),
            Point2D(0f, 5f)
        )
        val result = SutherlandHodgman.clip(subject, clipPolygon)
        
        for (p in result) {
            assertTrue(p.x in 0f..10f)
            assertTrue(p.y in 0f..5f)
        }
    }
}
