package com.lumina.pdf.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lumina.pdf.core.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.PI

// Represents a unique tile key for caching
data class TileKey(val page: Int, val row: Int, val col: Int, val zoomLevel: Int)

@Composable
fun AddIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.3f
        val stroke = 2.dp.toPx()
        drawLine(color = color, start = Offset(cx - len, cy), end = Offset(cx + len, cy), strokeWidth = stroke)
        drawLine(color = color, start = Offset(cx, cy - len), end = Offset(cx, cy + len), strokeWidth = stroke)
    }
}

@Composable
fun RemoveIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.3f
        val stroke = 2.dp.toPx()
        drawLine(color = color, start = Offset(cx - len, cy), end = Offset(cx + len, cy), strokeWidth = stroke)
    }
}

@Composable
fun SunIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val r = size.minDimension / 4f
        drawCircle(color = color, radius = r)
        val center = center
        val rayLength = size.minDimension / 6f
        for (i in 0 until 8) {
            val angle = i * (2.0 * PI / 8.0)
            val startX = (center.x + r * cos(angle)).toFloat()
            val startY = (center.y + r * sin(angle)).toFloat()
            val endX = (center.x + (r + rayLength) * cos(angle)).toFloat()
            val endY = (center.y + (r + rayLength) * sin(angle)).toFloat()
            drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
fun MoonIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val radius = size.minDimension / 3f
        val path = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center = center, radius = radius))
        }
        val subPath = Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(center = center - Offset(6f, 3f), radius = radius))
        }
        val moonPath = Path.combine(
            PathOperation.Difference,
            path,
            subPath
        )
        drawPath(path = moonPath, color = color)
    }
}

