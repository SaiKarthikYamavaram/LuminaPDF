package com.lumina.pdf

import androidx.compose.ui.draganddrop.DragAndDropEvent

/**
 * Reads a dropped PDF's name and bytes from a platform drag-and-drop event, or null if the
 * drop wasn't a PDF file (or this platform doesn't support reading dropped file contents).
 * Suspending because reading the dropped file's bytes is inherently async on some platforms.
 */
expect suspend fun extractDroppedPdfBytes(event: DragAndDropEvent): Pair<String, ByteArray>?
