package com.lumina.pdf

import androidx.compose.ui.draganddrop.DragAndDropEvent

// Reading a dropped file's bytes on the web requires bridging the browser's async File API
// (DataTransfer -> File.arrayBuffer()) through Compose's DragAndDropEvent.nativeEvent, which
// needs verification against a real browser session this environment can't provide. Left as an
// explicit follow-up rather than shipping unverified interop code — the drop target is still
// wired up (see App.kt), it just doesn't extract bytes here yet.
actual suspend fun extractDroppedPdfBytes(event: DragAndDropEvent): Pair<String, ByteArray>? = null
