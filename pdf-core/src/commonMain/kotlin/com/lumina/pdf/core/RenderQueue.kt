package com.lumina.pdf.core

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    data class Page(val pageIndex: Int, val width: Int, val height: Int, val onRendered: suspend (PdfBitmap?) -> Unit) : RenderRequest(RenderPriority.VISIBLE_PAGE)
    data class Tile(val pageIndex: Int, val region: PdfRect, val scale: Float, val displayWidth: Int = 0, val onRendered: suspend (PdfBitmap?) -> Unit) : RenderRequest(RenderPriority.VISIBLE_TILE)
    data class Thumbnail(
        val pageIndex: Int, 
        val width: Int, 
        val height: Int, 
        val isPrefetch: Boolean = false, 
        val onRendered: suspend (PdfBitmap?) -> Unit
    ) : RenderRequest(if (isPrefetch) RenderPriority.PREFETCH_PAGE else RenderPriority.VISIBLE_PAGE)
}

class RenderQueue(
    private val document: PdfDocument,
    private val coroutineScope: CoroutineScope,
    private val numWorkers: Int = 3
) {
    private val pendingRequests = mutableListOf<RenderRequest>()
    private val mutex = Mutex()
    private val wakeUpChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        repeat(numWorkers) {
            coroutineScope.launch(Dispatchers.Default) {
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
                                    val bitmap = document.renderTile(request.pageIndex, request.region, request.scale, request.displayWidth)
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
                        }
                    }
                }
            }
        }
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

                // Prevent duplicate requests to save rendering overhead
                val isDuplicate = when (request) {
                    is RenderRequest.Page -> pendingRequests.any { it is RenderRequest.Page && it.pageIndex == request.pageIndex }
                    is RenderRequest.Tile -> pendingRequests.any { it is RenderRequest.Tile && it.pageIndex == request.pageIndex && it.region == request.region }
                    is RenderRequest.Thumbnail -> pendingRequests.any { it is RenderRequest.Thumbnail && it.pageIndex == request.pageIndex }
                }
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
