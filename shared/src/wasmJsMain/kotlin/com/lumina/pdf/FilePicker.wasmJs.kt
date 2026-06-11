package com.lumina.pdf

import androidx.compose.runtime.Composable

actual class FilePicker actual constructor() {
    @Composable
    actual fun register(onFilePicked: (ByteArray, String) -> Unit): () -> Unit {
        return {
            // No-op
        }
    }
}
