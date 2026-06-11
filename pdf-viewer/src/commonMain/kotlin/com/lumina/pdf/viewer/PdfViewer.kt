package com.lumina.pdf.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.lumina.pdf.core.*
import kotlinx.coroutines.launch

// Represents a unique tile key for caching
data class TileKey(val page: Int, val row: Int, val col: Int, val zoomLevel: Int)

@Composable
fun PdfViewer(
    document: PdfDocument,
    state: PdfViewerState = remember { PdfViewerState() },
    config: PdfViewerConfig = PdfViewerConfig(),
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var pageCount by remember { mutableStateOf(0) }
    
    // Memory cache and render queue
    val cache = remember(document) { LruMemoryCache(maxSize = 100) } // Max 100 bitmaps (thumbnails/tiles)
    val renderQueue = remember(document) { RenderQueue(document, coroutineScope) }

    // State maps for current UI
    val thumbnails = remember { mutableStateMapOf<Int, ImageBitmap>() }
    val activeTiles = remember { mutableStateMapOf<TileKey, ImageBitmap>() }

    LaunchedEffect(document) {
        pageCount = document.pageCount()
    }

    LaunchedEffect(document, viewSize, state.zoom, state.scrollOffset) {
        if (viewSize.width == 0 || viewSize.height == 0 || pageCount == 0) return@LaunchedEffect
        
        val estimatedPageHeight = viewSize.width * 1.4f
        val totalScrollY = -state.scrollOffset.y
        
        val firstVisiblePage = (totalScrollY / (estimatedPageHeight * state.zoom)).toInt().coerceIn(0, pageCount - 1)
        val lastVisiblePage = ((totalScrollY + viewSize.height) / (estimatedPageHeight * state.zoom)).toInt().coerceIn(0, pageCount - 1)

        val pagesToRender = (firstVisiblePage..lastVisiblePage).toList()
        state.visiblePages = pagesToRender
        state.currentPage = firstVisiblePage

        // 1. Request Thumbnails for visible pages (if not already cached)
        for (page in pagesToRender) {
            val thumbKey = "thumb_$page"
            if (!thumbnails.containsKey(page)) {
                val cached = cache.get(thumbKey)
                if (cached != null) {
                    thumbnails[page] = cached.asImageBitmap()
                } else {
                    renderQueue.submit(RenderRequest.Thumbnail(page, viewSize.width / 2, (estimatedPageHeight / 2).toInt()) { bitmap ->
                        bitmap?.let {
                            cache.put(thumbKey, it)
                            thumbnails[page] = it.asImageBitmap()
                        }
                    })
                }
            }
        }

        // 2. Tile Calculation for zooming (if zoom > 1.5, we use tiles)
        if (state.zoom > 1.5f) {
            val tileSize = 512
            val zoomLevelInt = state.zoom.toInt()
            
            for (page in pagesToRender) {
                // Calculate local page coordinates of the viewport
                val pageTopY = page * estimatedPageHeight * state.zoom
                val localScrollY = (totalScrollY - pageTopY).coerceAtLeast(0f)
                
                val visibleWidth = viewSize.width / state.zoom
                val visibleHeight = viewSize.height / state.zoom
                
                val startCol = (0 / tileSize).toInt() // simplified: we render full width in columns for MVP
                val endCol = ((viewSize.width) / tileSize).toInt() + 1
                
                val startRow = (localScrollY / tileSize).toInt()
                val endRow = ((localScrollY + viewSize.height) / tileSize).toInt() + 1

                val requestedKeys = mutableSetOf<TileKey>()

                for (row in startRow..endRow) {
                    for (col in startCol..endCol) {
                        val key = TileKey(page, row, col, zoomLevelInt)
                        requestedKeys.add(key)
                        
                        val cacheKey = "tile_${page}_${row}_${col}_$zoomLevelInt"
                        if (!activeTiles.containsKey(key)) {
                            val cachedTile = cache.get(cacheKey)
                            if (cachedTile != null) {
                                activeTiles[key] = cachedTile.asImageBitmap()
                            } else {
                                // Request Tile
                                val tileRect = PdfRect(
                                    left = (col * tileSize).toFloat() / state.zoom,
                                    top = (row * tileSize).toFloat() / state.zoom,
                                    right = ((col + 1) * tileSize).toFloat() / state.zoom,
                                    bottom = ((row + 1) * tileSize).toFloat() / state.zoom
                                )
                                renderQueue.submit(RenderRequest.Tile(page, tileRect, state.zoom) { bitmap ->
                                    bitmap?.let {
                                        cache.put(cacheKey, it)
                                        activeTiles[key] = it.asImageBitmap()
                                    }
                                })
                            }
                        }
                    }
                }
                
                // Cleanup old tiles for this page
                val keysToRemove = activeTiles.keys.filter { it.page == page && it !in requestedKeys }
                for (k in keysToRemove) activeTiles.remove(k)
            }
        } else {
            // Cleanup all tiles if zoomed out
            activeTiles.clear()
        }

        // Cleanup invisible thumbnails
        val keysToRemove = thumbnails.keys.filter { it !in (firstVisiblePage - 1..lastVisiblePage + 1) }
        for (k in keysToRemove) thumbnails.remove(k)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomMultiplier, _ ->
                    val newZoom = (state.zoom * zoomMultiplier).coerceIn(1f, config.maxZoom)
                    state.zoom = newZoom
                    state.scrollOffset += pan
                }
            }
    ) {
        if (viewSize.width == 0 || viewSize.height == 0) return@Canvas

        val estimatedPageHeight = size.width * 1.4f
        val tileSize = 512

        translate(left = state.scrollOffset.x, top = state.scrollOffset.y) {
            scale(scale = state.zoom, pivot = Offset.Zero) {
                for (page in state.visiblePages) {
                    val pageTop = page * estimatedPageHeight
                    
                    // 1. Draw Thumbnail as base layer
                    val thumb = thumbnails[page]
                    if (thumb != null) {
                        translate(left = 0f, top = pageTop) {
                            drawImage(
                                image = thumb,
                                dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), estimatedPageHeight.toInt())
                            )
                        }
                    }

                    // 2. Draw high-res tiles on top if available
                    if (state.zoom > 1.5f) {
                        translate(left = 0f, top = pageTop) {
                            // Reverse scaling since tiles are already scaled to target resolution
                            scale(scale = 1f / state.zoom, pivot = Offset.Zero) {
                                activeTiles.filter { it.key.page == page }.forEach { (key, tileBitmap) ->
                                    translate(left = (key.col * tileSize).toFloat(), top = (key.row * tileSize).toFloat()) {
                                        drawImage(image = tileBitmap)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
