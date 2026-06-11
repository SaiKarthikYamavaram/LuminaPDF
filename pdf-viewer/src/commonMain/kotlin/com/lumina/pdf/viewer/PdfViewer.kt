package com.lumina.pdf.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.lumina.pdf.core.PdfDocument

@Composable
fun PdfViewer(
    document: PdfDocument,
    state: PdfViewerState = remember { PdfViewerState() },
    config: PdfViewerConfig = PdfViewerConfig(),
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    val pageBitmaps = remember { mutableStateMapOf<Int, ImageBitmap>() }
    var pageCount by remember { mutableStateOf(0) }

    LaunchedEffect(document) {
        pageCount = document.pageCount()
    }

    // Basic Virtualization and rendering for MVP
    LaunchedEffect(document, viewSize, state.zoom, state.scrollOffset) {
        if (viewSize.width == 0 || viewSize.height == 0 || pageCount == 0) return@LaunchedEffect
        
        // For continuous scroll, estimate visible pages based on scroll offset.
        // Simplification for Phase 1 MVP: assume fixed aspect ratio for pages to calculate visible ones,
        // or just render the current page + 1.
        val estimatedPageHeight = viewSize.width * 1.4f // A4 aspect ratio approximation
        val totalScrollY = -state.scrollOffset.y
        
        val firstVisiblePage = (totalScrollY / (estimatedPageHeight * state.zoom)).toInt().coerceIn(0, pageCount - 1)
        val lastVisiblePage = ((totalScrollY + viewSize.height) / (estimatedPageHeight * state.zoom)).toInt().coerceIn(0, pageCount - 1)

        val pagesToRender = (firstVisiblePage..lastVisiblePage).toList()
        state.visiblePages = pagesToRender
        state.currentPage = firstVisiblePage

        for (page in pagesToRender) {
            if (!pageBitmaps.containsKey(page)) {
                // Render placeholder or low-res here in real implementation.
                val bitmap = document.renderPage(
                    page = page,
                    width = (viewSize.width * state.zoom).toInt().coerceAtMost(2048),
                    height = (estimatedPageHeight * state.zoom).toInt().coerceAtMost(2048)
                )
                pageBitmaps[page] = bitmap.asImageBitmap()
                bitmap.close()
            }
        }

        // Cleanup invisible pages
        val keysToRemove = pageBitmaps.keys.filter { it !in (firstVisiblePage - 1..lastVisiblePage + 1) }
        for (key in keysToRemove) {
            pageBitmaps.remove(key)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomMultiplier, rotation ->
                    val newZoom = (state.zoom * zoomMultiplier).coerceIn(1f, config.maxZoom)
                    state.zoom = newZoom
                    
                    // Simple panning
                    state.scrollOffset += pan
                }
            }
    ) {
        if (viewSize.width == 0 || viewSize.height == 0) return@Canvas

        val estimatedPageHeight = size.width * 1.4f

        translate(left = state.scrollOffset.x, top = state.scrollOffset.y) {
            scale(scale = state.zoom, pivot = Offset.Zero) {
                for (page in state.visiblePages) {
                    val bitmap = pageBitmaps[page]
                    if (bitmap != null) {
                        val pageTop = page * estimatedPageHeight
                        translate(left = 0f, top = pageTop) {
                            drawImage(
                                image = bitmap,
                                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), estimatedPageHeight.toInt())
                            )
                        }
                    }
                }
            }
        }
    }
}
