package com.lumina.pdf

import androidx.compose.ui.draganddrop.DragAndDropEvent

// See DroppedFileSupport.js.kt — same reasoning, doubly true on wasmJs where the browser
// interop surface for the File API differs again and is unverified in this environment.
actual suspend fun extractDroppedPdfBytes(event: DragAndDropEvent): Pair<String, ByteArray>? = null
