package com.lumina.pdf.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lumina.pdf.core.*
import com.lumina.pdf.viewer.db.*
import com.lumina.pdf.viewer.theme.LuminaColors
import com.lumina.pdf.viewer.theme.LuminaRadius
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.PI

// Represents a unique tile key for caching
data class TileKey(val page: Int, val row: Int, val col: Int, val zoomLevel: Int)



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
    documentId: String? = null,
    documentTitle: String? = null,
    initialPage: Int? = null,
    state: PdfViewerState = remember { PdfViewerState() },
    config: PdfViewerConfig = PdfViewerConfig(),
    onClose: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val db = rememberPdfDatabase()
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var pageCount by remember { mutableStateOf(0) }

    // Smooths discrete mouse-wheel/trackpad scroll ticks into a short glide instead of teleporting
    // scrollOffset instantly per event. Only used by the wheel-scroll handler below; every other
    // input path (drag, pinch-zoom, scrollbar) already tracks the pointer continuously and sets
    // state.scrollOffset directly, which is already smooth.
    val scrollAnimatable = remember(document) { Animatable(state.scrollOffset, Offset.VectorConverter) }
    // The real cumulative destination of the current wheel gesture. Deliberately separate from
    // scrollAnimatable.value (the animation's current mid-flight position): retargeting from the
    // live interpolated value on every tick of a fast burst would throw away whatever distance of
    // each prior tick hadn't been visually traveled yet before the next tick interrupted it.
    // Tracking the target independently means every tick's full pan always gets accumulated.
    var scrollTarget by remember(document) { mutableStateOf(state.scrollOffset) }

    // Drives animated (as opposed to continuous-gesture) zoom changes: double-tap, the +/- and
    // fit-page toolbar buttons. Pinch-zoom and ctrl+scroll stay instant since those already track
    // the pointer continuously frame-by-frame and animating on top would fight live input.
    val zoomAnimatable = remember(document) { Animatable(state.zoom) }

    // State maps for current UI
    val thumbnails = remember { mutableStateMapOf<Int, ImageBitmap>() }
    val activeTiles = remember { mutableStateMapOf<TileKey, ImageBitmap>() }
    // Pages whose thumbnail render failed (RenderQueue delivered a null bitmap) — tap to retry.
    val renderFailed = remember { mutableStateMapOf<Int, Boolean>() }

    // Memory cache and render queue
    val cache = remember(document) {
        LruMemoryCache(
            // Never evict a bitmap that's still referenced by the on-screen state maps above -
            // otherwise cache pressure from newly loaded tiles/thumbnails can recycle a bitmap
            // that's still being drawn, crashing with "Canvas: trying to use a recycled bitmap".
            isPinned = { key ->
                thumbnails.keys.any { "thumb_$it" == key } ||
                    activeTiles.keys.any { "tile_${it.page}_${it.row}_${it.col}_${it.zoomLevel}" == key }
            }
        )
    } // 64MB byte budget, 300-entry cap (see LruMemoryCache defaults)
    val renderQueue = remember(document) { RenderQueue(document, coroutineScope) }

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
    var lastViewSize by remember { mutableStateOf(IntSize.Zero) }

    // The bar itself adds statusBarsPadding() on top of its 48dp content height,
    // so the content offset needs to reserve that same inset to avoid overlap.
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarHeight = 48.dp + statusBarInset
    val topPaddingPx = with(density) { topBarHeight.toPx() + 24.dp.toPx() }
    val bottomPaddingPx = with(density) { 24.dp.toPx() }
    val marginPx = with(density) { 16.dp.toPx() }
    val spacingPx = with(density) { maxOf(config.pageSpacing, 24.dp).toPx() }

    // Outline states
    var isOutlineOpen by remember { mutableStateOf(false) }
    var outlineItems by remember { mutableStateOf<List<OutlineItem>>(emptyList()) }

    // Notes states
    var isNotesOpen by remember { mutableStateOf(false) }
    val notes = remember { mutableStateListOf<DbNote>() }

    // Bookmark states
    val bookmarks = remember { mutableStateListOf<DbBookmark>() }

    // Fetch bookmarks on document load
    LaunchedEffect(document, documentId) {
        if (documentId != null) {
            bookmarks.clear()
            try {
                bookmarks.addAll(db.getBookmarks(documentId))
            } catch (e: Exception) {
                Logger.e(e) { "Failed to load bookmarks for document $documentId" }
            }
        }
    }

    fun toggleBookmark(page: Int) {
        if (documentId == null) return
        val existing = bookmarks.find { it.page == page }
        if (existing != null) {
            db.deleteBookmark(existing.id)
            bookmarks.remove(existing)
        } else {
            val bookmark = DbBookmark(
                id = (0..10000000).random().toString(),
                documentId = documentId,
                page = page,
                timestamp = getCurrentTimeMillis()
            )
            db.insertBookmark(bookmark)
            bookmarks.add(bookmark)
        }
    }

    // Fetch notes on document load
    LaunchedEffect(document, documentId) {
        if (documentId != null) {
            notes.clear()
            try {
                notes.addAll(db.getNotes(documentId))
            } catch (e: Exception) {
                Logger.e(e) { "Failed to load notes for document $documentId" }
            }
        }
    }

    // Persisted highlights. There's no positioned-text API to build real drag-to-select
    // from yet, so v1 highlighting piggybacks on search: tapping a search match toggles a
    // saved highlight for that match's rect.
    val highlights = remember { mutableStateListOf<DbHighlight>() }
    val savedHighlightColor = Color(0xFFFFD54F)

    // Fetch highlights on document load
    LaunchedEffect(document, documentId) {
        if (documentId != null) {
            highlights.clear()
            try {
                highlights.addAll(db.getHighlights(documentId))
            } catch (e: Exception) {
                Logger.e(e) { "Failed to load highlights for document $documentId" }
            }
        }
    }

    fun parseHighlightRect(rectsText: String): PdfRect? {
        val parts = rectsText.split(",").mapNotNull { it.toFloatOrNull() }
        if (parts.size != 4) return null
        return PdfRect(left = parts[0], top = parts[1], right = parts[2], bottom = parts[3])
    }

    // Toggles a saved highlight for a tapped search-match rect: removes it if that exact
    // match is already highlighted, otherwise adds and persists a new one.
    fun toggleHighlightAt(page: Int, rect: PdfRect, matchedText: String) {
        if (documentId == null) return
        val tolerance = 0.005f
        val existing = highlights.find { saved ->
            if (saved.page != page) return@find false
            val savedRect = parseHighlightRect(saved.rectsText) ?: return@find false
            kotlin.math.abs(savedRect.left - rect.left) < tolerance &&
                kotlin.math.abs(savedRect.top - rect.top) < tolerance &&
                kotlin.math.abs(savedRect.right - rect.right) < tolerance &&
                kotlin.math.abs(savedRect.bottom - rect.bottom) < tolerance
        }
        if (existing != null) {
            db.deleteHighlight(existing.id)
            highlights.remove(existing)
        } else {
            val newHighlight = DbHighlight(
                id = "hl_${documentId}_${getCurrentTimeMillis()}",
                documentId = documentId,
                page = page,
                rectsText = "${rect.left},${rect.top},${rect.right},${rect.bottom}",
                color = "#FFEB3B",
                timestamp = getCurrentTimeMillis()
            )
            db.insertHighlight(newHighlight)
            highlights.add(newHighlight)
        }
    }

    // Fetch outline on document load
    LaunchedEffect(document) {
        try {
            outlineItems = document.getOutline()
        } catch (e: Exception) {
            Logger.e(e) { "Failed to load document outline" }
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

    // Scrollbar/tooltip visibility (idle-based: visible while moving, fades out after 1.5s still)
    var isScrolling by remember { mutableStateOf(false) }
    // Top bar visibility (direction-based, like most reader apps): hides while scrolling down
    // into the content, comes back as soon as you scroll back up - independent of isScrolling
    // above, which only tracks whether scrolling is happening at all, not which way.
    var isTopBarVisible by remember { mutableStateOf(true) }
    var lastScrollY by remember { mutableStateOf(state.scrollOffset.y) }
    LaunchedEffect(state.scrollOffset) {
        // scrollOffset.y decreases as you scroll down (content moves up) and increases scrolling
        // back up - see clampScrollOffset's minScrollY/maxScrollY. A small deadzone avoids
        // flickering on sub-pixel jitter from animation rounding.
        val deltaY = state.scrollOffset.y - lastScrollY
        if (kotlin.math.abs(deltaY) > 2f) {
            isTopBarVisible = deltaY > 0
        }
        lastScrollY = state.scrollOffset.y

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
            var restored = false
            if (initialPage != null) {
                val fitZoom = getFitZoom()
                state.zoom = fitZoom
                val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
                val pageTop = initialPage * (estimatedPageHeight + spacingPx)
                val targetScrollY = topPaddingPx - pageTop * fitZoom
                val targetScrollX = (viewSize.width - viewSize.width * fitZoom) / 2f
                state.scrollOffset = clampScrollOffset(
                    Offset(targetScrollX, targetScrollY),
                    fitZoom,
                    viewSize.width.toFloat(),
                    viewSize.height.toFloat(),
                    estimatedPageHeight,
                    pageCount,
                    spacingPx,
                    topPaddingPx,
                    bottomPaddingPx,
                    marginPx
                )
                state.currentPage = initialPage
                restored = true
            } else if (documentId != null) {
                val saved = db.getOffset(documentId)
                if (saved != null) {
                    state.currentPage = saved.page
                    state.zoom = saved.zoom
                    state.scrollOffset = clampScrollOffset(
                        Offset(saved.scrollX, saved.scrollY),
                        saved.zoom,
                        viewSize.width.toFloat(),
                        viewSize.height.toFloat(),
                        viewSize.width.toFloat() * 1.4f,
                        pageCount,
                        spacingPx,
                        topPaddingPx,
                        bottomPaddingPx,
                        marginPx
                    )
                    restored = true
                }
            }
            if (!restored) {
                val fitZoom = getFitZoom()
                state.zoom = fitZoom
                state.scrollOffset = clampScrollOffset(
                    Offset.Zero, fitZoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
                    viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
                )
            }
            isInitialZoomSet = true
        }
    }

    LaunchedEffect(viewSize) {
        if (viewSize.width > 0 && viewSize.height > 0 && pageCount > 0) {
            if (isInitialZoomSet && lastViewSize != IntSize.Zero && lastViewSize != viewSize) {
                // Window resized!
                val oldWidth = lastViewSize.width.toFloat()
                val oldHeight = lastViewSize.height.toFloat()
                val newWidth = viewSize.width.toFloat()
                val newHeight = viewSize.height.toFloat()

                val oldEstimatedPageHeight = oldWidth * 1.4f
                val oldTotalHeight = pageCount * oldEstimatedPageHeight + (pageCount - 1) * spacingPx

                // Calculate old fit zoom
                val oldFitWidthZoom = (oldWidth - 2 * marginPx) / oldWidth
                val oldFitHeightZoom = (oldHeight - topPaddingPx - bottomPaddingPx - 2 * marginPx) / oldEstimatedPageHeight
                val oldFitZoom = min(oldFitWidthZoom, oldFitHeightZoom).coerceIn(config.minZoom, config.maxZoom)

                // Calculate new fit zoom
                val newEstimatedPageHeight = newWidth * 1.4f
                val newFitWidthZoom = (newWidth - 2 * marginPx) / newWidth
                val newFitHeightZoom = (newHeight - topPaddingPx - bottomPaddingPx - 2 * marginPx) / newEstimatedPageHeight
                val newFitZoom = min(newFitWidthZoom, newFitHeightZoom).coerceIn(config.minZoom, config.maxZoom)

                // Determine new zoom
                val wasFitZoom = kotlin.math.abs(state.zoom - oldFitZoom) < 0.01f
                val newZoom = if (wasFitZoom) {
                    newFitZoom
                } else {
                    state.zoom
                }

                // Calculate relative center in old document coordinates
                val oldCenterX = oldWidth / 2f
                val oldCenterY = oldHeight / 2f
                val oldDocCenterX = (oldCenterX - state.scrollOffset.x) / state.zoom
                val oldDocCenterY = (oldCenterY - state.scrollOffset.y) / state.zoom

                val relX = oldDocCenterX / oldWidth
                val relY = oldDocCenterY / oldTotalHeight

                // Calculate new scroll offsets based on the same relative center
                val newCenterX = newWidth / 2f
                val newCenterY = newHeight / 2f
                val newTotalHeight = pageCount * newEstimatedPageHeight + (pageCount - 1) * spacingPx

                val targetScrollX = newCenterX - (relX * newWidth) * newZoom
                val targetScrollY = newCenterY - (relY * newTotalHeight) * newZoom

                state.zoom = newZoom
                state.scrollOffset = clampScrollOffset(
                    Offset(targetScrollX, targetScrollY),
                    newZoom,
                    newWidth,
                    newHeight,
                    newEstimatedPageHeight,
                    pageCount,
                    spacingPx,
                    topPaddingPx,
                    bottomPaddingPx,
                    marginPx
                )
            }
            lastViewSize = viewSize
        }
    }

    // Save state when page, zoom, or offset changes (debounced)
    LaunchedEffect(documentId, state.currentPage, state.zoom, state.scrollOffset) {
        if (documentId == null || !isInitialZoomSet) return@LaunchedEffect
        // Debounce saves to prevent high frequency SQLite writes
        kotlinx.coroutines.delay(1000)
        db.saveOffset(
            documentId,
            PdfOffset(state.currentPage, state.zoom, state.scrollOffset.x, state.scrollOffset.y)
        )
    }

    // Save state synchronously when viewer is closed or disposed
    DisposableEffect(documentId) {
        onDispose {
            if (documentId != null && isInitialZoomSet) {
                db.saveOffset(
                    documentId,
                    PdfOffset(state.currentPage, state.zoom, state.scrollOffset.x, state.scrollOffset.y)
                )
            }
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

    // Animated counterpart to changeZoomWithFocus, for discrete/button-triggered zoom actions
    // (double-tap, toolbar +/-). Keeps the same focus point fixed under the cursor throughout
    // the animation by recomputing scrollOffset from the same anchor at each animated frame.
    fun changeZoomWithFocusAnimated(newZoom: Float, focus: Offset) {
        if (viewSize.width == 0 || viewSize.height == 0) return
        val oldZoom = state.zoom
        val clampedZoom = newZoom.coerceIn(config.minZoom, config.maxZoom)
        val documentPoint = (focus - state.scrollOffset) / oldZoom

        coroutineScope.launch {
            zoomAnimatable.snapTo(oldZoom)
            zoomAnimatable.animateTo(clampedZoom, tween(durationMillis = 200)) {
                state.zoom = value
                state.scrollOffset = clampScrollOffset(
                    focus - documentPoint * value, value, viewSize.width.toFloat(), viewSize.height.toFloat(),
                    viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
                )
            }
        }
    }

    // Keyboard-driven scroll (arrow keys / Page Up-Down), same animate-to-target pattern as the
    // wheel-scroll handler below, so keyboard nav glides instead of teleporting.
    fun animateScrollByOffset(offsetDelta: Offset) {
        if (viewSize.width == 0 || viewSize.height == 0) return
        val base = if (scrollAnimatable.isRunning) scrollTarget else state.scrollOffset
        val target = clampScrollOffset(
            base + offsetDelta, state.zoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
            viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
        )
        scrollTarget = target
        coroutineScope.launch {
            if (!scrollAnimatable.isRunning) scrollAnimatable.snapTo(state.scrollOffset)
            scrollAnimatable.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
            ) {
                state.scrollOffset = value
            }
        }
    }

    // Recalculate visible pages immediately on every scroll/zoom change (cheap: no cache/queue
    // work) so the Canvas draw block, which reads state.visiblePages directly, never goes stale.
    LaunchedEffect(document, viewSize, state.zoom, state.scrollOffset) {
        if (viewSize.width == 0 || viewSize.height == 0 || pageCount == 0) return@LaunchedEffect

        val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
        val viewportHeight = viewSize.height.toFloat()
        val viewportCenter = viewportHeight / 2f
        val pageStride = (estimatedPageHeight + spacingPx) * state.zoom
        val scrollY = state.scrollOffset.y

        // Pages sit at a uniform stride, so the visible range can be solved for directly instead
        // of scanning every page in the document - this used to be an O(pageCount) loop re-run on
        // every scroll/zoom animation frame, which got expensive for large documents.
        val lowerBound = kotlin.math.ceil((-scrollY - estimatedPageHeight * state.zoom) / pageStride).toInt()
        val upperBound = kotlin.math.floor((viewportHeight - scrollY) / pageStride).toInt()
        val firstPage = maxOf(lowerBound, 0)
        val lastPage = minOf(upperBound, pageCount - 1)

        val visibleList = mutableListOf<Int>()
        var firstVisible = -1
        var bestPage = -1
        var minDistance = Float.MAX_VALUE
        if (firstPage <= lastPage) {
            for (page in firstPage..lastPage) {
                val yStart = scrollY + page * pageStride
                val yEnd = yStart + estimatedPageHeight * state.zoom
                visibleList.add(page)
                if (firstVisible == -1) {
                    firstVisible = page
                }

                val pageCenter = (yStart + yEnd) / 2f
                val distance = kotlin.math.abs(pageCenter - viewportCenter)
                if (distance < minDistance) {
                    minDistance = distance
                    bestPage = page
                }
            }
        }

        state.visiblePages = visibleList
        if (bestPage != -1) {
            state.currentPage = bestPage
        }

        if (firstVisible != -1) {
            val lastVisible = visibleList.lastOrNull() ?: firstVisible
            val keysToRemove = thumbnails.keys.filter { it !in (firstVisible - 3..lastVisible + 3) }
            for (k in keysToRemove) thumbnails.remove(k)
        }
    }

    // Request thumbnails/tiles for the current viewport, debounced so a fast scroll/zoom gesture
    // doesn't flood the render queue with requests for pages flown past in a few milliseconds.
    LaunchedEffect(document, viewSize, state.zoom, state.scrollOffset) {
        if (viewSize.width == 0 || viewSize.height == 0 || pageCount == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(config.tileRequestDebounceMs)

        val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
        val visiblePages = state.visiblePages
        val firstVisiblePage = visiblePages.firstOrNull() ?: -1
        val lastVisiblePage = visiblePages.lastOrNull() ?: firstVisiblePage
        val pagesToLoad = if (firstVisiblePage != -1) {
            val startPrefetchPage = (firstVisiblePage - 2).coerceAtLeast(0)
            val endPrefetchPage = (lastVisiblePage + 2).coerceAtMost(pageCount - 1)
            (startPrefetchPage..endPrefetchPage).toSet()
        } else {
            visiblePages.toSet()
        }

        // Call clearObsolete to discard pending requests that are no longer needed
        renderQueue.clearObsolete(activePages = pagesToLoad, visiblePages = visiblePages.toSet())

        // 1. Request Thumbnails (including prefetch pages)
        for (page in pagesToLoad) {
            val thumbKey = "thumb_$page"
            if (!thumbnails.containsKey(page)) {
                val cached = cache.get(thumbKey)
                if (cached != null) {
                    thumbnails[page] = cached.asImageBitmap()
                } else {
                    renderQueue.submit(
                        RenderRequest.Thumbnail(
                            pageIndex = page,
                            // Rendered at full viewport width - this bitmap is drawn at full page
                            // size (dstSize below matches viewport size) any time tiles aren't
                            // active (zoom <= 1.5, i.e. most normal reading), so a downscaled
                            // source here was visibly blurry text at everyday zoom levels.
                            width = viewSize.width,
                            height = estimatedPageHeight.toInt(),
                            isPrefetch = page !in visiblePages
                        ) { bitmap ->
                            if (bitmap != null) {
                                cache.put(thumbKey, bitmap)
                                thumbnails[page] = bitmap.asImageBitmap()
                                renderFailed.remove(page)
                            } else {
                                renderFailed[page] = true
                            }
                        }
                    )
                }
            }
        }

        // 2. Request Tiles
        if (state.zoom > 1.5f) {
            val tileSize = 512
            val zoomLevelInt = state.zoom.toInt()

            for (page in visiblePages) {
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
                                renderQueue.submit(RenderRequest.Tile(page, tileRect, state.zoom, viewSize.width, estimatedPageHeight.toInt()) { bitmap ->
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
    }

    // Retry a single page's thumbnail render after a failure (invoked by tapping the failed page).
    fun retryPageRender(page: Int) {
        renderFailed.remove(page)
        val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
        renderQueue.submit(
            RenderRequest.Thumbnail(
                pageIndex = page,
                width = viewSize.width,
                height = estimatedPageHeight.toInt(),
                isPrefetch = false
            ) { bitmap ->
                if (bitmap != null) {
                    cache.put("thumb_$page", bitmap)
                    thumbnails[page] = bitmap.asImageBitmap()
                    renderFailed.remove(page)
                } else {
                    renderFailed[page] = true
                }
            }
        )
    }

    // Pulsing alpha driving the loading placeholder drawn under pages that haven't rendered yet.
    val loadingPulseTransition = rememberInfiniteTransition(label = "pdfPageLoadingPulse")
    val loadingPulseAlpha by loadingPulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pdfPageLoadingPulseAlpha"
    )

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
        LuminaColors.surfaceBaseDark
    } else {
        LuminaColors.surfaceBaseLight
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
                                changeZoomWithFocusAnimated(state.zoom + 0.15f, Offset(viewSize.width / 2f, viewSize.height / 2f))
                                true
                            } else false
                        }
                        Key.Minus -> {
                            if (isCtrl) {
                                changeZoomWithFocusAnimated(state.zoom - 0.15f, Offset(viewSize.width / 2f, viewSize.height / 2f))
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
                        Key.DirectionDown -> {
                            animateScrollByOffset(Offset(0f, -80f))
                            true
                        }
                        Key.DirectionUp -> {
                            animateScrollByOffset(Offset(0f, 80f))
                            true
                        }
                        Key.PageDown -> {
                            animateScrollByOffset(Offset(0f, -viewSize.height.toFloat()))
                            true
                        }
                        Key.PageUp -> {
                            animateScrollByOffset(Offset(0f, viewSize.height.toFloat()))
                            true
                        }
                        Key.MoveHome -> {
                            jumpToPage(0)
                            true
                        }
                        Key.MoveEnd -> {
                            if (pageCount > 0) jumpToPage(pageCount - 1)
                            true
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
                            changeZoomWithFocusAnimated(targetZoom, offset)
                        },
                        onTap = { tapOffset ->
                            focusRequester.requestFocus()
                            val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
                            for (page in state.visiblePages) {
                                if (renderFailed[page] == true) {
                                    val pageTop = page * (estimatedPageHeight + spacingPx)
                                    val screenTop = state.scrollOffset.y + pageTop * state.zoom
                                    val screenBottom = screenTop + estimatedPageHeight * state.zoom
                                    if (tapOffset.y in screenTop..screenBottom) {
                                        retryPageRender(page)
                                        break
                                    }
                                }
                            }

                            // Tapping a search match toggles a saved highlight for it; tapping an
                            // existing highlight directly (no search needed) removes it.
                            val documentPoint = (tapOffset - state.scrollOffset) / state.zoom
                            for (page in state.visiblePages) {
                                val pageTop = page * (estimatedPageHeight + spacingPx)
                                if (documentPoint.y < pageTop || documentPoint.y > pageTop + estimatedPageHeight) continue
                                val localX = documentPoint.x / viewSize.width.toFloat()
                                val localY = (documentPoint.y - pageTop) / estimatedPageHeight
                                val hit = searchMatches[page]?.firstOrNull { r ->
                                    localX in r.left..r.right && localY in r.top..r.bottom
                                }
                                if (hit != null) {
                                    toggleHighlightAt(page, hit, debouncedSearchQuery)
                                    break
                                }
                                val existingHighlight = highlights.firstOrNull { saved ->
                                    if (saved.page != page) return@firstOrNull false
                                    val r = parseHighlightRect(saved.rectsText) ?: return@firstOrNull false
                                    localX in r.left..r.right && localY in r.top..r.bottom
                                }
                                if (existingHighlight != null) {
                                    db.deleteHighlight(existingHighlight.id)
                                    highlights.remove(existingHighlight)
                                    break
                                }
                            }
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
                // Runs alongside detectTransformGestures above (purely observational - never
                // consumes) purely to track single-finger drag velocity and fling on release.
                // detectTransformGestures itself only tracks the finger 1:1 and stops dead the
                // instant it lifts, which is what made touch scrolling feel unlike every native
                // Android scroll surface (no momentum/deceleration).
                .pointerInput(viewSize, pageCount, state.zoom) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // A fresh touch always takes over from any fling still coasting from
                        // the previous gesture, so the new drag isn't fighting a decaying one.
                        coroutineScope.launch { scrollAnimatable.stop() }

                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPointerInputChange(down)
                        var isMultiTouch = false

                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size > 1) isMultiTouch = true
                            pressed.firstOrNull()?.let { velocityTracker.addPointerInputChange(it) }
                        } while (event.changes.any { it.pressed })

                        // Pinch-zoom gestures already end wherever the fingers left off; only a
                        // single-finger pan should coast on.
                        if (!isMultiTouch) {
                            val velocity = velocityTracker.calculateVelocity()
                            if (kotlin.math.hypot(velocity.x, velocity.y) > 200f) {
                                val decay = splineBasedDecay<Offset>(density)
                                coroutineScope.launch {
                                    scrollAnimatable.snapTo(state.scrollOffset)
                                    scrollAnimatable.animateDecay(Offset(velocity.x, velocity.y), decay) {
                                        state.scrollOffset = clampScrollOffset(
                                            value, state.zoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
                                            viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
                                        )
                                    }
                                }
                            }
                        }
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
                                        val scrollSpeed = 70f
                                        val pan = Offset(-delta.x * scrollSpeed, -delta.y * scrollSpeed)
                                        // Accumulate onto the real target, not onto scrollAnimatable's current
                                        // mid-flight value - see scrollTarget's declaration for why.
                                        val base = if (scrollAnimatable.isRunning) scrollTarget else state.scrollOffset
                                        val target = clampScrollOffset(
                                            base + pan, state.zoom, viewSize.width.toFloat(), viewSize.height.toFloat(),
                                            viewSize.width.toFloat() * 1.4f, pageCount, spacingPx, topPaddingPx, bottomPaddingPx, marginPx
                                        )
                                        scrollTarget = target
                                        coroutineScope.launch {
                                            if (!scrollAnimatable.isRunning) scrollAnimatable.snapTo(state.scrollOffset)
                                            scrollAnimatable.animateTo(
                                                targetValue = target,
                                                animationSpec = tween(durationMillis = 90, easing = LinearOutSlowInEasing)
                                            ) {
                                                state.scrollOffset = value
                                            }
                                        }
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
                    // Grouped once per draw pass instead of filtering activeTiles per visible page
                    // (was O(visiblePages x activeTiles.size) per frame during pan/zoom).
                    val tilesByPage: Map<Int, List<Map.Entry<TileKey, ImageBitmap>>> =
                        if (state.zoom > 1.5f) activeTiles.entries.groupBy { it.key.page } else emptyMap()

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
                        } else {
                            translate(left = 0f, top = pageTop) {
                                if (renderFailed[page] == true) {
                                    // Failed-render state: distinct warning tint + a small "!" glyph. Tap to retry.
                                    val warningColor = if (localNightMode) LuminaColors.statusDangerDark else LuminaColors.statusDangerLight
                                    drawRect(
                                        color = warningColor.copy(alpha = 0.14f),
                                        size = androidx.compose.ui.geometry.Size(size.width, estimatedPageHeight)
                                    )
                                    val cx = size.width / 2f
                                    val cy = estimatedPageHeight / 2f
                                    val glyphHalf = 14.dp.toPx()
                                    drawLine(
                                        color = warningColor,
                                        start = Offset(cx, cy - glyphHalf),
                                        end = Offset(cx, cy + glyphHalf * 0.15f),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = warningColor,
                                        radius = 1.8.dp.toPx(),
                                        center = Offset(cx, cy + glyphHalf * 0.55f)
                                    )
                                } else {
                                    // Loading placeholder: subtle pulsing tint while the page renders.
                                    val pulseTint = if (localNightMode) Color.White else Color.Black
                                    drawRect(
                                        color = pulseTint.copy(alpha = loadingPulseAlpha * 0.08f),
                                        size = androidx.compose.ui.geometry.Size(size.width, estimatedPageHeight)
                                    )
                                }
                            }
                        }

                        // Draw high-res tiles
                        if (state.zoom > 1.5f) {
                            translate(left = 0f, top = pageTop) {
                                scale(scale = 1f / state.zoom, pivot = Offset.Zero) {
                                    val pageTiles = tilesByPage[page] ?: emptyList()
                                    pageTiles.forEach { (key, tileBitmap) ->
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

                        // Draw saved highlights (persisted, drawn under any live search match)
                        for (saved in highlights) {
                            if (saved.page != page) continue
                            val rect = parseHighlightRect(saved.rectsText) ?: continue
                            val paddingX = 3.dp.toPx()
                            val paddingY = 2.dp.toPx()
                            drawRoundRect(
                                color = savedHighlightColor.copy(alpha = 0.45f),
                                topLeft = Offset(
                                    rect.left * size.width - paddingX,
                                    pageTop + rect.top * estimatedPageHeight - paddingY
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    (rect.right - rect.left) * size.width + 2 * paddingX,
                                    (rect.bottom - rect.top) * estimatedPageHeight + 2 * paddingY
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
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

        // 2. Top Bar Overlay - hides scrolling down into the content, comes back scrolling back
        // up (see isTopBarVisible above). Forced visible while search/outline/notes are open
        // regardless of scroll direction, since those overlays' own controls live in this bar
        // (e.g. centering a search match scrolls the page, which would otherwise hide the search
        // box the user is actively typing in).
        AnimatedVisibility(
            visible = isTopBarVisible || isSearchActive || isOutlineOpen || isNotesOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
        TopBar(
            title = documentTitle,
            // Desktop-width toolbar per the Figma revamp: zoom/fit/night-mode promoted to
            // always-visible icons instead of hiding behind the overflow menu. Same 700dp
            // threshold HomeScreen uses for its own compact/wide split.
            wide = with(density) { viewSize.width.toDp() } >= 700.dp,
            zoomPercent = (state.zoom * 100).toInt(),
            onZoomIn = {
                changeZoomWithFocusAnimated(state.zoom + 0.2f, Offset(viewSize.width / 2f, viewSize.height / 2f))
            },
            onZoomOut = {
                changeZoomWithFocusAnimated(state.zoom - 0.2f, Offset(viewSize.width / 2f, viewSize.height / 2f))
            },
            onFitPage = {
                val fitZoom = getFitZoom()
                val estimatedPageHeight = viewSize.width.toFloat() * 1.4f
                val pageTop = state.currentPage * (estimatedPageHeight + spacingPx)
                val targetScrollY = topPaddingPx - pageTop * fitZoom
                val targetScrollX = (viewSize.width - viewSize.width * fitZoom) / 2f
                val targetScroll = clampScrollOffset(
                    Offset(targetScrollX, targetScrollY),
                    fitZoom,
                    viewSize.width.toFloat(),
                    viewSize.height.toFloat(),
                    estimatedPageHeight,
                    pageCount,
                    spacingPx,
                    topPaddingPx,
                    bottomPaddingPx,
                    marginPx
                )
                val startZoom = state.zoom
                val startScroll = state.scrollOffset
                coroutineScope.launch {
                    zoomAnimatable.snapTo(startZoom)
                    zoomAnimatable.animateTo(fitZoom, tween(durationMillis = 200)) {
                        val t = if (fitZoom != startZoom) (value - startZoom) / (fitZoom - startZoom) else 1f
                        state.zoom = value
                        state.scrollOffset = Offset(
                            startScroll.x + (targetScroll.x - startScroll.x) * t,
                            startScroll.y + (targetScroll.y - startScroll.y) * t
                        )
                    }
                }
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
            onOutlineActiveChange = { 
                isOutlineOpen = it 
                if (it) isNotesOpen = false
            },
            isNotesActive = isNotesOpen,
            onNotesActiveChange = { 
                isNotesOpen = it 
                if (it) isOutlineOpen = false
            },
            currentPage = state.currentPage,
            pageCount = pageCount,
            onJumpToPage = { jumpToPage(it) },
            isCurrentPageBookmarked = bookmarks.any { it.page == state.currentPage },
            onToggleBookmark = { toggleBookmark(state.currentPage) },
            onExport = onExport,
            onClose = onClose
        )
        }

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
                val currentRatioState = rememberUpdatedState(currentRatio)
                val maxTravelState = rememberUpdatedState(maxTravel)
                val scrollRangeState = rememberUpdatedState(scrollRange)
                val topPaddingState = rememberUpdatedState(topPaddingPx)

                // Auto-hides like the tooltip below - visible while actively scrolling/dragging,
                // fades out shortly after (isScrolling flips back to false 1.5s after the last
                // scrollOffset change, same timer the tooltip already used).
                AnimatedVisibility(
                    visible = isScrolling || isDraggingScrollbar,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topBarHeight, bottom = 16.dp, end = 4.dp)
                        .width(20.dp)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { isDraggingScrollbar = true },
                                    onDragEnd = { isDraggingScrollbar = false },
                                    onDragCancel = { isDraggingScrollbar = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val currentRatioVal = currentRatioState.value
                                        val maxTravelVal = maxTravelState.value
                                        val scrollRangeVal = scrollRangeState.value
                                        val topPaddingVal = topPaddingState.value
                                        val newRatio = ((currentRatioVal * maxTravelVal + dragAmount.y) / maxTravelVal).coerceIn(0f, 1f)
                                        val newScrollOffsetY = topPaddingVal - newRatio * scrollRangeVal
                                        state.scrollOffset = state.scrollOffset.copy(y = newScrollOffsetY)
                                    }
                                )
                            }
                    ) {
                        // Contrast against the reading pane's own background (which follows
                        // localNightMode, not the app chrome's MaterialTheme) — using onSurface here
                        // instead could go near-invisible whenever the two themes disagree (e.g. app
                        // in dark system theme with night-mode reading toggled off to a cream page).
                        val scrollbarInkColor = if (localNightMode) LuminaColors.inkPrimaryDark else LuminaColors.inkPrimaryLight

                        // Track background
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(
                                    color = scrollbarInkColor.copy(alpha = 0.18f),
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
                                    color = scrollbarInkColor.copy(alpha = if (isDraggingScrollbar) 0.75f else 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
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
                .width(320.dp)
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
                            modifier = Modifier.size(24.dp).semantics { contentDescription = "Close outline" }
                        ) {
                            CloseIcon(
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (bookmarks.isNotEmpty()) {
                        Text(
                            text = "Bookmarks",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            bookmarks.sortedBy { it.page }.forEach { bookmark ->
                                Surface(
                                    onClick = {
                                        jumpToPage(bookmark.page)
                                        isOutlineOpen = false
                                    },
                                    shape = RoundedCornerShape(LuminaRadius.pill),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.semantics {
                                        contentDescription = "Jump to bookmarked page ${bookmark.page + 1}"
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${bookmark.page + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                db.deleteBookmark(bookmark.id)
                                                bookmarks.remove(bookmark)
                                            },
                                            modifier = Modifier.size(16.dp).semantics {
                                                contentDescription = "Remove bookmark for page ${bookmark.page + 1}"
                                            }
                                        ) {
                                            CloseIcon(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
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
                                                Modifier
                                                    .semantics(mergeDescendants = true) {}
                                                    .clickable {
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
                                        color = if (item.pageNumber >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.pageNumber >= 0) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = "${item.pageNumber + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Slide-out Notes Panel (Right side)
        AnimatedVisibility(
            visible = isNotesOpen,
            enter = androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { it }
            ),
            exit = androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { it }
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topBarHeight)
                .width(280.dp)
                .fillMaxHeight()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                var newNoteText by remember { mutableStateOf("") }
                var showCurrentPageOnly by remember { mutableStateOf(true) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "Notes & Annotations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { isNotesOpen = false },
                            modifier = Modifier.size(24.dp).semantics { contentDescription = "Close notes" }
                        ) {
                            CloseIcon(
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Page filter selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showCurrentPageOnly = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (showCurrentPageOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Page ${state.currentPage + 1}", fontWeight = if (showCurrentPageOnly) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(
                            onClick = { showCurrentPageOnly = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (!showCurrentPageOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("All Pages", fontWeight = if (!showCurrentPageOnly) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Add note textfield & button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newNoteText,
                            onValueChange = { newNoteText = it },
                            placeholder = { Text("Write a note...", style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            maxLines = 3
                        )
                        Button(
                            onClick = {
                                if (newNoteText.isNotBlank() && documentId != null) {
                                    val note = DbNote(
                                        id = (0..10000000).random().toString(),
                                        documentId = documentId,
                                        page = state.currentPage,
                                        text = newNoteText.trim(),
                                        timestamp = getCurrentTimeMillis()
                                    )
                                    db.insertNote(note)
                                    notes.add(0, note)
                                    newNoteText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newNoteText.isNotBlank()
                        ) {
                            Text("Add Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Notes list
                    val filteredNotes = remember(notes, showCurrentPageOnly, state.currentPage) {
                        if (showCurrentPageOnly) {
                            notes.filter { it.page == state.currentPage }
                        } else {
                            notes.sortedBy { it.page }
                        }
                    }

                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showCurrentPageOnly) "No notes on this page" else "No notes in document",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(filteredNotes.size) { index ->
                                val note = filteredNotes[index]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Page ${note.page + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .clickable { jumpToPage(note.page) }
                                                    .padding(vertical = 2.dp)
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    db.deleteNote(note.id)
                                                    notes.remove(note)
                                                },
                                                modifier = Modifier.size(20.dp).semantics { contentDescription = "Delete note" }
                                            ) {
                                                CloseIcon(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
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

