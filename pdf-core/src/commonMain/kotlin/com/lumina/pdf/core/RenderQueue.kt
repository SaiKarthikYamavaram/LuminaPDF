package com.lumina.pdf.core

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class RenderPriority {
    VISIBLE_TILE,
    VISIBLE_PAGE,
    PREFETCH_TILE,
    PREFETCH_PAGE,
    THUMBNAIL
}

sealed class RenderRequest(val priority: RenderPriority) {
    data class Page(val pageIndex: Int, val width: Int, val height: Int, val onRendered: (PdfBitmap?) -> Unit) : RenderRequest(RenderPriority.VISIBLE_PAGE)
    data class Tile(
        val pageIndex: Int,
        val region: PdfRect,
        val scale: Float,
        val fullPageWidthPx: Int,
        val fullPageHeightPx: Int,
        val onRendered: (PdfBitmap?) -> Unit
    ) : RenderRequest(RenderPriority.VISIBLE_TILE)
    data class Thumbnail(
        val pageIndex: Int, 
        val width: Int, 
        val height: Int, 
        val isPrefetch: Boolean = false, 
        val onRendered: (PdfBitmap?) -> Unit
    ) : RenderRequest(if (isPrefetch) RenderPriority.PREFETCH_PAGE else RenderPriority.VISIBLE_PAGE)
}

class RenderQueue(
    private val document: PdfDocument,
    private val coroutineScope: CoroutineScope
) {
    private val pendingRequests = mutableListOf<RenderRequest>()
    // The request currently being rendered (dequeued but not yet completed). submit() must check
    // this in addition to pendingRequests: without it, a slow render (e.g. a full-resolution tile)
    // could still be in flight when the same tile is requested again, producing two renders for
    // the same key. Both eventually call cache.put()/activeTiles[key]=... for that key, and the
    // second one closes the first bitmap - which may still be mid-draw on screen - crashing with
    // "Canvas: trying to use a recycled bitmap".
    private var inFlightRequest: RenderRequest? = null
    private val mutex = Mutex()
    private val wakeUpChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        coroutineScope.launch {
            while (true) {
                // Wait for a wake-up signal if there are no requests
                if (pendingRequests.isEmpty()) {
                    wakeUpChannel.receive()
                }

                val request = mutex.withLock {
                    if (pendingRequests.isEmpty()) null else {
                        // Find request with highest priority (lowest ordinal value)
                        val bestRequest = pendingRequests.minByOrNull { it.priority.ordinal }
                        if (bestRequest != null) {
                            pendingRequests.remove(bestRequest)
                            inFlightRequest = bestRequest
                        }
                        bestRequest
                    }
                }

                if (request != null) {
                    try {
                        when (request) {
                            is RenderRequest.Page -> {
                                val bitmap = document.renderPage(request.pageIndex, request.width, request.height)
                                request.onRendered(bitmap)
                            }
                            is RenderRequest.Tile -> {
                                val bitmap = document.renderTile(
                                    request.pageIndex, request.region, request.scale,
                                    request.fullPageWidthPx, request.fullPageHeightPx
                                )
                                request.onRendered(bitmap)
                            }
                            is RenderRequest.Thumbnail -> {
                                val bitmap = document.renderPage(request.pageIndex, request.width, request.height)
                                request.onRendered(bitmap)
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e(e) { "Failed to render request: $request" }
                        when (request) {
                            is RenderRequest.Page -> request.onRendered(null)
                            is RenderRequest.Tile -> request.onRendered(null)
                            is RenderRequest.Thumbnail -> request.onRendered(null)
                        }
                    } finally {
                        mutex.withLock {
                            if (inFlightRequest === request) inFlightRequest = null
                        }
                    }
                }
            }
        }
    }

    private fun isSameTarget(a: RenderRequest, b: RenderRequest): Boolean = when (a) {
        is RenderRequest.Page -> b is RenderRequest.Page && b.pageIndex == a.pageIndex
        is RenderRequest.Tile -> b is RenderRequest.Tile && b.pageIndex == a.pageIndex && b.region == a.region
        is RenderRequest.Thumbnail -> b is RenderRequest.Thumbnail && b.pageIndex == a.pageIndex
    }

    fun submit(request: RenderRequest) {
        coroutineScope.launch {
            mutex.withLock {
                // If it's a visible thumbnail request, check if we can upgrade an existing prefetch request
                if (request is RenderRequest.Thumbnail && !request.isPrefetch) {
                    val existingIndex = pendingRequests.indexOfFirst {
                        it is RenderRequest.Thumbnail && it.pageIndex == request.pageIndex
                    }
                    if (existingIndex != -1) {
                        val existing = pendingRequests[existingIndex] as RenderRequest.Thumbnail
                        if (existing.isPrefetch) {
                            pendingRequests[existingIndex] = request
                        }
                        return@withLock
                    }
                }

                // Prevent duplicate requests to save rendering overhead - and, critically, to avoid
                // two renders racing to close/replace the same cache entry (see inFlightRequest doc).
                val isDuplicate = inFlightRequest?.let { isSameTarget(request, it) } == true ||
                    pendingRequests.any { isSameTarget(request, it) }
                if (!isDuplicate) {
                    pendingRequests.add(request)
                    wakeUpChannel.trySend(Unit)
                }
            }
        }
    }

    fun clearObsolete(activePages: Set<Int>, visiblePages: Set<Int>) {
        coroutineScope.launch {
            mutex.withLock {
                pendingRequests.removeAll { request ->
                    when (request) {
                        is RenderRequest.Page -> request.pageIndex !in activePages
                        is RenderRequest.Thumbnail -> request.pageIndex !in activePages
                        is RenderRequest.Tile -> request.pageIndex !in visiblePages
                    }
                }
            }
        }
    }
}
