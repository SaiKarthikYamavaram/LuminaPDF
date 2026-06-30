package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import com.lumina.pdf.core.PdfBitmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lumina.pdf.core.DiskCache

actual fun PdfBitmap.asImageBitmap(): ImageBitmap {
    throw NotImplementedError("WasmJS rendering not yet implemented")
}

actual fun PdfBitmap.toByteArray(): ByteArray? = null

actual fun decodeByteArrayToPdfBitmap(bytes: ByteArray): PdfBitmap? = null

class WasmJsNoOpDiskCache : DiskCache {
    override suspend fun get(key: String): ByteArray? = null
    override suspend fun put(key: String, data: ByteArray) {}
    override suspend fun remove(key: String) {}
    override suspend fun clear() {}
}

@Composable
actual fun rememberDiskCache(): DiskCache {
    return remember { WasmJsNoOpDiskCache() }
}
