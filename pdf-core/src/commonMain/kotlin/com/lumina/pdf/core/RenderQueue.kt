package com.lumina.pdf.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class RenderPriority {
    VISIBLE_TILE,
    VISIBLE_PAGE,
    PREFETCH_TILE,
    PREFETCH_PAGE,
    THUMBNAIL
}

sealed class RenderRequest(val priority: RenderPriority) {
    data class Page(val pageIndex: Int, val width: Int, val height: Int, val onRendered: (PdfBitmap?) -> Unit) : RenderRequest(RenderPriority.VISIBLE_PAGE)
    data class Tile(val pageIndex: Int, val region: PdfRect, val scale: Float, val onRendered: (PdfBitmap?) -> Unit) : RenderRequest(RenderPriority.VISIBLE_TILE)
    data class Thumbnail(val pageIndex: Int, val width: Int, val height: Int, val onRendered: (PdfBitmap?) -> Unit) : RenderRequest(RenderPriority.THUMBNAIL)
}

class RenderQueue(
    private val document: PdfDocument,
    private val coroutineScope: CoroutineScope
) {
    // A channel for processing requests one by one to avoid OOM
    private val requestChannel = Channel<RenderRequest>(Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            // For MVP, process sequentially. 
            // In Phase 3 we might want to group or sort them by priority before processing.
            requestChannel.receiveAsFlow().collect { request ->
                try {
                    when (request) {
                        is RenderRequest.Page -> {
                            val bitmap = document.renderPage(request.pageIndex, request.width, request.height)
                            request.onRendered(bitmap)
                        }
                        is RenderRequest.Tile -> {
                            val bitmap = document.renderTile(request.pageIndex, request.region, request.scale)
                            request.onRendered(bitmap)
                        }
                        is RenderRequest.Thumbnail -> {
                            val bitmap = document.renderPage(request.pageIndex, request.width, request.height)
                            request.onRendered(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    when (request) {
                        is RenderRequest.Page -> request.onRendered(null)
                        is RenderRequest.Tile -> request.onRendered(null)
                        is RenderRequest.Thumbnail -> request.onRendered(null)
                    }
                }
            }
        }
    }

    fun submit(request: RenderRequest) {
        requestChannel.trySend(request)
    }
}