@Composable
fun TopBar(
    zoomPercent: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitPage: () -> Unit,
    nightMode: Boolean,
    onToggleNightMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "LuminaPDF",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Zoom controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(32.dp)
                ) {
                    RemoveIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.width(48.dp)
                ) {
                    Text(
                        text = "$zoomPercent%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(32.dp)
                ) {
                    AddIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }

                TextButton(
                    onClick = onFitPage,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Fit Page",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Night Mode Toggle
            IconButton(
                onClick = onToggleNightMode,
                modifier = Modifier.size(32.dp)
            ) {
                if (nightMode) {
                    SunIcon(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                } else {
                    MoonIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// Bounding/clamping helper for scrolling
fun clampScrollOffset(
    scrollOffset: Offset,
    zoom: Float,
    viewWidth: Float,
    viewHeight: Float,
    estimatedPageHeight: Float,
    pageCount: Int,
    spacingPx: Float,
    topPaddingPx: Float,
    bottomPaddingPx: Float,
    marginPx: Float
): Offset {
    val totalHeight = pageCount * estimatedPageHeight + (pageCount - 1) * spacingPx
    val totalHeightScaled = totalHeight * zoom
    
    val pageWidthScaled = viewWidth * zoom
    
    // X clamping
    val x = if (pageWidthScaled <= viewWidth) {
        (viewWidth - pageWidthScaled) / 2f
    } else {
        scrollOffset.x.coerceIn(-(pageWidthScaled - viewWidth), 0f)
    }
    
    // Y clamping
    val y = if (totalHeightScaled + topPaddingPx + bottomPaddingPx <= viewHeight) {
        topPaddingPx + (viewHeight - topPaddingPx - bottomPaddingPx - totalHeightScaled) / 2f
    } else {
        val minScrollY = viewHeight - totalHeightScaled - bottomPaddingPx
        val maxScrollY = topPaddingPx
        scrollOffset.y.coerceIn(minScrollY, maxScrollY)
    }
    
    return Offset(x, y)
}

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

    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    // Internal night mode state synchronized with config and top bar
    var localNightMode by remember { mutableStateOf(config.nightMode) }
    
    // Sync localNightMode when config changes
    LaunchedEffect(config.nightMode) {
        localNightMode = config.nightMode
    }

    var isInitialZoomSet by remember { mutableStateOf(false) }

    val topBarHeight = 40.dp
    val topPaddingPx = with(density) { topBarHeight.toPx() + 24.dp.toPx() }
    val bottomPaddingPx = with(density) { 24.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }
    val spacingPx = with(density) { maxOf(config.pageSpacing, 24.dp).toPx() }

    LaunchedEffect(document) {
        pageCount = document.pageCount()
        isInitialZoomSet = false
    }

    // Scrollbar/tooltip visibility
    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(state.scrollOffset) {
        isScrolling = true
        kotlinx.coroutines.delay(1500)
        isScrolling = false
    }

    // Calculate fit-page zoom helper
    fun getFitZoom(): Float {
        if (viewSize.width == 0 || viewSize.height == 0) return 1f
        val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
        val fitWidthZoom = (viewSize.width - 2 * marginPx) / viewSize.width
        val fitHeightZoom = (viewSize.height - topPaddingPx - bottomPaddingPx - 2 * marginPx) / estimatedPageHeight
        return min(fitWidthZoom, fitHeightZoom).coerceIn(config.minZoom, config.maxZoom)
    }

    // Set initial zoom once sizes are known
    LaunchedEffect(viewSize, pageCount) {
        if (viewSize.width > 0 && viewSize.height > 0 && pageCount > 0 && !isInitialZoomSet) {
            val fitZoom = getFitZoom()
            state.zoom = fitZoom
            state.scrollOffset = clampScrollOffset(
                Offset.Zero, fitZoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
                viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
            )
            isInitialZoomSet = true
        }
    }

    // Centroid-based zoom helper
    fun changeZoomWithFocus(newZoom: Float, focus: Offset) {
        if (viewSize.width == 0 || viewSize.height == 0) return
        val oldZoom = state.zoom
        val clampedZoom = newZoom.coerceIn(config.minZoom, config.maxZoom)
        
        val documentPoint = (focus - state.scrollOffset) / oldZoom
        val newScrollOffset = focus - documentPoint * clampedZoom
        
        state.zoom = clampedZoom
        state.scrollOffset = clampScrollOffset(
            newScrollOffset, clampedZoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
            viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
        )
    }

    // Recalculate and load visible pages/tiles
    LaunchedEffect(document, viewSize, state.zoom, state.scrollOffset) {
        if (viewSize.width == 0 || viewSize.height == 0 || pageCount == 0) return@LaunchedEffect
        
        val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
        val visibleList = mutableListOf<Int>()
        var firstVisible = -1
        for (page in 0 until pageCount) {
            val yStart = state.scrollOffset.y + page * (estimatedPageHeight + spacingPx) * state.zoom
            val yEnd = yStart + estimatedPageHeight * state.zoom
            if (yEnd >= 0f && yStart <= viewSize.height.toFloat()) {
                visibleList.add(page)
                if (firstVisible == -1) {
                    firstVisible = page
                }
            }
        }
        
        state.visiblePages = visibleList
        if (firstVisible != -1) {
            state.currentPage = firstVisible
        }

        // 1. Request Thumbnails
        for (page in state.visiblePages) {
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

        // 2. Request Tiles
        if (state.zoom > 1.5f) {
            val tileSize = 512
            val zoomLevelInt = state.zoom.toInt()
            
            for (page in state.visiblePages) {
                val localScrollY = (-state.scrollOffset.y - (page * (estimatedPageHeight + spacingPx) * state.zoom)).coerceAtLeast(0f)
                val startCol = 0
                val endCol = viewSize.width / tileSize + 1
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
                val keysToRemove = activeTiles.keys.filter { it.page == page && it !in requestedKeys }
                for (k in keysToRemove) activeTiles.remove(k)
            }
        } else {
            activeTiles.clear()
        }

        val firstVisiblePage = firstVisible
        val lastVisiblePage = visibleList.lastOrNull() ?: firstVisible
        val keysToRemove = thumbnails.keys.filter { it !in (firstVisiblePage - 1..lastVisiblePage + 1) }
        for (k in keysToRemove) thumbnails.remove(k)
    }

    val invertMatrix = floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
    val colorFilter = remember(localNightMode) {
        if (localNightMode) ColorFilter.colorMatrix(ColorMatrix(invertMatrix)) else null
    }

    val backgroundColor = if (localNightMode) {
        Color(0xFF2E2724)
    } else {
        Color(0xFFF1EAE0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val isCtrl = keyEvent.isCtrlPressed
                    when (keyEvent.key) {
                        Key.Equals -> {
                            if (isCtrl) {
                                changeZoomWithFocus(state.zoom + 0.15f, Offset(viewSize.width / 2f, viewSize.height / 2f))
                                true
                            } else false
                        }
                        Key.Minus -> {
                            if (isCtrl) {
                                changeZoomWithFocus(state.zoom - 0.15f, Offset(viewSize.width / 2f, viewSize.height / 2f))
                                true
                            } else false
                        }
                        else -> false
                    }
                } else false
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    focusRequester.requestFocus()
                }
            }
    ) {
        // 1. PDF Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            focusRequester.requestFocus()
                            val fitZoom = getFitZoom()
                            val targetZoom = if (state.zoom > fitZoom + 0.1f) fitZoom else 2.5f
                            changeZoomWithFocus(targetZoom, offset)
                        },
                        onTap = {
                            focusRequester.requestFocus()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoomMultiplier, _ ->
                        focusRequester.requestFocus()
                        val oldZoom = state.zoom
                        val newZoom = (state.zoom * zoomMultiplier).coerceIn(config.minZoom, config.maxZoom)
                        
                        val documentPoint = (centroid - state.scrollOffset) / oldZoom
                        val newScrollOffset = centroid - documentPoint * newZoom + pan
                        
                        state.zoom = newZoom
                        state.scrollOffset = clampScrollOffset(
                            newScrollOffset, newZoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
                            viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
                        )
                    }
                }
                .pointerInput(viewSize, pageCount, state.zoom) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.firstOrNull()
                                if (change != null) {
                                    val delta = change.scrollDelta
                                    val isCtrl = event.keyboardModifiers.isCtrlPressed
                                    if (isCtrl) {
                                        val zoomChange = if (delta.y > 0) -0.1f else 0.1f
                                        changeZoomWithFocus(state.zoom + zoomChange, change.position)
                                    } else {
                                        val scrollSpeed = 40f
                                        val pan = Offset(-delta.x * scrollSpeed, -delta.y * scrollSpeed)
                                        state.scrollOffset = clampScrollOffset(
                                            state.scrollOffset + pan, state.zoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
                                            viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
                                        )
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            if (viewSize.width == 0 || viewSize.height == 0) return@Canvas

            val estimatedPageHeight = size.width * 1.4f
            val tileSize = 512

            translate(left = state.scrollOffset.x, top = state.scrollOffset.y) {
                scale(scale = state.zoom, pivot = Offset.Zero) {
                    for (page in state.visiblePages) {
                        val pageTop = page * (estimatedPageHeight + spacingPx)
                        
                        // Draw Thumbnail
                        val thumb = thumbnails[page]
                        if (thumb != null) {
                            translate(left = 0f, top = pageTop) {
                                drawImage(
                                    image = thumb,
                                    dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), estimatedPageHeight.toInt()),
                                    colorFilter = colorFilter
                                )
                            }
                        }

                        // Draw high-res tiles
                        if (state.zoom > 1.5f) {
                            translate(left = 0f, top = pageTop) {
                                scale(scale = 1f / state.zoom, pivot = Offset.Zero) {
                                    activeTiles.filter { it.key.page == page }.forEach { (key, tileBitmap) ->
                                        translate(left = (key.col * tileSize).toFloat(), top = (key.row * tileSize).toFloat()) {
                                            drawImage(
                                                image = tileBitmap,
                                                colorFilter = colorFilter
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Top Bar Overlay
        TopBar(
            zoomPercent = (state.zoom * 100).toInt(),
            onZoomIn = {
                changeZoomWithFocus(state.zoom + 0.2f, Offset(viewSize.width / 2f, viewSize.height / 2f))
            },
            onZoomOut = {
                changeZoomWithFocus(state.zoom - 0.2f, Offset(viewSize.width / 2f, viewSize.height / 2f))
            },
            onFitPage = {
                val fitZoom = getFitZoom()
                state.zoom = fitZoom
                state.scrollOffset = clampScrollOffset(
                    Offset.Zero, fitZoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
                    viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
                )
            },
            nightMode = localNightMode,
            onToggleNightMode = { localNightMode = !localNightMode },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. Custom Interactive Scrollbar & Tooltip
        if (viewSize.height > 0 && pageCount > 0) {
            val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
            val totalHeight = pageCount * estimatedPageHeight + (pageCount - 1) * spacingPx
            val totalHeightScaled = totalHeight * state.zoom
            
            if (totalHeightScaled > viewSize.height.toFloat()) {
                val trackHeight = viewSize.height.toFloat() - topPaddingPx - bottomPaddingPx
                val thumbHeight = (trackHeight * (viewSize.height.toFloat() / totalHeightScaled)).coerceIn(with(density) { 48.dp.toPx() }, trackHeight)
                val maxTravel = trackHeight - thumbHeight
                
                val scrollRange = totalHeightScaled + topPaddingPx + bottomPaddingPx - viewSize.height.toFloat()
                val currentRatio = if (scrollRange > 0) {
                    ((topPaddingPx - state.scrollOffset.y) / scrollRange).coerceIn(0f, 1f)
                } else 0f
                val thumbY = currentRatio * maxTravel

                // Render scrollbar track and thumb
                var isDraggingScrollbar by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topBarHeight, bottom = 16.dp, end = 4.dp)
                        .width(20.dp)
                        .fillMaxHeight()
                        .pointerInput(maxTravel, scrollRange) {
                            detectDragGestures(
                                onDragStart = { isDraggingScrollbar = true },
                                onDragEnd = { isDraggingScrollbar = false },
                                onDragCancel = { isDraggingScrollbar = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newRatio = ((currentRatio * maxTravel + dragAmount.y) / maxTravel).coerceIn(0f, 1f)
                                    val newScrollOffsetY = topPaddingPx - newRatio * scrollRange
                                    state.scrollOffset = state.scrollOffset.copy(y = newScrollOffsetY)
                                }
                            )
                        }
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    
                    // Thumb
                    Box(
                        modifier = Modifier
                            .offset(y = with(density) { thumbY.toDp() })
                            .align(Alignment.TopCenter)
                            .width(8.dp)
                            .height(with(density) { thumbHeight.toDp() })
                            .background(
                                color = if (isDraggingScrollbar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                // Tooltip next to scrollbar
                AnimatedVisibility(
                    visible = isScrolling || isDraggingScrollbar,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topBarHeight, end = 28.dp)
                        .offset(y = with(density) { (thumbY + (thumbHeight / 2) - with(density) { 16.dp.toPx() }).toDp() }) // Align center of tooltip with center of thumb
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = "Page ${state.currentPage + 1} of $pageCount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

