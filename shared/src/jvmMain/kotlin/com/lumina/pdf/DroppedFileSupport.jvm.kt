package com.lumina.pdf

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import co.touchlab.kermit.Logger
import java.io.File
import java.net.URI

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun extractDroppedPdfBytes(event: DragAndDropEvent): Pair<String, ByteArray>? {
    val data = event.dragData()
    if (data !is DragData.FilesList) return null
    val path = data.readFiles().firstOrNull { it.endsWith(".pdf", ignoreCase = true) } ?: return null
    return try {
        val file = File(URI(path))
        file.name to file.readBytes()
    } catch (e: Exception) {
        Logger.e(e) { "Failed to read dropped file: $path" }
        null
    }
}
