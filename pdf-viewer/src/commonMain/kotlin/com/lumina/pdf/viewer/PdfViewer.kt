package com.lumina.pdf.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
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
fun SearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val r = size.minDimension / 4f
        val cx = size.width * 0.45f
        val cy = size.height * 0.45f
        drawCircle(
            color = color,
            radius = r,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        val startX = (cx + r * cos(PI/4)).toFloat()
        val startY = (cy + r * sin(PI/4)).toFloat()
        val endX = size.width * 0.8f
        val endY = size.height * 0.8f
        drawLine(color = color, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = stroke)
    }
}

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val len = size.minDimension * 0.25f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(color = color, start = Offset(cx - len, cy - len), end = Offset(cx + len, cy + len), strokeWidth = stroke)
        drawLine(color = color, start = Offset(cx - len, cy + len), end = Offset(cx + len, cy - len), strokeWidth = stroke)
    }
}

@Composable
fun ChevronUpIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.2f
        val path = Path().apply {
            moveTo(cx - len, cy + len / 2f)
            lineTo(cx, cy - len / 2f)
            lineTo(cx + len, cy + len / 2f)
        }
        drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
    }
}

@Composable
fun ChevronDownIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val len = size.minDimension * 0.2f
        val path = Path().apply {
            moveTo(cx - len, cy - len / 2f)
            lineTo(cx, cy + len / 2f)
            lineTo(cx + len, cy - len / 2f)
        }
        drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
    }
}

