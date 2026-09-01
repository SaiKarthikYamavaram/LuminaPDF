package com.lumina.pdf

import androidx.compose.ui.draganddrop.DragAndDropEvent

// Drag-and-drop file opening is a desktop/web affordance (OS file manager / browser onto the
// window) — iOS has no equivalent drop target, so there's nothing to extract here.
actual suspend fun extractDroppedPdfBytes(event: DragAndDropEvent): Pair<String, ByteArray>? = null
