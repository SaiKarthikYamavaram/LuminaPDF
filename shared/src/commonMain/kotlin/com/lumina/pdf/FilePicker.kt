package com.lumina.pdf

import androidx.compose.runtime.Composable

expect class FilePicker() {
    @Composable
    fun register(onFilePicked: (ByteArray, String) -> Unit): () -> Unit
}