@Composable
fun MenuIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val startX = w * 0.15f
        val endX = w * 0.85f
        
        val strokeWidth = 2.dp.toPx()
        drawLine(color = color, start = Offset(startX, h * 0.25f), end = Offset(endX, h * 0.25f), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(startX, h * 0.50f), end = Offset(endX, h * 0.50f), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(startX, h * 0.75f), end = Offset(endX, h * 0.75f), strokeWidth = strokeWidth)
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
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentMatchIndex: Int,
    totalMatches: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    isOutlineActive: Boolean,
    onOutlineActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            kotlinx.coroutines.delay(100)
            searchFocusRequester.requestFocus()
        }
    }

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
            if (isSearchActive) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SearchIcon(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .focusRequester(searchFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                                    onSearchActiveChange(false)
                                    onSearchQueryChange("")
                                    true
                                } else false
                            }
                            .weight(1f)
                            .height(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    if (searchQuery.isNotEmpty()) {
                        val matchText = if (totalMatches > 0) {
                            "${currentMatchIndex + 1} / $totalMatches"
                        } else {
                            "0 / 0"
                        }
                        Text(
                            text = matchText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onPrevMatch,
                        enabled = totalMatches > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        ChevronUpIcon(
                            color = if (totalMatches > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextMatch,
                        enabled = totalMatches > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        ChevronDownIcon(
                            color = if (totalMatches > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            onSearchActiveChange(false)
                            onSearchQueryChange("")
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        CloseIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                // Outline toggle button (Hamburger menu at the far left)
                IconButton(
                    onClick = { onOutlineActiveChange(!isOutlineActive) },
                    modifier = Modifier.size(32.dp)
                ) {
                    MenuIcon(
                        color = if (isOutlineActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "LuminaPDF",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onSearchActiveChange(true) },
                    modifier = Modifier.size(32.dp)
                ) {
                    SearchIcon(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
                
                Spacer(modifier = Modifier.width(4.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

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

    val systemDarkTheme = isSystemInDarkTheme()

    // Internal night mode state synchronized with config and top bar
    var localNightMode by remember(config.nightMode, systemDarkTheme) {
        mutableStateOf(config.nightMode ?: systemDarkTheme)
    }
    
    // Sync localNightMode when config or system theme changes
    LaunchedEffect(config.nightMode, systemDarkTheme) {
        localNightMode = config.nightMode ?: systemDarkTheme
    }

    var isInitialZoomSet by remember { mutableStateOf(false) }

    val topBarHeight = 40.dp
    val topPaddingPx = with(density) { topBarHeight.toPx() + 24.dp.toPx() }
    val bottomPaddingPx = with(density) { 24.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }
    val spacingPx = with(density) { maxOf(config.pageSpacing, 24.dp).toPx() }

    // Outline states
    var isOutlineOpen by remember { mutableStateOf(false) }
    var outlineItems by remember { mutableStateOf<List<OutlineItem>>(emptyList()) }

    // Fetch outline on document load
    LaunchedEffect(document) {
        try {
            outlineItems = document.getOutline()
        } catch (e: Exception) {
            outlineItems = emptyList()
        }
    }

    // Scroll directly to a selected page
    fun jumpToPage(pageIndex: Int) {
        if (viewSize.width == 0 || viewSize.height == 0) return
        val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
        val pageTop = pageIndex * (estimatedPageHeight + spacingPx)
        
        val targetScrollY = topPaddingPx - pageTop * state.zoom
        val targetScrollX = if (viewSize.width * state.zoom <= viewSize.width) {
            (viewSize.width - viewSize.width * state.zoom) / 2f
        } else {
            state.scrollOffset.x
        }
        
        state.scrollOffset = clampScrollOffset(
            Offset(targetScrollX, targetScrollY),
            state.zoom,
            viewSize.width.toFloat(),
            viewSize.height.toFloat(),
            estimatedPageHeight,
            pageCount,
            spacingPx,
            topPaddingPx,
            bottomPaddingPx,
            marginPx
        )
    }

    // Search states
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }
    val searchMatches = remember { mutableStateMapOf<Int, List<PdfRect>>() }
    var currentMatchIndex by remember { mutableStateOf(-1) }

    // Debounce search query updates to make search smooth and non-blocking
    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) {
            debouncedSearchQuery = ""
        } else {
            kotlinx.coroutines.delay(300)
            debouncedSearchQuery = searchQuery
        }
    }

    // Flattened matches for navigation, re-evaluating whenever searchMatches updates
    val allMatchesList = remember {
        derivedStateOf {
            val list = mutableListOf<Pair<Int, PdfRect>>()
            for (page in 0 until pageCount) {
                val pageMatches = searchMatches[page]
                if (pageMatches != null) {
                    for (match in pageMatches) {
                        list.add(Pair(page, match))
                    }
                }
            }
            list
        }
    }.value

    // Centering navigation logic
    fun centerMatch(pageIndex: Int, rect: PdfRect) {
        if (viewSize.width == 0 || viewSize.height == 0) return
        val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
        val pageTop = pageIndex * (estimatedPageHeight + spacingPx)
        
        val matchTopScaled = (pageTop + rect.top * estimatedPageHeight) * state.zoom
        val matchHeightScaled = (rect.bottom - rect.top) * estimatedPageHeight * state.zoom
        val matchCenterScaled = matchTopScaled + matchHeightScaled / 2f
        
        val targetScrollY = (viewSize.height / 2f) - matchCenterScaled
        
        val pageWidthScaled = viewSize.width.toFloat() * state.zoom
        val matchLeftScaled = rect.left * pageWidthScaled
        val matchWidthScaled = (rect.right - rect.left) * pageWidthScaled
        val matchCenterHorizontalScaled = matchLeftScaled + matchWidthScaled / 2f
        val targetScrollX = (viewSize.width / 2f) - matchCenterHorizontalScaled
        
        state.scrollOffset = clampScrollOffset(
            Offset(targetScrollX, targetScrollY),
            state.zoom,
            viewSize.width.toFloat(),
            viewSize.height.toFloat(),
            estimatedPageHeight,
            pageCount,
            spacingPx,
            topPaddingPx,
            bottomPaddingPx,
            marginPx
        )
    }

    // Scroll to active match when currentMatchIndex changes
    LaunchedEffect(currentMatchIndex) {
        if (currentMatchIndex in allMatchesList.indices) {
            val (page, rect) = allMatchesList[currentMatchIndex]
            centerMatch(page, rect)
        }
    }

    // Clamp currentMatchIndex when matches list changes
    LaunchedEffect(allMatchesList) {
        if (allMatchesList.isEmpty()) {
            currentMatchIndex = -1
        } else if (currentMatchIndex >= allMatchesList.size || currentMatchIndex < 0) {
            currentMatchIndex = 0
        }
    }

    // Perform text search page by page
    LaunchedEffect(debouncedSearchQuery, pageCount, document) {
        if (debouncedSearchQuery.isEmpty()) {
            searchMatches.clear()
            currentMatchIndex = -1
            return@LaunchedEffect
        }
        
        searchMatches.clear()
        for (page in 0 until pageCount) {
            kotlinx.coroutines.yield()
            try {
                val results = document.searchText(page, debouncedSearchQuery)
                if (results.isNotEmpty()) {
                    searchMatches[page] = results
                }
            } catch (e: Exception) {
                // Ignore search errors on single pages
            }
        }
        
        if (allMatchesList.isNotEmpty() && currentMatchIndex == -1) {
            currentMatchIndex = 0
        }
    }

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
                        Key.Escape -> {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
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

                        // Draw highlights
                        val pageMatches = searchMatches[page]
                        if (pageMatches != null) {
                            for (rect in pageMatches) {
                                val isActive = if (currentMatchIndex in allMatchesList.indices) {
                                    val activeMatch = allMatchesList[currentMatchIndex]
                                    activeMatch.first == page && activeMatch.second == rect
                                } else false
                                
                                val highlightColor = if (isActive) {
                                    Color(0xFFFF9800).copy(alpha = 0.5f) // Orange for active
                                } else {
                                    Color(0xFFFFEB3B).copy(alpha = 0.4f) // Yellow for inactive
                                }
                                
                                val paddingX = 3.dp.toPx()
                                val paddingY = 2.dp.toPx()
                                val x = rect.left * size.width - paddingX
                                val y = pageTop + rect.top * estimatedPageHeight - paddingY
                                val w = (rect.right - rect.left) * size.width + 2 * paddingX
                                val h = (rect.bottom - rect.top) * estimatedPageHeight + 2 * paddingY
                                
                                drawRoundRect(
                                    color = highlightColor,
                                    topLeft = Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(w, h),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
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
            isSearchActive = isSearchActive,
            onSearchActiveChange = { isSearchActive = it },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            currentMatchIndex = currentMatchIndex,
            totalMatches = allMatchesList.size,
            onNextMatch = {
                if (allMatchesList.isNotEmpty()) {
                    currentMatchIndex = (currentMatchIndex + 1) % allMatchesList.size
                }
            },
            onPrevMatch = {
                if (allMatchesList.isNotEmpty()) {
                    currentMatchIndex = (currentMatchIndex - 1 + allMatchesList.size) % allMatchesList.size
                }
            },
            isOutlineActive = isOutlineOpen,
            onOutlineActiveChange = { isOutlineOpen = it },
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

        // 4. Slide-out Table of Contents Panel
        AnimatedVisibility(
            visible = isOutlineOpen,
            enter = androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { -it }
            ),
            exit = androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { -it }
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topBarHeight)
                .width(240.dp)
                .fillMaxHeight()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Table of Contents",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { isOutlineOpen = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            CloseIcon(
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    if (outlineItems.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Outline Available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        val flatList = remember(outlineItems) {
                            val list = mutableListOf<Pair<OutlineItem, Int>>()
                            fun flatten(items: List<OutlineItem>, depth: Int) {
                                for (item in items) {
                                    list.add(Pair(item, depth))
                                    flatten(item.children, depth + 1)
                                }
                            }
                            flatten(outlineItems, 0)
                            list
                        }

                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(flatList.size) { index ->
                                val (item, depth) = flatList[index]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .then(
                                            if (item.pageNumber >= 0) {
                                                Modifier.clickable {
                                                    jumpToPage(item.pageNumber)
                                                    isOutlineOpen = false
                                                }
                                            } else Modifier
                                        )
                                        .padding(
                                            start = (depth * 16).dp + 8.dp,
                                            end = 8.dp,
                                            top = 8.dp,
                                            bottom = 8.dp
                                        )
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (depth == 0) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (item.pageNumber >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    if (item.pageNumber >= 0) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${item.pageNumber + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
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
}

