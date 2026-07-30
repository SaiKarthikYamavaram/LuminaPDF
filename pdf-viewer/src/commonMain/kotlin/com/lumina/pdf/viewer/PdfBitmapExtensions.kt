package com.lumina.pdf.viewer

import androidx.compose.ui.graphics.ImageBitmap
import com.lumina.pdf.core.PdfBitmap
import com.lumina.pdf.core.DiskCache

expect fun PdfBitmap.asImageBitmap(): ImageBitmap

expect fun PdfBitmap.toByteArray(): ByteArray?

expect fun decodeByteArrayToPdfBitmap(bytes: ByteArray): PdfBitmap?

@androidx.compose.runtime.Composable
expect fun rememberDiskCache(): DiskCache
